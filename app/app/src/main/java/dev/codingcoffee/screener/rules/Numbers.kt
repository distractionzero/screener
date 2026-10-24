package dev.codingcoffee.screener.rules

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat

const val UNKNOWN_REGION = "ZZ"
const val INDIA = "IN"

private val phoneUtil: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }

val supportedRegions: Set<String> get() = phoneUtil.supportedRegions

/** Country calling code for a region, e.g. 91 for IN, or 0 if the region is unknown. */
fun callingCode(region: String?): Int = region?.let { phoneUtil.getCountryCodeForRegion(it) } ?: 0

fun trunkPrefix(region: String): String? = phoneUtil.getNddPrefixForRegion(region, true)?.takeIf { it.isNotEmpty() }

/** E.164 for anything libphonenumber can parse, otherwise the number with formatting stripped. */
fun normalizeNumber(raw: String?, region: String?): String? {
    val cleaned = raw?.filterNot { it == ' ' || it == '-' || it == '(' || it == ')' }?.takeIf { it.isNotEmpty() }
        ?: return null
    return try {
        phoneUtil.format(phoneUtil.parse(cleaned, region), PhoneNumberFormat.E164)
    } catch (_: NumberParseException) {
        cleaned
    }
}

/** Spam numbers used to be stored without +91 or a leading 0, back when only India was supported. */
fun migrateSpamKey(key: String): String = if (key.startsWith("+")) key else normalizeNumber(key, INDIA) ?: key
