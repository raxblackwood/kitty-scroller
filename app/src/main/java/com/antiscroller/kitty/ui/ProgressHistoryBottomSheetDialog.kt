package com.antiscroller.kitty.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.antiscroller.kitty.R
import com.antiscroller.kitty.data.ScrollDataManager
import com.antiscroller.kitty.databinding.DialogProgressHistoryBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlin.math.roundToInt

/**
 * Bottom sheet dialog presenting a vertical bar graph of daily reel consumption.
 * Inspired by native Android/Digital Wellbeing usage charts with interactive day inspection.
 */
class ProgressHistoryBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: DialogProgressHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var dataManager: ScrollDataManager

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
        _binding = DialogProgressHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        dataManager = ScrollDataManager.getInstance(context)

        // Fetch past 7 days for the weekly chart
        val history = dataManager.getDailyHistory(7)

        // Summary Stats across the 7 days
        val daysUnderBudget = history.count { !it.isOverLimit }
        binding.tvStatDaysUnderBudget.text = "$daysUnderBudget / ${history.size}"

        val totalReels = history.sumOf { it.count }
        val avgReels = if (history.isNotEmpty()) (totalReels.toFloat() / history.size).roundToInt() else 0
        binding.tvStatDailyAvg.text = "$avgReels"

        binding.tvStatActiveCap.text = "${dataManager.maxDailyScrolls}"

        // Configure Vertical Bar Graph
        binding.chartDailyBars.setData(history)

        // Set initial selected day to Today
        val initialSelected = history.firstOrNull { it.isToday } ?: history.firstOrNull()
        if (initialSelected != null) {
            updateSelectedDetail(initialSelected)
        }

        // Handle user tap on any bar
        binding.chartDailyBars.onDaySelectedListener = { selectedItem ->
            updateSelectedDetail(selectedItem)
        }

        binding.btnDoneHistory.setOnClickListener {
            dismiss()
        }
    }

    private fun updateSelectedDetail(item: ScrollDataManager.DailyHistoryItem) {
        val context = requireContext()
        val title = if (item.isToday) "Today (${item.dayOfWeek})" else "${item.displayDate} (${item.dayOfWeek})"
        binding.tvSelectedDayTitle.text = title
        binding.tvSelectedDaySubtitle.text = "${item.count} of ${item.limit} reels watched (${item.dateString})"

        when {
            item.count == 0 -> {
                binding.tvSelectedBadge.text = "🌿 Mindful Day (0)"
                binding.tvSelectedBadge.setTextColor(context.getColor(R.color.accent_green))
            }
            !item.isOverLimit -> {
                val remaining = item.limit - item.count
                binding.tvSelectedBadge.text = "🎯 Under Limit ($remaining left)"
                binding.tvSelectedBadge.setTextColor(context.getColor(R.color.accent_green))
            }
            else -> {
                val excess = item.count - item.limit
                binding.tvSelectedBadge.text = "🔒 Limit Hit (+$excess reels)"
                binding.tvSelectedBadge.setTextColor(context.getColor(R.color.kitty_orange))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
