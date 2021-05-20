package exch

import cursors.Cursor
import cursors.context.Scalar
import cursors.io.IOMemento
import cursors.io.ISAMCursor
import cursors.io.RowVec
import cursors.io.writeISAM
import exchg.PlotThing
import org.junit.Test
import vec.macros.get
import vec.macros.t2
import vec.macros.t3
import vec.util._l
import vec.util.path
import java.nio.channels.FileChannel
import java.nio.file.Files
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.random.Random


interface adjFun

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
    })
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
            (0..assetCount).map { Array(seeds[0].nextInt(1, curveMotif.values().size)) { curveMotif.values()[it] } }

        val exchCursors: Cursor = cursors.Cursor(assetCount) { date: Int ->

            val colDef = { Scalar(IOMemento.IoDouble, "amt_" + date) }
            //sometimes cheaper
            //  =  Scalar(IOMemento.IoDouble, "amt_" + date).`⟲`

            RowVec(assetCount) { asset: Int ->
                (launch[asset] + seeds[1].nextDouble(-performance[asset],
                    performance[asset]) + sqrt(vigor[asset] * date)) * alg[asset].fold(date.toDouble()) { acc, curveMotif ->
                    curveMotif.motif(acc)
                } t2 colDef
            }
        }

        val path = "/tmp/myExchange".path
        if(!Files.exists(path))
            exchCursors.writeISAM(path.toString())


        FileChannel.open(path)!!.use { fc ->

            val isamCursor = ISAMCursor(path, fc)

            val rv:RowVec = isamCursor[0]

            val fg=PlotThing()

            fg.caption= _l[launch[0] , performance[0] , vigor   ,alg[0].toString()].toString()
            fg.payload=rv
            fg.repaint()

        }
Thread.sleep(999999999999L)

    }
}
