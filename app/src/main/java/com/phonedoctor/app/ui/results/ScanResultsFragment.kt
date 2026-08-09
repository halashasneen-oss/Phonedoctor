package com.phonedoctor.app.ui.results

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.phonedoctor.app.R
import com.phonedoctor.app.data.report.ReportTextGenerator
import com.phonedoctor.app.databinding.FragmentScanResultsBinding
import com.phonedoctor.app.databinding.ItemIssueRowBinding
import com.phonedoctor.app.domain.model.ScanReport
import com.phonedoctor.app.domain.util.HealthScoreCalculator
import com.phonedoctor.app.ui.common.issueMessageRes
import com.phonedoctor.app.ui.common.serviceLocator
import com.phonedoctor.app.ui.common.toUiModel
import com.phonedoctor.app.ui.common.viewBinding
import kotlinx.coroutines.launch

class ScanResultsFragment : Fragment(R.layout.fragment_scan_results) {

    private val binding by viewBinding(FragmentScanResultsBinding::bind)
    private val args: ScanResultsFragmentArgs by navArgs()

    private val viewModel: ScanResultsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return ScanResultsViewModel(serviceLocator(), args.reportId) as T
            }
        }
    }

    private val categoryAdapter = CategoryResultAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerCategories.adapter = categoryAdapter
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        binding.buttonViewDetails.setOnClickListener {
            val action = ScanResultsFragmentDirections.actionResultsToReport(args.reportId)
            findNavController().navigate(action)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.report.collect { report -> report?.let { render(it) } }
            }
        }
    }

    private fun render(report: ScanReport) {
        binding.textScore.text = getString(R.string.results_health_score_fmt, report.healthScore)
        binding.textScoreStatus.setText(HealthScoreCalculator.scoreToStatus(report.healthScore).toUiModel().labelRes)
        categoryAdapter.submitList(report.results)

        val issues = report.results.mapNotNull { result -> result.issueMessageRes()?.let { result to it } }
        binding.containerIssues.removeAllViews()
        if (issues.isEmpty()) {
            binding.textIssuesTitle.visibility = View.GONE
            binding.textNoIssues.visibility = View.VISIBLE
        } else {
            binding.textIssuesTitle.visibility = View.VISIBLE
            binding.textNoIssues.visibility = View.GONE
            val inflater = LayoutInflater.from(requireContext())
            issues.forEach { (_, messageRes) ->
                val rowBinding = ItemIssueRowBinding.inflate(inflater, binding.containerIssues, false)
                rowBinding.textIssue.setText(messageRes)
                binding.containerIssues.addView(rowBinding.root)
            }
        }

        binding.buttonShare.setOnClickListener { shareReport(report) }
    }

    private fun shareReport(report: ScanReport) {
        val deviceLabel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val text = ReportTextGenerator.generate(requireContext(), report, deviceLabel)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.report_title))
        }
        startActivity(Intent.createChooser(intent, getString(R.string.results_share_report)))
    }
}
