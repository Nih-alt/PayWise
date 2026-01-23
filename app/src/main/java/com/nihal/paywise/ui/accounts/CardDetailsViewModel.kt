package com.nihal.paywise.ui.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.repository.AccountRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.Account
import com.nihal.paywise.domain.model.CardStatement
import com.nihal.paywise.domain.model.Transaction
import com.nihal.paywise.domain.usecase.GetCardStatementUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth

data class CardDetailsUiState(
    val account: Account? = null,
    val statement: CardStatement? = null,
    val recentTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class CardDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val getCardStatementUseCase: GetCardStatementUseCase
) : ViewModel() {

    private val accountId: String = checkNotNull(savedStateHandle["accountId"])

    val uiState: StateFlow<CardDetailsUiState> = accountRepository.getAccountStream(accountId)
        .filterNotNull()
        .flatMapLatest { account ->
            val currentMonth = YearMonth.now()
            getCardStatementUseCase(account, currentMonth).flatMapLatest { statement ->
                if (statement != null) {
                    transactionRepository.getTransactionsBetweenStream(
                        statement.statementPeriodStart,
                        statement.statementPeriodEnd
                    ).map { txs ->
                        CardDetailsUiState(
                            account = account,
                            statement = statement,
                            recentTransactions = txs.filter { it.accountId == account.id },
                            isLoading = false
                        )
                    }
                } else {
                    flowOf(CardDetailsUiState(account = account, isLoading = false))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CardDetailsUiState())
}