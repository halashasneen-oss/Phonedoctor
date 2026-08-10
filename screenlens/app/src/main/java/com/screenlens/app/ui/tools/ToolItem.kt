package com.screenlens.app.ui.tools

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.screenlens.app.R

enum class ToolId {
    SCREEN_SCANNER, IMAGE_OCR, LIVE_OCR, TRANSLATOR, CLIPBOARD, QR_SCANNER, BARCODE_SCANNER, FLOATING_LENS, LANGUAGE_MANAGER
}

data class ToolItem(
    val id: ToolId,
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int
)

val toolItems = listOf(
    ToolItem(ToolId.SCREEN_SCANNER, R.drawable.ic_nav_scan, R.string.tool_screen_scanner, R.string.tool_screen_scanner_desc),
    ToolItem(ToolId.IMAGE_OCR, R.drawable.ic_type_image, R.string.tool_image_ocr, R.string.tool_image_ocr_desc),
    ToolItem(ToolId.LIVE_OCR, R.drawable.ic_type_camera, R.string.tool_live_ocr, R.string.tool_live_ocr_desc),
    ToolItem(ToolId.TRANSLATOR, R.drawable.ic_onboarding_translate, R.string.tool_translator, R.string.tool_translator_desc),
    ToolItem(ToolId.CLIPBOARD, R.drawable.ic_type_clipboard, R.string.tool_clipboard, R.string.tool_clipboard_desc),
    ToolItem(ToolId.QR_SCANNER, R.drawable.ic_type_qr, R.string.tool_qr_scanner, R.string.tool_qr_scanner_desc),
    ToolItem(ToolId.BARCODE_SCANNER, R.drawable.ic_type_qr, R.string.tool_barcode_scanner, R.string.tool_barcode_scanner_desc),
    ToolItem(ToolId.FLOATING_LENS, R.drawable.ic_onboarding_lens, R.string.tool_floating_lens, R.string.tool_floating_lens_desc),
    ToolItem(ToolId.LANGUAGE_MANAGER, R.drawable.ic_swap, R.string.tool_language_manager, R.string.tool_language_manager_desc)
)
