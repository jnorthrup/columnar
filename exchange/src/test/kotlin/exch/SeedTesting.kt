package exch

import org.junit.*
import vec.util._a
import kotlin.math.*
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
enum class curveMotifs(val motif: (Double) -> Double) {

//    sinX(Math::sin),
//    cosX(Math::cos),
//    tanX(Math::tan),

    /**
    (sin(x)-cos ( (sqrt x)))+ (sqrt x)/2
     */
    wobble1({ sqrt(it).let { (sin(it) - cos(it)) + it / 2 } }),

    /**
     * (sin( tan( x)))+sqrt ( x)
     */
    gallup1(
        { (sin(tan(it))) + sqrt(it) }),
    /**(4+sqrt( (x)))+(sin(x) - cos(x )-cos(x*2)-cos(x*3)-cos(x*4) ) */
    heartbeat({x->
        4.01+ (sqrt( (x))) +(sin(x) - cos(x )-cos(x*2)-cos(x*3)-cos(x*4) )
        })
    ;

}

class SeedTesting {
    @Test
    fun testSeed() {
        val days = 2000

        val rlist = _a[3, 14, 11].map(::Random)// x,y and z

        val cache: Array<DoubleArray> = rlist.map { r ->
            (0..days).map {
                r.nextDouble()
            }.toDoubleArray()
        }.toTypedArray()

//        Cursor(days) {}

    }

}