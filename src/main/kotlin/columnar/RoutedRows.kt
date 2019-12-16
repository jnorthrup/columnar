package columnar

import kotlinx.coroutines.flow.Flow

typealias RoutedRows = Pair<RowNormalizer, Pair<Flow<RouteHandle>, Int>>