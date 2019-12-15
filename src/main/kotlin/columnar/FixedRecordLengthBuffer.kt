package columnar

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.nio.ByteBuffer

open class FixedRecordLengthBuffer(val buf: ByteBuffer) :
    RowStore<Flow<ByteBuffer>>,
    FixedLength {
    override var values: suspend (Int) -> Flow<ByteBuffer> =
        { row -> flowOf(buf.position(recordLen * row).slice().limit(recordLen)) }
    override val recordLen = buf.duplicate().clear().run {
        while (hasRemaining() && get() != '\n'.toByte());
        position()
    }
    override val size: Int = (buf.limit() / recordLen)
}