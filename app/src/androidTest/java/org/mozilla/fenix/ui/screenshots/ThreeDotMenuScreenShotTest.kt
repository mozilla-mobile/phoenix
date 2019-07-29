package org.mozilla.fenix.ui.screenshots

import android.os.SystemClock
import android.widget.ImageView
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until

import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.helpers.TestAssetHelper

import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule

import br.com.concretesolutions.kappuccino.actions.ClickActions
import br.com.concretesolutions.kappuccino.extensions.type
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.Matchers
import org.junit.Before
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.click
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar
import org.mozilla.fenix.ui.robots.swipeToBottom

class ThreeDotMenuScreenShotTest : ScreenshotTest() {

    private lateinit var mockWebServer: MockWebServer
    @Rule @JvmField
    val localeTestRule = LocaleTestRule()

    @get:Rule
    var mActivityTestRule: ActivityTestRule<HomeActivity> = HomeActivityTestRule()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer().apply {
            setDispatcher(AndroidAssetDispatcher())
            start()
        }
    }

    @After
    fun tearDown() {
        mActivityTestRule.getActivity().finishAndRemoveTask()
        mockWebServer.shutdown()
    }

    @Test
    fun threeDotMenu() {
        homeScreen {
        }.openThreeDotMenu { }
        Screengrab.screenshot("three-dot-menu")
        device.pressBack()
    }

    @Test
    fun settingsTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings { }
        Screengrab.screenshot("settings")

        SystemClock.sleep(TestAssetHelper.waitingTimeShort)
        settingsAccount()
        Screengrab.screenshot("settings-sync")
        device.pressBack()

        settingsTheme()
        Screengrab.screenshot("settings-theme")
        device.pressBack()

        settingsSearch()
        Screengrab.screenshot("settings-search")
        device.pressBack()

        settingsAccessibility()
        Screengrab.screenshot("settings-accessibility")
        device.pressBack()

        settingsTp()
        Screengrab.screenshot("settings-tp")
        device.pressBack()
    }

    @Test
    fun settingsAfterScrollMenusTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
            swipeToBottom()
        }
        Screengrab.screenshot("settings-scroll-to-bottom")
        SystemClock.sleep(TestAssetHelper.waitingTime)

        settingsRemoveData()
        Screengrab.screenshot("settings-delete-browsing-data")
        device.pressBack()
        SystemClock.sleep(TestAssetHelper.waitingTime)

        settingsTelemetry()
        Screengrab.screenshot("settings-telemetry")
        device.pressBack()
    }

    @Test
    fun libraryTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openLibrary { }
        Screengrab.screenshot("library")

        bookmarksButton()
        Screengrab.screenshot("library-bookmarks")
        device.pressBack()
        historyButton()
        Screengrab.screenshot("library-history")
    }

    @Test
    fun bookmarksManagementTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openLibrary { }
        bookmarksButton()
        addBookmarkFolderButton()
        Screengrab.screenshot("add-folder-view")

        confirmAddFolderButton()
        Screengrab.screenshot("error-empty-folder-name")

        // Enter folder name and save
        addFolderName()
        confirmAddFolderButton()

        // Open folder menu
        bookmarkFolderMenu()
        Screengrab.screenshot("folder-menu")

        // Edit folder menu
        editBookmarkFolder()
        device.pressBack()
        Screengrab.screenshot("edit-bookmark-folder-menu")

        // Delete folder
        bookmarkFolderMenu()
        deleteBookmarkFolder()
        Screengrab.screenshot("delete-bookmark-folder-menu")
    }

    @Test
    fun tabMenuTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }
        device.wait(Until.findObjects(By.res("quick_action_sheet_handle")), TestAssetHelper.waitingTimeShort)
        openQuickActionMenu()
        Screengrab.screenshot("browser-tab-share-bookmark")
        device.findObject(UiSelector()
                .instance(2)
                .className(ImageView::class.java)).click()
        Screengrab.screenshot("browser-tab-menu")

        device.pressBack()
        device.findObject(UiSelector()
                .instance(1)
                .className(ImageView::class.java)).click()
        // Home screen with visited tabs
        device.wait(Until.findObjects(By.text("Save to collection")), TestAssetHelper.waitingTimeShort)
        openTabsMenu()
        Screengrab.screenshot("open-tabs-menu")
        device.pressBack()
        device.wait(Until.findObjects(By.text("Save to collection")), TestAssetHelper.waitingTimeShort)
        Screengrab.screenshot("save-collection-button")

        /*
        // Save tab to Collection -> problem pressing on Enter to get the collection saved
        saveToCollectionButton()
        Screengrab.screenshot("save-collection-view")

        // Go back to homescreen after saving one collection
        mDevice.wait(Until.findObjects(By.text("Save to collection")), TestAssetHelper.waitingTimeShort)
        Screengrab.screenshot("saved-tab")

        // Open Collection menu
        collectionsButton()
        Screengrab.screenshot("saved-collections-menu")
        pressImeActionButton()
        */
    }
}

fun bookmarksButton() = ClickActions.click { text(R.string.library_bookmarks) }
fun historyButton() = ClickActions.click { text(R.string.library_history) }
fun settingsAccount() = ClickActions.click { text(R.string.preferences_sync) }

fun settingsSearch() = ClickActions.click { text(R.string.preferences_search_engine) }
fun settingsTheme() = ClickActions.click { text(R.string.preferences_theme) }
fun settingsAccessibility() = ClickActions.click { text(R.string.preferences_accessibility) }
fun settingsTp() = ClickActions.click { text(R.string.preferences_tracking_protection) }
fun settingsRemoveData() = ClickActions.click { text(R.string.preferences_delete_browsing_data) }
fun settingsTelemetry() = ClickActions.click { text(R.string.preferences_data_collection) }

fun openTabsMenu() = Espresso.onView(Matchers.allOf(ViewMatchers.withId(R.id.tabs_overflow_button))).click()
fun openQuickActionMenu() = Espresso.onView(Matchers.allOf(ViewMatchers.withId(R.id.quick_action_sheet_handle))).click()
fun saveToCollectionButton() = Espresso.onView(Matchers.allOf(ViewMatchers.withId(R.id.save_tab_group_button))).click()
fun collectionsButton() = Espresso.onView(Matchers.allOf(ViewMatchers.withId(R.id.collection_overflow_button))).click()
fun addBookmarkFolderButton() = Espresso.onView(Matchers.allOf(ViewMatchers.withId(R.id.add_bookmark_folder))).click()
fun confirmAddFolderButton() = Espresso.onView(Matchers.allOf(ViewMatchers.withId(R.id.confirm_add_folder_button))).click()
fun addFolderName() = Espresso.onView(Matchers.allOf(ViewMatchers.withId(R.id.bookmark_add_folder_title_edit)))
        .type("folder")
fun bookmarkFolderMenu() = Espresso.onView(Matchers.allOf(ViewMatchers.withId(R.id.bookmark_overflow))).click()
fun editBookmarkFolder() = ClickActions.click { text(R.string.bookmark_menu_edit_button) }
fun deleteBookmarkFolder() = ClickActions.click { text(R.string.bookmark_menu_delete_button) }
