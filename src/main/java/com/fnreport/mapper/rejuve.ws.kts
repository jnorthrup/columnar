package com.fnreport.mapper

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.runBlocking

val d1names = listOf("SalesNo", "SalesAreaID", "date", "PluNo", "ItemName", "Quantity", "Amount", "TransMode")

val colspecs = listOf((0 to 11), (11 to 15), (15 to 25), (25 to 40), (40 to 60), (60 to 82), (82 to 103), (103 to 108))

@InternalCoroutinesApi
val d1 = Columnar(rs = FixedRecordLengthFile("/vol/aux/rejuve/rejuvesinceapril2019.fwf"), columns = d1names.zip(colspecs.zip(listOf(stringMapper(), stringMapper(), dateMapper(), stringMapper(), stringMapper(), floatMapper(), floatMapper(), stringMapper()))))

@InternalCoroutinesApi
val ds = d1.size

@UseExperimental(kotlinx.coroutines.InternalCoroutinesApi::class)
//@InternalCoroutinesApi
runBlocking {
    val d2 = d1[1, 2, 3, 5]
    val d3 = d2[1, 0, 2, 3]
    val d4 = d3.pivot(listOf(0, 2), 1, 3).pivot(listOf(0), 1, 2)
    val d5 = d4.group(0)
    (0 until 10).forEach {
        d5.values(it).let(System.out::println)
    }
}