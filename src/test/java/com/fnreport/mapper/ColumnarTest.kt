package com.fnreport.mapper

import io.kotlintest.TestCase
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.first

@UseExperimental(InternalCoroutinesApi::class)

class ColumnarTest : StringSpec() {

    val x = FixedRecordLengthFile("src/test/resources/caven20.fwf")
    val columns = listOf("date", "channel", "delivered", "ret").zip(
            arrayListOf((0 to 10), (10 to 84), (84 to 124), (124 to 164)).zip(
                    listOf(dateMapper(),
                            stringMapper(),
                            floatMapper(),
                            floatMapper(
                    ))))
    val cx = Columnar(x, columns)

    override fun beforeTest(testCase: TestCase) {

    }


    init {
        "values" {
            val values = decode(1)
            System.err.println(values)
        }

        "pivot" { }

        "group" { }
    }

    private suspend fun decode(row: Int): List<Any?> {

        return cx.values(1).first()


    }

}