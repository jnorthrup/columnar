package columnar

import kotlinx.coroutines.flow.Flow

typealias KeyRow = Pair<RowNormalizer, Pair<Flow<RowHandle>, Int>>