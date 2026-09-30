@file:OptIn(ExperimentalCoroutinesApi::class)

package com.example.decosocio.ui

import com.example.decosocio.data.demo.DemoBackend
import com.example.decosocio.data.demo.DemoControl
import com.example.decosocio.data.demo.DemoSeed
import com.example.decosocio.domain.repository.DateProvider
import com.example.decosocio.push.PushRegistrar
import com.example.decosocio.push.PushState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate

object FixedDates : DateProvider {
    override fun today() = LocalDate(2026, 9, 30)
    override fun nowEpochMillis() = 1_790_000_000_000L
}

class FakePush : PushRegistrar {
    override val state: StateFlow<PushState> = MutableStateFlow(PushState(sfmcConfigured = false))
    var loggedInAs: String? = null
    var marketingConsent: Boolean? = null
    var loggedOut = false
    override fun onLogin(contactKey: String) { loggedInAs = contactKey }
    override fun onLogout() { loggedOut = true }
    override fun setMarketingConsent(granted: Boolean) { marketingConsent = granted }
    override fun syncPushWithPermission() = Unit
    override fun notificationsPermitted() = true
    override fun showLocalTestNotification(title: String, body: String) = true
}

/** Demo backend with no artificial latency, optionally already signed in. */
fun demoBackend(control: DemoControl = DemoControl(), signedIn: Boolean = true): DemoBackend {
    val backend = DemoBackend(control, FixedDates, latencyMs = 0)
    if (signedIn) runBlocking { backend.login(DemoSeed.DEMO_EMAIL, DemoSeed.DEMO_PASSWORD) }
    return backend
}

/** Runs a ViewModel test with Dispatchers.Main replaced, so viewModelScope works on the JVM. */
fun vmTest(block: suspend TestScope.() -> Unit): TestResult {
    val dispatcher = UnconfinedTestDispatcher()
    Dispatchers.setMain(dispatcher)
    try {
        return runTest(dispatcher) { block() }
    } finally {
        Dispatchers.resetMain()
    }
}
