package com.phonedoctor.app.ui.help

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.phonedoctor.app.databinding.ItemFaqBinding

data class FaqItem(@StringRes val questionRes: Int, @StringRes val answerRes: Int)

class FaqAdapter(private val items: List<FaqItem>) : RecyclerView.Adapter<FaqAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemFaqBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFaqBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.textQuestion.setText(item.questionRes)
        holder.binding.textAnswer.setText(item.answerRes)
    }

    override fun getItemCount(): Int = items.size
}
