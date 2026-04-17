package com.example.tripapp2.ui.dashboard.mydata

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.example.tripapp2.R
import com.example.tripapp2.ui.common.baseModals.BaseModalFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Modal zmiany hasła — dwa pola: nowe hasło + potwierdzenie.
 * Używa BaseModalFragment (spójny styl z resztą aplikacji).
 */
class ChangePasswordModalFragment : BaseModalFragment() {

    private var onConfirm: ((String, String) -> Unit)? = null

    private var newPasswordInput: TextInputEditText? = null
    private var confirmPasswordInput: TextInputEditText? = null

    override fun onCreateBodyView(inflater: LayoutInflater, container: ViewGroup?): View {
        val density = requireContext().resources.displayMetrics.density
        val spacingMedium = (16 * density).toInt()

        val body = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, spacingMedium, 0, 0)
        }

        val newPasswordLayout = inflater.inflate(
            R.layout.widget_modal_input, body, false
        ) as ViewGroup
        newPasswordLayout.findViewById<TextInputLayout>(R.id.modalInputLayout).apply {
            hint = getString(R.string.my_data_new_password_hint)
            endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
        }
        newPasswordInput = newPasswordLayout.findViewById<TextInputEditText>(R.id.modalInputField).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        body.addView(newPasswordLayout)

        val spacing = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, spacingMedium
            )
        }
        body.addView(spacing)

        val confirmLayout = inflater.inflate(
            R.layout.widget_modal_input, body, false
        ) as ViewGroup
        confirmLayout.findViewById<TextInputLayout>(R.id.modalInputLayout).apply {
            hint = getString(R.string.my_data_confirm_password_hint)
            endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
        }
        confirmPasswordInput = confirmLayout.findViewById<TextInputEditText>(R.id.modalInputField).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        body.addView(confirmLayout)

        return body
    }

    override fun onCreateFooterView(inflater: LayoutInflater, container: ViewGroup?): View {
        val density = requireContext().resources.displayMetrics.density
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
            text = getString(R.string.dialog_button_cancel)
            (layoutParams as LinearLayout.LayoutParams).marginEnd = spacingSmall
            setOnClickListener { dismissAnimated() }
        }

        val confirmBtn = inflater.inflate(R.layout.widget_button_primary, footer, false) as MaterialButton
        confirmBtn.apply {
            text = getString(R.string.dialog_button_save)
            setOnClickListener {
                val newPassword = newPasswordInput?.text?.toString() ?: ""
                val confirmPassword = confirmPasswordInput?.text?.toString() ?: ""
                onConfirm?.invoke(newPassword, confirmPassword)
                dismissAnimated()
            }
        }

        footer.addView(cancelBtn)
        footer.addView(confirmBtn)
        return footer
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setModalTitle(getString(R.string.my_data_change_password_title))
    }

    companion object {
        fun newInstance(onConfirm: (String, String) -> Unit): ChangePasswordModalFragment {
            return ChangePasswordModalFragment().apply {
                this.onConfirm = onConfirm
            }
        }
    }
}