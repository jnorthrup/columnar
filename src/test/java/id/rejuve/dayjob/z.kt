package id.rejuve.dayjob

import columnar.*
import columnar.IOMemento.*
import columnar.context.Columnar
import columnar.context.FixedWidth
import columnar.context.NioMMap
import columnar.context.RowMajor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlin.coroutines.CoroutineContext

@ImplicitReflectionSerializer
@ExperimentalCoroutinesApi

suspend fun main() {
    val suffix = "_RD"
    val s = "/vol/aux/rejuve/rejuvesinceapril2019" +
            suffix +
            ".fwf"
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
    val names = vect0rOf("SalesNo", "SalesAreaID", "date", "PluNo", "ItemName", "Quantity", "Amount", "TransMode")


    val zip = names.zip(drivers)
    val columnar = Columnar.of(zip)
    val nioMMap = NioMMap(MappedFile(s), NioMMap.text(columnar.first))
    val fixedWidth: FixedWidth = fixedWidthOf(nioMMap, coords)
    val indexable = indexableOf(nioMMap, fixedWidth)

    val fromFwf = fromFwf(RowMajor(), fixedWidth, indexable, nioMMap, columnar)

    (cursorOf(fromFwf)).let { curs ->
        val scalars = curs.scalars
        val pai2 = scalars α Pai2<String, IOMemento>::pair
        System.err.println("" + pai2.toList())
        System.err.println("" + scalars[1].pair)
        System.err.println("" + (curs.toList() .first() .toList().map { it: Pai2<Any?, () -> CoroutineContext> -> it.pair }))    }

    (cursorOf(fromFwf))[2, 1, 3, 5].let { curs ->
        val scalars = curs.scalars
        val pai2 = scalars α Pai2<String, IOMemento>::pair
        System.err.println("" + pai2.toList())
        System.err.println("" + scalars[1].pair)
        System.err.println("" + (curs.toList() .first() .toList().map { it: Pai2<Any?, () -> CoroutineContext> -> it.pair }
                ))
    }
}

/*
val RowSeq.scalars
    get() = first() α { (_, b): Pai2<Any?, () -> CoroutineContext> ->
        val context = b()
        runBlocking<Pai2<String, IOMemento>>(context) {
            (coroutineContext[Arity.arityKey] as Scalar).let { (a, b): Scalar ->
                b!! t2 a
            }
        }
    }

typealias RowSeq = Sequence<RowVec>

@JvmName("vlike_RSequence_1")
operator fun RowSeq.get(vararg index: Int) = get(index)

@JvmName("vlike_RSequence_Iterable2")
operator fun RowSeq.get(indexes: Iterable<Int>) = this[indexes.toList().toIntArray()]

@JvmName("vlike_RSequence_IntArray3")
operator fun RowSeq.get(index: IntArray) = this.toList()[index].asSequence()

*/

