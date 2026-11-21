package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.BuildConfig
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class SealedBackendConfig(
    val baseUrl: String,
    val appSecret: String,
    val wsUrl: String
) {
    val isConfigured: Boolean
        get() = baseUrl.startsWith("http://") || baseUrl.startsWith("https://")
}

/**
 * Runtime vault that unseals build-time AES-256-GCM encrypted backend configuration
 * bound to the app's package name, extracts the live APK signing certificate SHA-256,
 * and computes per-request HMAC-SHA256 attestation signatures.
 */
object AppSecurityVault {

    private const val KEY_DOMAIN_TAG = "bm-seal-v1"
    private const val PLACEHOLDER_SECRET = "REPLACE_WITH_ADMIN_PANEL_APP_KEY"

    @Volatile
    private var cachedConfig: SealedBackendConfig? = null

    @Volatile
    private var cachedCertSha256: String? = null

    fun resolveConfig(context: Context): SealedBackendConfig {
        cachedConfig?.let { return it }
        val pkg = context.packageName
        val salt = BuildConfig.SEALED_SALT
        val rawBaseUrl = unseal(BuildConfig.SEALED_BASE_URL, pkg, salt).trim()
        val normalizedBaseUrl = if (rawBaseUrl.isNotEmpty() && !rawBaseUrl.endsWith("/")) {
            "$rawBaseUrl/"
        } else {
            rawBaseUrl
        }
        val rawSecret = unseal(BuildConfig.SEALED_APP_SECRET, pkg, salt).trim()
        val cleanSecret = if (rawSecret == PLACEHOLDER_SECRET) "" else rawSecret
        val rawWsUrl = unseal(BuildConfig.SEALED_WS_URL, pkg, salt).trim()

        val resolved = SealedBackendConfig(
            baseUrl = normalizedBaseUrl,
            appSecret = cleanSecret,
            wsUrl = rawWsUrl
        )
        cachedConfig = resolved
        return resolved
    }

    fun isConfigured(context: Context): Boolean = resolveConfig(context).isConfigured

    fun getBaseUrl(context: Context): String = resolveConfig(context).baseUrl

    fun getWsUrl(context: Context): String = resolveConfig(context).wsUrl

    /**
     * Encrypts a string using AES-256-GCM bound to [packageName] + [saltHex].
     * Mirrors the Gradle build-time sealing helper (also used in unit tests).
     */
    fun sealForTest(plainText: String, packageName: String, saltHex: String): String {
        if (plainText.isBlank()) return ""
        val keyBytes = deriveAesKey(packageName, saltHex)
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return encodeBase64(combined)
    }

    /**
     * Decrypts a Base64 AES-256-GCM payload bound to [packageName] + [saltHex].
     * Returns an empty string if tampered or decrypted under a different package name.
     */
    fun unseal(sealedBase64: String, packageName: String, saltHex: String): String {
        if (sealedBase64.isBlank() || packageName.isBlank() || saltHex.isBlank()) return ""
        return try {
            val combined = decodeBase64(sealedBase64)
            if (combined.size <= 12) return ""
            val iv = combined.copyOfRange(0, 12)
            val cipherText = combined.copyOfRange(12, combined.size)
            val keyBytes = deriveAesKey(packageName, saltHex)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    private fun deriveAesKey(packageName: String, saltHex: String): ByteArray {
        return MessageDigest.getInstance("SHA-256")
            .digest("$packageName:$saltHex:$KEY_DOMAIN_TAG".toByteArray(Charsets.UTF_8))
    }

    /**
     * Returns the current APK signing certificate SHA-256 as a 64-char uppercase hex string
     * (no colons). Gracefully returns empty string in headless/unit test environments.
     */
    @Suppress("DEPRECATION")
    fun getSigningCertSha256(context: Context): String {
        cachedCertSha256?.let { return it }
        val digestHex = try {
            val pm = context.packageManager
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signingInfo = info.signingInfo
                if (signingInfo == null) {
                    null
                } else if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
            } else {
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                info.signatures
            }
            val firstSig = signatures?.firstOrNull()
            if (firstSig != null) {
                val hash = MessageDigest.getInstance("SHA-256").digest(firstSig.toByteArray())
                hash.joinToString("") { "%02X".format(it) }
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
        cachedCertSha256 = digestHex
        return digestHex
    }

    /**
     * Computes lowercase hex HMAC-SHA256 of the canonical request payload:
     * packageName|certSha256|timestampSeconds|METHOD|/encoded/path
     */
    fun computeRequestSignature(
        packageName: String,
        certSha256: String,
        timestampSeconds: String,
        method: String,
        encodedPath: String,
        appSecret: String
    ): String {
        if (appSecret.isBlank()) return ""
        val normalizedCert = certSha256.replace(":", "").replace(" ", "").uppercase()
        val normalizedPath = if (encodedPath.startsWith("/")) encodedPath else "/$encodedPath"
        val canonical = "$packageName|$normalizedCert|$timestampSeconds|${method.uppercase()}|$normalizedPath"
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(appSecret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
            val raw = mac.doFinal(canonical.toByteArray(Charsets.UTF_8))
            raw.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Strips any server URLs, hostnames, or raw IP addresses from network error messages
     * so backend host details are never exposed to users in the UI.
     */
    fun sanitizeNetworkError(rawMessage: String?, fallback: String = "Unable to connect to cloud service. Please check your internet connection."): String {
        val msg = rawMessage?.trim().orEmpty()
        if (msg.isEmpty()) return fallback
        val lower = msg.lowercase()
        val looksLikeLowLevelNetworkError = lower.contains("http://") ||
            lower.contains("https://") ||
            lower.contains("unable to resolve host") ||
            lower.contains("failed to connect to") ||
            lower.contains("connection refused") ||
            lower.contains("timeout") ||
            lower.contains("timed out") ||
            lower.contains("sslhandshake") ||
            lower.contains("certpath") ||
            lower.contains("socket") ||
            lower.contains("cleartext") ||
            Regex("""\b\d{1,3}(\.\d{1,3}){3}\b""").containsMatchIn(msg) ||
            Regex("""\b[a-z0-9-]+\.[a-z]{2,}\b""", RegexOption.IGNORE_CASE).containsMatchIn(msg)
        return if (looksLikeLowLevelNetworkError) fallback else msg
    }

    // Pure Kotlin RFC-4648 Base64 codec (works on API 24+ and plain JVM unit tests)
    private const val BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    private fun encodeBase64(data: ByteArray): String {
        val out = StringBuilder(((data.size + 2) / 3) * 4)
        var i = 0
        while (i < data.size) {
            val b0 = data[i].toInt() and 0xFF
            val b1 = if (i + 1 < data.size) data[i + 1].toInt() and 0xFF else 0
            val b2 = if (i + 2 < data.size) data[i + 2].toInt() and 0xFF else 0
            val triplet = (b0 shl 16) or (b1 shl 8) or b2
            out.append(BASE64_ALPHABET[(triplet shr 18) and 0x3F])
            out.append(BASE64_ALPHABET[(triplet shr 12) and 0x3F])
            out.append(if (i + 1 < data.size) BASE64_ALPHABET[(triplet shr 6) and 0x3F] else '=')
            out.append(if (i + 2 < data.size) BASE64_ALPHABET[triplet and 0x3F] else '=')
            i += 3
        }
        return out.toString()
    }

    private fun decodeBase64(input: String): ByteArray {
        val clean = input.filterNot { it.isWhitespace() }
        val out = ByteArrayOutputStream((clean.length * 3) / 4)
        var buffer = 0
        var bitsCollected = 0
        for (ch in clean) {
            if (ch == '=') break
            val value = BASE64_ALPHABET.indexOf(ch)
            if (value < 0) continue
            buffer = (buffer shl 6) or value
            bitsCollected += 6
            if (bitsCollected >= 8) {
                bitsCollected -= 8
                out.write((buffer shr bitsCollected) and 0xFF)
            }
        }
        return out.toByteArray()
    }
}
