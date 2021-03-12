/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.crashes

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.lib.crash.Crash
import mozilla.components.support.test.ext.joinBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.DisableNavGraphProviderAssertionRule
import org.mozilla.fenix.R
import org.mozilla.fenix.components.Components
import org.mozilla.fenix.components.metrics.Event

import org.mozilla.fenix.utils.Settings

class CrashReporterControllerTest {

    @get:Rule
    val disableNavGraphProviderAssertionRule = DisableNavGraphProviderAssertionRule()

    private lateinit var components: Components
    private lateinit var crash: Crash
    private lateinit var sessionId: String
    private lateinit var navController: NavController
    private lateinit var settings: Settings

    @Before
    fun setup() {
        components = mockk(relaxed = true)
        crash = mockk()
        sessionId = "testId"
        navController = mockk(relaxed = true)
        settings = mockk()

        val currentDest: NavDestination = mockk()
        every { navController.currentDestination } returns currentDest
        every { currentDest.id } returns R.id.crashReporterFragment
    }

    @Test
    fun `reports crash reporter opened`() {
        CrashReporterController(crash, sessionId, navController, components, settings)

        verify { components.analytics.metrics.track(Event.CrashReporterOpened) }
    }

    @Test
    fun `handle close and restore tab`() {
        val controller = CrashReporterController(crash, sessionId, navController, components, settings)
        controller.handleCloseAndRestore(sendCrash = false)?.joinBlocking()

        verify { components.analytics.metrics.track(Event.CrashReporterClosed(false)) }
        verify { components.useCases.sessionUseCases.crashRecovery.invoke() }
        verify { navController.popBackStack() }
    }

    @Test
    fun `handle close and remove tab`() {
        val controller = CrashReporterController(crash, sessionId, navController, components, settings)
        controller.handleCloseAndRemove(sendCrash = false)?.joinBlocking()

        verify { components.analytics.metrics.track(Event.CrashReporterClosed(false)) }
        verify { components.useCases.tabsUseCases.removeTab(sessionId) }
        verify { components.useCases.sessionUseCases.crashRecovery.invoke() }
        verify {
            navController.navigate(CrashReporterFragmentDirections.actionGlobalHome(), null)
        }
    }

    @Test
    fun `don't submit report if setting is turned off`() {
        every { settings.isCrashReportingEnabled } returns false

        val controller = CrashReporterController(crash, sessionId, navController, components, settings)
        controller.handleCloseAndRestore(sendCrash = true)?.joinBlocking()

        verify { components.analytics.metrics.track(Event.CrashReporterClosed(false)) }
    }

    @Test
    fun `submit report if setting is turned on`() {
        every { settings.isCrashReportingEnabled } returns true

        val controller = CrashReporterController(crash, sessionId, navController, components, settings)
        controller.handleCloseAndRestore(sendCrash = true)?.joinBlocking()

        verify { components.analytics.crashReporter.submitReport(crash) }
        verify { components.analytics.metrics.track(Event.CrashReporterClosed(true)) }
    }
}
