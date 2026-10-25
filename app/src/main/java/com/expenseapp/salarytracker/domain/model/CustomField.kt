package com.expenseapp.salarytracker.domain.model

enum class CustomFieldType {
    TEXT,
    NUMBER,
    BOOLEAN,
    DATE,
    CHOICE;

    companion object {
        fun fromString(value: String?): CustomFieldType {
            if (value == null) return TEXT
            return entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: TEXT
        }
    }
}

data class CustomField(
    val id: Long = 0,
    val name: String,
    val type: CustomFieldType,
    val optionsJson: List<String> = emptyList(), // For CHOICE type
    val isRequired: Boolean = false
)
