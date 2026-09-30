package com.example.decosocio.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.example.decosocio.R
import com.example.decosocio.domain.model.BillingPeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()

@Composable
@ReadOnlyComposable
fun formatMoney(cents: Long): String {
    val format = NumberFormat.getCurrencyInstance(currentLocale())
    format.currency = Currency.getInstance("EUR")
    return format.format(cents / 100.0)
}

@Composable
@ReadOnlyComposable
fun formatDate(date: LocalDate): String =
    date.toJavaLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(currentLocale()))

@Composable
@ReadOnlyComposable
fun formatPrice(cents: Long, period: BillingPeriod): String {
    val suffix = when (period) {
        BillingPeriod.MONTHLY -> stringResource(R.string.per_month)
        BillingPeriod.QUARTERLY -> stringResource(R.string.per_quarter)
        BillingPeriod.YEARLY -> stringResource(R.string.per_year)
    }
    return formatMoney(cents) + " " + suffix
}
