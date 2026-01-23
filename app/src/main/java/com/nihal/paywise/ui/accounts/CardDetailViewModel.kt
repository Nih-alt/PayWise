package com.nihal.paywise.ui.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.repository.AccountRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.Account
import com.nihal.paywise.domain.model.Transaction
import com.nihal.paywise.domain.usecase.CardBillUiModel
import com.nihal.paywise.domain.usecase.GetCardBillUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CardBillDetailUiState(
    val account: Account? = null,
    val billInfo: CardBillUiModel? = null,
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class CardBillDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val getCardBillUseCase: GetCardBillUseCase
) : ViewModel() {

    private val accountId: String = checkNotNull(savedStateHandle["accountId"])

    val uiState: StateFlow<CardBillDetailUiState> = accountRepository.getAccountStream(accountId)
        .filterNotNull()
        .flatMapLatest { account ->
            getCardBillUseCase(account).flatMapLatest { bill ->
                if (bill != null) {
                    transactionRepository.getTransactionsByAccountStream(accountId).map { txs ->
                        CardBillDetailUiState(
                            account = account,
                            billInfo = bill,
                            transactions = txs.take(10), // Recent 10 for detail
                            isLoading = false
                        )
                    }
                } else {
                    flowOf(CardBillDetailUiState(account = account, isLoading = false))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CardBillDetailUiState())
}
