package com.developersbeeh.pharmaflow.di

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.developersbeeh.pharmaflow.features.cart.CartScreen
import com.developersbeeh.pharmaflow.features.cart.OrderSuccessScreen
import com.developersbeeh.pharmaflow.features.order_detail.ClientOrderDetailScreen // 💡 NOVO: Import do detalhe
import com.developersbeeh.pharmaflow.features.history.OrdersHistoryScreen
import com.developersbeeh.pharmaflow.features.home.ClientHomeScreen
import com.developersbeeh.pharmaflow.features.product_detail.ProductDetailScreen
import com.developersbeeh.pharmaflow.features.profile.ClientProfileScreen
import com.developersbeeh.pharmaflow.features.store_selection.ClientStoreSelectionScreen
import com.developersbeeh.pharmaflow.ui.navigation.AppNavigator
import javax.inject.Inject

class ClientNavigator @Inject constructor() : AppNavigator {
    @Composable
    override fun NavigateToHome() {
        val navController = rememberNavController()

        // O fluxo começa na escolha da unidade (Geolocalização simulada)
        val startRoute = "store_selection"

        NavHost(
            navController = navController,
            startDestination = startRoute
        ) {
            // 🏪 Tela 0: Seleção de Loja
            composable(
                route = "store_selection",
                exitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(500))
                },
                popEnterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(500))
                }
            ) {
                ClientStoreSelectionScreen(
                    onStoreSelected = {
                        // Navega para a Home e remove a seleção da pilha
                        navController.navigate("home") {
                            popUpTo("store_selection") { inclusive = true }
                        }
                    }
                )
            }

            // 🏠 Tela 1: Home (Vitrine)
            composable(
                route = "home",
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(500))
                },
                exitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(500))
                },
                popEnterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(500))
                }
            ) {
                ClientHomeScreen(
                    onNavigateToCart = { navController.navigate("cart") },
                    onNavigateToDetail = { productId -> navController.navigate("detail/$productId") },
                    onNavigateToProfile = { navController.navigate("profile") },
                    onNavigateToHistory = { navController.navigate("orders_history") }
                )
            }

            // 🛒 Tela 2: Carrinho
            composable(
                route = "cart",
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(500))
                },
                exitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(500))
                },
                popEnterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(500))
                }
            ) {
                CartScreen(
                    onBackClick = { navController.popBackStack() },
                    onNavigateToSuccess = {
                        // Navega para tela de sucesso e limpa o carrinho da pilha de voltar
                        navController.navigate("order_success") {
                            popUpTo("home") { inclusive = false }
                        }
                    }
                )
            }

            // 🎉 Tela 3: Sucesso do Pedido
            composable(
                route = "order_success",
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(500))
                }
            ) {
                OrderSuccessScreen(
                    onGoToHome = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    onGoToHistory = {
                        navController.navigate("orders_history") {
                            popUpTo("home") { inclusive = false }
                        }
                    }
                )
            }

            // 💊 Tela 4: Detalhes do Produto
            composable(
                route = "detail/{productId}",
                arguments = listOf(navArgument("productId") { type = NavType.StringType }),
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(500))
                },
                exitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(500))
                },
                popEnterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(500))
                }
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: ""
                ProductDetailScreen(
                    productId = productId,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // 📜 Tela 5: Histórico de Pedidos
            composable(
                route = "orders_history",
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(500))
                },
                exitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(500))
                },
                popEnterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(500))
                }
            ) {
                OrdersHistoryScreen(
                    onBackClick = { navController.popBackStack() },
                    // 💡 CONEXÃO: Passando a função de navegação para a tela de detalhes
                    onNavigateToDetail = { orderId ->
                        navController.navigate("order_detail/$orderId")
                    }
                )
            }

            // 🔎 Tela 5.1: Detalhe Específico do Pedido (Adicionado para fechar o ciclo)
            composable(
                route = "order_detail/{orderId}",
                arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(500))
                },
                exitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(500))
                }
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                ClientOrderDetailScreen(
                    orderId = orderId,
                    onBackClick = { navController.popBackStack() }
                )
            }


            // 👤 Tela 6: Perfil / Fidelidade
            composable(
                route = "profile",
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(500))
                },
                exitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(500))
                },
                popEnterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(500))
                }
            ) {
                ClientProfileScreen(
                    onBackClick = { navController.popBackStack() },
                    onLogoutSuccess = {
                        navController.navigate("store_selection") {
                            popUpTo("store_selection") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}