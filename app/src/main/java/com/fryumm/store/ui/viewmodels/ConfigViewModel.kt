package com.fryumm.store.ui.viewmodels

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fryumm.store.data.model.*
import com.fryumm.store.data.repository.StorageRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ConfigViewModel @Inject constructor(
    application: Application,
    private val storageRepository: StorageRepository,
    private val firestore: FirebaseFirestore
) : AndroidViewModel(application) {

    sealed class UiEvent {
        data class ShowError(val message: String) : UiEvent()
        data class ShowSuccess(val message: String) : UiEvent()
    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    var storeConfig by mutableStateOf(StoreConfig())
        private set

    var dynamicAppConfig by mutableStateOf(DynamicAppConfig())
        private set

    var darkStores by mutableStateOf<List<DarkStore>>(emptyList())
        private set

    var appUpdates by mutableStateOf<List<AppUpdateInfo>>(emptyList())
        private set

    var serviceRequests by mutableStateOf<List<ServiceRequest>>(emptyList())
        private set

    init {
        observeDarkStores()
        observeServiceRequests()
        observeAppUpdates()
        observeDynamicConfig()
        observeStoreConfig()
    }

    private fun observeDarkStores() {
        viewModelScope.launch {
            firestore.collection("dark_stores")
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.let {
                        darkStores = it.toObjects(DarkStore::class.java)
                    }
                }
        }
    }

    private fun observeServiceRequests() {
        viewModelScope.launch {
            firestore.collection("service_requests")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.let {
                        serviceRequests = it.toObjects(ServiceRequest::class.java)
                    }
                }
        }
    }

    private fun observeAppUpdates() {
        viewModelScope.launch {
            firestore.collection("app_updates")
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.let {
                        appUpdates = it.toObjects(AppUpdateInfo::class.java)
                    }
                }
        }
    }

    private fun observeDynamicConfig() {
        viewModelScope.launch {
            firestore.collection("settings").document("dynamic_app_config")
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.toObject(DynamicAppConfig::class.java)?.let {
                        dynamicAppConfig = it
                    }
                }
        }
    }

    private fun observeStoreConfig() {
        viewModelScope.launch {
            firestore.collection("settings").document("store_config")
                .addSnapshotListener { snapshot, _ ->
                    snapshot?.toObject(StoreConfig::class.java)?.let {
                        storeConfig = it
                    }
                }
        }
    }

    suspend fun uploadApk(uri: Uri, appId: String): String? {
        return storageRepository.uploadImage(uri, "apks/$appId/${UUID.randomUUID()}.apk")
    }

    fun updateAppRelease(info: AppUpdateInfo) {
        viewModelScope.launch {
            try {
                firestore.collection("app_updates").document(info.appId).set(info).await()
                _uiEvent.emit(UiEvent.ShowSuccess("New Update Released: ${info.appName} v${info.versionName}"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Update Failed: ${e.message}"))
            }
        }
    }

    fun updateDynamicConfig(config: DynamicAppConfig) {
        viewModelScope.launch {
            try {
                firestore.collection("settings").document("dynamic_app_config").set(config).await()
                dynamicAppConfig = config
                _uiEvent.emit(UiEvent.ShowSuccess("App Config Updated"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Update Failed"))
            }
        }
    }

    fun toggleProductInDeal(productId: String) {
        val currentFlashSale = dynamicAppConfig.flashSale ?: FlashSale()
        val currentProductIds = currentFlashSale.productIds.toMutableList()

        val isInDeal = currentProductIds.contains(productId)
        if (isInDeal) {
            currentProductIds.remove(productId)
        } else {
            currentProductIds.add(productId)
        }

        val updatedConfig = dynamicAppConfig.copy(
            flashSale = currentFlashSale.copy(productIds = currentProductIds)
        )
        updateDynamicConfig(updatedConfig)

        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowSuccess(if (isInDeal) "Removed from Deals" else "Added to Deals"))
        }
    }

    fun updateStoreConfig(config: StoreConfig) {
        viewModelScope.launch {
            try {
                firestore.collection("settings").document("store_config").set(config).await()
                storeConfig = config
                _uiEvent.emit(UiEvent.ShowSuccess("Store Settings Updated"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Update Failed"))
            }
        }
    }

    fun updateDarkStore(darkStore: DarkStore) {
        viewModelScope.launch {
            try {
                val id = darkStore.id.ifEmpty { firestore.collection("dark_stores").document().id }
                val finalStore = darkStore.copy(id = id)
                firestore.collection("dark_stores").document(id).set(finalStore).await()
                _uiEvent.emit(UiEvent.ShowSuccess("Dark Store '${finalStore.name}' Updated"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Failed to update Dark Store"))
            }
        }
    }

    fun deleteDarkStore(id: String) {
        viewModelScope.launch {
            try {
                firestore.collection("dark_stores").document(id).delete().await()
                _uiEvent.emit(UiEvent.ShowSuccess("Dark Store Removed"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Delete Failed"))
            }
        }
    }

    fun assignMerchantToDarkStore(merchantId: String, darkStoreId: String?) {
        viewModelScope.launch {
            try {
                firestore.collection("sellers").document(merchantId).update("darkStoreId", darkStoreId).await()

                darkStores.forEach { ds ->
                    val newList = ds.assignedMerchantIds.toMutableList()
                    if (ds.id == darkStoreId) {
                        if (!newList.contains(merchantId)) newList.add(merchantId)
                    } else {
                        newList.remove(merchantId)
                    }
                    if (newList != ds.assignedMerchantIds) {
                        firestore.collection("dark_stores").document(ds.id).update("assignedMerchantIds", newList).await()
                    }
                }

                _uiEvent.emit(UiEvent.ShowSuccess("Merchant Assignment Updated"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Assignment Failed"))
            }
        }
    }
}
