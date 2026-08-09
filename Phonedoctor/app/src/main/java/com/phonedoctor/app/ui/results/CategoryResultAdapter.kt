package com.phonedoctor.app.ui.results

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.phonedoctor.app.databinding.ItemCategoryResultBinding
import com.phonedoctor.app.domain.model.CategoryResult
import com.phonedoctor.app.ui.common.toUiModel

class CategoryResultAdapter : RecyclerView.Adapter<CategoryResultAdapter.ViewHolder>() {

    private var items: List<CategoryResult> = emptyList()

    fun submitList(newItems: List<CategoryResult>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemCategoryResultBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val categoryUi = item.category.toUiModel()
        val statusUi = item.status.toUiModel()
        holder.binding.imageIcon.setImageResource(categoryUi.iconRes)
        holder.binding.textLabel.setText(categoryUi.labelRes)
        holder.binding.textStatus.setText(statusUi.labelRes)
        holder.binding.textStatus.setBackgroundResource(statusUi.pillBackgroundRes)
        holder.binding.textStatus.setTextColor(holder.binding.root.context.getColor(statusUi.textColorRes))
    }

    override fun getItemCount(): Int = items.size
}
