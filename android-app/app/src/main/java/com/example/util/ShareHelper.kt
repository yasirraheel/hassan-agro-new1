package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ShareHelper {
    fun getShareUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun sharePdfOnWhatsApp(
        context: Context,
        pdfFile: File,
        caption: String
    ): Boolean {
        val uri = getShareUri(context, pdfFile)
        val pm = context.packageManager
        val whatsAppPackage = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (whatsAppPackage != null) {
                setPackage(whatsAppPackage)
            }
        }
        return try {
            context.startActivity(
                if (whatsAppPackage != null) intent
                else Intent.createChooser(intent, "Share Invoice via WhatsApp")
            )
            true
        } catch (_: Exception) {
            shareFileGeneric(context, pdfFile, "application/pdf", "Share Invoice", caption)
        }
    }

    fun sharePdfViaEmail(
        context: Context,
        pdfFile: File,
        recipientEmail: String,
        subject: String,
        body: String
    ): Boolean {
        val uri = getShareUri(context, pdfFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            if (recipientEmail.isNotBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail.trim()))
            }
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(Intent.createChooser(intent, "Send Invoice via Email"))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun shareFileGeneric(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String,
        extraText: String = ""
    ): Boolean {
        val uri = getShareUri(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            if (extraText.isNotBlank()) {
                putExtra(Intent.EXTRA_TEXT, extraText)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(Intent.createChooser(intent, chooserTitle))
            true
        } catch (_: Exception) {
            false
        }
    }
}
