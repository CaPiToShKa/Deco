package com.example.decosocio.domain.model

import com.example.decosocio.domain.ProfileField

data class Address(
    val street: String,
    val postalCode: String,
    val city: String,
)

/**
 * The member as the app sees it. [contactKey] is the same identifier as the SFMC
 * Contact Key / SubscriberKey, so push registrations and Data Extension rows line up.
 */
data class MemberProfile(
    val contactKey: String,
    val memberNumber: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?,
    val address: Address?,
    val nif: String?,
    val preferredLanguage: String,
) {
    val fullName: String get() = "$firstName $lastName".trim()
}

data class ProfileUpdate(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?,
    val address: Address?,
)

fun MemberProfile.toUpdate(): ProfileUpdate =
    ProfileUpdate(firstName, lastName, email, phone, address)

fun MemberProfile.apply(update: ProfileUpdate): MemberProfile = copy(
    firstName = update.firstName.trim(),
    lastName = update.lastName.trim(),
    email = update.email.trim(),
    phone = update.phone?.trim()?.ifEmpty { null },
    address = update.address?.let {
        Address(it.street.trim(), it.postalCode.trim(), it.city.trim())
    },
)

object ProfileValidator {
    private val emailRegex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")

    /** Portuguese postal code: 4 digits, hyphen, 3 digits (e.g. 1050-021). */
    private val postalCodeRegex = Regex("^\\d{4}-\\d{3}$")
    private val phoneRegex = Regex("^\\+?[0-9 ]{9,16}$")

    fun validate(update: ProfileUpdate): Set<ProfileField> = buildSet {
        if (update.firstName.isBlank()) add(ProfileField.FIRST_NAME)
        if (update.lastName.isBlank()) add(ProfileField.LAST_NAME)
        if (!emailRegex.matches(update.email.trim())) add(ProfileField.EMAIL)
        val phone = update.phone?.trim().orEmpty()
        if (phone.isNotEmpty() && !phoneRegex.matches(phone)) add(ProfileField.PHONE)
        update.address?.let { address ->
            val anyFilled = address.street.isNotBlank() ||
                address.postalCode.isNotBlank() ||
                address.city.isNotBlank()
            if (anyFilled) {
                if (address.street.isBlank()) add(ProfileField.STREET)
                if (!postalCodeRegex.matches(address.postalCode.trim())) add(ProfileField.POSTAL_CODE)
                if (address.city.isBlank()) add(ProfileField.CITY)
            }
        }
    }
}
