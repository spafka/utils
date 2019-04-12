import io.github.spafka.spark.internal.Logging

class UtilsTest {

  import org.junit.Test


  @Test def _1 = {
    import io.github.spafka.spark.util.Utils


    println(Utils.localHostNameForURI())
    println(Utils.localHostName())

    new A().log1()

  }

}


class A extends Logging {

  def log1() = {
    logInfo(s"三生三世")
  }
}
