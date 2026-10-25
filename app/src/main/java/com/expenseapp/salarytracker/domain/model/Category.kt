package com.expenseapp.salarytracker.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val iconName: String = "category",
    val colorHex: String = "#3B82F6",
    val parentCategoryId: Long? = null,
    val isArchived: Boolean = false,
    val sortOrder: Int = 0
) {
    companion object {
        val DEFAULT_EXPENSE_CATEGORIES = listOf(
            Category(name = "Food", iconName = "restaurant", colorHex = "#F59E0B", sortOrder = 1),
            Category(name = "Transport", iconName = "directions_bus", colorHex = "#3B82F6", sortOrder = 2),
            Category(name = "Groceries", iconName = "shopping_cart", colorHex = "#10B981", sortOrder = 3),
            Category(name = "Rent", iconName = "home", colorHex = "#8B5CF6", sortOrder = 4),
            Category(name = "Utilities", iconName = "bolt", colorHex = "#EF4444", sortOrder = 5),
            Category(name = "Mobile", iconName = "smartphone", colorHex = "#06B6D4", sortOrder = 6),
            Category(name = "Internet", iconName = "wifi", colorHex = "#6366F1", sortOrder = 7),
            Category(name = "Shopping", iconName = "shopping_bag", colorHex = "#EC4899", sortOrder = 8),
            Category(name = "Entertainment", iconName = "movie", colorHex = "#F97316", sortOrder = 9),
            Category(name = "Health", iconName = "medical_services", colorHex = "#14B8A6", sortOrder = 10),
            Category(name = "Education", iconName = "school", colorHex = "#84CC16", sortOrder = 11),
            Category(name = "Office", iconName = "work", colorHex = "#64748B", sortOrder = 12),
            Category(name = "Family", iconName = "diversity_1", colorHex = "#D946EF", sortOrder = 13),
            Category(name = "Donation", iconName = "volunteer_activism", colorHex = "#E11D48", sortOrder = 14),
            Category(name = "Savings", iconName = "savings", colorHex = "#059669", sortOrder = 15),
            Category(name = "Other Expense", iconName = "more_horiz", colorHex = "#94A3B8", sortOrder = 16)
        )

        val DEFAULT_INCOME_CATEGORIES = listOf(
            Category(name = "Salary", iconName = "payments", colorHex = "#10B981", sortOrder = 17),
            Category(name = "Consulting / Freelance", iconName = "work", colorHex = "#06B6D4", sortOrder = 18),
            Category(name = "Business", iconName = "business", colorHex = "#3B82F6", sortOrder = 19),
            Category(name = "Investments", iconName = "trending_up", colorHex = "#8B5CF6", sortOrder = 20),
            Category(name = "Bonus / Gift", iconName = "card_giftcard", colorHex = "#F59E0B", sortOrder = 21),
            Category(name = "Rental Income", iconName = "real_estate_agent", colorHex = "#14B8A6", sortOrder = 22),
            Category(name = "Cashback / Refund", iconName = "currency_exchange", colorHex = "#059669", sortOrder = 23),
            Category(name = "Other Income", iconName = "payments", colorHex = "#16A34A", sortOrder = 24)
        )

        val DEFAULT_DEBT_CATEGORIES = listOf(
            Category(name = "Loan Received", iconName = "account_balance_wallet", colorHex = "#E11D48", sortOrder = 25),
            Category(name = "Debt Repayment", iconName = "credit_card", colorHex = "#F97316", sortOrder = 26),
            Category(name = "Borrowed Money", iconName = "handshake", colorHex = "#8B5CF6", sortOrder = 27),
            Category(name = "Salary Advance", iconName = "receipt_long", colorHex = "#DC2626", sortOrder = 28),
            Category(name = "Loan Given", iconName = "payments", colorHex = "#0284C7", sortOrder = 29),
            Category(name = "Debt", iconName = "credit_card", colorHex = "#DC2626", sortOrder = 30)
        )

        val DEFAULT_CATEGORIES = DEFAULT_EXPENSE_CATEGORIES + DEFAULT_INCOME_CATEGORIES + DEFAULT_DEBT_CATEGORIES + listOf(
            Category(name = "Salary / Income", iconName = "payments", colorHex = "#16A34A", sortOrder = 31),
            Category(name = "Other", iconName = "more_horiz", colorHex = "#94A3B8", sortOrder = 32)
        )
    }
}
