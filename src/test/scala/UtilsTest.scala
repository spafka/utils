class UtilsTest {

  import org.junit.Test


  @Test def _1 = {
    import io.github.spafka.spark.util.Utils


    println(Utils.localHostNameForURI())
    println(Utils.localHostName())


  }
}
