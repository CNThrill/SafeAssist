package com.safeassist.maramag.utils

object MaramagAddressData {

    private fun puroks(count: Int): List<String> = (1..count).map { "Purok $it" }

    // Official 20 barangays of Maramag, Bukidnon with corresponding purok lists.
    // Purok dropdowns remain editable so residents in named/sub-puroks can type if needed.
    private val barangayPuroks: Map<String, List<String>> = linkedMapOf(
        "Anahawon" to puroks(7),
        "Bagongsilang" to puroks(6),
        "Base Camp" to puroks(10),
        "Bayabason" to puroks(7),
        "Camp 1" to puroks(10),
        "Colambugon" to puroks(6),
        "Dagumba-an" to puroks(8),
        "Danggawan" to puroks(7),
        "Dologon" to puroks(12),
        "Kiharong" to puroks(5),
        "Kisanday" to puroks(8),
        "Kuya" to puroks(8),
        "La Roxas" to puroks(6),
        "North Poblacion" to puroks(12),
        "Panadtalan" to puroks(9),
        "Panalsalan" to puroks(6),
        "San Miguel" to puroks(8),
        "San Roque" to puroks(7),
        "South Poblacion" to puroks(12),
        "Tubigon" to puroks(6)
    )

    val BARANGAYS: List<String> = barangayPuroks.keys.toList()

    fun puroksForBarangay(barangay: String): List<String> {
        val trimmed = barangay.trim()
        return barangayPuroks[trimmed] ?: puroks(10)
    }
}
