package org.bereft

interface MarketData {
    val open: Double
    val high: Double
    val low: Double
    val close: Double
    val volume: Double
    val closeTime: Long
}