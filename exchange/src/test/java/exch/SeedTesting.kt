package exch

import cursors.Cursor
import cursors.at
import cursors.context.Scalar.Companion.Scalar
import cursors.io.*
import cursors.mirror
import exchg.PlotThing
import org.junit.Test
import vec.macros.*
import vec.macros.V3ct0r_.left
import vec.macros.V3ct0r_.mid
import vec.macros.V3ct0r_.right
import vec.macros.Vect02_.left
import vec.macros.Vect02_.right
import vec.util._a
import vec.util._l
import vec.util._v
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
    private val scalar = Scalar(IOMemento.IoInt, "non")


    /**
     * if this is true then the mode of the swing gui plotter shows each asset singularly as red line, accompanied by
     * the potential 3 dependencies 1 link deep
     *
     * when false this shows all the assets stacked
     */
    val showinfluence = false

    /**
     * this produces sin waves
     *
     * each of these "assets" has 0-3 random links to previous rows.   the asset in question is additive of the linked
     * assets, which may themselves be linked.
     *
     * the "standing wave" and "relink" lambda function
     * handle the instantiation magic of defining functions on an inner scope that has not yet been populated.
     *
     * there is some instantiation magic to
     */
    @Test
    fun variablecoil() {
        val seeds = _l[3, 14, 11].map(::Random)
        val pixels = 1000
        val datapoints = 20000

        val assetCount = 10
        val (rand_x, rand_y: Random, rand_z: Random) = (seeds)


        val maxX = (datapoints * 0.80).toInt()


        val arraySeeds: V3ct0r<DoubleArray, Array<Int?>, Int> = Array(assetCount) { y ->
            _a[
                    rand_y.nextDouble(-1.0, 40.0),
                    rand_y.nextDouble(-1.0, 40.0),
                    rand_y.nextDouble(-1.0, 40.0)
            ] t2 _a[// 1/6 probability of at least one link
                    (rand_y.nextInt(-2 * max(1, y), y - 1)).takeIf { it >= 0 },
                    (rand_y.nextInt(-2 * max(1, y), y - 1)).takeIf { it >= 0 },
                    (rand_y.nextInt(-2 * max(1, y), y - 1)).takeIf { it >= 0 }
            ] t3 rand_x.nextInt(19001, datapoints)
        }.toVect0r()

        val scalar1 = Scalar(
            IOMemento.IoDouble,
            "v"/*_${String.format("%05d", x)}*/
        ).`⟲`

        val scalar2 = Scalar(IOMemento.IoDouble, "non").`⟲`
        lateinit var underlying: Cursor

        val c1: Cursor = (arraySeeds.size) t2 { y: Int ->
            val (va, vb, assetLengths) = arraySeeds.left t2 arraySeeds.mid t3 arraySeeds.right
            val (start, magnitude, frequency) = va[y]
            fun standingwave(x: Int) = start + sin(x.toDouble() / frequency) * magnitude

            RowVec(datapoints) { x: Int ->
                fun relink(y1: Int, d: Double): Double {
                    var d1 = d
                    vb[y1].forEach {
                        it?.also { d1 += ((underlying at it)[x]).first as Double }
                    }
                    return d1
                }
                when {
                    x > assetLengths[y] -> Random.nextDouble() t2 scalar2
                    else -> relink(y, standingwave(x)) t2 scalar1
                }
            }
        }
        underlying = c1
        var assetRow = -1
        val fg = PlotThing()
        fg.addWindowListener(closer)
        val displayCurs = c1
        if (!showinfluence) {
            fg.nextAction = object : AbstractAction() {
                override fun actionPerformed(p0: ActionEvent?) {
                    val yspan = 1000.0
                    val yincrement = 2.5// assetCount.toDouble()/yspan
                    fg.payload = Cursor(assetCount) { rownum: Int ->
                        val ypos = yincrement * (1 + rownum) - 0
                        (displayCurs at rownum).let { (a, b) ->
                            a t2 { x: Int ->
                                b(x).let { (a1, sc) -> ((a1 as? Double)?.plus(ypos - 50) ?: a1) t2 sc }
                            }
                        }
                    }//.mirror()
                    fg.repaint()
                }
            }
        } else {
            fg.nextAction = object : AbstractAction() {
                override fun actionPerformed(p0: ActionEvent?) {
                    ++assetRow
                    val tripl3 = arraySeeds[assetRow]
                    "asset: ${
                        String.format(
                            "%05d",
                            assetRow
                        )
                    } ${tripl3.first.toList()} ${tripl3.second.toList()}".also {
                        fg.caption = it
                    }

                    val j1 = _v[displayCurs at assetRow]
                    val supt = tripl3.second.mapNotNull {
                        it?.let { displayCurs at it }
                        //?: RowVec(0) { x: Int -> 0.0 t2 Scalar(IOMemento.IoDouble).`⟲` }
                    }.toVect0r()

                    val combine = combine(j1, supt)
                    val inverted = combine.mirror() /*Cursor(combine.first) { y: Int ->
                            val rv = combine at y
                            RowVec(rv.first) { x: Int ->
                                rv.second(pixels - x)
                            }
                        }*/
                    fg.payload = inverted

                    fg.repaint()
                }
            }
        }
        (fg.nextAction as AbstractAction).actionPerformed(ActionEvent(this, assetRow, toString()))
        while (keepalive) Thread.sleep(1000)
    }


    /**
     * testSeed was an initial attempt at simulating an exchange of assets which are plot functions with perturbance
     * and parental link relationships.
     *
     * the initial goal was to write a serialization to isam and csv
     *
     * the FunkyGui class was added to view the data one asset at a time
     *
     * the data ppears good enough to train a system on one exchange to predict momentum, and to have an unending
     * source of new exchanges to prove model convergences of the generalized rules which are an artist's
     * tunable approximation of how crypto in particular has asset correlation based on marktecap trickle-down
     *
     */
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

        val exchCursors: Cursor = Cursor(assetCount) { assetRow: Int ->
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

                val o: Vect0r<CellMeta> = baseRowVec.right
                (when {
                    y > 5 -> {
                        baseRowVec.left.mapIndexedToList { i, any ->
                            (any as Double) +
                                    (classRowVec.left[i] as Double) * (influence)
                        }.toVect0r()
                    }
                    else -> baseRowVec.left
                } as Vect0r<Any?>).zip(o)
            }
            //write multiclass csv
            if (!Files.exists(classedName.path)) fiveClasses.writeISAM(classedName)
        }
        val fg = PlotThing()
        var assetRow = 0
        fg.addWindowListener(closer)


        FileChannel.open(classedName.path)!!.use { fileChannel ->
            val displayCurs = ISAMCursor(classedName.path, fileChannel)
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
                            assetRow < 5 -> _v[rv]
                            else -> combine(_v[rv], _v[displayCurs at (assetRow % 5)])
                        }
                        fg.repaint()
                        ++assetRow
                    }
                }
            (fg.nextAction as AbstractAction).actionPerformed(ActionEvent(this, assetRow, toString()))
            while (keepalive) Thread.sleep(1000)
        }
    }

    var keepalive = true
    val closer = object : WindowAdapter() {
        override fun windowClosing(e: WindowEvent) {
            println("Closed")
            e.window.dispose()
            keepalive = false
        }
    }
}
