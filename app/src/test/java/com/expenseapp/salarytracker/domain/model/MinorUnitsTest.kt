package com.expenseapp.salarytracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MinorUnitsTest {

    @Test
    fun testExactAdditionAndSubtraction() {
        val a = MinorUnits(1050L) // ৳10.50
        val b = MinorUnits(2025L) // ৳20.25

        val sum = a + b
        assertEquals(3075L, sum.amountInMinorUnits)

        val diff = b - a
        assertEquals(975L, diff.amountInMinorUnits)
    }

    @Test
    fun testNoLossOfPrecisionOverLargeAdditions() {
        // Adding 1 poisha (1 minor unit) 10,000 times must equal exactly 10,000L (৳100.00)
        var total = MinorUnits.ZERO
        val onePoisha = MinorUnits(1L)
        for (i in 1..10000) {
            total += onePoisha
        }
        assertEquals(10000L, total.amountInMinorUnits)
        assertEquals(BigDecimal("100.00"), total.toBigDecimal(2))
    }

    @Test
    fun testFormatting() {
        val amount = MinorUnits(125050L) // ৳1,250.50
        assertEquals("৳1,250.50", amount.format("৳"))

        val negative = MinorUnits(-5000L) // -৳50.00
        assertEquals("-৳50.00", negative.format("৳"))

        val zero = MinorUnits(0L)
        assertEquals("৳0.00", zero.format("৳"))
    }

    @Test
    fun testParseFromDecimalString() {
        assertEquals(MinorUnits(8000L), MinorUnits.fromDecimalString("80"))
        assertEquals(MinorUnits(8050L), MinorUnits.fromDecimalString("80.50"))
        assertEquals(MinorUnits(123456L), MinorUnits.fromDecimalString("1,234.56"))
        assertEquals(MinorUnits.ZERO, MinorUnits.fromDecimalString(""))
    }

    @Test
    fun testComparisons() {
        val a = MinorUnits(500L)
        val b = MinorUnits(1000L)
        assertTrue(a < b)
        assertFalse(a > b)
        assertTrue(MinorUnits(100L).isPositive())
        assertTrue(MinorUnits(-100L).isNegative())
        assertTrue(MinorUnits.ZERO.isZero())
    }
}
