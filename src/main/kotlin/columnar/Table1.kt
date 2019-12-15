package columnar

import kotlinx.coroutines.flow.Flow

typealias Table1 = suspend (Int) -> Array<Flow<Any?>>