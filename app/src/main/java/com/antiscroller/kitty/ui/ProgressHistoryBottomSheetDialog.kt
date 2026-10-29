package com.antiscroller.kitty.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antiscroller.kitty.R
import com.antiscroller.kitty.data.ScrollDataManager
import com.antiscroller.kitty.databinding.DialogProgressHistoryBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlin.math.roundToInt

/**
 * Bottom sheet dialog presenting vertical daily reel progress history.
 * Displays past 14 days breakdown, streak summary, and boundary metrics.
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

        val history = dataManager.getDailyHistory(14)

        // Summary Stats
        val daysUnderBudget = history.count { !it.isOverLimit }
        binding.tvStatDaysUnderBudget.text = "$daysUnderBudget / ${history.size}"

        val totalReels = history.sumOf { it.count }
        val avgReels = if (history.isNotEmpty()) (totalReels.toFloat() / history.size).roundToInt() else 0
        binding.tvStatDailyAvg.text = "$avgReels reels"

        binding.tvStatActiveCap.text = "${dataManager.maxDailyScrolls} reels"

        // Vertical List
        binding.rvHistoryDays.layoutManager = LinearLayoutManager(context)
        binding.rvHistoryDays.adapter = HistoryAdapter(history)

        binding.btnDoneHistory.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class HistoryAdapter(
        private val items: List<ScrollDataManager.DailyHistoryItem>
    ) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvDayTitle: TextView = itemView.findViewById(R.id.tvDayTitle)
            val tvDaySubtitle: TextView = itemView.findViewById(R.id.tvDaySubtitle)
            val tvReelsRatio: TextView = itemView.findViewById(R.id.tvReelsRatio)
            val tvDayBadge: TextView = itemView.findViewById(R.id.tvDayBadge)
            val progressDay: LinearProgressIndicator = itemView.findViewById(R.id.progressDay)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = layoutInflater.inflate(R.layout.item_history_day, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            val context = holder.itemView.context

            holder.tvDayTitle.text = if (item.isToday) "${item.displayDate} 📍" else item.displayDate
            holder.tvDaySubtitle.text = item.dayOfWeek
            holder.tvReelsRatio.text = "${item.count} / ${item.limit} reels"

            val maxCap = item.limit.coerceAtLeast(1)
            holder.progressDay.max = maxCap
            holder.progressDay.progress = item.count.coerceAtMost(maxCap)

            when {
                item.count == 0 -> {
                    holder.tvDayBadge.text = "🌿 Mindful (0)"
                    val greenColor = context.getColor(R.color.accent_green)
                    holder.tvDayBadge.setTextColor(greenColor)
                    holder.progressDay.setIndicatorColor(greenColor)
                }
                !item.isOverLimit -> {
                    holder.tvDayBadge.text = "🎯 Under Limit"
                    val greenColor = context.getColor(R.color.accent_green)
                    holder.tvDayBadge.setTextColor(greenColor)
                    holder.progressDay.setIndicatorColor(greenColor)
                }
                else -> {
                    holder.tvDayBadge.text = "🔒 Limit Hit"
                    val alertColor = context.getColor(R.color.kitty_orange)
                    holder.tvDayBadge.setTextColor(alertColor)
                    holder.progressDay.setIndicatorColor(alertColor)
                }
            }
        }

        override fun getItemCount(): Int = items.size
    }
}
