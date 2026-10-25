package com.expenseapp.salarytracker.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.expenseapp.salarytracker.domain.model.Account
import com.expenseapp.salarytracker.domain.model.Category
import com.expenseapp.salarytracker.domain.model.Transaction
import com.expenseapp.salarytracker.domain.repository.AccountRepository
import com.expenseapp.salarytracker.domain.repository.CategoryRepository
import com.expenseapp.salarytracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionFilter(
    val query: String = "",
    val categoryId: Long? = null,
    val accountId: Long? = null
)

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryId: Long? = null,
    val selectedAccountId: Long? = null
)

class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _selectedAccountId = MutableStateFlow<Long?>(null)

    private val filterFlow = combine(
        _searchQuery,
        _selectedCategoryId,
        _selectedAccountId
    ) { query, catId, accId ->
        TransactionFilter(query, catId, accId)
    }

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.getAllTransactions(),
        categoryRepository.getAllCategories(),
        accountRepository.getAllAccounts(),
        filterFlow
    ) { allTx, cats, accs, filter ->
        val filtered = allTx.filter { tx ->
            val matchesQuery = filter.query.isBlank() || tx.note.contains(filter.query, ignoreCase = true)
            val matchesCat = filter.categoryId == null || tx.categoryId == filter.categoryId
            val matchesAcc = filter.accountId == null || tx.accountId == filter.accountId || tx.destinationAccountId == filter.accountId
            matchesQuery && matchesCat && matchesAcc
        }

        TransactionsUiState(
            transactions = filtered,
            categories = cats,
            accounts = accs,
            searchQuery = filter.query,
            selectedCategoryId = filter.categoryId,
            selectedAccountId = filter.accountId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategoryFilterSelected(categoryId: Long?) {
        _selectedCategoryId.value = if (_selectedCategoryId.value == categoryId) null else categoryId
    }

    fun onAccountFilterSelected(accountId: Long?) {
        _selectedAccountId.value = if (_selectedAccountId.value == accountId) null else accountId
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategoryId.value = null
        _selectedAccountId.value = null
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.insertTransaction(transaction)
        }
    }

    class Factory(
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val accountRepository: AccountRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TransactionsViewModel(
                transactionRepository,
                categoryRepository,
                accountRepository
            ) as T
        }
    }
}
