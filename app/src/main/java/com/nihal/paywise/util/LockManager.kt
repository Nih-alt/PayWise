package com.nihal.paywise.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.nihal.paywise.data.local.AppLockRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LockManager(
    private val appLockRepository: AppLockRepository,
    private val application: Application
) : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var lastBackgroundTime: Long = 0

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        application.registerActivityLifecycleCallbacks(this)
        CoroutineScope(Dispatchers.Main).launch {
            val appLockEnabled = appLockRepository.appLockEnabledFlow.first()
            val autoLockMinutes = appLockRepository.autoLockMinutesFlow.first()
            val lastUnlockTime = appLockRepository.lastUnlockTimeFlow.first() ?: 0
            val timeSinceLastUnlock = System.currentTimeMillis() - lastUnlockTime
            val timeout = autoLockMinutes * 60 * 1000
            if (appLockEnabled && timeSinceLastUnlock > timeout) {
                _isLocked.value = true
            } else {
                _isLocked.value = false
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        lastBackgroundTime = System.currentTimeMillis()
    }

    override fun onStart(owner: LifecycleOwner) {
        CoroutineScope(Dispatchers.Main).launch {
            val appLockEnabled = appLockRepository.appLockEnabledFlow.first()
            if (appLockEnabled) {
                val autoLockMinutes = appLockRepository.autoLockMinutesFlow.first()
                val timeout = autoLockMinutes * 60 * 1000
                if (System.currentTimeMillis() - lastBackgroundTime > timeout) {
                    _isLocked.value = true
                }
            }
        }
    }

    fun unlock() {
        _isLocked.value = false
        CoroutineScope(Dispatchers.Main).launch {
            appLockRepository.markUnlockedNow()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
