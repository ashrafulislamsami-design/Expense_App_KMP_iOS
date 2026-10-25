package com.expenseapp.salarytracker.shared

import com.expenseapp.salarytracker.shared.model.MinorUnits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MinorUnitsSharedTest {

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
        var total = MinorUnits.ZERO
        val onePoisha = MinorUnits(1L)
        for (i in 1..10000) {
            total += onePoisha
        }
        assertEquals(10000L, total.amountInMinorUnits)
        assertEquals("৳100.00", total.format("৳"))
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
    fun testCompactFormatting() {
        assertEquals("৳1,250", MinorUnits(125000L).formatCompact("৳"))
        assertEquals("৳1,250.50", MinorUnits(125050L).formatCompact("৳"))
        assertEquals("৳0", MinorUnits.ZERO.formatCompact("৳"))
    }

    @Test
    fun testParseFromDecimalString() {
        assertEquals(MinorUnits(8000L), MinorUnits.fromDecimalString("80"))
        assertEquals(MinorUnits(8050L), MinorUnits.fromDecimalString("80.50"))
        assertEquals(MinorUnits(123456L), MinorUnits.fromDecimalString("1,234.56"))
        assertEquals(MinorUnits(-5025L), MinorUnits.fromDecimalString("-50.25"))
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
