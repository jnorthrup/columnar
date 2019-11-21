package com.fnreport.mapper


import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

fun main() {
    val d1names = listOf("SalesNo", "SalesAreaID", "date", "PluNo", "ItemName", "Quantity", "Amount", "TransMode")
    val colspecs = listOf((0 to 11), (11 to 15), (15 to 25), (25 to 40), (40 to 60), (60 to 82), (82 to 103), (103 to 108))
    val mappings = listOf(stringMapper(), stringMapper(), dateMapper(), stringMapper(), stringMapper(), { i -> btoa(i)?.toFloat() ?: 0f }, floatMapper(), stringMapper())

    @UseExperimental(kotlinx.coroutines.InternalCoroutinesApi::class)
    runBlocking { }

    @UseExperimental(kotlinx.coroutines.InternalCoroutinesApi::class)
    runBlocking {

        //        @UseExperimental(kotlinx.coroutines.InternalCoroutinesApi::class)
        val d1: Columnar = Columnar(rs = FixedRecordLengthFile("/vol/aux/rejuve/rejuvesinceapril2019.fwf"), columns = d1names.zip(colspecs.zip(mappings)).toTypedArray())
        println(" #d1")
        println(""+d1.values(0 ))
        val ds = d1.size
        println(" #d2")
        val d2 = d1[2, 1, 3, 5]
        println(""+d2.values(0 ))

        //fillna 0
        d2[listOf(3) to { any -> any ?: 0f }]
        println(" #d3")
        val d3 = d2
        println(d3.columns.map { (a, b) -> a })
        println(""+d3.values(0 ))
        println(" #d4")
        val d4 = d2.pivot(listOf(0, 2), 1, 3)
        println(d4.columns.map { (a, b) -> a })
        println(""+d4.values(0 ))
        println(" #d5")
        val d5 = d4.pivot(listOf(0), 1, *(2 until d4.columns.size).toList().toIntArray())

        println(d5.columns.map { (a, b) -> a })
        println(""+d5.values(0 ))
        println(" #d6")

        val d6 = d5.group(0)
        println(""+d6.values(0 )) 

    }

}