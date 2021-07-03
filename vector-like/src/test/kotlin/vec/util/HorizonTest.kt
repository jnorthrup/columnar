package vec.util

import org.junit.Test
import vec.macros.*
import java.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
class HorizonTest {
    @Test
    fun horizonTest() {
        val v = Vect0r(5000) { x: Int -> x }
        val v2size = 100
        val v2: Vect0r<Int> = v2size t2 { x: Int ->
            horizon(x, v2size, v.size)
        }
        System.err.println(v2.toList())
        horizonRatioHunt(v2size, v.size)

    }

    fun horizonRatioHunt(viewSize: Int, modelSize: Int) = run {
        var acc = 0
        for (i in 0 until viewSize) {
            val horizon = horizon(i, viewSize, modelSize)//.also { logDebug { "$it,$acc" } }
            if ((horizon - acc) > 1) break
            acc = i

        }
        val res = _l[
                acc.toDouble() / viewSize.toDouble(),
                acc.toDouble() / modelSize.toDouble(),
                viewSize.toDouble() / modelSize.toDouble(),
        ]
        val res2 = res.let {
            res + _l[
                    it[0] * it[1],
                    it[1] *it[2],
                    acc.toDouble()*it[1],
            (viewSize-acc).toDouble()*it[1],
            (viewSize-acc).toDouble()*it[2],
            ]
        }
        logDebug { ("ratio ascends at($acc, $viewSize,${modelSize})=${res2}") }

        res2
    }

    @Test
    fun horizonTest2() {
        val v = Vect0r(50000) { x: Int -> x }
        val v2size = 1000
        val v2: Vect0r<Int> = v2size t2 { x: Int ->
            horizon(x, v2size, v.size)
        }
        System.err.println(v2[0 until 100].toList())
        horizonRatioHunt(v2size, v.size)
    }

    @Test
    fun horizonTest3() {
        val v = Vect0r(2034190) { x: Int -> x }
        val v2size = 5000
        val v2: Vect0r<Int> = v2size t2 { x: Int ->
            horizon(x, v2size, v.size)
        }
        System.err.println(v2[0 until 200].toList())
        System.err.println("...")
        System.err.println(v2[(v2.size - 50) until v2.size].toList())
        horizonRatioHunt(v2size, v.size)
    }

    @Test
    fun horizonTest4() {
        val v = Vect0r(3_000_000) { x: Int -> x }
        val v2size = 20
        val v2: Vect0r<Int> = v2size t2 { x: Int ->
            horizon(x, v2size, v.size)
        }
        System.err.println(v2[0 until 20].toList())
        horizonRatioHunt(v2size, v.size)
        horizonRatioHunt(100,kotlin.time.Duration.days(14).inWholeMinutes.toInt())
        horizonRatioHunt(200,kotlin.time.Duration.days(14).inWholeMinutes.toInt())
        horizonRatioHunt(300,kotlin.time.Duration.days(14).inWholeMinutes.toInt())
        horizonRatioHunt(500,kotlin.time.Duration.days(14).inWholeMinutes.toInt())
        horizonRatioHunt(3000,kotlin.time.Duration.days(14).inWholeMinutes.toInt())
    }
}
/*
wc -l `find -name *csv`|sort -n
        52 ./BTC/GYEN/final-BTC-GYEN-1m.csv
      5161 ./ETH/UAH/final-ETH-UAH-1m.csv
     25321 ./DOGE/BIDR/final-DOGE-BIDR-1m.csv
     51361 ./DOGE/RUB/final-DOGE-RUB-1m.csv
     65327 ./BNB/UAH/final-BNB-UAH-1m.csv
    105647 ./ADA/RUB/final-ADA-RUB-1m.csv
    125597 ./ADA/AUD/final-ADA-AUD-1m.csv
    125597 ./DOT/BRL/final-DOT-BRL-1m.csv
    135677 ./ADA/BRL/final-ADA-BRL-1m.csv
    135677 ./ADA/GBP/final-ADA-GBP-1m.csv
    135677 ./ADA/TRY/final-ADA-TRY-1m.csv
    135677 ./DOT/GBP/final-DOT-GBP-1m.csv
    160078 ./DOGE/GBP/final-DOGE-GBP-1m.csv
    160078 ./DOT/TRY/final-DOT-TRY-1m.csv
    167278 ./BTC/VAI/final-BTC-VAI-1m.csv
    170158 ./DOGE/AUD/final-DOGE-AUD-1m.csv
    170158 ./DOGE/BRL/final-DOGE-BRL-1m.csv
    175798 ./DOGE/EUR/final-DOGE-EUR-1m.csv
    175798 ./DOGE/TRY/final-DOGE-TRY-1m.csv
    246094 ./SUSHI/DOWNUSDT/final-SUSHI-DOWNUSDT-1m.csv
    247552 ./SUSHI/UPUSDT/final-SUSHI-UPUSDT-1m.csv
    286447 ./ADA/EUR/final-ADA-EUR-1m.csv
    296527 ./BNB/BRL/final-BNB-BRL-1m.csv
    306607 ./DOT/EUR/final-DOT-EUR-1m.csv
    306607 ./ETH/BRL/final-ETH-BRL-1m.csv
    331087 ./BTC/BRL/final-BTC-BRL-1m.csv
    375658 ./DOT/DOWNUSDT/final-DOT-DOWNUSDT-1m.csv
    377092 ./DOT/UPUSDT/final-DOT-UPUSDT-1m.csv
    391507 ./SUSHI/BNB/final-SUSHI-BNB-1m.csv
    391507 ./SUSHI/BTC/final-SUSHI-BTC-1m.csv
    391507 ./SUSHI/BUSD/final-SUSHI-BUSD-1m.csv
    391508 ./SUSHI/USDT/final-SUSHI-USDT-1m.csv
    397448 ./DOT/BIDR/final-DOT-BIDR-1m.csv
    409948 ./DOT/BNB/final-DOT-BNB-1m.csv
    409948 ./DOT/BTC/final-DOT-BTC-1m.csv
    409948 ./DOT/BUSD/final-DOT-BUSD-1m.csv
    409948 ./DOT/USDT/final-DOT-USDT-1m.csv
    421048 ./BNB/DAI/final-BNB-DAI-1m.csv
    421048 ./BTC/DAI/final-BTC-DAI-1m.csv
    421048 ./ETH/DAI/final-ETH-DAI-1m.csv
    426474 ./XTZ/UPUSDT/final-XTZ-UPUSDT-1m.csv
    426493 ./XTZ/DOWNUSDT/final-XTZ-DOWNUSDT-1m.csv
    426749 ./BNB/DOWNUSDT/final-BNB-DOWNUSDT-1m.csv
    427048 ./BNB/AUD/final-BNB-AUD-1m.csv
    428188 ./BNB/UPUSDT/final-BNB-UPUSDT-1m.csv
    436648 ./BTC/AUD/final-BTC-AUD-1m.csv
    436649 ./ETH/AUD/final-ETH-AUD-1m.csv
    438388 ./DOGE/BUSD/final-DOGE-BUSD-1m.csv
    456655 ./ADA/DOWNUSDT/final-ADA-DOWNUSDT-1m.csv
    458095 ./ADA/UPUSDT/final-ADA-UPUSDT-1m.csv
    462688 ./ETH/UPUSDT/final-ETH-UPUSDT-1m.csv
    462689 ./ETH/DOWNUSDT/final-ETH-DOWNUSDT-1m.csv
    481408 ./BNB/BIDR/final-BNB-BIDR-1m.csv
    481408 ./BTC/BIDR/final-BTC-BIDR-1m.csv
    481408 ./ETH/BIDR/final-ETH-BIDR-1m.csv
    489958 ./BTC/UAH/final-BTC-UAH-1m.csv
    496918 ./BNB/GBP/final-BNB-GBP-1m.csv
    496918 ./BTC/GBP/final-BTC-GBP-1m.csv
    496918 ./ETH/GBP/final-ETH-GBP-1m.csv
    532722 ./BNB/ZAR/final-BNB-ZAR-1m.csv
    532722 ./BTC/ZAR/final-BTC-ZAR-1m.csv
    532722 ./ETH/ZAR/final-ETH-ZAR-1m.csv
    548740 ./BTC/DOWNUSDT/final-BTC-DOWNUSDT-1m.csv
    548757 ./BTC/UPUSDT/final-BTC-UPUSDT-1m.csv
    587848 ./BNB/IDRT/final-BNB-IDRT-1m.csv
    587848 ./BTC/IDRT/final-BTC-IDRT-1m.csv
    682166 ./XTZ/BUSD/final-XTZ-BUSD-1m.csv
    738266 ./BNB/EUR/final-BNB-EUR-1m.csv
    738266 ./BTC/EUR/final-BTC-EUR-1m.csv
    738266 ./ETH/EUR/final-ETH-EUR-1m.csv
    751169 ./ETH/TRY/final-ETH-TRY-1m.csv
    758306 ./BNB/TRY/final-BNB-TRY-1m.csv
    758306 ./BTC/TRY/final-BTC-TRY-1m.csv
    784046 ./BNB/RUB/final-BNB-RUB-1m.csv
    784046 ./BTC/RUB/final-BTC-RUB-1m.csv
    784046 ./ETH/RUB/final-ETH-RUB-1m.csv
    800306 ./ADA/BUSD/final-ADA-BUSD-1m.csv
    839973 ./BTC/NGN/final-BTC-NGN-1m.csv
    844563 ./ETH/BUSD/final-ETH-BUSD-1m.csv
    883263 ./XTZ/BNB/final-XTZ-BNB-1m.csv
    883263 ./XTZ/BTC/final-XTZ-BTC-1m.csv
    883263 ./XTZ/USDT/final-XTZ-USDT-1m.csv
    890521 ./BTC/BUSD/final-BTC-BUSD-1m.csv
    890522 ./BNB/BUSD/final-BNB-BUSD-1m.csv
    999363 ./DOGE/BTC/final-DOGE-BTC-1m.csv
    999363 ./DOGE/USDT/final-DOGE-USDT-1m.csv
   1143182 ./ADA/USDC/final-ADA-USDC-1m.csv
   1163342 ./BNB/TUSD/final-BNB-TUSD-1m.csv
   1163343 ./BTC/TUSD/final-BTC-TUSD-1m.csv
   1163344 ./ETH/TUSD/final-ETH-TUSD-1m.csv
   1283940 ./ADA/TUSD/final-ADA-TUSD-1m.csv
   1289741 ./ETH/USDC/final-ETH-USDC-1m.csv
   1289751 ./BNB/USDC/final-BNB-USDC-1m.csv
   1289751 ./BTC/USDC/final-BTC-USDC-1m.csv
   1312200 ./BNB/PAX/final-BNB-PAX-1m.csv
   1312200 ./BTC/PAX/final-BTC-PAX-1m.csv
   1312202 ./ETH/PAX/final-ETH-PAX-1m.csv
   1636388 ./ADA/BNB/final-ADA-BNB-1m.csv
   1636388 ./ADA/USDT/final-ADA-USDT-1m.csv
   1832340 ./ADA/ETH/final-ADA-ETH-1m.csv
   1832341 ./ADA/BTC/final-ADA-BTC-1m.csv
   1867416 ./BNB/USDT/final-BNB-USDT-1m.csv
   1983630 ./ETH/USDT/final-ETH-USDT-1m.csv
   1983631 ./BTC/USDT/final-BTC-USDT-1m.csv
   1994848 ./BNB/ETH/final-BNB-ETH-1m.csv
   2033587 ./BNB/BTC/final-BNB-BTC-1m.csv
   2034190 ./ETH/BTC/final-ETH-BTC-1m.csv
  71438298 total

 */