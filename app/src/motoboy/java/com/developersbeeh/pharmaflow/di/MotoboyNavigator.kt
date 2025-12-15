package com.developersbeeh.pharmaflow.di

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.developersbeeh.pharmaflow.features.delivery.DeliveryHomeScreen
// --- CORREÇÃO: O Import abaixo é essencial para o Motoboy enxergar a tela ---
import com.developersbeeh.pharmaflow.features.order_detail.ClientOrderDetailScreen
import com.developersbeeh.pharmaflow.ui.navigation.AppNavigator
import javax.inject.Inject

class MotoboyNavigator @Inject constructor() : AppNavigator {

    @Composable
    override fun NavigateToHome() {
        val navController = rememberNavController()
        val startRoute = "delivery_home"

        NavHost(
            navController = navController,
            startDestination = startRoute
        ) {
            // 🏍️ Tela Principal: Feed de Entregas
            composable(
                route = "delivery_home",
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(500)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(500)) },
                popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(500)) }
            ) {
                DeliveryHomeScreen(
                    onNavigateToDetail = { orderId ->
                        navController.navigate("delivery_detail/$orderId")
                    },
                    onLogout = { /* Tratado no ViewModel */ }
                )
            }

            // 📦 Detalhe do Pedido (Compartilhado)
            composable(
                route = "delivery_detail/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(500)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(500)) },
                popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(500)) }
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""

                ClientOrderDetailScreen(
                    orderId = orderId,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}