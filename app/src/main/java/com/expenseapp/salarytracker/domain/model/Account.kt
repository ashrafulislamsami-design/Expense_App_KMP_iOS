package com.expenseapp.salarytracker.domain.model

enum class AccountType(val displayName: String) {
    CASH("Cash"),
    BANK("Bank Account"),
    BKASH("bKash"),
    NAGAD("Nagad"),
    DEBIT_CARD("Debit Card"),
    CREDIT_CARD("Credit Card"),
    MOBILE_WALLET("Mobile Wallet"),
    CUSTOM("Custom Account");

    companion object {
        fun fromString(value: String): AccountType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CASH
        }
    }
}

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val initialBalance: MinorUnits = MinorUnits.ZERO,
    val currency: String = "BDT",
    val isArchived: Boolean = false,
    val sortOrder: Int = 0,
    val colorHex: String = "#10B981"
)
