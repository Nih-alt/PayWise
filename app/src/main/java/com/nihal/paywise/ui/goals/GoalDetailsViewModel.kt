package com.nihal.paywise.ui.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.repository.AccountRepository
import com.nihal.paywise.data.repository.SavingsGoalRepository
import com.nihal.paywise.data.repository.TransactionRepository
import com.nihal.paywise.domain.model.*
import com.nihal.paywise.domain.usecase.AddGoalAllocationUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GoalDetailsUiState(
    val goal: SavingsGoal? = null,
    val allocations: List<Transaction> = emptyList(),
    val isLoading: Boolean = true
)

class GoalDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val addGoalAllocationUseCase: AddGoalAllocationUseCase
) : ViewModel() {

    private val goalId: String = checkNotNull(savedStateHandle["goalId"])

    val accounts = accountRepository.getAllAccountsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<GoalDetailsUiState> = savingsGoalRepository.getGoalWithProgress(goalId)
        .flatMapLatest { goal ->
            if (goal == null) flowOf(GoalDetailsUiState(isLoading = false))
            else transactionRepository.getAllTransactionsStream().map { txs ->
                GoalDetailsUiState(
                    goal = goal,
                    allocations = txs.filter { it.goalId == goalId }.sortedByDescending { it.timestamp },
                    isLoading = false
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GoalDetailsUiState())

    fun addAllocation(fromAccountId: String, amountPaise: Long, note: String?) {
        viewModelScope.launch {
            addGoalAllocationUseCase(goalId, fromAccountId, amountPaise, note)
        }
    }
    
    fun deleteGoal(onSuccess: () -> Unit) {
        viewModelScope.launch {
            uiState.value.goal?.let {
                savingsGoalRepository.deleteGoal(it)
                onSuccess()
            }
        }
    }
}