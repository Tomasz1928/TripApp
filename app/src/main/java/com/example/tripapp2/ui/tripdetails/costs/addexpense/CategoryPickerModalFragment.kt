package com.example.tripapp2.ui.tripdetails.costs.addexpense

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tripapp2.R
import com.example.tripapp2.ui.common.baseModals.BaseModalFragment

class CategoryPickerModalFragment : BaseModalFragment() {

    private var onCategorySelected: ((ExpenseCategory) -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setModalTitle(getString(R.string.add_expense_category_hint))
    }

    override fun onCreateBodyView(inflater: LayoutInflater, container: ViewGroup?): View {
        val recyclerView = RecyclerView(requireContext()).apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = CategoryAdapter(ExpenseCategories.ALL) { category ->
                onCategorySelected?.invoke(category)
                dismissAnimated()
            }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val rowCount = (ExpenseCategories.ALL.size + 2) / 3 // ceil division
        limitListHeightIfNeeded(recyclerView, rowCount, 80, 5)

        return recyclerView
    }

    companion object {
        fun newInstance(
            onCategorySelected: (ExpenseCategory) -> Unit
        ): CategoryPickerModalFragment {
            return CategoryPickerModalFragment().apply {
                this.onCategorySelected = onCategorySelected
            }
        }
    }
}

class CategoryAdapter(
    private val categories: List<ExpenseCategory>,
    private val onCategoryClick: (ExpenseCategory) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.categoryIcon)
        val name: TextView = view.findViewById(R.id.categoryName)

        fun bind(category: ExpenseCategory) {
            icon.setImageResource(category.iconResId)
            name.setText(category.nameResId)
            itemView.setOnClickListener { onCategoryClick(category) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_picker, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount() = categories.size
}