package exch

import cursors.Cursor
import cursors.at
import cursors.context.Scalar
import cursors.get
import cursors.io.*
import cursors.macros.join
import cursors.unaryMinus
import exchg.PlotThing
import org.junit.Test
import vec.macros.*
import vec.util._a
import vec.util._l
import vec.util.path
import java.awt.event.ActionEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.nio.channels.FileChannel
import java.nio.file.Files
import javax.swing.AbstractAction
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random


class SeedTesting {
    val modelPoints = 2000


    private val scalar = Scalar(IOMemento.IoInt, "non")

    @Test
    fun variablecoil() {
        val seeds = _l[3, 14, 11].map(::Random)

        val assetCount = 250
        val (rand_x, rand_y: Random, rand_z: Random) = (seeds)

        var datapoints = 10000


        val assetLengths = Array(assetCount) {
            rand_x.nextInt(125, 445)
        }

        val arraySeeds = Array(assetCount) { y ->
            _a[
                    rand_y.nextDouble(1.0, 10.0),
                    rand_y.nextDouble(1.0, 10.0),
                    rand_y.nextDouble(1.0, 10.0)
            ] t2 _a[// 1/6 probability of at least one link
                    (rand_y.nextInt(-6 * max(1, y), y - 1)).takeIf { it >= 0 },
                    (rand_y.nextInt(-6 * max(1, y), y - 1)).takeIf { it >= 0 },
                    (rand_y.nextInt(-6 * max(1, y), y - 1)).takeIf { it >= 0 }
            ] t3 assetLengths[y]
        }

        val scalar1 = Scalar(
            IOMemento.IoDouble,
            "v"/*_${String.format("%05d", x)}*/
        ).`⟲`
        val scalar2 = Scalar(IOMemento.IoInt, "non").`⟲`
        lateinit var underlying: Cursor

        val solidBase: Cursor =
            Cursor(arraySeeds.size) { y: Int ->
                val (start, magnitude, frequency) = arraySeeds[y].first
                val alen = arraySeeds[y].third
                fun standingwave(x: Int) = start + sin(x.toDouble() / frequency) * magnitude
                fun relink(x: Int, y1: Int, d: Double): Double {
                    var d1 = d
                    arraySeeds[y1].second.forEach {
                        it?.also {
                            val pai21 = underlying at it
                            val pai2 = pai21[x]
                            val first = pai2.first
                            val d2 = first as Double
                            d1 += d2
                        }
                    }
/*
                    d1 = linkAdd?.let { d1 +  linkAdd) } ?: d1
                    d1 = linkSub?.let { d1 + (underlying at linkSub)[x].first as Double } ?: d1
                    d1 = linkMul?.let { d1 + (underlying at linkMul)[x].first as Double } ?: d1
*/
                    return d1
                }

/*                    linkAdd?.let { i: Int -> ((underlying at i)[x].first as? Double)?.plus(d1) }
//                        ?.also { v1 -> logDebug { "$d1 plussed $linkAdd /${((underlying at linkAdd)[x]).first} as $v1" } }
                        ?: d1  .let { d2 ->
                                linkSub?.let { i: Int -> ((underlying at i)[x].first as? Double)?.plus(-d2) }
//                                    ?.also { v2 -> logDebug { "$d2 minussed $linkSub /${((underlying at linkSub)[x]).first} to $v2" } }
                                    ?: d2
                            }
                            .let { d3 ->
                                linkMul?.let { i: Int -> ((underlying at i)[x].first as? Double)?.times(d3) }
//                                    ?.also { v3 -> logDebug { "$d3 mult $linkMul /${((underlying at linkMul)[x]).first} to $v3" } }
                                    ?: d3
                            }.also { v4 -> "returning $v4 originally $d1 " }*/

                RowVec(modelPoints) { x: Int ->
                    when {
                        x == 0 -> alen t2 { Scalar(IOMemento.IoInt, "ct") }
                        x <= alen -> {
                            val d = standingwave(x)
                            val relink = relink(x, y, d)
                            relink t2 scalar1
                        }
                        else -> Random.nextDouble() t2 scalar2

                    }
                }
            }
        underlying = solidBase

//        solidBase.showRandom()
        var assetRow = -1
        val fg = PlotThing()
        fg.addWindowListener(
            object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent) {
                    println("Closed")
                    e.window.dispose()
                    keepalive = false
                }
            })
        val displayCurs = solidBase
        fg.nextAction =
            object : AbstractAction() {
                override fun actionPerformed(p0: ActionEvent?) {
                    ++assetRow
                    "asset: ${
                        String.format(
                            "%05d",
                            assetRow
                        )
                    } ${arraySeeds[assetRow].first.toList()} ${arraySeeds[assetRow].second.toList()}".also {
                        fg.caption = it
                    }

                    val j = join(displayCurs[displayCurs.colIdx[-"ct"]])
                    val j1 = Cursor(1) { y: Int -> j at assetRow }
                    val supt = arraySeeds[assetRow].second.map {
                        it?.let { j at it } ?: RowVec(0) { x: Int -> 0.0 t2 Scalar(IOMemento.IoDouble).`⟲` }
                    }.toVect0r() as Cursor

                    fg.payload = combine(j1, supt)
                    fg.repaint()
                }
            }
        (fg.nextAction as AbstractAction).actionPerformed(ActionEvent(this, assetRow, toString()))
        while (keepalive) Thread.sleep(1000)
    }


    //            val  (tdp,startingY:Double,interval:Double)=   datapoints t2 rand_y.nextDouble() t3 rand_z.nextDouble(0.1,7.0)
//            RowVec(500){x->
//
//            }
//        }
//
//        Cursor(assetCount){
//
//        }

    var keepalive = true

    @Test
    fun testSeed() {
        val seeds = _l[3, 14, 11].map(::Random)
        val assetCount = 10000
        val (
                /**initial*/
            launch,
                /**y+-*/
            performance,
                /**x+=sqrt(it)*/
            vigor,
        ) = seeds.map { r -> DoubleArray(assetCount) { r.nextDouble() } }

        val alg =
            (0 until assetCount).map {
                Array(
                    seeds[0].nextInt(
                        1,
                        curveMotif.values().size
                    )
                ) { curveMotif.values()[seeds[0].nextInt(curveMotif.values().size)] }
            }

        val exchCursors: Cursor = cursors.Cursor(assetCount) { assetRow: Int ->

            //sometimes cheaper
            //  =  Scalar(IOMemento.IoDouble, "amt_" + y).`⟲`

            val assetLaunch = launch[assetRow]
            val assetPerformance = performance[assetRow]
            val assetVigor = vigor[assetRow]
            val arrayOfCurveMotifs = alg[assetRow]

            RowVec(assetCount) { dateCol: Int ->
                (assetLaunch + seeds[1].nextDouble() * assetPerformance + sqrt(assetVigor * dateCol)) * arrayOfCurveMotifs.fold(
                    dateCol.toDouble()
                ) { acc, curveMotif ->
                    curveMotif.motif(acc)
                } t2 { Scalar(IOMemento.IoDouble, String.format("amt_%6d", dateCol)) }
            }
        }

        val path = "/tmp/myExchange".path

        exchCursors.writeISAM(path.toString())


        val classedName = "/tmp/myExchange5.isam"
        FileChannel.open(path)!!.use { fc ->
            val isamCursor = ISAMCursor(path, fc)
            if (!Files.exists("/tmp/myExchange.csv".path))
                isamCursor.writeCSV("/tmp/myExchange.csv")

            val fiveClasses = Cursor(isamCursor.size) { y: Int ->
                val baseRowVec = isamCursor at y
                val influence by lazy { seeds[2].nextDouble() }
                val classRowVec by lazy { isamCursor.at(y.rem(5)) }

                (when {
                    y > 5 -> {
                        baseRowVec.left.mapIndexedToList { i, any ->
                            (any as Double) +
                                    (classRowVec.left[i] as Double) * (influence)
                        }.toVect0r()
                    }
                    else -> baseRowVec.left
                } as Vect0r<*>).zip(baseRowVec.right)
            }
            //write multiclass csv
            if (!Files.exists(classedName.path)) fiveClasses.writeISAM(classedName)
        }
        val fg = PlotThing()
        var assetRow = 0
        fg.addWindowListener(
            object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent) {
                    println("Closed")
                    e.window.dispose()
                    keepalive = false
                }
            })


        FileChannel.open(classedName.path)!!.use {
            val displayCurs = ISAMCursor(classedName.path, it)
            fg.nextAction =
                object : AbstractAction() {
                    override fun actionPerformed(p0: ActionEvent?) {
                        val rv: RowVec = displayCurs at assetRow
                        "asset: ${String.format("%05d", assetRow)} ${
                            _l[launch[assetRow],
                                    performance[assetRow],
                                    vigor[assetRow],
                                    alg[assetRow].toList().toString()]
                        }".also { fg.caption = it }

                        fg.payload = when {
                            assetRow < 5 -> Cursor(1) { y -> rv }
                            else -> combine(Cursor(1) { y -> rv }, Cursor(1) { y -> displayCurs at assetRow.rem(5) })
                        }
                        fg.repaint()
                        ++assetRow
                    }
                }
            (fg.nextAction as AbstractAction).actionPerformed(ActionEvent(this, assetRow, toString()))
            while (keepalive) Thread.sleep(1000)
        }
    }
}
