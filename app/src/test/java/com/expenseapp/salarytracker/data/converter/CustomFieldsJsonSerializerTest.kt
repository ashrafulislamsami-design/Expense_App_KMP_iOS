package com.expenseapp.salarytracker.data.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomFieldsJsonSerializerTest {

    @Test
    fun testEmptyMapSerialization() {
        val serialized = CustomFieldsJsonSerializer.serialize(emptyMap())
        assertEquals("{}", serialized)

        val deserialized = CustomFieldsJsonSerializer.deserialize(serialized)
        assertTrue(deserialized.isEmpty())
    }

    @Test
    fun testCustomFieldsRoundTrip() {
        val original = mapOf(
            "Office/Personal" to "Personal",
            "Transport Type" to "Rickshaw",
            "Location" to "Dhanmondi, Dhaka",
            "Payment Method" to "bKash",
            "Notes" to "Tea with friends",
            "Custom Tags" to "friends,evening"
        )

        val serialized = CustomFieldsJsonSerializer.serialize(original)
        val deserialized = CustomFieldsJsonSerializer.deserialize(serialized)

        assertEquals(original, deserialized)
    }

    @Test
    fun testEscapingQuotesAndSpecialCharacters() {
        val original = mapOf(
            "Note" to "He said \"hello\" and left\nNew line here",
            "Path" to "C:\\Data\\Finance"
        )

        val serialized = CustomFieldsJsonSerializer.serialize(original)
        val deserialized = CustomFieldsJsonSerializer.deserialize(serialized)

        assertEquals(original["Note"], deserialized["Note"])
        assertEquals(original["Path"], deserialized["Path"])
    }
}
