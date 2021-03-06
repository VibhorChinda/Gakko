package com.ieeevit.gakko.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ieeevit.gakko.data.repository.AuthRepository


@Suppress("UNCHECKED_CAST")
class SplashViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SplashViewModel(authRepository) as T
    }
}