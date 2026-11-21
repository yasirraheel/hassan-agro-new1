import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

fun loadEnvSecret(key: String, defaultValue: String = ""): String {
  val merged = Properties()
  listOf(".env.example", ".env").forEach { fileName ->
    val f = rootProject.file(fileName)
    if (f.exists()) {
      f.inputStream().use { merged.load(it) }
    }
  }
  val fromEnv = System.getenv(key)?.trim()
  val raw = if (!fromEnv.isNullOrEmpty()) fromEnv else merged.getProperty(key)?.trim().orEmpty()
  val unquoted = raw.removeSurrounding("\"").removeSurrounding("'").trim()
  return unquoted.ifEmpty { defaultValue }
}

fun sealBuildSecret(plainText: String, appId: String, saltHex: String): String {
  if (plainText.isBlank()) return ""
  val keyBytes = MessageDigest.getInstance("SHA-256")
    .digest("$appId:$saltHex:bm-seal-v1".toByteArray(Charsets.UTF_8))
  val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
  val cipher = Cipher.getInstance("AES/GCM/NoPadding")
  cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, iv))
  val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
  val combined = ByteArray(iv.size + encrypted.size)
  System.arraycopy(iv, 0, combined, 0, iv.size)
  System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
  return Base64.getEncoder().encodeToString(combined)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    val appId = "com.aistudio.hassanagro.k7m2px"
    applicationId = appId
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    val saltBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
    val sealedSaltHex = saltBytes.joinToString("") { "%02x".format(it) }
    val rawBaseUrl = loadEnvSecret("BACKEND_BASE_URL", "https://ledger.hassanagro.com/")
    val rawAppSecret = loadEnvSecret("APP_CLIENT_SECRET", "")
    val rawWsUrl = loadEnvSecret("BACKEND_WS_URL", "")

    buildConfigField("String", "SEALED_SALT", "\"$sealedSaltHex\"")
    buildConfigField("String", "SEALED_BASE_URL", "\"${sealBuildSecret(rawBaseUrl, appId, sealedSaltHex)}\"")
    buildConfigField("String", "SEALED_APP_SECRET", "\"${sealBuildSecret(rawAppSecret, appId, sealedSaltHex)}\"")
    buildConfigField("String", "SEALED_WS_URL", "\"${sealBuildSecret(rawWsUrl, appId, sealedSaltHex)}\"")
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // AI Studio can use its project keystore; local builds use Android's default debug key.
      signingConfig = if (rootProject.file("debug.keystore").exists()) {
        signingConfigs.getByName("debugConfig")
      } else {
        signingConfigs.getByName("debug")
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
  ignoreList.add("BACKEND_BASE_URL")
  ignoreList.add("APP_CLIENT_SECRET")
  ignoreList.add("BACKEND_WS_URL")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.mlkit.barcode.scanning)
  // implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  // implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
