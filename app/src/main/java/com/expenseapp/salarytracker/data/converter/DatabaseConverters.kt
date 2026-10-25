package com.expenseapp.salarytracker.data.converter

import androidx.room.TypeConverter
import com.expenseapp.salarytracker.domain.model.AccountType
import com.expenseapp.salarytracker.domain.model.BudgetPeriod
import com.expenseapp.salarytracker.domain.model.CustomFieldType
import com.expenseapp.salarytracker.domain.model.GoalType
import com.expenseapp.salarytracker.domain.model.ReportingCycleMode
import com.expenseapp.salarytracker.domain.model.SalaryFrequency
import com.expenseapp.salarytracker.domain.model.TransactionType
import com.expenseapp.salarytracker.domain.model.TreeState
import java.time.LocalDate
import java.time.LocalTime

class DatabaseConverters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? = time?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it) }

    @TypeConverter
    fun fromTransactionType(type: TransactionType?): String? = type?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? =
        value?.let { TransactionType.fromString(it) }

    @TypeConverter
    fun fromAccountType(type: AccountType?): String? = type?.name

    @TypeConverter
    fun toAccountType(value: String?): AccountType? =
        value?.let { AccountType.fromString(it) }

    @TypeConverter
    fun fromGoalType(type: GoalType?): String? = type?.name

    @TypeConverter
    fun toGoalType(value: String?): GoalType? =
        value?.let { GoalType.fromString(it) }

    @TypeConverter
    fun fromTreeState(state: TreeState?): String? = state?.name

    @TypeConverter
    fun toTreeState(value: String?): TreeState? =
        value?.let { TreeState.fromString(it) }

    @TypeConverter
    fun fromBudgetPeriod(period: BudgetPeriod?): String? = period?.name

    @TypeConverter
    fun toBudgetPeriod(value: String?): BudgetPeriod? =
        value?.let { BudgetPeriod.fromString(it) }

    @TypeConverter
    fun fromSalaryFrequency(frequency: SalaryFrequency?): String? = frequency?.name

    @TypeConverter
    fun toSalaryFrequency(value: String?): SalaryFrequency? =
        value?.let { SalaryFrequency.fromString(it) }

    @TypeConverter
    fun fromReportingCycleMode(mode: ReportingCycleMode?): String? = mode?.name

    @TypeConverter
    fun toReportingCycleMode(value: String?): ReportingCycleMode? =
        value?.let { ReportingCycleMode.fromString(it) }

    @TypeConverter
    fun fromCustomFieldType(type: CustomFieldType?): String? = type?.name

    @TypeConverter
    fun toCustomFieldType(value: String?): CustomFieldType? =
        value?.let { CustomFieldType.fromString(it) }

    @TypeConverter
    fun fromRecurrenceFrequency(freq: com.expenseapp.salarytracker.domain.model.RecurrenceFrequency?): String? = freq?.name

    @TypeConverter
    fun toRecurrenceFrequency(value: String?): com.expenseapp.salarytracker.domain.model.RecurrenceFrequency? =
        value?.let { com.expenseapp.salarytracker.domain.model.RecurrenceFrequency.fromString(it) }

    @TypeConverter
    fun fromStringList(list: List<String>?): String? = list?.joinToString(";;")

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrEmpty()) emptyList() else value.split(";;")
}

