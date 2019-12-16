package columnar

import arrow.core.Option


typealias Decorator=Option<xform>

typealias RowNormalizer = Array<Triple<String, ByteBufferNormalizer,Decorator>>