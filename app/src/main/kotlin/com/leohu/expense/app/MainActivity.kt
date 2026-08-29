package com.leohu.expense.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.leohu.expense.domain.usecase.ApprovePaymentRecordUseCase
import com.leohu.expense.domain.usecase.CleanupOldApprovedRecordsUseCase
import com.leohu.expense.ui.feature.approval.ApprovalDetailScreen
import com.leohu.expense.ui.feature.approval.ApprovalDetailViewModel
import com.leohu.expense.ui.feature.approval.ApprovalListScreen
import com.leohu.expense.ui.feature.approval.ApprovalViewModel
import com.leohu.expense.ui.feature.cards.CardListScreen
import com.leohu.expense.ui.feature.cards.CardViewModel
import com.leohu.expense.ui.feature.ewallets.EWalletListScreen
import com.leohu.expense.ui.feature.ewallets.EWalletViewModel
import com.leohu.expense.ui.feature.history.HistoryScreen
import com.leohu.expense.ui.feature.history.HistoryViewModel
import com.leohu.expense.ui.feature.home.*
import com.leohu.expense.ui.feature.settings.SettingsScreen
import com.leohu.expense.ui.feature.settings.SettingsViewModel
import com.leohu.expense.ui.feature.manual.ManualEntryScreen
import com.leohu.expense.ui.feature.manual.ManualEntryViewModel
import com.leohu.expense.ui.feature.voice.VoiceInputScreen
import com.leohu.expense.ui.theme.ExpenseAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val repository = (application as ExpenseApplication).repository
        val approveUseCase = ApprovePaymentRecordUseCase(repository)
        val cleanupUseCase = CleanupOldApprovedRecordsUseCase(repository)
        val preferenceHelper = (application as ExpenseApplication).preferenceHelper

        setContent {
            ExpenseAppTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repository))
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToApproval = { navController.navigate("approval_list") },
                            onNavigateToHistory = { navController.navigate("history") },
                            onNavigateToFailedList = { navController.navigate("failed_list") },
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToPreParseEdit = { imageIds ->
                                navController.navigate("pre_parse_edit/${imageIds.joinToString(",")}")
                            },
                            onNavigateToManualEntry = { navController.navigate("manual_entry") },
                            onNavigateToVoiceInput = { navController.navigate("voice_input") }
                        )
                    }
                    composable("voice_input") {
                        VoiceInputScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onSuccess = { navController.popBackStack() }
                        )
                    }
                    composable("manual_entry") {
                        val viewModel: ManualEntryViewModel = viewModel(
                            factory = ManualEntryViewModel.Factory(repository, preferenceHelper)
                        )
                        ManualEntryScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "pre_parse_edit/{imageIds}",
                        arguments = listOf(navArgument("imageIds") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val imageIdsStr = backStackEntry.arguments?.getString("imageIds") ?: ""
                        val imageIds = imageIdsStr.split(",").filter { it.isNotEmpty() }
                        val viewModel: PreParseEditViewModel = viewModel(
                            factory = PreParseEditViewModel.Factory(repository, imageIds)
                        )
                        PreParseEditScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onParseComplete = { navController.popBackStack() }
                        )
                    }
                    composable("approval_list") {
                        val viewModel: ApprovalViewModel = viewModel(factory = ApprovalViewModel.Factory(repository, approveUseCase))
                        ApprovalListScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToDetail = { recordId -> navController.navigate("approval_detail/$recordId") }
                        )
                    }
                    composable(
                        route = "approval_detail/{recordId}",
                        arguments = listOf(navArgument("recordId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val recordId = backStackEntry.arguments?.getString("recordId") ?: return@composable
                        val viewModel: ApprovalDetailViewModel = viewModel(
                            key = recordId,
                            factory = ApprovalDetailViewModel.Factory(repository, approveUseCase, preferenceHelper, recordId)
                        )
                        ApprovalDetailScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("history") {
                        val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory(repository))
                        HistoryScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToDetail = { imageId, recordId -> 
                                if (recordId != null) {
                                    navController.navigate("approval_detail/$recordId")
                                } else {
                                    navController.navigate("failed_detail/$imageId")
                                }
                            }
                        )
                    }
                    composable("failed_list") {
                        val viewModel: FailedItemViewModel = viewModel(factory = FailedItemViewModel.Factory(repository))
                        FailedItemListScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToDetail = { imageId -> navController.navigate("failed_detail/$imageId") }
                        )
                    }
                    composable(
                        route = "failed_detail/{imageId}",
                        arguments = listOf(navArgument("imageId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val imageId = backStackEntry.arguments?.getString("imageId") ?: return@composable
                        val viewModel: FailedItemDetailViewModel = viewModel(
                            key = imageId,
                            factory = FailedItemDetailViewModel.Factory(repository, imageId)
                        )
                        FailedItemDetailScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("cards") {
                        val viewModel: CardViewModel = viewModel(factory = CardViewModel.Factory(repository))
                        CardListScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("ewallets") {
                        val viewModel: EWalletViewModel = viewModel(factory = EWalletViewModel.Factory(repository))
                        EWalletListScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        val viewModel: SettingsViewModel = viewModel(
                            factory = SettingsViewModel.Factory(
                                repository,
                                cleanupUseCase,
                                (application as ExpenseApplication).preferenceHelper
                            )
                        )
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToCards = { navController.navigate("cards") },
                            onNavigateToEWallets = { navController.navigate("ewallets") }
                        )
                    }
                }
            }
        }
    }
}
