/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.AuthType
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.service.fxa.SyncEngine
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.service.fxa.manager.SyncEnginesStorage

/**
 * A view to help manage the sync preference in the "Logins and passwords" and "Credit cards"
 * settings. The provided [syncPreference] is used to navigate to the different fragments
 * that manages the sync account authentication. A toggle  will be also added
 * depending on the sync account status.
 *
 * @param syncPreference The sync [SyncPreference] to update and handle navigation.
 * @param lifecycleOwner View lifecycle owner used to determine when to cancel UI jobs.
 * @param accountManager An instance of [FxaAccountManager].
 * @param syncEngine The sync engine that will be used for the sync status lookup.
 * @param notLoggedInTitle Text to label the setting with when user is not logged in.
 * @param loggedInTitle Text to label the setting with when user is logged in.
 * @param onSignInToSyncClicked A callback executed when the [syncPreference] is clicked with a
 * preference status of "Sign in to Sync".
 * @param onReconnectClicked A callback executed when the [syncPreference] is clicked with a
 * preference status of "Reconnect".
 */
@Suppress("LongParameterList")
class SyncPreferenceView(
    private val syncPreference: SyncPreference,
    lifecycleOwner: LifecycleOwner,
    accountManager: FxaAccountManager,
    private val syncEngine: SyncEngine,
    private val notLoggedInTitle: String,
    private val loggedInTitle: String,
    private val onSignInToSyncClicked: () -> Unit = {},
    private val onReconnectClicked: () -> Unit = {}
) {

    init {
        accountManager.register(object : AccountObserver {
            override fun onAuthenticated(account: OAuthAccount, authType: AuthType) {
                MainScope().launch { updateSyncPreferenceStatus() }
            }

            override fun onLoggedOut() {
                MainScope().launch { updateSyncPreferenceNeedsLogin() }
            }

            override fun onAuthenticationProblems() {
                MainScope().launch { updateSyncPreferenceNeedsReauth() }
            }
        }, owner = lifecycleOwner)

        val accountExists = accountManager.authenticatedAccount() != null
        val needsReauth = accountManager.accountNeedsReauth()

        when {
            needsReauth -> updateSyncPreferenceNeedsReauth()
            accountExists -> updateSyncPreferenceStatus()
            !accountExists -> updateSyncPreferenceNeedsLogin()
        }
    }

    /**
     * Shows the current status of the sync preference ("On"/"Off") for the logged in user.
     */
    private fun updateSyncPreferenceStatus() {
        syncPreference.apply {
            isSwitchWidgetVisible = true

            val syncEnginesStatus = SyncEnginesStorage(context).getStatus()
            val syncStatus = syncEnginesStatus.getOrElse(syncEngine) { false }

            title = loggedInTitle
            isChecked = syncStatus

            setOnPreferenceChangeListener { _, newValue ->
                SyncEnginesStorage(context).setStatus(syncEngine, newValue as Boolean)
                true
            }
        }
    }

    /**
     * Display that the user can "Sign in to Sync" when the user is logged off.
     */
    private fun updateSyncPreferenceNeedsLogin() {
        syncPreference.apply {
            isSwitchWidgetVisible = false

            title = notLoggedInTitle

            setOnPreferenceChangeListener { _, _ ->
                onSignInToSyncClicked()
                false
            }
        }
    }

    /**
     * Displays that the user needs to "Reconnect" to fix their account problems with sync.
     */
    private fun updateSyncPreferenceNeedsReauth() {
        syncPreference.apply {
            isSwitchWidgetVisible = false

            title = notLoggedInTitle

            setOnPreferenceChangeListener { _, _ ->
                onReconnectClicked()
                false
            }
        }
    }
}
