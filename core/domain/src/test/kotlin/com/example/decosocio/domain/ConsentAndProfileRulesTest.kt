package com.example.decosocio.domain

import com.example.decosocio.domain.model.Address
import com.example.decosocio.domain.model.ConsentPurpose
import com.example.decosocio.domain.model.ConsentRules
import com.example.decosocio.domain.model.ConsentSource
import com.example.decosocio.domain.model.ProfileUpdate
import com.example.decosocio.domain.model.ProfileValidator
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ConsentAndProfileRulesTest {

    @Test
    fun everythingIsOffByDefault() {
        val consents = ConsentRules.defaults()
        assertTrue(ConsentPurpose.entries.none { consents.isGranted(it) })
        assertFalse(consents.hasBeenAsked)
    }

    @Test
    fun onboardingRecordsEveryChoiceEvenWhenOff() {
        val all = ConsentPurpose.entries.associateWith { it == ConsentPurpose.NEWSLETTER }
        val after = ConsentRules.apply(ConsentRules.defaults(), all, 1000L, ConsentSource.ONBOARDING)
        assertEquals(ConsentPurpose.entries.size, after.history.size)
        assertTrue(after.isGranted(ConsentPurpose.NEWSLETTER))
        assertTrue(after.hasBeenAsked)
        assertTrue(after.history.all { it.textVersion == ConsentRules.TEXT_VERSION })
    }

    @Test
    fun laterSavesOnlyRecordRealChanges() {
        val all = ConsentPurpose.entries.associateWith { false }
        val onboarded = ConsentRules.apply(ConsentRules.defaults(), all, 1000L, ConsentSource.ONBOARDING)
        val unchanged = ConsentRules.apply(onboarded, all, 2000L, ConsentSource.SETTINGS)
        assertSame(onboarded, unchanged)

        val changed = ConsentRules.apply(
            onboarded,
            all + (ConsentPurpose.MARKETING_PUSH to true),
            3000L,
            ConsentSource.SETTINGS,
        )
        assertEquals(onboarded.history.size + 1, changed.history.size)
        assertEquals(ConsentPurpose.MARKETING_PUSH, changed.history.first().purpose)
        assertEquals(3000L, changed.history.first().changedAtEpochMs)
    }

    @Test
    fun marketingPushNeedsConsentAndPermission() {
        val granted = ConsentRules.apply(
            ConsentRules.defaults(),
            mapOf(ConsentPurpose.MARKETING_PUSH to true),
            1L,
            ConsentSource.SETTINGS,
        )
        assertTrue(ConsentRules.marketingPushAllowed(granted, notificationPermissionGranted = true))
        assertFalse(ConsentRules.marketingPushAllowed(granted, notificationPermissionGranted = false))
        assertFalse(ConsentRules.marketingPushAllowed(ConsentRules.defaults(), notificationPermissionGranted = true))
    }

    private val valid = ProfileUpdate(
        firstName = "Ana",
        lastName = "Ribeiro",
        email = "ana@example.pt",
        phone = "+351 912 345 678",
        address = Address("Rua de Exemplo 10", "1050-021", "Lisboa"),
    )

    @Test
    fun validProfilePasses() {
        assertTrue(ProfileValidator.validate(valid).isEmpty())
        assertTrue(ProfileValidator.validate(valid.copy(phone = null, address = null)).isEmpty())
    }

    @Test
    fun invalidFieldsAreReported() {
        val errors = ProfileValidator.validate(
            valid.copy(
                firstName = " ",
                email = "ana@",
                phone = "12",
                address = Address("Rua", "1050021", ""),
            ),
        )
        assertEquals(
            setOf(ProfileField.FIRST_NAME, ProfileField.EMAIL, ProfileField.PHONE, ProfileField.POSTAL_CODE, ProfileField.CITY),
            errors,
        )
    }
}
