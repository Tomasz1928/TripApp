package com.example.tripapp2.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.tripapp2.R

class GenericModalFragment : DialogFragment() {

    private var title: String? = null
    private var bodyView: View? = null

    companion object {
        fun newInstance(title: String, bodyView: View? = null): GenericModalFragment {
            val fragment = GenericModalFragment()
            fragment.title = title
            fragment.bodyView = bodyView
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_generic_modal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val modalTitle = view.findViewById<TextView>(R.id.modalTitle)
        val closeButton = view.findViewById<ImageView>(R.id.closeButton)
        val modalBodyContainer = view.findViewById<FrameLayout>(R.id.modalBodyContainer)

        modalTitle.text = title ?: ""
        closeButton.setOnClickListener { dismiss() }

        // NAPRAWIONE: Sprawdzamy czy view ma już parenta i go usuwamy
        bodyView?.let {
            // Jeśli view jest już gdzieś dodany, usuń go z poprzedniego parenta
            (it.parent as? ViewGroup)?.removeView(it)
            modalBodyContainer.addView(it)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}