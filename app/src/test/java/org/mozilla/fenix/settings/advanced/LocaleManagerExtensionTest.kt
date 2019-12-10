/* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.advanced

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import mozilla.components.support.locale.LocaleManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class LocaleManagerExtensionTest {

    @Before
    fun setup() {
        mockkStatic("org.mozilla.fenix.settings.advanced.LocaleManagerExtensionKt")
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `build supported locale list`() {
        val list = LocaleManager.getSupportedLocales()
        assertEquals(26, (list as ArrayList).size)
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `match current stored locale string with a Locale from our list`() {
        val context: Context = mockk()
        mockkObject(LocaleManager)
        val otherLocale = Locale("fr")
        val selectedLocale = Locale("en", "UK")
        val localeList = ArrayList<Locale>()
        localeList.add(otherLocale)
        localeList.add(selectedLocale)

        every { LocaleManager.getCurrentLocale(context) } returns selectedLocale

        assertEquals(selectedLocale, LocaleManager.getSelectedLocale(context, localeList))
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `match null stored locale with the default Locale from our list`() {
        val context: Context = mockk()
        mockkObject(LocaleManager)
        val firstLocale = Locale("fr")
        val secondLocale = Locale("en", "UK")
        val localeList = ArrayList<Locale>()
        localeList.add(firstLocale)
        localeList.add(secondLocale)

        every { LocaleManager.getCurrentLocale(context) } returns null

        assertEquals("en-US", LocaleManager.getSelectedLocale(context, localeList).toLanguageTag())
    }
}
