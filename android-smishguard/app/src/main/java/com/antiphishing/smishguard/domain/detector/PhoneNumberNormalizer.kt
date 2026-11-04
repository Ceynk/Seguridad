package com.antiphishing.smishguard.domain.detector

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil

data class NormalizedPhoneNumber(
    val e164: String?,
    val countryCode: Int?,
    val isValid: Boolean
)

object PhoneNumberNormalizer {

    private const val DEFAULT_REGION = "CO"
    private val util = PhoneNumberUtil.getInstance()

    fun normalize(rawNumber: String, defaultRegion: String = DEFAULT_REGION): NormalizedPhoneNumber {
        val trimmed = rawNumber.trim()
        if (trimmed.isBlank()) {
            return NormalizedPhoneNumber(null, null, false)
        }
        return try {
            val parsed = util.parse(trimmed, defaultRegion)
            val valid = util.isValidNumber(parsed)
            NormalizedPhoneNumber(
                e164 = if (valid) util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164) else null,
                countryCode = parsed.countryCode.takeIf { it > 0 },
                isValid = valid
            )
        } catch (_: NumberParseException) {
            NormalizedPhoneNumber(null, null, false)
        }
    }

    fun isInternational(rawNumber: String, defaultRegion: String = DEFAULT_REGION): Boolean {
        val parsed = try {
            util.parse(rawNumber.trim(), defaultRegion)
        } catch (_: NumberParseException) {
            return false
        }
        return util.getRegionCodeForNumber(parsed) != defaultRegion
    }
}
