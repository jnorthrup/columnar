package id.rejuve

import columnar.*
import columnar.IOMemento.*
import columnar.context.*
import io.kotlintest.specs.StringSpec

class DayJobTest : StringSpec() {

    val suffix = "_RD"
    val s = "/vol/aux/rejuve/rejuvesinceapril2019" + suffix + ".fwf"
    val coords = vZipWithNext(
            intArrayOf(
                    0, 11,
                    11, 15,
                    15, 25,
                    25, 40,
                    40, 60,
                    60, 82,
                    82, 103,
                    103, 108
            )
    )
    val drivers = vect0rOf(
            IoString,
            IoString,
            IoLocalDate,
            IoString,
            IoString,
            IoFloat,
            IoFloat,
            IoString
    )
    val names = vect0rOf(
            "SalesNo",    //        0
            "SalesAreaID",    //    1
            "date",    //           2
            "PluNo",    //          3
            "ItemName",    //       4
            "Quantity",    //       5
            "Amount",    //         6
            "TransMode"    //       7
    )


    val zip = names.zip(drivers)
    val columnar = Columnar.of(zip)
    val nioMMap = NioMMap(MappedFile(s), NioMMap.text(columnar.first))
    val fixedWidth: FixedWidth = fixedWidthOf(nioMMap, coords)
    val indexable = indexableOf(nioMMap, fixedWidth)

    init {
/*        "resample" {
            val fromFwf = fromFwf(RowMajor(), fixedWidth, indexable, nioMMap, columnar)

            (cursorOf(fromFwf)).let { curs ->
                System.err.println("record count=" + curs.first())
                val scalars = curs.scalars
                val pai2 = scalars α Scalar::pair
                System.err.println("" + pai2.toList())
                System.err.println("" + scalars[1].pair)
                System.err.println("" + curs.toList().first().reify)
                System.err.println("" + curs.toList().first().left)
            }
        }*/
//        "pivot" {
//
//            val curs = cursorOf(fromFwf(RowMajor(), fixedWidth, indexable, nioMMap, columnar))
//            curs.let { curs ->
//                System.err.println("record count=" + curs.first())
//                val scalars = curs.scalars
//            }
//            val pivot = curs[2, 1, 3, 5].resample(0).pivot(intArrayOf(0), intArrayOf(1, 2), intArrayOf(3))
//
//            System.err.println("" + pivot.scalars.size() + " columns ")
//            System.err.println("" + pivot.scalars.map { scalar: Scalar -> scalar.second }.toList())
//        }
        "pivot+group" {

            val curs = cursorOf(fromFwf(RowMajor(), fixedWidth, indexable, nioMMap, columnar))
            curs.let { curs ->
                System.err.println("record count=" + curs.first())
                val scalars = curs.scalars
            }
            val pivot =
                curs[2, 1, 3, 5].resample(0).pivot(intArrayOf(0), intArrayOf(1, 2), intArrayOf(3)).group(sortedSetOf(0))

            System.err.println("" + pivot.scalars.size() + " columns ")
            System.err.println("" + pivot.scalars.map { scalar: Scalar -> scalar.second }.toList())
            pivot.toSequence().map { pai2 ->  pai2.map {
                it.pair
            } }
        }

    }
}