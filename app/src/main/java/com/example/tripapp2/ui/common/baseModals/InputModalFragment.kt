package com.example.tripapp2.ui.common.baseModals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.example.tripapp2.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * InputModalFragment — modal z jednym polem tekstowym + confirm/cancel.
 * Zastępuje AlertDialog z custom view (np. dialog_add_placeholder).
 *
 * Użycie:
 * InputModalFragment.newInstance(
 *     title = "Dodaj uczestnika",
 *     hint = "Nazwa uczestnika",
 *     confirmText = "Dodaj",
 *     onConfirm = { text -> viewModel.addPlaceholder(text) }
 * ).show(parentFragmentManager, "input")
 */
class InputModalFragment : BaseModalFragment() {

    private var hint: String? = null
    private var confirmText: String? = null
    private var cancelText: String? = null
    private var onConfirm: ((String) -> Unit)? = null

    private var inputField: TextInputEditText? = null

    override fun onCreateBodyView(inflater: LayoutInflater, container: ViewGroup?): View {
        // Inflate z XML — gwarantuje że styl InputLayout jest poprawnie zaaplikowany
        val body = inflater.inflate(R.layout.widget_modal_input, container, false)

        val inputLayout = body.findViewById<TextInputLayout>(R.id.modalInputLayout)
        inputField = body.findViewById(R.id.modalInputField)

        inputLayout.hint = hint ?: ""

        return body
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

        val cancelBtn = inflater.inflate(R.layout.widget_button_secondary, footer, false) as MaterialButton
        cancelBtn.apply {
            text = cancelText ?: getString(R.string.dialog_button_cancel)
            (layoutParams as LinearLayout.LayoutParams).marginEnd = spacingSmall
            setOnClickListener { dismissAnimated() }
        }

        val confirmBtn = inflater.inflate(R.layout.widget_button_primary, footer, false) as MaterialButton
        confirmBtn.apply {
            text = confirmText ?: getString(R.string.dialog_button_add)
            setOnClickListener {
                val value = inputField?.text?.toString() ?: ""
                onConfirm?.invoke(value)
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
            hint: String? = null,
            confirmText: String? = null,
            cancelText: String? = null,
            onConfirm: (String) -> Unit
        ): InputModalFragment {
            return InputModalFragment().apply {
                this.hint = hint
                this.confirmText = confirmText
                this.cancelText = cancelText
                this.onConfirm = onConfirm
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