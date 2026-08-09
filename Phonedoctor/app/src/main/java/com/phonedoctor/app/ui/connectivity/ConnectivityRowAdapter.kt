package com.phonedoctor.app.ui.connectivity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.phonedoctor.app.databinding.ItemCategoryResultBinding

data class ConnectivityRow(
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
    @StringRes val statusRes: Int,
    @DrawableRes val pillRes: Int,
    @ColorRes val textColorRes: Int
)

class ConnectivityRowAdapter : RecyclerView.Adapter<ConnectivityRowAdapter.ViewHolder>() {

    private var items: List<ConnectivityRow> = emptyList()

    fun submitList(newItems: List<ConnectivityRow>) {
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
        holder.binding.imageIcon.setImageResource(item.iconRes)
        holder.binding.textLabel.setText(item.labelRes)
        holder.binding.textStatus.setText(item.statusRes)
        holder.binding.textStatus.setBackgroundResource(item.pillRes)
        holder.binding.textStatus.setTextColor(holder.binding.root.context.getColor(item.textColorRes))
    }

    override fun getItemCount(): Int = items.size
}
