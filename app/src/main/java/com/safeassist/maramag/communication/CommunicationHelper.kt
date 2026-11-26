package com.safeassist.maramag.communication

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri

object CommunicationHelper {

    sealed class ActionResult {
        data object Success : ActionResult()
        data class Error(val message: String) : ActionResult()
    }

    fun copyToClipboard(context: Context, label: String, text: String): ActionResult {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return ActionResult.Error("Clipboard service unavailable on this device.")
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
            ActionResult.Success
        } catch (e: Exception) {
            ActionResult.Error("Could not copy text: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    fun openDialer(context: Context, phoneNumber: String): ActionResult {
        val cleaned = phoneNumber.trim()
        if (cleaned.isBlank()) {
            return ActionResult.Error("No phone number is configured.")
        }
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${Uri.encode(cleaned)}")
        }
        return try {
            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                ActionResult.Success
            } else {
                ActionResult.Error("No calling application found on this device. You can copy the number instead.")
            }
        } catch (e: Exception) {
            ActionResult.Error("Unable to open phone dialer: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    fun openSmsApp(context: Context, phoneNumber: String, messageBody: String): ActionResult {
        return openSmsApp(context, listOf(phoneNumber), messageBody)
    }

    fun openSmsApp(context: Context, phoneNumbers: List<String>, messageBody: String): ActionResult {
        val cleaned = phoneNumbers
            .map { num -> num.filter { it.isDigit() || it == '+' } }
            .filter { it.isNotBlank() }
            .distinct()
        val uri = if (cleaned.isNotEmpty()) {
            Uri.parse("smsto:${cleaned.joinToString(";") { Uri.encode(it) }}")
        } else {
            Uri.parse("smsto:")
        }
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", messageBody)
            putExtra(Intent.EXTRA_TEXT, messageBody)
        }
        return try {
            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                ActionResult.Success
            } else {
                ActionResult.Error("No SMS app found. Copy your message instead.")
            }
        } catch (e: Exception) {
            ActionResult.Error("Unable to open SMS app: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
