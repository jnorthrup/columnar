package exch

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
    cosHalfX({ kotlin.math.cos(it / 2.0) }),
/*
    //    tanX(Math::tan),
    byHalf({ it / 2.0 }),*/

    /** (sin(x)-cos ( (sqrt x)))+ (sqrt x)/2 */
    wobble1({ kotlin.math.sqrt(it).let { (kotlin.math.sin(it) - kotlin.math.cos(it)) + it / 2.0 } }),

    /** (sin( tan( x)))+sqrt ( x) */
    gallup1({ (kotlin.math.sin(kotlin.math.tan(it))) + kotlin.math.sqrt(it) }),
    heartbeat({ x ->
        4.01 + (kotlin.math.sqrt((x))) + (kotlin.math.sin(x) - kotlin.math.cos(x) - kotlin.math.cos(x * 2.0) - kotlin.math.cos(
            x * 3.0
        ) - kotlin.math.cos(x * 4.0))
    }),
    sinc1({ x -> kotlin.math.sin(x) / x }),
    sinc2({ x ->
        val x2 = x * kotlin.math.PI
        kotlin.math.sin(x2) / x2
    })
    ;
}