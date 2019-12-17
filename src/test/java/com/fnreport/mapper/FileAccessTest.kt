package com.fnreport.mapper


import arrow.core.none
import columnar.*
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration.Companion.Stable
import kotlinx.serialization.list
import kotlinx.serialization.serializer
import java.io.File
import java.time.LocalDate


@ImplicitReflectionSerializer
@UseExperimental(InternalCoroutinesApi::class)
class FileAccessTest : StringSpec() {
    val columns: RowNormalizer = listOf("date", "channel", "delivered", "ret").zip(
        listOf((0 to 10), (10 to 84), (84 to 124), (124 to 164))
            .zip(
                listOf(
                    LocalDate::class,
                    String::class,
                    Float::class,
                    Float::class
                )
            )
    ).map { it by none<xform>() }.toTypedArray()
    val f20 = FixedRecordLengthFile("src/test/resources/caven20.fwf")
    val f4 = FixedRecordLengthFile("src/test/resources/caven4.fwf")

    init {
        "things"{
            val x = suspend {
                val c4 = columns reify f4
                val tmpName = File.createTempFile("fat4", ".fwf").absolutePath
                System.err.println("using " + tmpName)

                val columns1 = columns
                val toFwf2 = c4.toFwf2(tmpName)
                val value = RowBinMeta.RowBinMetaList(toFwf2)
                val jsonElement =
                    Json(Stable).toJson(RowBinMeta::class.serializer().list, value)
                System.err.println("" + jsonElement.toString())
                tmpName to jsonElement.toString()
            }()

            suspend{

            }
            println()
        }
    }

}
