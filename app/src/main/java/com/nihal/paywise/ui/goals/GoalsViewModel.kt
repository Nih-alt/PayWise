package com.nihal.paywise.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.repository.SavingsGoalRepository
import com.nihal.paywise.domain.model.SavingsGoal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class GoalsViewModel(
    private val savingsGoalRepository: SavingsGoalRepository
) : ViewModel() {

    val activeGoals: StateFlow<List<SavingsGoal>> = savingsGoalRepository.getActiveGoalsWithProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createGoal(title: String, targetPaise: Long, color: Long) {
        viewModelScope.launch {
            val goal = SavingsGoal(
                id = UUID.randomUUID().toString(),
                title = title,
                targetAmountPaise = targetPaise,
                color = color
            )
            savingsGoalRepository.insertGoal(goal)
        }
    }
}