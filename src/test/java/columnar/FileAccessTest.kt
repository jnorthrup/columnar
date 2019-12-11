package columnar

import arrow.core.Option
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer

@UseExperimental(InternalCoroutinesApi::class)
class FileAccessTest : StringSpec() {
    val columns: RowDecoder = listOf("date", "channel", "delivered", "ret").zip(
        listOf((0 to 10), (10 to 84), (84 to 124), (124 to 164))
            .zip(
                listOf(
                    dateMapper,
                    stringMapper,
                    floatMapper,
                    floatMapper
                )
            )
    ).toTypedArray()
    val f20 = FixedRecordLengthFile("src/test/resources/caven20.fwf")
    val f4 = FixedRecordLengthFile("src/test/resources/caven4.fwf")

    init {
        "things"{


            val c4: Pair<Array<Pair<String, Option<(Any?) -> Any?>>>, Pair<Flow<Array<Any?>>, Int>> = columns reify f4
            val lineage = c4.first  .zip( columns)

            ByteBuffer.allocate(20*1024*1024)

            val nc = !columns
            c4.f.collect{


            }




            println()
        }
    }

}