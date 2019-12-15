package columnar

import kotlinx.coroutines.flow.Flow

typealias RoutedRows = Pair<Array<Column>, Pair<Flow<RouteHandle>, Int>>