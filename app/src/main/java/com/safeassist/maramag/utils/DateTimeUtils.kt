package com.safeassist.maramag.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private val displayFormatter = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault())

    fun formatTimestamp(millis: Long): String {
        return displayFormatter.format(Date(millis))
    }
}
