package com.example.tripapp2.ui.common.baseModals
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.example.tripapp2.R

/**
 * ListPickerModalFragment — modal z listą elementów do wyboru.
 * Zastępuje AlertDialog.Builder z setItems().
 * Scroll pojawia się gdy elementów > 5.
 *
 * Użycie:
 * ListPickerModalFragment.newInstance(
 *     title = "Wybierz płatnika",
 *     items = listOf("Adam", "Ewa", "Jan"),
 *     onItemSelected = { index -> viewModel.onPayerSelected(participants[index].id) }
 * ).show(parentFragmentManager, "picker")
 */
class ListPickerModalFragment : BaseModalFragment() {

    private var items: List<String> = emptyList()
    private var onItemSelected: ((Int) -> Unit)? = null

    override fun onCreateBodyView(inflater: LayoutInflater, container: ViewGroup?): View {
        val scrollView = ScrollView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val listContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val itemHeightDp = 48
        val density = resources.displayMetrics.density

        items.forEachIndexed { index, item ->
            val itemView = TextView(requireContext()).apply {
                text = item
                setTextAppearance(R.style.Text_Body)
                val paddingPx = (12 * density).toInt()
                setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                setBackgroundResource(android.R.attr.selectableItemBackground.let {
                    val attrs = intArrayOf(it)
                    val ta = requireContext().obtainStyledAttributes(attrs)
                    val drawableResId = ta.getResourceId(0, 0)
                    ta.recycle()
                    drawableResId
                })
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (itemHeightDp * density).toInt()
                )
                gravity = android.view.Gravity.CENTER_VERTICAL

                setOnClickListener {
                    onItemSelected?.invoke(index)
                    dismissAnimated()
                }
            }
            listContainer.addView(itemView)

            // Divider between items (not after last)
            if (index < items.size - 1) {
                val divider = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (1 * density).toInt()
                    )
                    setBackgroundColor(resources.getColor(R.color.divider, null))
                }
                listContainer.addView(divider)
            }
        }

        scrollView.addView(listContainer)

        // Ograniczenie do ~5 elementów
        limitListHeightIfNeeded(scrollView, items.size, itemHeightDp, 5)

        return scrollView
    }

    companion object {
        fun newInstance(
            title: String,
            items: List<String>,
            onItemSelected: (Int) -> Unit
        ): ListPickerModalFragment {
            return ListPickerModalFragment().apply {
                this.items = items
                this.onItemSelected = onItemSelected
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