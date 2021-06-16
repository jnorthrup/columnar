package cursors.io

import cursors.Cursor
import cursors.at
import cursors.context.Scalar.Companion.Scalar
import org.junit.Assert.assertEquals
import org.junit.Test
import vec.macros.`⟲`
import vec.macros.get
import vec.macros.size
import vec.macros.t2
import vec.util.path
import java.nio.channels.FileChannel
import java.time.Instant
import kotlin.random.Random

class ISAMCursorKtTest {
    @Test
    fun testIoInstant() {

        val testInstant = Instant.now()
        System.err.println(testInstant)
        val c0: Cursor = cursors.Cursor(1) { y: Int ->
            RowVec(1) { x: Int ->
                testInstant t2 Scalar(IOMemento.IoInstant, "ASDF").`⟲`
            }
        }

        val fname = "target/IoInstant.isam"
        c0.also { c: Cursor ->
            c.writeISAM(fname)
        }
        FileChannel.open(fname.path).use { fc ->
            val c2 = ISAMCursor(fname.path, fc)
            assertEquals((c0 at 0)[0].first as Instant, (c2 at 0)[0].first as Instant)
            assertEquals((c0.size), (c2.size))
            System.err.println((c2 at 0)[0].first as Instant)
        }
    }

    @Test
    fun testIoInstant2() {

        val testInstant = Instant.now()
        System.err.println(testInstant)
        val c0: Cursor = cursors.Cursor(2) { y: Int ->
            RowVec(7) { x: Int ->
                testInstant t2 Scalar(IOMemento.IoInstant, "ASDF").`⟲`
            }
        }

        val fname = "target/IoInstant.isam"
        c0.also { c: Cursor ->
            c.writeISAM(fname)
        }
        FileChannel.open(fname.path).use { fc ->
            val c2 = ISAMCursor(fname.path, fc)
            assertEquals((c0 at 0)[0].first as Instant, (c2 at 0)[0].first as Instant)
            assertEquals((c0.size), (c2.size))
            assertEquals((c0 at 0)[1].first as Instant, (c2 at 0)[1].first as Instant)
            assertEquals((c0 at 0)[2].first as Instant, (c2 at 0)[2].first as Instant)
            assertEquals((c0 at 0)[3].first as Instant, (c2 at 0)[3].first as Instant)
            assertEquals((c0 at 0)[4].first as Instant, (c2 at 0)[4].first as Instant)
            assertEquals((c0 at 0)[5].first as Instant, (c2 at 0)[5].first as Instant)
            assertEquals((c0 at 0)[6].first as Instant, (c2 at 0)[6].first as Instant)
            assertEquals((c0 at 1)[1].first as Instant, (c2 at 1)[1].first as Instant)
            assertEquals((c0 at 1)[2].first as Instant, (c2 at 1)[2].first as Instant)
            assertEquals((c0 at 1)[3].first as Instant, (c2 at 1)[3].first as Instant)
            assertEquals((c0 at 1)[4].first as Instant, (c2 at 1)[4].first as Instant)
            assertEquals((c0 at 1)[5].first as Instant, (c2 at 1)[5].first as Instant)
            assertEquals((c0 at 1)[6].first as Instant, (c2 at 1)[6].first as Instant)
            System.err.println((c2 at 0)[5].first as Instant)
        }
    }

    @Test
    fun testIoInstant3() {

        val testInstant = Instant.now()
        val testDouble = Random.nextDouble()
        System.err.println(testInstant)
        System.err.println(testDouble)
        val c0: Cursor = cursors.Cursor(2) { y: Int ->
            RowVec(7) { x: Int ->
                when (x.rem(2)) {
                    0 -> testInstant t2 Scalar(IOMemento.IoInstant, "ASDF").`⟲`
                    else -> testDouble t2 Scalar(IOMemento.IoDouble, "JOID").`⟲`


                }
            }
        }

        val fname = "target/IoInstant.isam"
        c0.also { c: Cursor ->
            c.writeISAM(fname)
        }
        FileChannel.open(fname.path).use { fc ->
            val c2 = ISAMCursor(fname.path, fc)
            assertEquals((c0 at 0)[0].first as Instant, (c2 at 0)[0].first as Instant)
            assertEquals((c0.size), (c2.size))
            assertEquals((c0 at 0)[1].first    , (c2 at 0)[1].first  )
            assertEquals((c0 at 0)[2].first  , (c2 at 0)[2].first )
            assertEquals((c0 at 0)[3].first  , (c2 at 0)[3].first )
            assertEquals((c0 at 0)[4].first  , (c2 at 0)[4].first )
            assertEquals((c0 at 0)[5].first  , (c2 at 0)[5].first )
            assertEquals((c0 at 0)[6].first  , (c2 at 0)[6].first )
            assertEquals((c0 at 1)[1].first  , (c2 at 1)[1].first )
            assertEquals((c0 at 1)[2].first  , (c2 at 1)[2].first )
            assertEquals((c0 at 1)[3].first  , (c2 at 1)[3].first )
            assertEquals((c0 at 1)[4].first  , (c2 at 1)[4].first )
            assertEquals((c0 at 1)[5].first  , (c2 at 1)[5].first )
            assertEquals((c0 at 1)[6].first  , (c2 at 1)[6].first )
            System.err.println((c2 at 1)[5].first )
        }
    }
    @Test
    fun testIoInstant4() {

        val testInstant = Instant.now()
        val testDouble = Random.nextDouble()
        val testInt = Random.nextInt()
        System.err.println(testInstant)
        System.err.println(testDouble)
        System.err.println(testInt)
        val c0: Cursor = cursors.Cursor(2) { y: Int ->
            RowVec(7) { x: Int ->
                when (x.rem(2)) {
                    0 -> testInstant t2 Scalar(IOMemento.IoInstant, "ASDF").`⟲`
                    1 -> testInt t2 Scalar(IOMemento.IoInt, "ESXA").`⟲`
                    else -> testDouble t2 Scalar(IOMemento.IoDouble, "JOID").`⟲`


                }
            }
        }

        val fname = "target/IoInstant.isam"
        c0.also { c: Cursor ->
            c.writeISAM(fname)
        }
        FileChannel.open(fname.path).use { fc ->
            val c2 = ISAMCursor(fname.path, fc)
            assertEquals((c0 at 0)[0].first as Instant, (c2 at 0)[0].first as Instant)
            assertEquals((c0.size), (c2.size))
            assertEquals((c0 at 0)[1].first  , (c2 at 0)[1].first  )
            assertEquals((c0 at 0)[2].first  , (c2 at 0)[2].first )
            assertEquals((c0 at 0)[3].first  , (c2 at 0)[3].first )
            assertEquals((c0 at 0)[4].first  , (c2 at 0)[4].first )
            assertEquals((c0 at 0)[5].first  , (c2 at 0)[5].first )
            assertEquals((c0 at 0)[6].first  , (c2 at 0)[6].first )
            assertEquals((c0 at 1)[1].first  , (c2 at 1)[1].first )
            assertEquals((c0 at 1)[2].first  , (c2 at 1)[2].first )
            assertEquals((c0 at 1)[3].first  , (c2 at 1)[3].first )
            assertEquals((c0 at 1)[4].first  , (c2 at 1)[4].first )
            assertEquals((c0 at 1)[5].first  , (c2 at 1)[5].first )
            assertEquals((c0 at 1)[6].first  , (c2 at 1)[6].first )
            System.err.println((c2 at 1)[5].first )
        }
    }
}