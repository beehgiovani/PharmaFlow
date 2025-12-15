package com.developersbeeh.pharmaflow.di

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.developersbeeh.pharmaflow.features.AdminRoutes
import com.developersbeeh.pharmaflow.features.dashboard.AdminDashboardScreen
import com.developersbeeh.pharmaflow.features.marketing.OfferCreatorScreen
import com.developersbeeh.pharmaflow.features.orders.AdminOrdersScreen
import com.developersbeeh.pharmaflow.features.products.AddProductScreen
import com.developersbeeh.pharmaflow.features.products.CatalogSearchScreen
import com.developersbeeh.pharmaflow.features.products.ImportProductsScreen
import com.developersbeeh.pharmaflow.features.products.InventoryScreen
import com.developersbeeh.pharmaflow.features.products.PriceCheckScreen
import com.developersbeeh.pharmaflow.features.products.StockListScreen
import com.developersbeeh.pharmaflow.features.products.ocr.BarcodeScanScreen
import com.developersbeeh.pharmaflow.features.products.ocr.OCRScreen
import com.developersbeeh.pharmaflow.features.settings.StoreSelectionScreen
import com.developersbeeh.pharmaflow.features.settings.StoreSettingsScreen
import com.developersbeeh.pharmaflow.features.team.TeamScreen
import com.developersbeeh.pharmaflow.ui.navigation.AppNavigator
import javax.inject.Inject

class AdminNavigator @Inject constructor() : AppNavigator {
    @Composable
    override fun NavigateToHome() {
        val navController = rememberNavController()
        val startRoute = "store_selection"

        NavHost(navController = navController, startDestination = startRoute) {

            // 🏪 Tela 0: Seleção de Loja
            composable("store_selection") {
                StoreSelectionScreen(onStoreSelected = {
                    navController.navigate(AdminRoutes.DASHBOARD) { popUpTo("store_selection") { inclusive = true } }
                })
            }

            // 📊 Tela 1: Dashboard
            composable(AdminRoutes.DASHBOARD) {
                AdminDashboardScreen(onNavigate = { route -> navController.navigate(route) })
            }

            // 🛒 Tela de Pedidos
            composable(
                route = AdminRoutes.ORDERS,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(500)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(500)) }
            ) {
                AdminOrdersScreen(onBackClick = { navController.popBackStack() })
            }

            // 👥 Tela de Equipe
            composable(
                route = AdminRoutes.TEAM,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(500)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(500)) }
            ) {
                TeamScreen(onBackClick = { navController.popBackStack() })
            }

            // 🎨 Tela de Marketing (A CORREÇÃO PRINCIPAL ESTÁ AQUI)
            composable(
                route = AdminRoutes.MARKETING,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(500)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(500)) }
            ) {
                OfferCreatorScreen(
                    onBackClick = { navController.popBackStack() },
                    navController = navController // Passando o navController corretamente!
                )
            }

            // 📦 Modo Inventário (Balanço Rápido)
            composable(
                route = AdminRoutes.INVENTORY,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(500)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(500)) }
            ) {
                InventoryScreen(onBackClick = { navController.popBackStack() })
            }

            // 🔍 Consulta de Preço (Scanner de Balcão)
            composable(
                route = AdminRoutes.PRICE_CHECK,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(500)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(500)) }
            ) {
                PriceCheckScreen(
                    onBackClick = { navController.popBackStack() },
                    onRegisterNewProduct = { code ->
                        // Passa o código lido para a tela de cadastro
                        navController.previousBackStackEntry?.savedStateHandle?.set("barcode_result", code)
                        navController.navigate(AdminRoutes.ADD_PRODUCT)
                    }
                )
            }

            // 📂 Importação de Arquivos (JSON/CSV)
            composable(
                route = AdminRoutes.IMPORT_CSV,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(500)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(500)) }
            ) {
                ImportProductsScreen(onBackClick = { navController.popBackStack() })
            }

            // ➕ Tela 2: Novo Produto / Edição
            composable(
                route = "${AdminRoutes.ADD_PRODUCT}?productId={productId}",
                arguments = listOf(navArgument("productId") { type = NavType.StringType; nullable = true; defaultValue = null }),
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(500)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(500)) }
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")
                AddProductScreen(
                    onBackClick = { navController.popBackStack() },
                    navController = navController,
                    productId = productId
                )
            }

            // 📦 Tela 3: Lista de Estoque
            composable(
                route = AdminRoutes.STOCK_LIST,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(500)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(500)) }
            ) {
                StockListScreen(
                    onBackClick = { navController.popBackStack() },
                    onAddProductClick = { navController.navigate(AdminRoutes.ADD_PRODUCT) },
                    onEditProductClick = { productId -> navController.navigate("${AdminRoutes.ADD_PRODUCT}?productId=$productId") },
                    // CONEXÃO CRÍTICA: Botão de importar leva para a tela de Importação
                    onImportClick = { navController.navigate(AdminRoutes.IMPORT_CSV) }
                )
            }

            // ⚙️ Tela 4: Configurações
            composable(AdminRoutes.SETTINGS) {
                StoreSettingsScreen(onBackClick = { navController.popBackStack() })
            }

            // 📸 OCR (Texto)
            composable(AdminRoutes.OCR_SCAN) {
                OCRScreen(
                    onBackClick = { navController.popBackStack() },
                    onTextCaptured = { text ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("ocr_result", text)
                        navController.popBackStack()
                    }
                )
            }

            // 📸 Barcode (EAN)
            composable(AdminRoutes.BARCODE_SCAN) {
                BarcodeScanScreen(
                    onBackClick = { navController.popBackStack() },
                    onBarcodeCaptured = { barcode ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("barcode_result", barcode)
                        navController.popBackStack()
                    }
                )
            }

            // 🌎 Catálogo Mestre
            composable("catalog_search") {
                CatalogSearchScreen(
                    onBackClick = { navController.popBackStack() },
                    onProductSelected = { globalProduct ->
                        navController.previousBackStackEntry?.savedStateHandle?.apply {
                            set("import_name", globalProduct.name)
                            set("import_ean", globalProduct.ean)
                            set("import_desc", globalProduct.description)
                            set("import_img", globalProduct.imageUrl)
                            set("import_cat", globalProduct.category)
                        }
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}