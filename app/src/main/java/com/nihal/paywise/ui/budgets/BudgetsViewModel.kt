package com.nihal.paywise.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.repository.BudgetRepository
import com.nihal.paywise.data.repository.CategoryRepository
import com.nihal.paywise.domain.model.BudgetStatus
import com.nihal.paywise.domain.model.Category
import com.nihal.paywise.domain.model.CategoryKind
import com.nihal.paywise.domain.usecase.GetBudgetStatusUseCase
import com.nihal.paywise.domain.usecase.UpsertBudgetUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth

data class BudgetsUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val overallStatus: BudgetStatus? = null,
    val categoryStatuses: List<CategoryBudgetUiModel> = emptyList(),
    val isLoading: Boolean = true
)

data class CategoryBudgetUiModel(
    val category: Category,
    val status: BudgetStatus?
)

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModel(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val getBudgetStatusUseCase: GetBudgetStatusUseCase,
    private val upsertBudgetUseCase: UpsertBudgetUseCase
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth = _currentMonth.asStateFlow()

    private val categories = categoryRepository.getCategoriesByKindStream(CategoryKind.EXPENSE)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<BudgetsUiState> = combine(
        _currentMonth,
        categories
    ) { month, cats ->
        month to cats
    }.flatMapLatest { (month, cats) ->
        getBudgetStatusUseCase(month).map { status ->
            BudgetsUiState(
                currentMonth = month,
                overallStatus = status.overall,
                categoryStatuses = cats.map { cat ->
                    CategoryBudgetUiModel(
                        category = cat,
                        status = status.byCategory.find { it.categoryId == cat.id }
                    )
                },
                isLoading = false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetsUiState())

    fun goPrevMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun goNextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun setOverallBudget(amountPaise: Long) {
        viewModelScope.launch {
            upsertBudgetUseCase(_currentMonth.value, null, amountPaise)
        }
    }

    fun setCategoryBudget(categoryId: String, amountPaise: Long) {
        viewModelScope.launch {
            upsertBudgetUseCase(_currentMonth.value, categoryId, amountPaise)
        }
    }

    fun copyPreviousMonthBudgets() {
        viewModelScope.launch {
            val prevMonth = _currentMonth.value.minusMonths(1)
            val prevBudgets = budgetRepository.observeBudgetsForMonth(prevMonth.toString()).first()
            prevBudgets.forEach { prevBudget ->
                upsertBudgetUseCase(_currentMonth.value, prevBudget.categoryId, prevBudget.amountPaise)
            }
        }
    }
}
