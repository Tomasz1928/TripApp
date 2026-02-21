package com.example.tripapp2.ui.common.baseModals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * GenericModalFragment — prosty modal z dynamiczną treścią (body view).
 * ZMIGOWANY na BaseModalFragment.
 *
 * Zachowuje kompatybilność wsteczną — newInstance(title, bodyView) nadal działa.
 */
class GenericModalFragment : BaseModalFragment() {

    private var bodyViewProvider: (() -> View?)? = null

    override fun onCreateBodyView(inflater: LayoutInflater, container: ViewGroup?): View? {
        return bodyViewProvider?.invoke()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString("modal_title")?.let { setModalTitle(it) }
    }

    companion object {
        /**
         * Tworzy instancję z tytułem i opcjonalnym body view.
         * Body view jest dostarczane przez lambda (lazy) — bezpieczne dla lifecycle.
         */
        fun newInstance(
            title: String,
            bodyViewProvider: (() -> View?)? = null
        ): GenericModalFragment {
            return GenericModalFragment().apply {
                this.bodyViewProvider = bodyViewProvider
                arguments = Bundle().apply {
                    putString("modal_title", title)
                }
            }
        }

        /**
         * Kompatybilność wsteczna — przyjmuje View bezpośrednio.
         * UWAGA: View musi być tworzony po attach do Activity.
         */
        @Deprecated("Użyj wersji z lambda bodyViewProvider", ReplaceWith("newInstance(title) { bodyView }"))
        fun newInstance(title: String, bodyView: View? = null): GenericModalFragment {
            return GenericModalFragment().apply {
                this.bodyViewProvider = { bodyView }
                arguments = Bundle().apply {
                    putString("modal_title", title)
                }
            }
        }
    }
}