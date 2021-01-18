import org.junit.Assert

infix fun Any?.shouldBe(that: Any?) {
     Assert.assertEquals(that, this)
}