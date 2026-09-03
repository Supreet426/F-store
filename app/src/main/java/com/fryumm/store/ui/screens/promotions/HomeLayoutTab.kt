package com.fryumm.store.ui.screens.promotions

import androidx.compose.runtime.Composable
import com.fryumm.store.data.model.DynamicAppConfig
import com.fryumm.store.ui.screens.appmanagement.HomeScreenLayoutTab
import com.fryumm.store.ui.viewmodels.StoreViewModel

@Composable
fun HomeLayoutTab(config: DynamicAppConfig, viewModel: StoreViewModel, onUpdate: (DynamicAppConfig) -> Unit) {
    HomeScreenLayoutTab(
        config = config,
        onConfigChange = onUpdate
    )
}
