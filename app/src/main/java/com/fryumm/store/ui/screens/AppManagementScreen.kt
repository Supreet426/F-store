package com.fryumm.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.ui.screens.appmanagement.*
import com.fryumm.store.ui.theme.FStorePurple

@Composable
fun AppManagementScreen(viewModel: StoreViewModel, navController: NavHostController) {
    var config by remember { mutableStateOf(viewModel.dynamicAppConfig) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(viewModel.dynamicAppConfig) {
        config = viewModel.dynamicAppConfig
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (navController.previousBackStackEntry != null) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Column {
                    Text("Ecosystem Controls", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("Manage consumer app & global configs", fontSize = 11.sp, color = Color.Gray)
                }
            }
            Button(
                onClick = { viewModel.updateDynamicConfig(config) },
                colors = ButtonDefaults.buttonColors(containerColor = FStorePurple),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("SAVE & GO LIVE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = FStorePurple,
            divider = {}
        ) {
            listOf("Home Layout", "Rewards & Pass", "Maintenance").forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> HomeScreenLayoutTab(
                    config = config,
                    onConfigChange = { config = it }
                )
                1 -> EcosystemRewardsTab(
                    config = config,
                    onConfigChange = { config = it }
                )
                2 -> MaintenanceTab(
                    viewModel = viewModel
                )
            }
        }
    }
}
