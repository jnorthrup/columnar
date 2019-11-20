package com.fnreport.mapper

import io.kotlintest.TestCase
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.flow.*
import java.nio.ByteBuffer

class FixedRecordLengthBufferTest0 : StringSpec() {

    override fun beforeTest(testCase: TestCase) {
    }

    init {
        "FixedRecordLengthFile" {
            //colspecs=(0 , 10), (10 t, 84), (84 , 124), (124 , 164)
            val x = FixedRecordLengthFile("src/test/resources/caven20.fwf")
            val flow = x[0, 19, 10]

            suspend1(flow)
        }
    }

     private inline suspend fun suspend1(flow: Flow<ByteBuffer>) {
         val ccc = flow.toList()

         val reified = ccc
                 .map { byteBuffer ->
                     val bb = ByteArray(byteBuffer.remaining());
                     byteBuffer.get(bb)
                       String(bb)
                  }

         val trim = reified[1].trim()
         System.err.println( trim )//.shouldBe("2017-07-060100865/0101010106/13-14/01                                               4.000000000000                          0E-12" )

         trim.shouldBe("2017-07-060100865/0101010106/13-14/01                                               4.000000000000                          0E-12" )

     }
    private inline suspend fun suspend2(flow: Flow<ByteBuffer>) {
         val ccc = flow.toList()

         val reified = ccc
                 .map { byteBuffer ->
                     val bb = ByteArray(byteBuffer.remaining());
                     byteBuffer.get(bb)
                       String(bb)
                  }

         val trim = reified[1].trim()
         System.err.println( trim )
     }
}
