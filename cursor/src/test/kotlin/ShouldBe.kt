import org.junit.jupiter.api.Assertions

infix fun Any?.shouldBe(that: Any?) {
    Assertions.assertEquals(that, this)
}