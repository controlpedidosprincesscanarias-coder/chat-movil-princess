// Chat Dpto. Compras Princess (Android). La idea y el desarrollo de la aplicación son de Víctor Alejandro Martín Andrade,
// para el Departamento de Central de Compras de Princess Hotels en Canarias.
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Firma de la versión publicada: la ponen las variables de entorno del workflow (secretos de GitHub)
// o un firma.properties local que nunca se sube (ver .gitignore). Sin firma, solo se compila «debug».
val firma = Properties().apply {
    val f = rootProject.file("firma.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun valorFirma(clave: String): String? = System.getenv("FIRMA_" + clave.uppercase()) ?: firma.getProperty(clave)

android {
    namespace = "com.princesscanarias.chat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.princesscanarias.chat"
        minSdk = 26
        targetSdk = 35
        versionCode = 110       // ← subir en cada versión (1.0.0 → 100, 1.0.2 → 102…)
        versionName = "1.0.10"  // ← debe coincidir con la etiqueta de la Release (v + versionName)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        val almacen = valorFirma("almacen")
        if (almacen != null && file(almacen).exists()) {
            create("publicada") {
                storeFile = file(almacen)
                storePassword = valorFirma("clave_almacen")
                keyAlias = valorFirma("alias")
                keyPassword = valorFirma("clave_alias")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("publicada")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += setOf("META-INF/INDEX.LIST", "META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*")
    }
}

// (v1.0.6) El CHANGELOG.md del repositorio va dentro de la .apk (assets/CHANGELOG.md) para ver las notas de cada
// versión en Ajustes sin Internet. Se copia en cada compilación, así que siempre es el de esa versión.
val carpetaNotas = layout.buildDirectory.dir("generated/notas")
val copiarNotas by tasks.registering(Copy::class) {
    from(rootProject.file("CHANGELOG.md"))
    into(carpetaNotas)
}
android.sourceSets["main"].assets.srcDir(carpetaNotas)
tasks.named("preBuild") { dependsOn(copiarNotas) }

dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(bom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.socket:socket.io-client:2.1.1") {
        exclude(group = "org.json", module = "json")   // Android ya trae org.json
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("io.coil-kt:coil-compose:2.7.0")      // (v1.0.4) fotos de perfil

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")       // org.json real para las pruebas en la JVM
}
