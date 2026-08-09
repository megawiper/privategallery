package com.privategallery.app.ui.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privategallery.app.domain.model.MediaItem
import com.privategallery.app.domain.repository.MediaRepository
import com.privategallery.app.domain.usecase.EditSaveMode
import com.privategallery.app.domain.usecase.SaveEditResult
import com.privategallery.app.domain.usecase.SaveEditedImageUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditorUiState(
    val isLoading: Boolean = true,
    val originalItem: MediaItem? = null,
    val previewBitmap: Bitmap? = null,
    val editState: EditState = EditState(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saveComplete: Boolean = false
)

/**
 * STEP 34-40 — IMAGE EDITOR
 *
 * Owns the non-destructive EditState (STEP 34), delegates all rendering
 * to ImageEditRenderer (STEP 35-38) for both preview and final save, and
 * routes undo/redo (STEP 39) through EditHistory. Save (STEP 40) never
 * touches the original until SaveEditedImageUseCase verifies the result.
 */
class EditorViewModel(
    private val context: Context,
    private val mediaRepository: MediaRepository,
    private val saveEditedImageUseCase: SaveEditedImageUseCase,
    private val mediaId: Long
) : ViewModel() {

    private var sourceBitmap: Bitmap? = null
    private lateinit var history: EditHistory

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val item = mediaRepository.getMediaById(mediaId)
            if (item == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, saveError = "Media not found")
                return@launch
            }
            val bitmap = withContext(Dispatchers.IO) { loadBitmap(Uri.parse(item.uriString)) }
            if (bitmap == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, saveError = "Could not load image")
                return@launch
            }
            sourceBitmap = bitmap
            history = EditHistory(EditState())
            _uiState.value = EditorUiState(
                isLoading = false,
                originalItem = item,
                previewBitmap = bitmap,
                editState = EditState()
            )
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        }
    }

    fun updateCrop(crop: CropState) = applyEdit { it.copy(crop = crop) }
    fun updateAdjustments(adjustments: AdjustmentState) = applyEdit { it.copy(adjustments = adjustments) }
    fun updateFilter(filter: FilterState) = applyEdit { it.copy(filter = filter) }
    fun addDrawingElement(element: DrawingElement) = applyEdit {
        it.copy(drawingElements = it.drawingElements + element)
    }

    private fun applyEdit(transform: (EditState) -> EditState) {
        if (!::history.isInitialized) return
        val newState = transform(_uiState.value.editState)
        history.push(newState)
        renderPreview(newState)
    }

    fun undo() {
        if (!::history.isInitialized) return
        renderPreview(history.undo())
    }

    fun redo() {
        if (!::history.isInitialized) return
        renderPreview(history.redo())
    }

    private fun renderPreview(state: EditState) {
        val source = sourceBitmap ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val rendered = ImageEditRenderer.render(source, state)
            _uiState.value = _uiState.value.copy(
                editState = state,
                previewBitmap = rendered,
                canUndo = history.canUndo,
                canRedo = history.canRedo
            )
        }
    }

    /** STEP 40 — Save Changes: Replace Original / Save as New / Cancel. */
    fun save(mode: EditSaveMode, onComplete: (Uri) -> Unit) {
        val item = _uiState.value.originalItem ?: return
        val bitmap = _uiState.value.previewBitmap ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null)
            val result = saveEditedImageUseCase.invoke(
                originalUri = Uri.parse(item.uriString),
                editedBitmap = bitmap,
                displayName = item.displayName,
                mimeType = item.mimeType,
                mode = mode
            )
            when (result) {
                is SaveEditResult.Success -> {
                    mediaRepository.refresh()
                    _uiState.value = _uiState.value.copy(isSaving = false, saveComplete = true)
                    onComplete(result.resultUri)
                }
                is SaveEditResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isSaving = false, saveError = result.reason)
                }
            }
        }
    }
}
