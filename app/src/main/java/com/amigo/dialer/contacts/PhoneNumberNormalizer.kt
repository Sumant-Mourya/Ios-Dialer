package com.amigo.dialer.contacts

object PhoneNumberNormalizer {
    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        // keep digits only
        var digits = raw.filter { it.isDigit() }
        // drop leading 91 if looks like country code
        if (digits.startsWith("91") && digits.length > 10) {
            digits = digits.removePrefix("91")
        }
        // drop one leading 0 if still longer than 10
        if (digits.startsWith("0") && digits.length > 10) {
            digits = digits.removePrefix("0")
        }
        // trim to last 10 for matching
        if (digits.length > 10) {
            digits = digits.takeLast(10)
        }
        return digits
    }
}
