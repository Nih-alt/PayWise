package com.nihal.paywise.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.local.UserPreferencesRepository
import com.nihal.paywise.domain.model.*
import com.nihal.paywise.domain.usecase.GetCategoryBreakdownUseCase
import com.nihal.paywise.domain.usecase.GetFixedVsDiscretionaryUseCase
import com.nihal.paywise.domain.usecase.GetMonthlyTrendUseCase
import com.nihal.paywise.util.MoneyFormatter
import com.nihal.paywise.util.PayCycleResolver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.*

enum class ReportTab {
    BREAKDOWN, TREND, FIXED_VS_DISCRETIONARY
}

data class ReportsUiState(
    val selectedTab: ReportTab = ReportTab.BREAKDOWN,
    val rangePreset: ReportRangePreset = ReportRangePreset.THIS_MONTH,
    val selectedMonth: YearMonth = YearMonth.now(),
    val customRange: Pair<Instant, Instant>? = null,
    val totalSpentText: String = "₹0.00",
    val categoryBreakdown: List<CategoryBreakdownRow> = emptyList(),
    val trend: List<MonthlyTrendRow> = emptyList(),
    val fixedVsDiscretionary: List<SpendingGroupRow> = emptyList(),
    val isLoading: Boolean = true,
    val dateRangeText: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    private val getCategoryBreakdownUseCase: GetCategoryBreakdownUseCase,
    private val getMonthlyTrendUseCase: GetMonthlyTrendUseCase,
    private val getFixedVsDiscretionaryUseCase: GetFixedVsDiscretionaryUseCase,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(ReportTab.BREAKDOWN)
    private val _rangePreset = MutableStateFlow(ReportRangePreset.THIS_MONTH)
    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    private val _customRange = MutableStateFlow<Pair<Instant, Instant>?>(null)

    private val _dateRange = combine(
        _rangePreset, _selectedMonth, _customRange, userPreferencesRepository.salarySettingsFlow
    ) { preset, month, custom, salarySettings ->
        val zone = ZoneId.systemDefault()
        when (preset) {
            ReportRangePreset.THIS_MONTH -> {
                val cycle = PayCycleResolver.resolve(
                    now = month.atDay(15).atStartOfDay(zone).toInstant(),
                    settings = salarySettings,
                    zoneId = zone
                )
                cycle.start to cycle.end
            }
            ReportRangePreset.LAST_30_DAYS -> {
                val end = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant()
                val start = LocalDate.now().minusDays(30).atStartOfDay(zone).toInstant()
                start to end
            }
            ReportRangePreset.YEAR_TO_DATE -> {
                val start = LocalDate.now().withDayOfYear(1).atStartOfDay(zone).toInstant()
                val end = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant()
                start to end
            }
            ReportRangePreset.LAST_12_MONTHS -> {
                val end = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant()
                val start = LocalDate.now().minusMonths(11).withDayOfMonth(1).atStartOfDay(zone).toInstant()
                start to end
            }
            ReportRangePreset.CUSTOM -> {
                custom ?: (Instant.now() to Instant.now())
            }
        }
    }

    val uiState: StateFlow<ReportsUiState> = combine(
        _selectedTab, _rangePreset, _selectedMonth, _customRange, _dateRange
    ) { tab, preset, month, custom, range ->
        val (start, end) = range
        
        // Fetch data based on range
        val breakdownFlow = getCategoryBreakdownUseCase(start, end)
        val trendFlow = getMonthlyTrendUseCase(end)
        val fixedVsDiscFlow = getFixedVsDiscretionaryUseCase(start, end)

        combine(breakdownFlow, trendFlow, fixedVsDiscFlow) { breakdown, trend, fixedVsDisc ->
            val total = breakdown.sumOf { it.totalAmount }
            ReportsUiState(
                selectedTab = tab,
                rangePreset = preset,
                selectedMonth = month,
                customRange = custom,
                totalSpentText = MoneyFormatter.formatPaise(total),
                categoryBreakdown = breakdown,
                trend = trend,
                fixedVsDiscretionary = fixedVsDisc,
                isLoading = false,
                dateRangeText = formatDateRange(start, end)
            )
        }
    }.flatMapLatest { it }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsUiState())

    fun setTab(tab: ReportTab) {
        _selectedTab.value = tab
    }

    fun setPreset(preset: ReportRangePreset) {
        _rangePreset.value = preset
    }

    fun setMonth(month: YearMonth) {
        _selectedMonth.value = month
        _rangePreset.value = ReportRangePreset.THIS_MONTH
    }

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
        _rangePreset.value = ReportRangePreset.THIS_MONTH
    }

    fun prevMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
        _rangePreset.value = ReportRangePreset.THIS_MONTH
    }

    fun setCustomRange(start: Instant, end: Instant) {
        _customRange.value = start to end
        _rangePreset.value = ReportRangePreset.CUSTOM
    }

    private fun formatDateRange(start: Instant, end: Instant): String {
        val zone = ZoneId.systemDefault()
        val startDate = start.atZone(zone).toLocalDate()
        val endDate = end.atZone(zone).toLocalDate().minusDays(1)
        return if (startDate == endDate) {
            startDate.toString()
        } else {
            "$startDate - $endDate"
        }
    }
}