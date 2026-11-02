package com.antiscroller.kitty.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.antiscroller.kitty.R
import com.antiscroller.kitty.data.ScrollDataManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlin.math.roundToInt

/**
 * Bottom sheet dialog presenting a vertical bar graph of daily reel consumption.
 * Adapts seamlessly between "Sticker Pop" Light theme and "Midnight OLED" Dark theme.
 */
class ProgressHistoryBottomSheetDialog : BottomSheetDialogFragment() {

    private lateinit var dataManager: ScrollDataManager
    private var isLightMode: Boolean = false

    private lateinit var tvStatDaysUnderBudget: TextView
    private lateinit var tvStatDailyAvg: TextView
    private lateinit var tvStatActiveCap: TextView
    private lateinit var chartDailyBars: DailyBarChartView
    private lateinit var tvSelectedDayTitle: TextView
    private lateinit var tvSelectedDaySubtitle: TextView
    private lateinit var tvSelectedBadge: TextView
    private lateinit var btnDoneHistory: View

    companion object {
        const val TAG = "ProgressHistoryBottomSheet"

        fun newInstance(): ProgressHistoryBottomSheetDialog {
            return ProgressHistoryBottomSheetDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        dataManager = ScrollDataManager.getInstance(context)
        isLightMode = (dataManager.getUiTheme() == ScrollDataManager.UI_THEME_LIGHT)

        val layoutRes = if (isLightMode) R.layout.dialog_progress_history_light else R.layout.dialog_progress_history
        val view = inflater.inflate(layoutRes, container, false)

        tvStatDaysUnderBudget = view.findViewById(R.id.tvStatDaysUnderBudget)
        tvStatDailyAvg = view.findViewById(R.id.tvStatDailyAvg)
        tvStatActiveCap = view.findViewById(R.id.tvStatActiveCap)
        chartDailyBars = view.findViewById(R.id.chartDailyBars)
        tvSelectedDayTitle = view.findViewById(R.id.tvSelectedDayTitle)
        tvSelectedDaySubtitle = view.findViewById(R.id.tvSelectedDaySubtitle)
        tvSelectedBadge = view.findViewById(R.id.tvSelectedBadge)
        btnDoneHistory = view.findViewById(R.id.btnDoneHistory)

        return view
    }

    override fun onStart() {
        super.onStart()
        val sheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        sheet?.background = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configure theme on chart
        chartDailyBars.isLightMode = isLightMode

        // Fetch past 7 days for the weekly chart
        val history = dataManager.getDailyHistory(7)

        // Summary Stats across the 7 days
        val daysUnderBudget = history.count { !it.isOverLimit }
        tvStatDaysUnderBudget.text = "$daysUnderBudget / ${history.size}"

        val totalReels = history.sumOf { it.count }
        val avgReels = if (history.isNotEmpty()) (totalReels.toFloat() / history.size).roundToInt() else 0
        tvStatDailyAvg.text = "$avgReels"

        tvStatActiveCap.text = "${dataManager.maxDailyScrolls}"

        // Configure Vertical Bar Graph
        chartDailyBars.setData(history)

        // Set initial selected day to Today
        val initialSelected = history.firstOrNull { it.isToday } ?: history.firstOrNull()
        if (initialSelected != null) {
            updateSelectedDetail(initialSelected)
        }

        // Handle user tap on any bar
        chartDailyBars.onDaySelectedListener = { selectedItem ->
            updateSelectedDetail(selectedItem)
        }

        btnDoneHistory.setOnClickListener {
            dismiss()
        }
    }

    private fun updateSelectedDetail(item: ScrollDataManager.DailyHistoryItem) {
        val context = requireContext()
        val title = if (item.isToday) "Today (${item.dayOfWeek})" else "${item.displayDate} (${item.dayOfWeek})"
        tvSelectedDayTitle.text = title
        tvSelectedDaySubtitle.text = "${item.count} of ${item.limit} reels watched (${item.dateString})"

        val greenColor = if (isLightMode) Color.parseColor("#0B6B45") else context.getColor(R.color.accent_green)
        val alertColor = if (isLightMode) Color.parseColor("#C22215") else context.getColor(R.color.kitty_orange)

        when {
            item.count == 0 -> {
                tvSelectedBadge.text = "🌿 Mindful Day (0)"
                tvSelectedBadge.setTextColor(greenColor)
            }
            !item.isOverLimit -> {
                val remaining = item.limit - item.count
                tvSelectedBadge.text = "🎯 Under Limit ($remaining left)"
                tvSelectedBadge.setTextColor(greenColor)
            }
            else -> {
                val excess = item.count - item.limit
                tvSelectedBadge.text = "🔒 Limit Hit (+$excess reels)"
                tvSelectedBadge.setTextColor(alertColor)
            }
        }
    }
}
