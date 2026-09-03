package com.fryumm.store.ui.screens.useractivity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fryumm.store.ui.viewmodels.StoreViewModel
import com.fryumm.store.data.model.UserProfile

@Composable
fun StaffAdminTab(
    viewModel: StoreViewModel,
    filteredUsers: List<UserProfile>,
    onUserClick: (UserProfile) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(filteredUsers) { user ->
            UserListItem(
                user = user, 
                onClick = { onUserClick(user) },
                onStatusChange = { viewModel.updateUserStatus(user.uid, it) }
            )
        }
    }
}
