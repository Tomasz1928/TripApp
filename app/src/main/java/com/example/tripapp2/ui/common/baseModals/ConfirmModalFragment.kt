package com.example.tripapp2.ui.common.baseModals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.tripapp2.R
import com.google.android.material.button.MaterialButton

/**
 * ConfirmModalFragment — lekki modal potwierdzenia.
 * Zastępuje AlertDialog.Builder z setTitle/setMessage/setPositiveButton/setNegativeButton.
 *
 * Użycie:
 * ConfirmModalFragment.newInstance(
 *     title = "Usunąć?",
 *     message = "Czy na pewno chcesz usunąć tego uczestnika?",
 *     confirmText = "Usuń",
 *     cancelText = "Anuluj",
 *     confirmStyle = ConfirmStyle.DANGER,
 *     onConfirm = { viewModel.delete(id) }
 * ).show(parentFragmentManager, "confirm")
 */
class ConfirmModalFragment : BaseModalFragment() {

    private var message: String? = null
    private var confirmText: String? = null
    private var cancelText: String? = null
    private var confirmStyle: ConfirmStyle = ConfirmStyle.PRIMARY
    private var onConfirm: (() -> Unit)? = null
    private var onCancel: (() -> Unit)? = null

    enum class ConfirmStyle {
        PRIMARY,  // Niebieski przycisk
        DANGER    // Czerwony przycisk
    }

    override fun onCreateBodyView(inflater: LayoutInflater, container: ViewGroup?): View {
        return TextView(requireContext()).apply {
            text = message ?: ""
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            textSize = 14f // text_size_body
        }
    }

    override fun onCreateFooterView(inflater: LayoutInflater, container: ViewGroup?): View {
        val density = resources.displayMetrics.density
        val spacingSmall = (8 * density).toInt()

        val footer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Cancel button — outlined style via XML inflate
        val cancelBtn = inflater.inflate(R.layout.widget_button_secondary, footer, false) as MaterialButton
        cancelBtn.apply {
            text = cancelText ?: getString(R.string.dialog_button_cancel)
            (layoutParams as LinearLayout.LayoutParams).marginEnd = spacingSmall
            setOnClickListener {
                onCancel?.invoke()
                dismissAnimated()
            }
        }

        // Confirm button
        val confirmBtn = inflater.inflate(R.layout.widget_button_primary, footer, false) as MaterialButton
        confirmBtn.apply {
            text = confirmText ?: getString(R.string.dialog_button_ok)
            when (confirmStyle) {
                ConfirmStyle.PRIMARY -> { /* domyślny styl — primary */ }
                ConfirmStyle.DANGER -> {
                    setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.error))
                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                }
            }
            setOnClickListener {
                onConfirm?.invoke()
                dismissAnimated()
            }
        }

        footer.addView(cancelBtn)
        footer.addView(confirmBtn)
        return footer
    }

    companion object {
        fun newInstance(
            title: String,
            message: String,
            confirmText: String? = null,
            cancelText: String? = null,
            confirmStyle: ConfirmStyle = ConfirmStyle.PRIMARY,
            onConfirm: () -> Unit,
            onCancel: (() -> Unit)? = null
        ): ConfirmModalFragment {
            return ConfirmModalFragment().apply {
                this.message = message
                this.confirmText = confirmText
                this.cancelText = cancelText
                this.confirmStyle = confirmStyle
                this.onConfirm = onConfirm
                this.onCancel = onCancel
                arguments = Bundle().apply {
                    putString("modal_title", title)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString("modal_title")?.let { setModalTitle(it) }
    }
}