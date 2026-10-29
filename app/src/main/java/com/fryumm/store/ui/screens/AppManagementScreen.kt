package com.fryumm.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import com.fryumm.store.ui.screens.appmanagement.HomeScreenLayoutTab
import com.fryumm.store.ui.viewmodels.StoreViewModel

@Suppress("UNUSED_PARAMETER")
@Composable
fun AppManagementScreen(viewModel: StoreViewModel, navController: NavHostController) {
    var config by remember { mutableStateOf(viewModel.dynamicAppConfig) }

    LaunchedEffect(viewModel.dynamicAppConfig) {
        config = viewModel.dynamicAppConfig
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        HomeScreenLayoutTab(
            config = config,
        ) {
            config = it
            viewModel.updateDynamicConfig(it)
        }
    }
}
