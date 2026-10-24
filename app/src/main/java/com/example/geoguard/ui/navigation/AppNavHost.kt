package com.example.geoguard.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
            PlaceholderScreen(text = "Create Group: coming in Lesson 6")
        }

        composable(
            route = Routes.GROUP_DETAIL,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId").orEmpty()
            PlaceholderScreen(text = "Group $groupId: coming in Lesson 7")
        }
    }
}

@Composable
private fun PlaceholderScreen(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text)
    }
}