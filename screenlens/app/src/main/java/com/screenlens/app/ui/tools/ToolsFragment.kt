package com.screenlens.app.ui.tools

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.screenlens.app.R
import com.screenlens.app.databinding.FragmentToolsBinding
import com.screenlens.app.ui.common.FloatingLensController

class ToolsFragment : Fragment(R.layout.fragment_tools) {

    private var _binding: FragmentToolsBinding? = null
    private val binding get() = _binding!!
    private val floatingLensController = FloatingLensController(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentToolsBinding.bind(view)

        binding.toolsGrid.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.toolsGrid.adapter = ToolsAdapter { tool -> onToolClicked(tool) }
    }

    private fun onToolClicked(tool: ToolItem) {
        val navController = findNavController()
        when (tool.id) {
            ToolId.SCREEN_SCANNER -> navController.navigate(R.id.action_tools_to_scan)
            ToolId.IMAGE_OCR -> navController.navigate(R.id.action_tools_to_imageOcr)
            ToolId.LIVE_OCR -> navController.navigate(R.id.action_tools_to_liveOcr)
            ToolId.TRANSLATOR -> navController.navigate(R.id.action_tools_to_translator)
            ToolId.CLIPBOARD -> navController.navigate(R.id.action_tools_to_clipboard)
            ToolId.QR_SCANNER, ToolId.BARCODE_SCANNER -> navController.navigate(R.id.action_tools_to_qrScanner)
            ToolId.LANGUAGE_MANAGER -> navController.navigate(R.id.action_tools_to_languageManager)
            ToolId.FLOATING_LENS -> floatingLensController.setEnabled(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
