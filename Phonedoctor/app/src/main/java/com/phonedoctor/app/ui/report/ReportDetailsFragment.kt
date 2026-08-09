package com.phonedoctor.app.ui.report

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.FileProvider
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
import com.phonedoctor.app.data.report.PdfReportGenerator
import com.phonedoctor.app.data.report.ReportTextGenerator
import com.phonedoctor.app.databinding.FragmentReportDetailsBinding
import com.phonedoctor.app.domain.model.ScanReport
import com.phonedoctor.app.domain.util.FormatUtils
import com.phonedoctor.app.ui.common.serviceLocator
import com.phonedoctor.app.ui.common.viewBinding
import com.phonedoctor.app.ui.results.CategoryResultAdapter
import kotlinx.coroutines.launch

class ReportDetailsFragment : Fragment(R.layout.fragment_report_details) {

    private val binding by viewBinding(FragmentReportDetailsBinding::bind)
    private val args: ReportDetailsFragmentArgs by navArgs()

    private val viewModel: ReportDetailsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return ReportDetailsViewModel(serviceLocator(), args.reportId) as T
            }
        }
    }

    private val categoryAdapter = CategoryResultAdapter()
    private var currentReport: ScanReport? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerCategories.adapter = categoryAdapter
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
        binding.buttonShare.setOnClickListener { currentReport?.let { shareAsText(it) } }
        binding.buttonExportPdf.setOnClickListener { currentReport?.let { shareAsPdf(it) } }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.report.collect { report -> report?.let { render(it) } }
            }
        }
    }

    private fun render(report: ScanReport) {
        currentReport = report
        binding.textDevice.text = "${Build.MANUFACTURER} ${Build.MODEL}"
        binding.textDate.text = FormatUtils.formatDateTime(report.timestampMillis)
        binding.textHealth.text = "${report.healthScore}%"
        categoryAdapter.submitList(report.results)
    }

    private fun shareAsText(report: ScanReport) {
        val deviceLabel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val text = ReportTextGenerator.generate(requireContext(), report, deviceLabel)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.report_title))
        }
        startActivity(Intent.createChooser(intent, getString(R.string.report_share)))
    }

    private fun shareAsPdf(report: ScanReport) {
        val file = PdfReportGenerator.generate(requireContext(), report)
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.report_export_pdf)))
    }
}
