package com.example

import com.example.util.AppSecurityVault
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSecurityVaultTest {

    @Test
    fun sealAndUnseal_succeedsOnlyForMatchingPackageAndSalt() {
        val pkg = "com.business.manager"
        val salt = "0123456789abcdef0123456789abcdef"
        val plainUrl = "https://ledger.hassanagro.com/"
        val plainSecret = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"

        val sealedUrl = AppSecurityVault.sealForTest(plainUrl, pkg, salt)
        val sealedSecret = AppSecurityVault.sealForTest(plainSecret, pkg, salt)

        assertNotEquals(plainUrl, sealedUrl)
        assertNotEquals(plainSecret, sealedSecret)

        // Matching package name unseals cleanly
        assertEquals(plainUrl, AppSecurityVault.unseal(sealedUrl, pkg, salt))
        assertEquals(plainSecret, AppSecurityVault.unseal(sealedSecret, pkg, salt))

        // Cloned / repackaged APK with a different package name cannot decrypt
        assertEquals("", AppSecurityVault.unseal(sealedUrl, "com.clone.fakeapp", salt))
        assertEquals("", AppSecurityVault.unseal(sealedSecret, "com.clone.fakeapp", salt))
        assertEquals("", AppSecurityVault.unseal(sealedSecret, pkg, "wrongsalt"))
    }

    @Test
    fun computeRequestSignature_matchesExpectedHmacFormat() {
        val sig1 = AppSecurityVault.computeRequestSignature(
            packageName = "com.business.manager",
            certSha256 = "aa:bb:cc:dd",
            timestampSeconds = "1730000000",
            method = "post",
            encodedPath = "/api/v1/auth/login",
            appSecret = "super-secret-key"
        )
        val sig2 = AppSecurityVault.computeRequestSignature(
            packageName = "com.business.manager",
            certSha256 = "AABBCCDD",
            timestampSeconds = "1730000000",
            method = "POST",
            encodedPath = "api/v1/auth/login",
            appSecret = "super-secret-key"
        )
        val sigDifferentSecret = AppSecurityVault.computeRequestSignature(
            packageName = "com.business.manager",
            certSha256 = "AABBCCDD",
            timestampSeconds = "1730000000",
            method = "POST",
            encodedPath = "/api/v1/auth/login",
            appSecret = "wrong-secret-key"
        )

        assertEquals(64, sig1.length)
        assertTrue(sig1.all { it in '0'..'9' || it in 'a'..'f' })
        assertEquals(sig1, sig2)
        assertNotEquals(sig1, sigDifferentSecret)
    }

    @Test
    fun sanitizeNetworkError_hidesServerHostAndPreservesFriendlyMessages() {
        val hostLeak = AppSecurityVault.sanitizeNetworkError("Unable to resolve host \"ledger.hassanagro.com\": No address associated with hostname")
        assertTrue(!hostLeak.contains("hassanagro", ignoreCase = true))

        val ipLeak = AppSecurityVault.sanitizeNetworkError("Failed to connect to /192.168.1.50:443")
        assertTrue(!ipLeak.contains("192.168.1.50"))

        val friendly = AppSecurityVault.sanitizeNetworkError("Invalid email or password")
        assertEquals("Invalid email or password", friendly)
    }

    @Test
    fun buildConfig_unsealsBuildTimeEncryptedServerUrl() {
        val pkg = "com.business.manager"
        assertTrue(BuildConfig.SEALED_SALT.length == 32)
        assertTrue(BuildConfig.SEALED_BASE_URL.isNotBlank())
        assertTrue(!BuildConfig.SEALED_BASE_URL.contains("http"))

        val decryptedUrl = AppSecurityVault.unseal(
            BuildConfig.SEALED_BASE_URL,
            pkg,
            BuildConfig.SEALED_SALT
        )
        assertTrue(decryptedUrl.startsWith("http://") || decryptedUrl.startsWith("https://"))
    }
}
