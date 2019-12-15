package columnar

import java.io.Closeable

class FixedRecordLengthFile(
    filename: String, origin: MappedFile = MappedFile(
        filename
    )
) : FixedRecordLengthBuffer(origin.mappedByteBuffer),
    Closeable by origin