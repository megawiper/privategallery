package com.privategallery.app.core.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

/**
 * Generic factory so each feature ViewModel can declare its own
 * constructor(mediaRepository: MediaRepository, ...) without every
 * screen writing its own boilerplate ViewModelProvider.Factory.
 */
class SimpleViewModelFactory<T : ViewModel>(
    private val create: () -> T
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>, extras: CreationExtras): VM {
        return create() as VM
    }
}
