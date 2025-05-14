//package cursors
//
//import cursors.context.Scalar
//import cursors.io.IOMemento
//import cursors.io.RowVec
//import kotlin.test.*
//import java.util.EnumMap
//import java.time.Instant // Not used in this specific test, but good for KLine context
//import cursors.context.Scalar.Companion.Scalar // For factory
//import cursors.context.`⟲` // For context provider
//import cursors.io.colIdx
//import cursors.io.scalars // Extension property for Cursor
//import vec.macros.Vect02_.left
//
//// Define an inline class for KLine data for type safety and minimal overhead
//@JvmInline
//value class KLine(val data: Vect0r<Any>) {
//  // Accessors based on KLineColumn order
//  inline val openTime: Long get() = data[KLineColumn.OPEN_TIME.ordinal] as Long
//  inline val open: Double get() = data[KLineColumn.OPEN.ordinal] as Double
//  inline val high: Double get() = data[KLineColumn.HIGH.ordinal] as Double
//  inline val val low: Double get() = data[KLineColumn.LOW.ordinal] as Double
//  inline val close: Double get() = data[KLineColumn.CLOSE.ordinal] as Double
//  inline val volume: Double get() = data[KLineColumn.VOLUME.ordinal] as Double
//  inline val closeTime: Long get() = data[KLineColumn.CLOSE_TIME.ordinal] as Long
//  inline val quoteAssetVolume: Double get() = data[KLineColumn.QUOTE_ASSET_VOLUME.ordinal] as Double
//  inline val numberOfTrades: Int get() = data[KLineColumn.NUMBER_OF_TRADES.ordinal] as Int
//  inline val takerBuyBaseAssetVolume: Double get() = data[KLineColumn.TAKER_BUY_BASE_ASSET_VOLUME.ordinal] as Double
//  inline val takerBuyQuoteAssetVolume: Double get() = data[KLineColumn.TAKER_BUY_QUOTE_ASSET_VOLUME.ordinal] as Double
//
//  val isBullish: Boolean get() = close > open
//  val isBearish: Boolean get() = close < open
//  val range: Double get() = high - low
//}
//
//// Enum to model KLine columns for type-safe access and metadata
//enum class KLineColumn(val colName: String, val type: IOMemento) {
//  OPEN_TIME("openTime", IOMemento.IoLong),
//  OPEN("open", IOMemento.IoDouble),
//  HIGH("high", IOMemento.IoDouble),
//  LOW("low", IOMemento.IoDouble),
//  CLOSE("close", IOMemento.IoDouble),
//  VOLUME("volume", IOMemento.IoDouble),
//  CLOSE_TIME("closeTime", IOMemento.IoLong),
//  QUOTE_ASSET_VOLUME("quoteAssetVolume", IOMemento.IoDouble),
//  NUMBER_OF_TRADES("numberOfTrades", IOMemento.IoInt),
//  TAKER_BUY_BASE_ASSET_VOLUME("takerBuyBaseAssetVolume", IOMemento.IoDouble),
//  TAKER_BUY_QUOTE_ASSET_VOLUME("takerBuyQuoteAssetVolume", IOMemento.IoDouble),
//  IGNORE("ignore", IOMemento.IoString); // Typically present in Binance data
//
//  fun toScalar(): Scalar = Scalar(type, colName)
//}
//
//// Simulate a columnar ISAM builder
//class ColumnarKLineStore {
//  // scar: Changed to EnumMap for potentially better performance with enum keys.
//  private val columns = EnumMap<KLineColumn, MutableList<Any>>(KLineColumn::class.java)
//
//  fun addKLine(parsedCsvRow: List<String>) {
//    // Simplified parsing, assumes CSV maps directly to KLineColumn order
//    if (parsedCsvRow.size < KLineColumn.entries.size) return // Skip malformed rows
//
//    KLineColumn.entries.forEachIndexed { index, kLineCol ->
//      val list = columns.getOrPut(kLineCol) { mutableListOf() }
//      val valueStr = parsedCsvRow[index]
//      // scar: Added IoInt case.
//      list.add(when (kLineCol.type) {
//        IOMemento.IoLong -> valueStr.toLong()
//        IOMemento.IoDouble -> valueStr.toDouble()
//        IOMemento.IoInt -> valueStr.toInt()
//        IOMemento.IoString -> valueStr
//        else -> valueStr // Fallback for other types, though KLineColumn defines types explicitly
//      })
//    }
//  }
//
//  // Creates a Cursor view over the columnar data
//  fun getCursor(): Cursor {
//    if (columns.isEmpty() || columns.values.any { it.isEmpty() }) {
//      return SimpleCursor(Vect0r(0) { Scalar(IOMemento.IoNothing, "empty_kline_store") }, Vect0r(0) { Vect0r(0){""} })
//    }
//
//    val numRows = columns.values.first().size
//    val scalars = Vect0r(KLineColumn.entries.size) { i -> KLineColumn.entries[i].toScalar() }
//
//    // Create a Vect0r of RowVecs. Each RowVec represents a row, but internally
//    // it holds references to the columnar data and the column index.
//    val rows = Vect0r(numRows) { rowIndex ->
//      RowVec(Vect0r(KLineColumn.entries.size) { colIndex ->
//        // This is the core of the columnar view: accessing data by column then row
//        val klineCol = KLineColumn.entries[colIndex]
//        val columnData = columns[klineCol] ?: error("Column data not found for ${klineCol.colName}")
//        columnData[rowIndex]
//      })
//    }
//
//    return SimpleCursor(scalars, rows)
//  }
//}
//
//// DSEL Extensions for Cursor related to KLines
//fun Cursor.kline(index: Int): KLine = KLine(this.at(index).left)
//
//inline fun <reified T: Any> Cursor.klineColumn(column: KLineColumn): Vect0r<T> {
//    val colIdx = this.scalars.indexOfFirst { it.name == column.colName }
//    if (colIdx == -1) throw IllegalArgumentException("Column ${column.colName} not found in cursor")
//    return Vect0r(this.size) { rowIndex ->
//        (this.at(rowIndex)[colIdx].first as T)
//    }
//}
//
//val Cursor.openTimes: Vect0r<Long> get() = klineColumn(KLineColumn.OPEN_TIME)
//val Cursor.opens: Vect0r<Double> get() = klineColumn(KLineColumn.OPEN)
//val Cursor.highs: Vect0r<Double> get() = klineColumn(KLineColumn.HIGH)
//val Cursor.lows: Vect0r<Double> get() = klineColumn(KLineColumn.LOW)
//val Cursor.closes: Vect0r<Double> get() = klineColumn(KLineColumn.CLOSE)
//val Cursor.volumes: Vect0r<Double> get() = klineColumn(KLineColumn.VOLUME)
//val Cursor.numTrades: Vect0r<Int> get() = klineColumn(KLineColumn.NUMBER_OF_TRADES)
//
//fun Cursor.filterKLines(predicate: (KLine) -> Boolean): Cursor {
//    val matchingRowIndices = mutableListOf<Int>()
//    for (i in 0 until this.size) {
//        if (predicate(this.kline(i))) {
//            matchingRowIndices.add(i)
//        }
//    }
//    if (matchingRowIndices.isEmpty()) return SimpleCursor(this.scalars, Vect0r(0){Vect0r(0){""}})
//    return this[matchingRowIndices.toIntArray()]
//}
//
//class DatabinanceKlineIsamTest {
//
//  @Test
//  fun `should process KLine CSV data into columnar store and access via cursor`() {
//    val csvData = listOf(
//      "1672531200000,0.1,0.12,0.09,0.11,1000.0,1672531259999,100.11,10,500.0,50.05,ignore",
//      "1672531260000,0.11,0.15,0.10,0.14,1500.0,1672531319999,150.14,15,700.0,70.14,ignore",
//      "1672531320000,0.14,0.14,0.12,0.13,1200.0,1672531379999,120.13,12,600.0,60.13,ignore"
//    )
//
//    val store = ColumnarKLineStore()
//    csvData.forEach { row ->
//      store.addKLine(row.split(','))
//    }
//
//    val cursor = store.getCursor()
//
//    // Test basic cursor properties
//    assertEquals(expected = 3, actual = cursor.size, message = "Cursor should have 3 rows")
//    assertEquals(expected = KLineColumn.entries.size, actual = cursor.scalars.size, "Cursor should have ${KLineColumn.entries.size} columns")
//
//    // Verify first row, close price
//    val firstRowVec = cursor.at(0)
//    val closePriceFirstRow = firstRowVec[KLineColumn.CLOSE.ordinal].first
//    assertEquals(expected = 0.11, actual = closePriceFirstRow as Double, message = "Close price of first KLine")
//
//    // Verify second row, open time via KLine value class accessor
//    val klineDataSecondRow = KLine(cursor.at(1).left) // Wrap row data in KLine value class
//    val openTimeSecondRow = klineDataSecondRow.openTime
//    assertEquals(expected = 1672531260000L, actual = openTimeSecondRow, message = "Open time of second KLine")
//    assertTrue(klineDataSecondRow.isBullish, "Second KLine should be bullish")
//
//    // Verify third row, volume
//    val volumeThirdRow = (cursor.at(2)[KLineColumn.VOLUME.ordinal].first as Double)
//    assertEquals(expected = 1200.0, actual = volumeThirdRow, message = "Volume of third KLine")
//    assertEquals(0.02, cursor.kline(2).range, 1e-9, "Range of third KLine")
//
//    // Verify scalar metadata
//    // scar: Scalar is Pai2<TypeMemento, String?>, .second is name, .first is type
//    assertEquals(expected = "volume", actual = cursor.scalars[KLineColumn.VOLUME.ordinal].second)
//    assertEquals(expected = IOMemento.IoDouble, actual = cursor.scalars[KLineColumn.VOLUME.ordinal].first)
//    assertEquals(expected = "numberOfTrades", actual = cursor.scalars[KLineColumn.NUMBER_OF_TRADES.ordinal].second)
//    assertEquals(expected = IOMemento.IoInt, actual = cursor.scalars[KLineColumn.NUMBER_OF_TRADES.ordinal].first)
//
//    // Demonstrate DSEL extensions
//    assertEquals(0.1, cursor.opens[0], "DSEL: First open price")
//    assertEquals(0.14, cursor.closes[1], "DSEL: Second close price")
//    assertEquals(10, cursor.numTrades[0], "DSEL: Number of trades in first KLine")
//    assertEquals(1672531200000L, cursor.kline(0).openTime, "DSEL: Open time of first KLine object")
//
//    val highVolumeKlines = cursor.filterKLines { it.volume > 1000.0 }
//    assertEquals(2, highVolumeKlines.size, "DSEL: Should be 2 KLines with volume > 1000")
//    assertEquals(0.14, highVolumeKlines.kline(0).close, "DSEL: Close of first high-volume KLine")
//    assertEquals(0.13, highVolumeKlines.kline(1).close, "DSEL: Close of second high-volume KLine")
//  }
//}
