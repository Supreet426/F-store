package com.fryumm.store.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.ui.theme.FStorePurple
import com.fryumm.store.ui.screens.promotions.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionScreen(viewModel: StoreViewModel, navController: NavHostController) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Cart Suggestions", "Home Layout", "Product Ads", "Global Banners", 
        "Search Redirects", "Video Ads", "Smart Deals", "Milestone Rewards", 
        "Sponsored Brands", "App Header", "Push Broadcast", "Global Popup", "Broadcast Center"
    )
    val config = viewModel.dynamicAppConfig

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Coupons & Promo Offers", fontWeight = FontWeight.Black)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.updateDynamicConfig(config) }) {
                        Text("SAVE ALL CHANGES", fontWeight = FontWeight.ExtraBold, color = FStorePurple)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                edgePadding = 16.dp,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = FStorePurple,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) FStorePurple else Color.Gray
                            )
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> SequenceTab(config, viewModel::updateDynamicConfig)
                    1 -> HomeLayoutTab(config, viewModel, viewModel::updateDynamicConfig)
                    2 -> ProductAdsTab(config, viewModel, viewModel::updateDynamicConfig)
                    3 -> BannersTab(config, viewModel::updateDynamicConfig)
                    4 -> RetargetingTab(config, viewModel::updateDynamicConfig)
                    5 -> VideoAdsTab(config, viewModel::updateDynamicConfig)
                    6 -> SmartDealsTab(config, viewModel, viewModel::updateDynamicConfig)
                    7 -> ReferralSettingsTab(config, viewModel::updateDynamicConfig)
                    8 -> SponsoredTab(config, viewModel::updateDynamicConfig)
                    9 -> AppHeaderTab(config, viewModel, viewModel::updateDynamicConfig)
                    10 -> PushNotificationTab(viewModel)
                    11 -> PopupNotificationTab(config, viewModel, viewModel::updateDynamicConfig)
                    12 -> NotificationTab(viewModel)
                }
            }
        }
    }
}
