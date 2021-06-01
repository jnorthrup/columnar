package exch

import cursors.Cursor
import cursors.at
import cursors.context.Scalar
import cursors.io.*
import exchg.PlotThing
import org.junit.Test
import vec.macros.*
import vec.util._l
import vec.util.path
import java.awt.event.ActionEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.nio.channels.FileChannel
import java.nio.file.Files
import javax.swing.AbstractAction
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.random.Random


/**
 * some curves that seem non-trivial for a learning challenge
 *
 * (sin(x)-cos ( (sqrt x)))+ (sqrt x)/2
![image](https://user-images.githubusercontent.com/73514/118878939-b7496800-b922-11eb-8710-eb2418231cc4.png)


(sin( tan( x)))+sqrt ( x)
![image](https://user-images.githubusercontent.com/73514/118879094-e1028f00-b922-11eb-93bc-12570cfd7e6d.png)

(4+sqrt( (x)))+(sin(x) - cos(x )-cos(x*2)-cos(x*3)-cos(x*4) )
![image](https://user-images.githubusercontent.com/73514/118879197-ff688a80-b922-11eb-84bf-e3f818399d55.png)


 */
enum class curveMotif(val motif: (Double) -> Double) {

    sinX(Math::sin),
    cosHalfX({ cos(it / 2.0) }),
/*
    //    tanX(Math::tan),
    byHalf({ it / 2.0 }),*/

    /** (sin(x)-cos ( (sqrt x)))+ (sqrt x)/2 */
    wobble1({ sqrt(it).let { (sin(it) - cos(it)) + it / 2.0 } }),

    /** (sin( tan( x)))+sqrt ( x) */
    gallup1({ (sin(tan(it))) + sqrt(it) }),
    heartbeat({ x ->
        4.01 + (sqrt((x))) + (sin(x) - cos(x) - cos(x * 2.0) - cos(x * 3.0) - cos(x * 4.0))
    }),
    sinc1({x->sin(x)/x}),
    sinc2({x->
        val x2 = x * kotlin.math.PI
        sin(x2)/x2})
    ;
}

class SeedTesting {
    @Test
    fun testSeed() {
        val days = 2000
        val assetCount = 10000
        val seeds = _l[3, 14, 11].map(::Random)
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
                Array(seeds[0].nextInt(1,
                    curveMotif.values().size)) { curveMotif.values()[seeds[0].nextInt(curveMotif.values().size)] }
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
                    dateCol.toDouble()) { acc, curveMotif ->
                    curveMotif.motif(acc)
                } t2 { Scalar(IOMemento.IoDouble, String.format("amt_%6d", dateCol)) }
            }
        }

        val path = "/tmp/myExchange".path

        exchCursors.writeISAM(path.toString())

        var keepalive = true

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
                        val rv: RowVec = displayCurs[assetRow]
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
