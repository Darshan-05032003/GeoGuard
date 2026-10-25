package com.example.geoguard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.geoguard.ui.creategroup.CreateGroupScreen
import com.example.geoguard.ui.creategroup.CreateGroupViewModel
import com.example.geoguard.ui.groupdetail.GroupDetailScreen
import com.example.geoguard.ui.groupdetail.GroupDetailViewModel
import com.example.geoguard.ui.groups.GroupsViewModel
import com.example.geoguard.ui.groups.MyGroupsScreen

object Routes {
    const val GROUPS = "groups"
    const val CREATE_GROUP = "create_group"
    const val GROUP_DETAIL = "group_detail/{groupId}"

    fun groupDetail(groupId: String) = "group_detail/$groupId"
}

@Composable
fun AppNavHost(onLogout: () -> Unit) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.GROUPS) {

        composable(Routes.GROUPS) {
            val groupsViewModel: GroupsViewModel = viewModel()
            val uiState by groupsViewModel.uiState.collectAsState()

            MyGroupsScreen(
                uiState = uiState,
                onGroupClick = { groupId -> navController.navigate(Routes.groupDetail(groupId)) },
                onCreateClick = { navController.navigate(Routes.CREATE_GROUP) },
                onJoin = groupsViewModel::joinGroup,
                onClearMessages = groupsViewModel::clearMessages,
                onLogout = onLogout
            )
        }

        composable(Routes.CREATE_GROUP) {
            val createViewModel: CreateGroupViewModel = viewModel()
            val uiState by createViewModel.uiState.collectAsState()

            CreateGroupScreen(
                uiState = uiState,
                onUseCurrentLocation = createViewModel::fetchCurrentLocation,
                onPermissionDenied = createViewModel::onPermissionDenied,
                onRadiusChange = createViewModel::onRadiusChange,
                onCreate = createViewModel::createGroup,
                onBack = { navController.popBackStack() },
                onCreated = { groupId ->
                    navController.navigate(Routes.groupDetail(groupId)) {
                        popUpTo(Routes.CREATE_GROUP) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.GROUP_DETAIL,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) {
            val detailViewModel: GroupDetailViewModel = viewModel()
            val uiState by detailViewModel.uiState.collectAsState()

            GroupDetailScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() }
            )
        }
    }
}