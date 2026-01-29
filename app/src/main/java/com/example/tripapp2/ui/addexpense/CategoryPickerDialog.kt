package com.example.tripapp2.ui.addexpense

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tripapp2.R

class CategoryPickerDialog(
    context: Context,
    private val onCategorySelected: (ExpenseCategory) -> Unit
) {

    private lateinit var dialog: AlertDialog

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_category_picker, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.categoriesRecycler)

        recyclerView.layoutManager = GridLayoutManager(context, 3)

        dialog = AlertDialog.Builder(context)
            .setTitle(R.string.add_expense_category_hint)
            .setView(view)
            .setNegativeButton(R.string.dialog_button_cancel, null)
            .create()

        // Adapter AFTER dialog is created
        recyclerView.adapter = CategoryAdapter(ExpenseCategories.ALL) { category ->
            onCategorySelected(category)
            dialog.dismiss()
        }
    }

    fun show() {
        dialog.show()
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

            itemView.setOnClickListener {
                onCategoryClick(category)
            }
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