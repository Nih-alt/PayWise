package com.nihal.paywise.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.domain.model.CategoryReportItem
import com.nihal.paywise.domain.model.FixedVsDiscretionaryReport
import com.nihal.paywise.domain.model.TrendItem
import com.nihal.paywise.domain.usecase.GetCategoryBreakdownUseCase
import com.nihal.paywise.domain.usecase.GetFixedVsDiscretionaryUseCase
import com.nihal.paywise.domain.usecase.GetMonthlyTrendUseCase
import com.nihal.paywise.util.MoneyFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.YearMonth
import java.time.ZoneId

enum class ReportMode {
    MONTH, YEAR
}

data class ReportsUiState(
    val selectedMode: ReportMode = ReportMode.MONTH,
    val selectedMonth: YearMonth = YearMonth.now(),
    val totalSpentText: String = "₹0.00",
    val categoryBreakdown: List<CategoryReportItem> = emptyList(),
    val trend: List<TrendItem> = emptyList(),
    val fixedVsDiscretionary: FixedVsDiscretionaryReport? = null,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    private val getCategoryBreakdownUseCase: GetCategoryBreakdownUseCase,
    private val getMonthlyTrendUseCase: GetMonthlyTrendUseCase,
    private val getFixedVsDiscretionaryUseCase: GetFixedVsDiscretionaryUseCase
) : ViewModel() {

    private val _reportMode = MutableStateFlow(ReportMode.MONTH)
    private val _selectedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<ReportsUiState> = combine(
        _reportMode,
        _selectedMonth
    ) { mode, month ->
        val start = if (mode == ReportMode.MONTH) {
            month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        } else {
            month.atEndOfMonth().minusMonths(11).withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        }
        val end = month.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
        
        Triple(mode, month, start to end)
    }.flatMapLatest { (mode, month, range) ->
        combine(
            getCategoryBreakdownUseCase(range.first, range.second),
            getMonthlyTrendUseCase(month),
            getFixedVsDiscretionaryUseCase(range.first, range.second)
        ) { breakdown, trend, fixedVsDisc ->
            val total = breakdown.sumOf { it.amountPaise }
            ReportsUiState(
                selectedMode = mode,
                selectedMonth = month,
                totalSpentText = MoneyFormatter.formatPaise(total),
                categoryBreakdown = breakdown,
                trend = trend,
                fixedVsDiscretionary = fixedVsDisc,
                isLoading = false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsUiState())

    fun setMode(mode: ReportMode) {
        _reportMode.value = mode
    }

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }

    fun prevMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }
}
