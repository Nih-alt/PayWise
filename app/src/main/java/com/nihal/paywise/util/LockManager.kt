package com.nihal.paywise.util

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.nihal.paywise.data.local.AppLockRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LockManager(
    private val repository: AppLockRepository,
    application: Application
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        // Initial check
        scope.launch {
            if (repository.isLockEnabled()) {
                _isLocked.value = true
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        scope.launch {
            if (!repository.isLockEnabled()) {
                _isLocked.value = false
                return@launch
            }

            val lastUnlocked = repository.getLastUnlockedTime()
            val settings = repository.settings.first()
            val timeoutMillis = settings.autoLockMinutes * 60 * 1000L
            
            if (System.currentTimeMillis() - lastUnlocked > timeoutMillis) {
                _isLocked.value = true
            }
        }
    }

    fun unlock() {
        scope.launch {
            repository.markUnlocked()
            _isLocked.value = false
        }
    }
    
    fun lock() {
        _isLocked.value = true
    }
}