package com.screenlens.app.ui.languagemanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.screenlens.app.R
import com.screenlens.app.databinding.ItemLanguageRowBinding
import com.screenlens.app.translate.TranslateLanguageInfo

class LanguageRowAdapter(
    private val installed: Boolean,
    private val onAction: (TranslateLanguageInfo) -> Unit
) : RecyclerView.Adapter<LanguageRowAdapter.ViewHolder>() {

    private var items: List<TranslateLanguageInfo> = emptyList()
    private var pendingCode: String? = null

    fun submit(newItems: List<TranslateLanguageInfo>, pending: String?) {
        items = newItems
        pendingCode = pending
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemLanguageRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLanguageRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        holder.binding.languageName.text = "${item.englishName} · ${item.nativeName}"
        holder.binding.languageStatus.text = context.getString(
            if (installed) R.string.status_installed else R.string.status_not_installed
        )
        holder.binding.actionButton.setText(if (installed) R.string.action_remove else R.string.action_download)

        val isPending = item.code == pendingCode
        holder.binding.rowProgress.visibility = if (isPending) View.VISIBLE else View.GONE
        holder.binding.actionButton.visibility = if (isPending) View.GONE else View.VISIBLE
        holder.binding.actionButton.setOnClickListener { onAction(item) }
    }

    override fun getItemCount(): Int = items.size
}
