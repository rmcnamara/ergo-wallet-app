package org.ergoplatform

import java.math.RoundingMode

const val nanoPowerOfTen = 9

class ErgoAmount(val nanoErgs: Long) {

    companion object {
        val ZERO = ErgoAmount(0)
    }

    constructor(ergString: String) : this(
        if (ergString.isBlank()) 0 else ergString.toBigDecimal()
            .movePointRight(nanoPowerOfTen).longValueExact()
    )

    override fun toString(): String {
        return nanoErgs.toBigDecimal().movePointLeft(nanoPowerOfTen).toPlainString()
    }

    /**
     * converts the amount to a string rounded to given number of decimals places
     */
    fun toStringRoundToDecimals(numDecimals: Int = 4): String {
        return nanoErgs.toBigDecimal().movePointLeft(nanoPowerOfTen)
            .setScale(numDecimals, RoundingMode.HALF_UP)
            .toPlainString()
    }

    fun toStringTrimTrailingZeros(): String {
        return toString().trimEnd('0').trimEnd('.')
    }

    /**
     * @return double amount, only for representation purposes because double has floating point issues
     */
    fun toDouble(): Double {
        val microErgs = nanoErgs / (1000L * 100L)
        val ergs = microErgs.toDouble() / 10000.0
        return ergs
    }

    operator fun plus(other: ErgoAmount): ErgoAmount {
        return ErgoAmount(this.nanoErgs + other.nanoErgs)
    }

    operator fun minus(other: ErgoAmount): ErgoAmount {
        return ErgoAmount(this.nanoErgs - other.nanoErgs)
    }
}

fun String.toErgoAmount(): ErgoAmount? {
    try {
        return ErgoAmount(this)
    } catch (t: Throwable) {
        return null
    }
}