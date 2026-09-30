package com.example.decosocio.ui.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.decosocio.R
import com.example.decosocio.domain.model.ConsentPurpose

fun ConsentPurpose.titleRes(): Int = when (this) {
    ConsentPurpose.NEWSLETTER -> R.string.consent_newsletter_title
    ConsentPurpose.MARKETING_PUSH -> R.string.consent_push_title
    ConsentPurpose.PARTNER_OFFERS -> R.string.consent_partners_title
    ConsentPurpose.PERSONALISATION -> R.string.consent_personalisation_title
}

fun ConsentPurpose.descriptionRes(): Int = when (this) {
    ConsentPurpose.NEWSLETTER -> R.string.consent_newsletter_description
    ConsentPurpose.MARKETING_PUSH -> R.string.consent_push_description
    ConsentPurpose.PARTNER_OFFERS -> R.string.consent_partners_description
    ConsentPurpose.PERSONALISATION -> R.string.consent_personalisation_description
}

/** One consent with its own switch; the whole row is the touch target and is read as a switch. */
@Composable
fun ConsentRow(
    purpose: ConsentPurpose,
    granted: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = granted, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(stringResource(purpose.titleRes()), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(purpose.descriptionRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // The Row handles the toggle, so the Switch itself is not separately focusable.
        Switch(checked = granted, onCheckedChange = null, enabled = enabled)
    }
}
