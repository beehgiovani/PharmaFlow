plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    alias(libs.plugins.ksp)

}

android {
    namespace = "com.developersbeeh.pharmaflow"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.developersbeeh.pharmaflow"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Habilita suporte a vetores para ícones antigos se necessário
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Recomendado ativar para Release
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        @Suppress("DEPRECATION")
        freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
        @Suppress("DEPRECATION")
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
    buildFeatures {
        compose = true
        // Importante: Habilita geração da classe BuildConfig para checar FLAVOR no código
        buildConfig = true
    }

    // 🏗 CONFIGURAÇÃO DOS FLAVORS (APPS SEPARADOS)
    flavorDimensions += "userType"

    productFlavors {
        // 🟦 App do CLIENTE (Vendas, Fidelidade)
        create("client") {
            dimension = "userType"
            applicationId = "com.developersbeeh.pharmaflow"
            // Define o nome do app na tela do celular
            resValue("string", "app_name", "PharmaFlow")
            // Cria uma constante para checar no código se necessário
            buildConfigField("boolean", "IS_ADMIN", "false")
            buildConfigField("boolean", "IS_MOTOBOY", "false") // Novo Flag
        }

        // 🟩 App do ADMIN (Gestão, OCR, Impressão)
        create("admin") {
            dimension = "userType"
            applicationId = "com.developersbeeh.pharmaflow.admin"
            versionNameSuffix = "-admin"
            // Define o nome do app na tela do celular
            resValue("string", "app_name", "PharmaFlow Admin")
            // Cria uma constante para checar no código
            buildConfigField("boolean", "IS_ADMIN", "true")
            buildConfigField("boolean", "IS_MOTOBOY", "false") // Novo Flag
        }
        // 🟥 App do MOTOBOY (NOVO)
        create("motoboy") {
            dimension = "userType"
            applicationId = "com.developersbeeh.pharmaflow.motoboy"
            versionNameSuffix = "-moto"
            resValue("string", "app_name", "PharmaFlow Entregas")
            // Flags para controle de lógica
            buildConfigField("boolean", "IS_ADMIN", "false")
            buildConfigField("boolean", "IS_MOTOBOY", "true")
        }
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // --- Padrão (Mantido do seu arquivo original) ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ==========================================================
    // 🚀 ADIÇÕES DO BLUEPRINT (Dependências Profissionais)
    // ==========================================================

    // 🧭 Navegação Compose
    implementation(libs.androidx.navigation.compose)

    // 💉 Hilt (Injeção de Dependência)
    implementation(libs.hilt.android)
    //noinspection UseTomlInstead
    ksp("com.google.dagger:hilt-android-compiler:2.57.2")
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.barcode.scanning)
    // 🔥 Firebase (Auth, Firestore, Crashlytics)
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")

    // 🖼 Coil (Carregamento de Imagens Assíncrono)
    implementation(libs.coil.compose)

    // 💾 Room Database (Banco de dados local / Cache Offline)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // 🧩 Icons Extended (Para ter acesso a todos os ícones Material)
    implementation(libs.androidx.compose.material.icons.extended)


    // Dependência para o pacote de ícones FontAwesome (inclui WhatsApp)
    implementation(libs.iconics.compose)
    // ==========================================================
    // Dependência para o pacote de ícones FontAwesome (inclui WhatsApp)
    // Use a versão resolvida mais recente que termina em "-kotlin"
    implementation(libs.fontawesome.typeface)
    // 📦 DEPENDÊNCIAS ESPECÍFICAS POR FLAVOR
    // ==========================================================
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    // 🟩 Apenas para ADMIN
    // OCR (Reconhecimento de Texto) do Google ML Kit
    //noinspection UseTomlInstead
    "adminImplementation"("com.google.mlkit:text-recognition:16.0.1")
    // Biblioteca de Gráficos (Dashboard) - Ex: Vico
    "adminImplementation"(libs.compose.m3)

    implementation(libs.barcode.scanning)

    // ML Kit - Subject Segmentation (Remoção de Fundo)
    implementation(libs.tasks.vision) // Use a specific stable version

    implementation(libs.gson)
    // 📸 CameraX (Core)
    // Funcionalidade principal da CameraX
    implementation(libs.androidx.camera.core.v151)

    // Integração com a API Camera2 (necessário para a maioria dos dispositivos)
    implementation(libs.androidx.camera.camera2.v151)

    // Observação do ciclo de vida (Lifecycle) para iniciar/parar a câmera automaticamente
    implementation(libs.androidx.camera.lifecycle.v151)

    // Componentes de interface do usuário, como o PreviewView
    implementation(libs.androidx.camera.view.v151)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    implementation(libs.compose)
    implementation(libs.core)
    implementation(libs.views) // Opcional,

// ML Kit - Recorte de Objetos (Subject Segmentation)
    implementation("com.google.android.gms:play-services-mlkit-subject-segmentation:16.0.0-beta1")

    // Corrotinas para Task API do Google
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    // Gerador de QR Code (ZXing)
    implementation("com.google.zxing:core:3.5.4")


    // Paginação
    implementation("androidx.paging:paging-runtime-ktx:3.3.6")
    implementation("androidx.paging:paging-compose:3.3.6")
}