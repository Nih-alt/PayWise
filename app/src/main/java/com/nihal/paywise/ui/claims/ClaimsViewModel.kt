package com.nihal.paywise.ui.claims

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nihal.paywise.data.local.entity.ClaimEntity
import com.nihal.paywise.data.local.entity.ClaimStatus
import com.nihal.paywise.data.repository.ClaimRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ClaimsViewModel(
    private val claimRepository: ClaimRepository
) : ViewModel() {

    val allClaims: StateFlow<List<ClaimEntity>> = claimRepository.observeAllClaims()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createClaim(title: String) {
        viewModelScope.launch {
            val claim = ClaimEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                status = ClaimStatus.DRAFT,
                notes = null
            )
            claimRepository.upsertClaim(claim, emptyList())
        }
    }
}
