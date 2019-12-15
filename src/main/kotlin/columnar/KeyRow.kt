package columnar

import kotlinx.coroutines.flow.Flow

typealias KeyRow = Pair<Array<Column>, Pair<Flow<RowHandle>, Int>>