package cursors.tbd

import org.junit.Test
import vec.macros.combine
import vec.util._v


class testXYZ {
    @Test
    fun tetstXyz() {
        val matrix =
            _v[_v[
                    _v[1, 2, 3],
                    _v[4, 5, 6],
                    _v[5, 8, 9],
                    _v[6, 11, 12],
            ], _v[
                    _v[1, 2, 3],
                    _v[4, 5, 6],
                    _v[5, 8, 9],
                    _v[6, 11, 12],
            ], _v[
                    _v[1, 2, 3],
                    _v[4, 5, 6],
                    _v[5, 8, 9],
                    _v[6, 11, 12],
            ], _v[
                    _v[1, 2, 3],
                    _v[4, 5, 6],
                    _v[5, 8, 9],
                    _v[6, 11, 12],
            ], _v[
                    _v[1, 2, 3],
                    _v[4, 5, 6],
                    _v[5, 8, 9],
                    _v[6, 11, 12],
            ]]
        val ccc = combine(combine(matrix))


    }
}
