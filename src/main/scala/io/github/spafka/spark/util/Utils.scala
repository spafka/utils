/**
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *
  **/

package io.github.spafka.spark.util

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

/** CallSite represents a place in user code. It can have a short and a long form. */
case class CallSite(shortForm: String, longForm: String)

object CallSite {
  val SHORT_FORM = "callSite.short"
  val LONG_FORM = "callSite.long"
  val empty = CallSite("", "")
}


import java.io._
import java.lang.management.ManagementFactory
import java.math.{MathContext, RoundingMode}
import java.net._
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.util.concurrent.TimeUnit.NANOSECONDS
import java.util.concurrent.atomic.AtomicBoolean
import java.util.{Locale, Random, UUID}

import io.github.spafka.spark.internal.Logging

import scala.collection.JavaConverters._
import scala.collection.Map
import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.util.Try
import scala.util.control.{ControlThrowable, NonFatal}

/**
  * Various utility methods used by Spark.
  */
object Utils extends Logging {

  import java.util

  import org.apache.commons.lang3.SystemUtils

  val random = new Random()


  @volatile var cachedLocalDir: String = ""


  val MAX_DIR_CREATION_ATTEMPTS: Int = 10
  @volatile var localRootDirs: Array[String] = null

  /**
    * The performance overhead of creating and logging strings for wide schemas can be large. To
    * limit the impact, we bound the number of fields to include by default. This can be overridden
    * by setting the 'spark.debug.maxToStringFields' conf in SparkEnv.
    */
  val DEFAULT_MAX_TO_STRING_FIELDS = 25


  /** Whether we have warned about plan string truncation yet. */
  val truncationWarningPrinted = new AtomicBoolean(false)

  /** Serialize an object using Java serialization */
  def serialize[T](o: T): Array[Byte] = {
    val bos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(bos)
    oos.writeObject(o)
    oos.close()
    bos.toByteArray
  }

  /** Deserialize an object using Java serialization */
  def deserialize[T](bytes: Array[Byte]): T = {
    val bis = new ByteArrayInputStream(bytes)
    val ois = new ObjectInputStream(bis)
    ois.readObject.asInstanceOf[T]
  }

  /** Deserialize an object using Java serialization and the given ClassLoader */
  def deserialize[T](bytes: Array[Byte], loader: ClassLoader): T = {
    val bis = new ByteArrayInputStream(bytes)
    val ois = new ObjectInputStream(bis) {
      override def resolveClass(desc: ObjectStreamClass): Class[_] = {
        // scalastyle:off classforname
        Class.forName(desc.getName, false, loader) // scalastyle:on classforname
      }
    }
    ois.readObject.asInstanceOf[T]
  }

  /**
    * Run a segment of code using a different context class loader in the current thread
    */
  def withContextClassLoader[T](ctxClassLoader: ClassLoader)(fn: => T): T = {
    val oldClassLoader = Thread.currentThread().getContextClassLoader()
    try {
      Thread.currentThread().setContextClassLoader(ctxClassLoader)
      fn
    } finally {
      Thread.currentThread().setContextClassLoader(oldClassLoader)
    }
  }

  /**
    * Primitive often used when writing [[java.nio.ByteBuffer]] to [[java.io.DataOutput]]
    */
  def writeByteBuffer(bb: ByteBuffer, out: DataOutput): Unit = {
    if (bb.hasArray) {
      out.write(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining())
    } else {
      val originalPosition = bb.position()
      val bbval = new Array[Byte](bb.remaining())
      bb.get(bbval)
      out.write(bbval)
      bb.position(originalPosition)
    }
  }

  /**
    * Primitive often used when writing [[java.nio.ByteBuffer]] to [[java.io.OutputStream]]
    */
  def writeByteBuffer(bb: ByteBuffer, out: OutputStream): Unit = {
    if (bb.hasArray) {
      out.write(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining())
    } else {
      val originalPosition = bb.position()
      val bbval = new Array[Byte](bb.remaining())
      bb.get(bbval)
      out.write(bbval)
      bb.position(originalPosition)
    }
  }

  /**
    * JDK equivalent of `chmod 700 file`.
    *
    * @param file the file whose permissions will be modified
    * @return true if the permissions were successfully changed, false otherwise.
    */
  def chmod700(file: File): Boolean = {
    file.setReadable(false, false) && file.setReadable(true, true) && file.setWritable(false, false) && file.setWritable(true, true) && file.setExecutable(false, false) && file.setExecutable(true, true)
  }

  /**
    * Create a directory inside the given parent directory. The directory is guaranteed to be
    * newly created, and is not marked for automatic deletion.
    */
  def createDirectory(root: String, namePrefix: String = "spark"): File = {
    var attempts = 0
    val maxAttempts = MAX_DIR_CREATION_ATTEMPTS
    var dir: File = null
    while (dir == null) {
      attempts += 1
      if (attempts > maxAttempts) {
        throw new IOException("Failed to create a temp directory (under " + root + ") after " + maxAttempts + " attempts!")
      }
      try {
        dir = new File(root, namePrefix + "-" + UUID.randomUUID.toString)
        if (dir.exists() || !dir.mkdirs()) {
          dir = null
        }
      } catch {
        case e: SecurityException => dir = null;
      }
    }

    dir.getCanonicalFile
  }

  /**
    * Create a temporary directory inside the given parent directory. The directory will be
    * automatically deleted when the VM shuts down.
    */
  def createTempDir(root: String = System.getProperty("java.io.tmpdir"), namePrefix: String = "spark"): File = {
    val dir = createDirectory(root, namePrefix)
    dir
  }

  /**
    * Copy all data from an InputStream to an OutputStream. NIO way of file stream to file stream
    * copying is disabled by default unless explicitly set transferToEnabled as true,
    * the parameter transferToEnabled should be configured by spark.file.transferTo = [true|false].
    */
  def copyStream(in: InputStream, out: OutputStream, closeStreams: Boolean = false, transferToEnabled: Boolean = false): Long = {
    tryWithSafeFinally {
      if (in.isInstanceOf[FileInputStream] && out.isInstanceOf[FileOutputStream] && transferToEnabled) {
        // When both streams are File stream, use transferTo to improve copy performance.
        val inChannel = in.asInstanceOf[FileInputStream].getChannel()
        val outChannel = out.asInstanceOf[FileOutputStream].getChannel()
        val size = inChannel.size()
        copyFileStreamNIO(inChannel, outChannel, 0, size)
        size
      } else {
        var count = 0L
        val buf = new Array[Byte](8192)
        var n = 0
        while (n != -1) {
          n = in.read(buf)
          if (n != -1) {
            out.write(buf, 0, n)
            count += n
          }
        }
        count
      }
    } {
      if (closeStreams) {
        try {
          in.close()
        } finally {
          out.close()
        }
      }
    }
  }

  def copyFileStreamNIO(input: FileChannel, output: FileChannel, startPosition: Long, bytesToCopy: Long): Unit = {
    val initialPos = output.position()
    var count = 0L // In case transferTo method transferred less data than we have required.
    while (count < bytesToCopy) {
      count += input.transferTo(count + startPosition, bytesToCopy - count, output)
    }
    assert(count == bytesToCopy, s"request to copy $bytesToCopy bytes, but actually copied $count bytes.")

    // Check the position after transferTo loop to see if it is in the right position and
    // give user information if not.
    // Position will not be increased to the expected length after calling transferTo in
    // kernel version 2.6.32, this issue can be seen in
    // https://bugs.openjdk.java.net/browse/JDK-7052359
    // This will lead to stream corruption issue when using sort-based shuffle (SPARK-3948).
    val finalPos = output.position()
    val expectedPos = initialPos + bytesToCopy
    assert(finalPos == expectedPos,
      s"""
         |Current position $finalPos do not equal to expected position $expectedPos
         |after transferTo, please check your kernel version to see if it is 2.6.32,
         |this is a kernel bug which will lead to unexpected behavior when using transferTo.
         |You can set spark.file.transferTo = false to disable this NIO feature.
           """.stripMargin)
  }


  /**
    * A file name may contain some invalid URI characters, such as " ". This method will convert the
    * file name to a raw path accepted by `java.net.URI(String)`.
    *
    * Note: the file name must not contain "/" or "\"
    */
  def encodeFileNameToURIRawPath(fileName: String): String = {
    require(!fileName.contains("/") && !fileName.contains("\\")) // `file` and `localhost` are not used. Just to prevent URI from parsing `fileName` as
    // scheme or host. The prefix "/" is required because URI doesn't accept a relative path.
    // We should remove it after we get the raw path.
    new URI("file", null, "localhost", -1, "/" + fileName, null, null).getRawPath.substring(1)
  }

  /**
    * Get the file name from uri's raw path and decode it. If the raw path of uri ends with "/",
    * return the name before the last "/".
    */
  def decodeFileNameInURI(uri: URI): String = {
    val rawPath = uri.getRawPath
    val rawFileName = rawPath.split("/").last
    new URI("file:///" + rawFileName).getPath.substring(1)
  }


  /** Records the duration of running `body`. */
  def timeTakenMs[T](body: => T): (T, Long) = {
    val startTime = System.nanoTime()
    val result = body
    val endTime = System.nanoTime()
    (result, math.max(NANOSECONDS.toMillis(endTime - startTime), 0))
  }

  /** Records the duration of running `body`. */
  def logTimeTakenMs[T](body: => T): T = {
    val startTime = System.nanoTime()
    val result = body
    val endTime = System.nanoTime()
    import sun.reflect.Reflection
    logInfo(s"${Reflection.getCallerClass(2)} ${math.max(NANOSECONDS.toMillis(endTime - startTime), 0)} ms")

    result
  }


  /**
    * Get the local host's IP address in dotted-quad format (e.g. 1.2.3.4).
    * Note, this is typically not used from within core spark.
    */
  lazy val localIpAddress: InetAddress = findLocalInetAddress()

  def findLocalInetAddress(): InetAddress = {
    val defaultIpOverride = System.getenv("SPARK_LOCAL_IP")
    if (defaultIpOverride != null) {
      InetAddress.getByName(defaultIpOverride)
    } else {
      val address = InetAddress.getLocalHost
      if (address.isLoopbackAddress) {
        // Address resolves to something like 127.0.1.1, which happens on Debian; try to find
        // a better address using the local network interfaces
        // getNetworkInterfaces returns ifs in reverse order compared to ifconfig output order
        // on unix-like system. On windows, it returns in index order.
        // It's more proper to pick ip address following system output order.
        val activeNetworkIFs = NetworkInterface.getNetworkInterfaces.asScala.toSeq
        val reOrderedNetworkIFs = if (isWindows) activeNetworkIFs else activeNetworkIFs.reverse

        for (ni <- reOrderedNetworkIFs) {
          val addresses = ni.getInetAddresses.asScala.filterNot(addr => addr.isLinkLocalAddress || addr.isLoopbackAddress).toSeq
          if (addresses.nonEmpty) {
            val addr = addresses.find(_.isInstanceOf[Inet4Address]).getOrElse(addresses.head)
            // because of Inet6Address.toHostName may add interface at the end if it knows about it
            val strippedAddress = InetAddress.getByAddress(addr.getAddress) // We've found an address that looks reasonable!
            logWarning("Your hostname, " + InetAddress.getLocalHost.getHostName + " resolves to" + " a loopback address: " + address.getHostAddress + "; using " + strippedAddress.getHostAddress + " instead (on interface " + ni.getName + ")")
            logWarning("Set SPARK_LOCAL_IP if you need to bind to another address")
            return strippedAddress
          }
        }
        logWarning("Your hostname, " + InetAddress.getLocalHost.getHostName + " resolves to" + " a loopback address: " + address.getHostAddress + ", but we couldn't find any" + " external IP address!")
        logWarning("Set SPARK_LOCAL_IP if you need to bind to another address")
      }
      address
    }
  }

  var customHostname: Option[String] = sys.env.get("SPARK_LOCAL_HOSTNAME")

  /**
    * Allow setting a custom host name because when we run on Mesos we need to use the same
    * hostname it reports to the master.
    */
  def setCustomHostname(hostname: String) {
    // DEBUG code
    Utils.checkHost(hostname)
    customHostname = Some(hostname)
  }

  /**
    * Get the local machine's FQDN.
    */
  def localCanonicalHostName(): String = {
    customHostname.getOrElse(localIpAddress.getCanonicalHostName)
  }

  /**
    * Get the local machine's hostname.
    */
  def localHostName(): String = {
    customHostname.getOrElse(localIpAddress.getHostAddress)
  }

  /**
    * Get the local machine's URI.
    */
  def localHostNameForURI(): String = {
    import com.google.common.net.InetAddresses
    customHostname.getOrElse(InetAddresses.toUriString(localIpAddress))
  }

  def checkHost(host: String) {
    assert(host != null && host.indexOf(':') == -1, s"Expected hostname (not IP) but got $host")
  }

  def checkHostPort(hostPort: String) {
    assert(hostPort != null && hostPort.indexOf(':') != -1, s"Expected host and port but got $hostPort")
  }

  // Typically, this will be of order of number of nodes in cluster
  // If not, we should change it to LRUCache or something.
  val hostPortParseResults = new ConcurrentHashMap[String, (String, Int)]()

  def parseHostPort(hostPort: String): (String, Int) = {
    // Check cache first.
    val cached = hostPortParseResults.get(hostPort)
    if (cached != null) {
      return cached
    }

    val indx: Int = hostPort.lastIndexOf(':') // This is potentially broken - when dealing with ipv6 addresses for example, sigh ...
    // but then hadoop does not support ipv6 right now.
    // For now, we assume that if port exists, then it is valid - not check if it is an int > 0
    if (-1 == indx) {
      val retval = (hostPort, 0)
      hostPortParseResults.put(hostPort, retval)
      return retval
    }

    val retval = (hostPort.substring(0, indx).trim(), hostPort.substring(indx + 1).trim().toInt)
    hostPortParseResults.putIfAbsent(hostPort, retval)
    hostPortParseResults.get(hostPort)
  }

  /**
    * Return the string to tell how long has passed in milliseconds.
    */
  def getUsedTimeMs(startTimeMs: Long): String = {
    " " + (System.currentTimeMillis - startTimeMs) + " ms"
  }


  /**
    * Convert a quantity in bytes to a human-readable string such as "4.0 MB".
    */
  def bytesToString(size: Long): String = bytesToString(BigInt(size))

  def bytesToString(size: BigInt): String = {
    val EB = 1L << 60
    val PB = 1L << 50
    val TB = 1L << 40
    val GB = 1L << 30
    val MB = 1L << 20
    val KB = 1L << 10

    if (size >= BigInt(1L << 11) * EB) {
      // The number is too large, show it in scientific notation.
      BigDecimal(size, new MathContext(3, RoundingMode.HALF_UP)).toString() + " B"
    } else {
      val (value, unit) = {
        if (size >= 2 * EB) {
          (BigDecimal(size) / EB, "EB")
        } else if (size >= 2 * PB) {
          (BigDecimal(size) / PB, "PB")
        } else if (size >= 2 * TB) {
          (BigDecimal(size) / TB, "TB")
        } else if (size >= 2 * GB) {
          (BigDecimal(size) / GB, "GB")
        } else if (size >= 2 * MB) {
          (BigDecimal(size) / MB, "MB")
        } else if (size >= 2 * KB) {
          (BigDecimal(size) / KB, "KB")
        } else {
          (BigDecimal(size), "B")
        }
      }
      "%.1f %s".formatLocal(Locale.US, value, unit)
    }
  }

  /**
    * Returns a human-readable string representing a duration such as "35ms"
    */
  def msDurationToString(ms: Long): String = {
    val second = 1000
    val minute = 60 * second
    val hour = 60 * minute
    val locale = Locale.US

    ms match {
      case t if t < second => "%d ms".formatLocal(locale, t)
      case t if t < minute => "%.1f s".formatLocal(locale, t.toFloat / second)
      case t if t < hour => "%.1f m".formatLocal(locale, t.toFloat / minute)
      case t => "%.2f h".formatLocal(locale, t.toFloat / hour)
    }
  }

  /**
    * Convert a quantity in megabytes to a human-readable string such as "4.0 MB".
    */
  def megabytesToString(megabytes: Long): String = {
    bytesToString(megabytes * 1024L * 1024L)
  }

  /**
    * Execute a command and return the process running the command.
    */
  def executeCommand(command: Seq[String], workingDir: File = new File("."), extraEnvironment: Map[String, String] = Map.empty, redirectStderr: Boolean = true): Process = {
    val builder = new ProcessBuilder(command: _*).directory(workingDir)
    val environment = builder.environment()
    for ((key, value) <- extraEnvironment) {
      environment.put(key, value)
    }
    val process = builder.start()
    if (redirectStderr) {
      val threadName = "redirect stderr for command " + command(0)

      def log(s: String): Unit = logInfo(s)

      processStreamByLine(threadName, process.getErrorStream, log)
    }
    process
  }


  /**
    * Return and start a daemon thread that processes the content of the input stream line by line.
    */
  def processStreamByLine(threadName: String, inputStream: InputStream, processLine: String => Unit): Thread = {
    val t = new Thread(threadName) {
      override def run() {
        for (line <- Source.fromInputStream(inputStream).getLines()) {
          processLine(line)
        }
      }
    }
    t.setDaemon(true)
    t.start()
    t
  }


  /**
    * Execute a block of code that returns a value, re-throwing any non-fatal uncaught
    * exceptions as IOException. This is used when implementing Externalizable and Serializable's
    * read and write methods, since Java's serializer will not report non-IOExceptions properly;
    * see SPARK-4080 for more context.
    */
  def tryOrIOException[T](block: => T): T = {
    try {
      block
    } catch {
      case e: IOException => logError("Exception encountered", e)
        throw e
      case NonFatal(e) => logError("Exception encountered", e)
        throw new IOException(e)
    }
  }

  /** Executes the given block. Log non-fatal errors if any, and only throw fatal errors */
  def tryLogNonFatalError(block: => Unit) {
    try {
      block
    } catch {
      case NonFatal(t) => logError(s"Uncaught exception in thread ${Thread.currentThread().getName}", t)
    }
  }

  /**
    * Execute a block of code, then a finally block, but if exceptions happen in
    * the finally block, do not suppress the original exception.
    *
    * This is primarily an issue with `finally { out.close() }` blocks, where
    * close needs to be called to clean up `out`, but if an exception happened
    * in `out.write`, it's likely `out` may be corrupted and `out.close` will
    * fail as well. This would then suppress the original/likely more meaningful
    * exception from the original `out.write` call.
    */
  def tryWithSafeFinally[T](block: => T)(finallyBlock: => Unit): T = {
    var originalThrowable: Throwable = null
    try {
      block
    } catch {
      case t: Throwable => // Purposefully not using NonFatal, because even fatal exceptions
        // we don't want to have our finallyBlock suppress
        originalThrowable = t
        throw originalThrowable
    } finally {
      try {
        finallyBlock
      } catch {
        case t: Throwable if (originalThrowable != null && originalThrowable != t) => originalThrowable.addSuppressed(t)
          logWarning(s"Suppressing exception in finally: ${t.getMessage}", t)
          throw originalThrowable
      }
    }
  }


  // A regular expression to match classes of the internal Spark API's
  // that we want to skip when finding the call site of a method.
  val SPARK_CORE_CLASS_REGEX =
  """^org\.apache\.spark(\.api\.java)?(\.util)?(\.rdd)?(\.broadcast)?\.[A-Z]""".r
  val SPARK_SQL_CLASS_REGEX = """^org\.apache\.spark\.sql.*""".r

  /** Default filtering function for finding call sites using `getCallSite`. */
  def sparkInternalExclusionFunction(className: String): Boolean = {
    val SCALA_CORE_CLASS_PREFIX = "scala"
    val isSparkClass = SPARK_CORE_CLASS_REGEX.findFirstIn(className).isDefined || SPARK_SQL_CLASS_REGEX.findFirstIn(className).isDefined
    val isScalaClass = className.startsWith(SCALA_CORE_CLASS_PREFIX) // If the class is a Spark internal class or a Scala class, then exclude.
    isSparkClass || isScalaClass
  }

  /**
    * When called inside a class in the spark package, returns the name of the user code class
    * (outside the spark package) that called into Spark, as well as which Spark method they called.
    * This is used, for example, to tell users where in their code each RDD got created.
    *
    * @param skipClass Function that is used to exclude non-user-code classes.
    */
  def getCallSite(skipClass: String => Boolean = sparkInternalExclusionFunction): CallSite = {
    // Keep crawling up the stack trace until we find the first function not inside of the spark
    // package. We track the last (shallowest) contiguous Spark method. This might be an RDD
    // transformation, a SparkContext function (such as parallelize), or anything else that leads
    // to instantiation of an RDD. We also track the first (deepest) user method, file, and line.
    var lastSparkMethod = "<unknown>"
    var firstUserFile = "<unknown>"
    var firstUserLine = 0
    var insideSpark = true
    val callStack = new ArrayBuffer[String]() :+ "<unknown>"

    Thread.currentThread.getStackTrace().foreach { ste: StackTraceElement => // When running under some profilers, the current stack trace might contain some bogus
      // frames. This is intended to ensure that we don't crash in these situations by
      // ignoring any frames that we can't examine.
      if (ste != null && ste.getMethodName != null && !ste.getMethodName.contains("getStackTrace")) {
        if (insideSpark) {
          if (skipClass(ste.getClassName)) {
            lastSparkMethod = if (ste.getMethodName == "<init>") {
              // Spark method is a constructor; get its class name
              ste.getClassName.substring(ste.getClassName.lastIndexOf('.') + 1)
            } else {
              ste.getMethodName
            }
            callStack(0) = ste.toString // Put last Spark method on top of the stack trace.
          } else {
            if (ste.getFileName != null) {
              firstUserFile = ste.getFileName
              if (ste.getLineNumber >= 0) {
                firstUserLine = ste.getLineNumber
              }
            }
            callStack += ste.toString
            insideSpark = false
          }
        } else {
          callStack += ste.toString
        }
      }
    }

    val callStackDepth = System.getProperty("spark.callstack.depth", "20").toInt
    val shortForm = if (firstUserFile == "HiveSessionImpl.java") {
      // To be more user friendly, show a nicer string for queries submitted from the JDBC
      // server.
      "Spark JDBC Server Query"
    } else {
      s"$lastSparkMethod at $firstUserFile:$firstUserLine"
    }
    val longForm = callStack.take(callStackDepth).mkString("\n")

    CallSite(shortForm, longForm)
  }


  def isSpace(c: Char): Boolean = {
    " \t\r\n".indexOf(c) != -1
  }

  /**
    * Split a string of potentially quoted arguments from the command line the way that a shell
    * would do it to determine arguments to a command. For example, if the string is 'a "b c" d',
    * then it would be parsed as three arguments: 'a', 'b c' and 'd'.
    */
  def splitCommandString(s: String): Seq[String] = {
    val buf = new ArrayBuffer[String]
    var inWord = false
    var inSingleQuote = false
    var inDoubleQuote = false
    val curWord = new StringBuilder

    def endWord() {
      buf += curWord.toString
      curWord.clear()
    }

    var i = 0
    while (i < s.length) {
      val nextChar = s.charAt(i)
      if (inDoubleQuote) {
        if (nextChar == '"') {
          inDoubleQuote = false
        } else if (nextChar == '\\') {
          if (i < s.length - 1) {
            // Append the next character directly, because only " and \ may be escaped in
            // double quotes after the shell's own expansion
            curWord.append(s.charAt(i + 1))
            i += 1
          }
        } else {
          curWord.append(nextChar)
        }
      } else if (inSingleQuote) {
        if (nextChar == '\'') {
          inSingleQuote = false
        } else {
          curWord.append(nextChar)
        } // Backslashes are not treated specially in single quotes
      } else if (nextChar == '"') {
        inWord = true
        inDoubleQuote = true
      } else if (nextChar == '\'') {
        inWord = true
        inSingleQuote = true
      } else if (!isSpace(nextChar)) {
        curWord.append(nextChar)
        inWord = true
      } else if (inWord && isSpace(nextChar)) {
        endWord()
        inWord = false
      }
      i += 1
    }
    if (inWord || inDoubleQuote || inSingleQuote) {
      endWord()
    }
    buf
  }

  /* Calculates 'x' modulo 'mod', takes to consideration sign of x,
  * i.e. if 'x' is negative, than 'x' % 'mod' is negative too
  * so function return (x % mod) + mod in that case.
  */ def nonNegativeMod(x: Int, mod: Int): Int = {
    val rawMod = x % mod
    rawMod + (if (rawMod < 0) mod else 0)
  }

  // Handles idiosyncrasies with hash (add more as required)
  // This method should be kept in sync with
  // org.apache.spark.network.util.JavaUtils#nonNegativeHash().
  def nonNegativeHash(obj: AnyRef): Int = {

    // Required ?
    if (obj eq null) return 0

    val hash = obj.hashCode
    // math.abs fails for Int.MinValue
    val hashAbs = if (Int.MinValue != hash) math.abs(hash) else 0

    // Nothing else to guard against ?
    hashAbs
  }

  /**
    * NaN-safe version of `java.lang.Double.compare()` which allows NaN values to be compared
    * according to semantics where NaN == NaN and NaN is greater than any non-NaN double.
    */
  def nanSafeCompareDoubles(x: Double, y: Double): Int = {
    val xIsNan: Boolean = java.lang.Double.isNaN(x)
    val yIsNan: Boolean = java.lang.Double.isNaN(y)
    if ((xIsNan && yIsNan) || (x == y)) 0 else if (xIsNan) 1 else if (yIsNan) -1 else if (x > y) 1 else -1
  }

  /**
    * NaN-safe version of `java.lang.Float.compare()` which allows NaN values to be compared
    * according to semantics where NaN == NaN and NaN is greater than any non-NaN float.
    */
  def nanSafeCompareFloats(x: Float, y: Float): Int = {
    val xIsNan: Boolean = java.lang.Float.isNaN(x)
    val yIsNan: Boolean = java.lang.Float.isNaN(y)
    if ((xIsNan && yIsNan) || (x == y)) 0 else if (xIsNan) 1 else if (yIsNan) -1 else if (x > y) 1 else -1
  }

  /**
    * Returns the system properties map that is thread-safe to iterator over. It gets the
    * properties which have been set explicitly, as well as those for which only a default value
    * has been defined.
    */
  def getSystemProperties: Map[String, String] = {
    System.getProperties.stringPropertyNames().asScala.map(key => (key, System.getProperty(key))).toMap
  }

  /**
    * Method executed for repeating a task for side effects.
    * Unlike a for comprehension, it permits JVM JIT optimization
    */
  def times(numIters: Int)(f: => Unit): Unit = {
    var i = 0
    while (i < numIters) {
      f
      i += 1
    }
  }

  /**
    * Timing method based on iterations that permit JVM JIT optimization.
    *
    * @param numIters number of iterations
    * @param f        function to be executed. If prepare is not None, the running time of each call to f
    *                 must be an order of magnitude longer than one millisecond for accurate timing.
    * @param prepare  function to be executed before each call to f. Its running time doesn't count.
    * @return the total time across all iterations (not counting preparation time)
    */
  def timeIt(numIters: Int)(f: => Unit, prepare: Option[() => Unit] = None): Long = {
    if (prepare.isEmpty) {
      val start = System.currentTimeMillis
      times(numIters)(f)
      System.currentTimeMillis - start
    } else {
      var i = 0
      var sum = 0L
      while (i < numIters) {
        prepare.get.apply()
        val start = System.currentTimeMillis
        f
        sum += System.currentTimeMillis - start
        i += 1
      }
      sum
    }
  }

  /**
    * Counts the number of elements of an iterator using a while loop rather than calling
    * [[scala.collection.Iterator#size]] because it uses a for loop, which is slightly slower
    * in the current version of Scala.
    */
  def getIteratorSize(iterator: Iterator[_]): Long = {
    var count = 0L
    while (iterator.hasNext) {
      count += 1L
      iterator.next()
    }
    count
  }

  /**
    * Generate a zipWithIndex iterator, avoid index value overflowing problem
    * in scala's zipWithIndex
    */
  def getIteratorZipWithIndex[T](iterator: Iterator[T], startIndex: Long): Iterator[(T, Long)] = {
    new Iterator[(T, Long)] {
      require(startIndex >= 0, "startIndex should be >= 0.")
      var index: Long = startIndex - 1L

      def hasNext: Boolean = iterator.hasNext

      def next(): (T, Long) = {
        index += 1L
        (iterator.next(), index)
      }
    }
  }

  /**
    * Creates a symlink.
    *
    * @param src absolute path to the source
    * @param dst relative path for the destination
    */
  def symlink(src: File, dst: File): Unit = {
    if (!src.isAbsolute()) {
      throw new IOException("Source must be absolute")
    }
    if (dst.isAbsolute()) {
      throw new IOException("Destination must be relative")
    }
    Files.createSymbolicLink(dst.toPath, src.toPath)
  }


  /** Return the class name of the given object, removing all dollar signs */
  def getFormattedClassName(obj: AnyRef): String = {
    getSimpleName(obj.getClass).replace("$", "")
  }


  /**
    * Whether the underlying operating system is Windows.
    */
  val isWindows = SystemUtils.IS_OS_WINDOWS

  /**
    * Whether the underlying operating system is Mac OS X.
    */
  val isMac = SystemUtils.IS_OS_MAC_OSX

  /**
    * Pattern for matching a Windows drive, which contains only a single alphabet character.
    */
  val windowsDrive = "([a-zA-Z])".r

  /**
    * Indicates whether Spark is currently running unit tests.
    */
  def isTesting: Boolean = {
    sys.env.contains("SPARK_TESTING") || sys.props.contains("spark.testing")
  }

  /**
    * Terminates a process waiting for at most the specified duration.
    *
    * @return the process exit value if it was successfully terminated, else None
    */
  def terminateProcess(process: Process, timeoutMs: Long): Option[Int] = {
    // Politely destroy first
    process.destroy()
    if (process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
      // Successful exit
      Option(process.exitValue())
    } else {
      try {
        process.destroyForcibly()
      } catch {
        case NonFatal(e) => logWarning("Exception when attempting to kill process", e)
      } // Wait, again, although this really should return almost immediately
      if (process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
        Option(process.exitValue())
      } else {
        logWarning("Timed out waiting to forcibly kill process")
        None
      }
    }
  }

  /**
    * Return the stderr of a process after waiting for the process to terminate.
    * If the process does not terminate within the specified timeout, return None.
    */
  def getStderr(process: Process, timeoutMs: Long): Option[String] = {
    val terminated = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
    if (terminated) {
      Some(Source.fromInputStream(process.getErrorStream).getLines().mkString("\n"))
    } else {
      None
    }
  }

  /**
    * Execute the given block, logging and re-throwing any uncaught exception.
    * This is particularly useful for wrapping code that runs in a thread, to ensure
    * that exceptions are printed, and to avoid having to catch Throwable.
    */
  def logUncaughtExceptions[T](f: => T): T = {
    try {
      f
    } catch {
      case ct: ControlThrowable => throw ct
      case t: Throwable => logError(s"Uncaught exception in thread ${Thread.currentThread().getName}", t)
        throw t
    }
  }

  /** Executes the given block in a Try, logging any uncaught exceptions. */
  def tryLog[T](f: => T): Try[T] = {
    try {
      val res = f
      scala.util.Success(res)
    } catch {
      case ct: ControlThrowable => throw ct
      case t: Throwable => logError(s"Uncaught exception in thread ${Thread.currentThread().getName}", t)
        scala.util.Failure(t)
    }
  }

  /** Returns true if the given exception was fatal. See docs for scala.util.control.NonFatal. */
  def isFatalError(e: Throwable): Boolean = {
    e match {
      case NonFatal(_) | _: InterruptedException | _: NotImplementedError | _: ControlThrowable | _: LinkageError => false
      case _ => true
    }
  }

  /**
    * Return a well-formed URI for the file described by a user input string.
    *
    * If the supplied path does not contain a scheme, or is a relative path, it will be
    * converted into an absolute path with a file:// scheme.
    */
  def resolveURI(path: String): URI = {
    try {
      val uri = new URI(path)
      if (uri.getScheme() != null) {
        return uri
      } // make sure to handle if the path has a fragment (applies to yarn
      // distributed cache)
      if (uri.getFragment() != null) {
        val absoluteURI = new File(uri.getPath()).getAbsoluteFile().toURI()
        return new URI(absoluteURI.getScheme(), absoluteURI.getHost(), absoluteURI.getPath(), uri.getFragment())
      }
    } catch {
      case e: URISyntaxException =>
    }
    new File(path).getAbsoluteFile().toURI()
  }

  /** Resolve a comma-separated list of paths. */
  def resolveURIs(paths: String): String = {
    if (paths == null || paths.trim.isEmpty) {
      ""
    } else {
      paths.split(",").filter(_.trim.nonEmpty).map { p => Utils.resolveURI(p) }.mkString(",")
    }
  }

  /** Return all non-local paths from a comma-separated list of paths. */
  def nonLocalPaths(paths: String, testWindows: Boolean = false): Array[String] = {
    val windows = isWindows || testWindows
    if (paths == null || paths.trim.isEmpty) {
      Array.empty
    } else {
      paths.split(",").filter { p =>
        val uri = resolveURI(p)
        Option(uri.getScheme).getOrElse("file") match {
          case windowsDrive(d) if windows => false
          case "local" | "file" => false
          case _ => true
        }
      }
    }
  }

  /**
    * Implements the same logic as JDK `java.lang.String#trim` by removing leading and trailing
    * non-printable characters less or equal to '\u0020' (SPACE) but preserves natural line
    * delimiters according to [[java.util.Properties]] load method. The natural line delimiters are
    * removed by JDK during load. Therefore any remaining ones have been specifically provided and
    * escaped by the user, and must not be ignored
    *
    * @param str
    * @return the trimmed value of str
    */
  def trimExceptCRLF(str: String): String = {
    val nonSpaceOrNaturalLineDelimiter: Char => Boolean = { ch =>
      ch > ' ' || ch == '\r' || ch == '\n'
    }

    val firstPos = str.indexWhere(nonSpaceOrNaturalLineDelimiter)
    val lastPos = str.lastIndexWhere(nonSpaceOrNaturalLineDelimiter)
    if (firstPos >= 0 && lastPos >= 0) {
      str.substring(firstPos, lastPos + 1)
    } else {
      ""
    }
  }


  def userPort(base: Int, offset: Int): Int = {
    (base + offset - 1024) % (65536 - 1024) + 1024
  }

  /**
    * Attempt to start a service on the given port, or fail after a number of attempts.
    * Each subsequent attempt uses 1 + the port used in the previous attempt (unless the port is 0).
    *
    * @param startPort    The initial port to start the service on.
    * @param startService Function to start service on a given port.
    *                     This is expected to throw java.net.BindException on port collision.
    * @param conf         A SparkConf used to get the maximum number of retries when binding to a port.
    * @param serviceName  Name of the service.
    * @return (service: T, port: Int)
    */
  def startServiceOnPort[T](startPort: Int, startService: Int => (T, Int), conf: util.HashMap[String, String], serviceName: String = ""): (T, Int) = {

    require(startPort == 0 || (1024 <= startPort && startPort < 65536), "startPort should be between 1024 and 65535 (inclusive), or 0 for a random free port.")

    val serviceString = if (serviceName.isEmpty) "" else s" '$serviceName'"
    val maxRetries = conf.getOrDefault("maxRetries", "10").toInt
    for (offset <- 0 to maxRetries) {
      // Do not increment port if startPort is 0, which is treated as a special port
      val tryPort = if (startPort == 0) {
        startPort
      } else {
        userPort(startPort, offset)
      }
      try {
        val (service, port) = startService(tryPort)
        logInfo(s"Successfully started service$serviceString on port $port.")
        return (service, port)
      } catch {
        case e: Exception if e.isInstanceOf[BindException] => if (offset >= maxRetries) {
          val exceptionMessage = if (startPort == 0) {
            s"${e.getMessage}: Service$serviceString failed after " + s"$maxRetries retries (on a random free port)! " + s"Consider explicitly setting the appropriate binding address for " + s"the service$serviceString (for example spark.driver.bindAddress " + s"for SparkDriver) to the correct binding address."
          } else {
            s"${e.getMessage}: Service$serviceString failed after " + s"$maxRetries retries (starting from $startPort)! Consider explicitly setting " + s"the appropriate port for the service$serviceString (for example spark.ui.port " + s"for SparkUI) to an available port or increasing spark.port.maxRetries."
          }
          val exception = new BindException(exceptionMessage) // restore original stack trace
          exception.setStackTrace(e.getStackTrace)
          throw exception
        }
          if (startPort == 0) {
            // As startPort 0 is for a random free port, it is most possibly binding address is
            // not correct.
            logWarning(s"Service$serviceString could not bind on a random free port. " + "You may check whether configuring an appropriate binding address.")
          } else {
            logWarning(s"Service$serviceString could not bind on port $tryPort. " + s"Attempting port ${tryPort + 1}.")
          }
      }
    } // Should never happen
    throw new RuntimeException(s"Failed to start service$serviceString on port $startPort")
  }

  /**
    * configure a new log4j level
    */
  def setLogLevel(l: org.apache.log4j.Level) {
    org.apache.log4j.Logger.getRootLogger().setLevel(l)
  }

  /**
    * Return the current system LD_LIBRARY_PATH name
    */
  def libraryPathEnvName: String = {
    if (isWindows) {
      "PATH"
    } else if (isMac) {
      "DYLD_LIBRARY_PATH"
    } else {
      "LD_LIBRARY_PATH"
    }
  }

  /**
    * Return the prefix of a command that appends the given library paths to the
    * system-specific library path environment variable. On Unix, for instance,
    * this returns the string LD_LIBRARY_PATH="path1:path2:$LD_LIBRARY_PATH".
    */
  def libraryPathEnvPrefix(libraryPaths: Seq[String]): String = {
    val libraryPathScriptVar = if (isWindows) {
      s"%${libraryPathEnvName}%"
    } else {
      "$" + libraryPathEnvName
    }
    val libraryPath = (libraryPaths :+ libraryPathScriptVar).mkString("\"", File.pathSeparator, "\"")
    val ampersand = if (Utils.isWindows) {
      " &"
    } else {
      ""
    }
    s"$libraryPathEnvName=$libraryPath$ampersand"
  }


  def tryWithResource[R <: Closeable, T](createResource: => R)(f: R => T): T = {
    val resource = createResource
    try f.apply(resource) finally resource.close()
  }

  /**
    * Returns a path of temporary file which is in the same directory with `path`.
    */
  def tempFileWith(path: File): File = {
    new File(path.getAbsolutePath + "." + UUID.randomUUID())
  }

  /**
    * Returns the name of this JVM process. This is OS dependent but typically (OSX, Linux, Windows),
    * this is formatted as PID@hostname.
    */
  def getProcessName(): String = {
    ManagementFactory.getRuntimeMXBean().getName()
  }


  def stringToSeq(str: String): Seq[String] = {
    str.split(",").map(_.trim()).filter(_.nonEmpty)
  }

  /**
    * Safer than Class obj's getSimpleName which may throw Malformed class name error in scala.
    * This method mimicks scalatest's getSimpleNameOfAnObjectsClass.
    */
  def getSimpleName(cls: Class[_]): String = {
    try {
      return cls.getSimpleName
    } catch {
      case err: InternalError => return stripDollars(stripPackages(cls.getName))
    }
  }

  /**
    * Remove the packages from full qualified class name
    */
  def stripPackages(fullyQualifiedName: String): String = {
    fullyQualifiedName.split("\\.").takeRight(1)(0)
  }

  /**
    * Remove trailing dollar signs from qualified class name,
    * and return the trailing part after the last dollar sign in the middle
    */
  def stripDollars(s: String): String = {
    val lastDollarIndex = s.lastIndexOf('$')
    if (lastDollarIndex < s.length - 1) {
      // The last char is not a dollar sign
      if (lastDollarIndex == -1 || !s.contains("$iw")) {
        // The name does not have dollar sign or is not an intepreter
        // generated class, so we should return the full string
        s
      } else {
        // The class name is intepreter generated,
        // return the part after the last dollar sign
        // This is the same behavior as getClass.getSimpleName
        s.substring(lastDollarIndex + 1)
      }
    } else {
      // The last char is a dollar sign
      // Find last non-dollar char
      val lastNonDollarChar = s.reverse.find(_ != '$')
      lastNonDollarChar match {
        case None => s
        case Some(c) => val lastNonDollarIndex = s.lastIndexOf(c)
          if (lastNonDollarIndex == -1) {
            s
          } else {
            // Strip the trailing dollar signs
            // Invoke stripDollars again to get the simple name
            stripDollars(s.substring(0, lastNonDollarIndex + 1))
          }
      }
    }
  }

  /**
    * Regular expression matching full width characters.
    *
    * Looked at all the 0x0000-0xFFFF characters (unicode) and showed them under Xshell.
    * Found all the full width characters, then get the regular expression.
    */
  val fullWidthRegex = ("""[""" + // scalastyle:off nonascii
    """\u1100-\u115F""" +
    """\u2E80-\uA4CF""" +
    """\uAC00-\uD7A3""" +
    """\uF900-\uFAFF""" +
    """\uFE10-\uFE19""" +
    """\uFE30-\uFE6F""" +
    """\uFF00-\uFF60""" +
    """\uFFE0-\uFFE6""" + // scalastyle:on nonascii
    """]""").r

  /**
    * Return the number of half widths in a given string. Note that a full width character
    * occupies two half widths.
    *
    * For a string consisting of 1 million characters, the execution of this method requires
    * about 50ms.
    */
  def stringHalfWidth(str: String): Int = {
    if (str == null) 0 else str.length + fullWidthRegex.findAllIn(str).size
  }
}
