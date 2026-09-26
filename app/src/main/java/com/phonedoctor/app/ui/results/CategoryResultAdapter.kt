package com.phonedoctor.app.ui.results

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.phonedoctor.app.R
import com.phonedoctor.app.databinding.ItemCategoryResultBinding
import com.phonedoctor.app.domain.model.CategoryResult
import com.phonedoctor.app.domain.model.DiagnosticConfidence
import com.phonedoctor.app.domain.model.DiagnosticEvidenceType
import com.phonedoctor.app.domain.model.ScoreImpact
import com.phonedoctor.app.ui.common.toUiModel

class CategoryResultAdapter :
    RecyclerView.Adapter<CategoryResultAdapter.ViewHolder>() {

    private var items: List<CategoryResult> = emptyList()

    fun submitList(newItems: List<CategoryResult>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(
        val binding: ItemCategoryResultBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding = ItemCategoryResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = items[position]
        val context = holder.binding.root.context
        val categoryUi = item.category.toUiModel()
        val statusUi = item.status.toUiModel()

        holder.binding.imageIcon.setImageResource(categoryUi.iconRes)
        holder.binding.textLabel.setText(categoryUi.labelRes)
        holder.binding.textStatus.setText(statusUi.labelRes)
        holder.binding.textStatus.setBackgroundResource(
            statusUi.pillBackgroundRes
        )
        holder.binding.textStatus.setTextColor(
            context.getColor(statusUi.textColorRes)
        )

        holder.binding.textSummary.text = item.summary
        holder.binding.textSummary.visibility = View.VISIBLE

        val detail = item.detail?.takeIf { it.isNotBlank() }
        holder.binding.textDetail.text = detail.orEmpty()
        holder.binding.textDetail.visibility = if (detail == null) {
            View.GONE
        } else {
            View.VISIBLE
        }

        holder.binding.textEvidence.text = context.getString(
            R.string.result_evidence_fmt,
            context.getString(evidenceLabel(item.evidenceType)),
            context.getString(impactLabel(item.scoreImpact)),
            context.getString(confidenceLabel(item.confidence))
        )
        holder.binding.textEvidence.visibility = View.VISIBLE
    }

    override fun getItemCount(): Int = items.size

    private fun evidenceLabel(
        evidence: DiagnosticEvidenceType
    ): Int = when (evidence) {
        DiagnosticEvidenceType.MEASURED -> R.string.result_evidence_measured
        DiagnosticEvidenceType.CAPABILITY -> R.string.result_evidence_capability
        DiagnosticEvidenceType.CURRENT_STATE -> R.string.result_evidence_current_state
        DiagnosticEvidenceType.USER_VERIFIED -> R.string.result_evidence_user_verified
        DiagnosticEvidenceType.ESTIMATED -> R.string.result_evidence_estimated
    }

    private fun impactLabel(impact: ScoreImpact): Int = when (impact) {
        ScoreImpact.HEALTH -> R.string.result_impact_health
        ScoreImpact.INFORMATIONAL -> R.string.result_impact_informational
    }

    private fun confidenceLabel(
        confidence: DiagnosticConfidence
    ): Int = when (confidence) {
        DiagnosticConfidence.HIGH -> R.string.result_confidence_high
        DiagnosticConfidence.MEDIUM -> R.string.result_confidence_medium
        DiagnosticConfidence.LOW -> R.string.result_confidence_low
    }
}
