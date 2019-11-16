package com.fnreport.mapper

import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector

@UseExperimental(InternalCoroutinesApi::class)
class CsvFileTest : StringSpec() {

    init {
     /*   "x"{
            val csvFile = CsvFile("src/test/resources/caven20.csv")
            val flow = csvFile[0 until csvFile.size]
            doflow(flow)
        }
    */}

    private suspend fun doflow(flow: Flow<Flow<VariableRecordLengthBuffer>>) {
      /*  flow.collect{

        }
    */}

}