package com.example.tripapp2.ui.receipt

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tripapp2.R
import com.example.tripapp2.data.repository.ReceiptRepository
import com.example.tripapp2.data.util.ImageCompressor
import com.example.tripapp2.ui.common.baseModals.ConfirmModalFragment
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ReceiptHandler — zarządza logiką rachunku wewnątrz ExpenseDetailModalFragment.
 *
 * @param canManageReceipt true jeśli user jest uwzględniony w kosztach (isMine)
 * @param receiptHash hash rachunku z tripDetails — używany do invalidacji cache
 */
class ReceiptHandler(
    private val fragment: Fragment,
    private val expenseId: String,
    private val expenseName: String,
    private var hasReceipt: Boolean,
    private val canManageReceipt: Boolean,
    private val receiptHash: String? = null
) {
    companion object {
        private const val TAG = "ReceiptHandler"
    }

    private val receiptRepository = ReceiptRepository.getInstance()
    private val context: Context get() = fragment.requireContext()

    private var addButton: TextView? = null
    private var viewContainer: View? = null
    private var viewButton: TextView? = null
    private var longPressHint: TextView? = null
    private var actionsContainer: LinearLayout? = null
    private var viewButtonSmall: View? = null
    private var changeButton: View? = null
    private var deleteButton: View? = null
    private var progressBar: ProgressBar? = null

    private var cameraPhotoUri: Uri? = null
    private var pendingCameraCapture = false
    private var imagePickerLauncher: ActivityResultLauncher<Intent>? = null

    fun registerLaunchers(fragment: Fragment) {
        if (!canManageReceipt) return
        imagePickerLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data ?: cameraPhotoUri
                if (uri != null) handleSelectedImage(uri)
                else Toast.makeText(context, R.string.receipt_image_pick_failed, Toast.LENGTH_SHORT).show()
            }
            pendingCameraCapture = false
        }
    }

    fun createReceiptRow(): View {
        val inflater = android.view.LayoutInflater.from(context)
        val row = inflater.inflate(R.layout.item_receipt_row, null, false)

        addButton = row.findViewById(R.id.receiptAddButton)
        viewContainer = row.findViewById(R.id.receiptViewContainer)
        viewButton = row.findViewById(R.id.receiptViewButton)
        longPressHint = row.findViewById(R.id.receiptLongPressHint)
        actionsContainer = row.findViewById(R.id.receiptActionsContainer)
        viewButtonSmall = row.findViewById(R.id.receiptViewButtonSmall)
        changeButton = row.findViewById(R.id.receiptChangeButton)
        deleteButton = row.findViewById(R.id.receiptDeleteButton)
        progressBar = row.findViewById(R.id.receiptProgress)

        setupClickListeners()
        updateState()
        return row
    }

    private fun setupClickListeners() {
        addButton?.setOnClickListener { if (canManageReceipt) openImagePicker() }
        viewButton?.setOnClickListener { openReceiptViewer() }
        if (canManageReceipt) {
            viewButton?.setOnLongClickListener { showManageActions(); true }
        }
        viewButtonSmall?.setOnClickListener { openReceiptViewer() }
        changeButton?.setOnClickListener { if (canManageReceipt) openImagePicker() }
        deleteButton?.setOnClickListener { if (canManageReceipt) showDeleteConfirmation() }
    }

    private fun updateState() {
        if (hasReceipt) {
            addButton?.visibility = View.GONE
            viewContainer?.visibility = View.VISIBLE
            actionsContainer?.visibility = View.GONE
            progressBar?.visibility = View.GONE
            longPressHint?.visibility = if (canManageReceipt) View.VISIBLE else View.GONE
        } else if (canManageReceipt) {
            addButton?.visibility = View.VISIBLE
            viewContainer?.visibility = View.GONE
            actionsContainer?.visibility = View.GONE
            progressBar?.visibility = View.GONE
        } else {
            addButton?.visibility = View.GONE
            viewContainer?.visibility = View.GONE
            actionsContainer?.visibility = View.GONE
            progressBar?.visibility = View.GONE
        }
    }

    fun shouldShowRow(): Boolean = hasReceipt || canManageReceipt

    private fun showManageActions() {
        viewContainer?.visibility = View.GONE
        actionsContainer?.visibility = View.VISIBLE
    }

    private fun showLoading() {
        addButton?.visibility = View.GONE
        viewContainer?.visibility = View.GONE
        actionsContainer?.visibility = View.GONE
        progressBar?.visibility = View.VISIBLE
    }

    private fun openImagePicker() {
        try {
            imagePickerLauncher?.launch(createImageChooserIntent())
        } catch (e: Exception) {
            Toast.makeText(context, R.string.receipt_picker_open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageChooserIntent(): Intent {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply { type = "image/*" }
        val cameraIntents = mutableListOf<Intent>()
        try {
            val photoFile = createTempImageFile()
            if (photoFile != null) {
                cameraPhotoUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
                cameraIntents.add(Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                })
                pendingCameraCapture = true
            }
        } catch (_: Exception) {}
        return Intent.createChooser(galleryIntent, context.getString(R.string.receipt_chooser_title)).apply {
            if (cameraIntents.isNotEmpty()) putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toTypedArray())
        }
    }

    private fun createTempImageFile(): File? = try {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        File.createTempFile("RECEIPT_${ts}_", ".jpg", context.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
    } catch (_: Exception) { null }

    private fun handleSelectedImage(uri: Uri) {
        showLoading()
        fragment.lifecycleScope.launch {
            try {
                val base64 = ImageCompressor.compressToBase64(uri, context)
                if (base64 == null) { Toast.makeText(context, R.string.receipt_compress_failed, Toast.LENGTH_SHORT).show(); updateState(); return@launch }
                val result = receiptRepository.uploadReceipt(expenseId, base64)
                result.onSuccess { dto ->
                    if (dto.success) { hasReceipt = true; Toast.makeText(context, R.string.receipt_uploaded, Toast.LENGTH_SHORT).show() }
                    else Toast.makeText(context, dto.message ?: context.getString(R.string.receipt_upload_error), Toast.LENGTH_SHORT).show()
                }.onFailure { e ->
                    Toast.makeText(context, context.getString(R.string.receipt_error_generic, e.message ?: ""), Toast.LENGTH_SHORT).show()
                }
                updateState()
            } catch (e: Exception) {
                Toast.makeText(context, R.string.receipt_upload_failed, Toast.LENGTH_SHORT).show(); updateState()
            }
        }
    }

    /**
     * Otwiera fullscreen viewer — przekazuje receiptHash do getReceipt
     * aby cache mógł porównać i refetchować jeśli hash się zmienił.
     */
    private fun openReceiptViewer() {
        showLoading()
        fragment.lifecycleScope.launch {
            try {
                val base64 = receiptRepository.getReceipt(expenseId, receiptHash)
                if (base64 != null) {
                    context.startActivity(Intent(context, ReceiptViewerActivity::class.java).apply {
                        putExtra(ReceiptViewerActivity.EXTRA_IMAGE_BASE64, base64)
                        putExtra(ReceiptViewerActivity.EXTRA_EXPENSE_NAME, expenseName)
                    })
                } else {
                    Toast.makeText(context, R.string.receipt_fetch_failed, Toast.LENGTH_SHORT).show()
                }
                updateState()
            } catch (e: Exception) {
                Toast.makeText(context, R.string.receipt_fetch_error, Toast.LENGTH_SHORT).show(); updateState()
            }
        }
    }

    private fun showDeleteConfirmation() {
        ConfirmModalFragment.newInstance(
            title = context.getString(R.string.receipt_delete_title),
            message = context.getString(R.string.receipt_delete_message),
            confirmText = context.getString(R.string.dialog_button_delete),
            confirmStyle = ConfirmModalFragment.ConfirmStyle.DANGER,
            onConfirm = { performDelete() }
        ).show(fragment.parentFragmentManager, "delete_receipt_confirm")
    }

    private fun performDelete() {
        showLoading()
        fragment.lifecycleScope.launch {
            try {
                val result = receiptRepository.deleteReceipt(expenseId)
                result.onSuccess { dto ->
                    if (dto.success) { hasReceipt = false; Toast.makeText(context, R.string.receipt_deleted, Toast.LENGTH_SHORT).show() }
                    else Toast.makeText(context, dto.message ?: context.getString(R.string.receipt_delete_error), Toast.LENGTH_SHORT).show()
                }.onFailure { e ->
                    Toast.makeText(context, context.getString(R.string.receipt_error_generic, e.message ?: ""), Toast.LENGTH_SHORT).show()
                }
                updateState()
            } catch (e: Exception) {
                Toast.makeText(context, R.string.receipt_delete_failed, Toast.LENGTH_SHORT).show(); updateState()
            }
        }
    }
}
