package com.nihal.paywise.ui.claims

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.local.dao.ClaimDao
import com.nihal.paywise.data.local.entity.ClaimEntity
import com.nihal.paywise.data.local.entity.ClaimItemEntity
import com.nihal.paywise.data.local.entity.ClaimStatus
import com.nihal.paywise.data.repository.AccountRepository
import com.nihal.paywise.data.repository.ClaimRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.Transaction
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant

data class ClaimDetailsUiState(
    val claim: ClaimEntity? = null,
    val items: List<Transaction> = emptyList(),
    val totalAmountPaise: Long = 0L,
    val isLoading: Boolean = true
)

class ClaimDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val claimRepository: ClaimRepository,
    private val claimDao: ClaimDao,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val claimId: String = checkNotNull(savedStateHandle["claimId"])

    val accounts = accountRepository.getAllAccountsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<ClaimDetailsUiState> = flow {
        val claim = claimRepository.getClaimById(claimId)
        emit(claim)
    }.flatMapLatest { claim ->
        if (claim == null) flowOf(ClaimDetailsUiState(isLoading = false))
        else claimDao.observeItemsForClaim(claimId).flatMapLatest { items ->
            val txnIds = items.map { it.txnId }
            transactionRepository.getAllTransactionsStream().map { allTxs ->
                val linkedTxs = allTxs.filter { it.id in txnIds }
                val total = items.sumOf { it.includeAmountPaise }
                ClaimDetailsUiState(
                    claim = claim,
                    items = linkedTxs,
                    totalAmountPaise = total,
                    isLoading = false
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ClaimDetailsUiState())

    fun updateStatus(status: ClaimStatus) {
        viewModelScope.launch {
            uiState.value.claim?.let {
                val updated = it.copy(
                    status = status,
                    submittedAt = if (status == ClaimStatus.SUBMITTED) Instant.now() else it.submittedAt,
                    approvedAt = if (status == ClaimStatus.APPROVED) Instant.now() else it.approvedAt
                )
                claimRepository.upsertClaim(updated, claimDao.observeItemsForClaim(claimId).first())
            }
        }
    }

    fun markReimbursed(accountId: String, amountPaise: Long) {
        viewModelScope.launch {
            claimRepository.markAsReimbursed(claimId, accountId, amountPaise, transactionRepository)
        }
    }
    
    fun deleteClaim(onSuccess: () -> Unit) {
        viewModelScope.launch {
            claimRepository.deleteClaim(claimId)
            onSuccess()
        }
    }
}
