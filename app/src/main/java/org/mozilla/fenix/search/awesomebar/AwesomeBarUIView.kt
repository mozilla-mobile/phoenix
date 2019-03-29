package org.mozilla.fenix.search.awesomebar

/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.fragment_search.*
import mozilla.components.browser.awesomebar.BrowserAwesomeBar
import mozilla.components.browser.search.SearchEngine
import mozilla.components.feature.awesomebar.provider.ClipboardSuggestionProvider
import mozilla.components.feature.awesomebar.provider.HistoryStorageSuggestionProvider
import mozilla.components.feature.awesomebar.provider.SearchSuggestionProvider
import mozilla.components.feature.awesomebar.provider.SessionSuggestionProvider
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.support.ktx.android.graphics.drawable.toBitmap
import org.jetbrains.anko.textColor
import org.mozilla.fenix.DefaultThemeManager
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.mvi.UIView
import org.mozilla.fenix.search.SearchFragmentDirections
import org.mozilla.fenix.utils.Settings

@SuppressWarnings("TooManyFunctions")
class AwesomeBarUIView(
    private val container: ViewGroup,
    actionEmitter: Observer<AwesomeBarAction>,
    changesObservable: Observable<AwesomeBarChange>
) :
    UIView<AwesomeBarState, AwesomeBarAction, AwesomeBarChange>(
        container,
        actionEmitter,
        changesObservable
    ) {
    override val view: BrowserAwesomeBar = LayoutInflater.from(container.context)
        .inflate(R.layout.component_awesomebar, container, true)
        .findViewById(R.id.awesomeBar)

    var state: AwesomeBarState? = null
        private set

    private var clipboardSuggestionProvider: ClipboardSuggestionProvider? = null
    private var sessionProvider: SessionSuggestionProvider? = null
    private var historyStorageProvider: HistoryStorageSuggestionProvider? = null
    private var shortcutsEnginePickerProvider: ShortcutsSuggestionProvider? = null

    private val searchSuggestionProvider: SearchSuggestionProvider?
        get() = searchSuggestionFromShortcutProvider ?: defaultSearchSuggestionProvider!!

    private var defaultSearchSuggestionProvider: SearchSuggestionProvider? = null
    private var searchSuggestionFromShortcutProvider: SearchSuggestionProvider? = null

    private val loadUrlUseCase = object : SessionUseCases.LoadUrlUseCase {
        override fun invoke(url: String) {
            actionEmitter.onNext(AwesomeBarAction.URLTapped(url))
        }
    }

    private val searchUseCase = object : SearchUseCases.SearchUseCase {
        override fun invoke(searchTerms: String, searchEngine: SearchEngine?) {
            actionEmitter.onNext(AwesomeBarAction.SearchTermsTapped(searchTerms, searchEngine))
        }
    }

    private val shortcutSearchUseCase = object : SearchUseCases.SearchUseCase {
        override fun invoke(searchTerms: String, searchEngine: SearchEngine?) {
            actionEmitter.onNext(AwesomeBarAction.SearchTermsTapped(searchTerms, state?.suggestionEngine))
        }
    }

    init {
        with(container.context) {
            clipboardSuggestionProvider = ClipboardSuggestionProvider(
                this,
                loadUrlUseCase,
                getDrawable(R.drawable.ic_link)!!.toBitmap(),
                getString(R.string.awesomebar_clipboard_title)
                )

            sessionProvider =
                SessionSuggestionProvider(
                    components.core.sessionManager,
                    components.useCases.tabsUseCases.selectTab,
                    components.utils.icons
                )

            historyStorageProvider =
                HistoryStorageSuggestionProvider(
                    components.core.historyStorage,
                    loadUrlUseCase,
                    components.utils.icons
                )

            if (Settings.getInstance(container.context).showSearchSuggestions()) {
                val draw = getDrawable(R.drawable.ic_search)
                draw?.setTint(ContextCompat.getColor(this, R.color.search_text))

                defaultSearchSuggestionProvider =
                    SearchSuggestionProvider(
                        searchEngine = components.search.searchEngineManager.getDefaultSearchEngine(this),
                        searchUseCase = searchUseCase,
                        fetchClient = components.core.client,
                        mode = SearchSuggestionProvider.Mode.MULTIPLE_SUGGESTIONS,
                        limit = 3,
                        icon = draw?.toBitmap()
                    )
            }

            shortcutsEnginePickerProvider =
                    ShortcutsSuggestionProvider(
                        components.search.searchEngineManager,
                        this,
                        ::selectShortcutEngine,
                        ::selectShortcutEngineSettings)
        }
    }

    private fun showShortcutEnginePicker() {
        with(container.context) {
            search_shortcuts_button.background = getDrawable(R.drawable.search_pill_background)
            search_shortcuts_button.compoundDrawables[0].setTint(ContextCompat.getColor(this,
                DefaultThemeManager.resolveAttribute(R.attr.pillWrapperBackground, this)))
            search_shortcuts_button.textColor = ContextCompat.getColor(this,
                DefaultThemeManager.resolveAttribute(R.attr.pillWrapperBackground, this))

            view.removeAllProviders()
            view.addProviders(shortcutsEnginePickerProvider!!)
        }
    }

    private fun hideShortcutEnginePicker() {
        with(container.context) {
            search_shortcuts_button.setBackgroundColor(ContextCompat.getColor(this,
                DefaultThemeManager.resolveAttribute(R.attr.pillWrapperBackground, this)))
            search_shortcuts_button.compoundDrawables[0].setTint(ContextCompat.getColor(this,
                DefaultThemeManager.resolveAttribute(R.attr.searchShortcutsTextColor, this)))
            search_shortcuts_button.textColor = ContextCompat.getColor(this,
                DefaultThemeManager.resolveAttribute(R.attr.searchShortcutsTextColor, this))

            view.removeProviders(shortcutsEnginePickerProvider!!)
        }
    }

    private fun showSuggestionProviders() {
        if (Settings.getInstance(container.context).showSearchSuggestions()) {
            view.addProviders(searchSuggestionProvider!!)
        }

        view.addProviders(
            clipboardSuggestionProvider!!,
            historyStorageProvider!!,
            sessionProvider!!
        )
    }

    private fun selectShortcutEngine(engine: SearchEngine) {
        actionEmitter.onNext(AwesomeBarAction.SearchShortcutEngineSelected(engine))
    }

    private fun setShortcutEngine(engine: SearchEngine) {
        with(container.context) {

            val draw = getDrawable(R.drawable.ic_search)
            draw?.setTint(ContextCompat.getColor(this, R.color.search_text))

            searchSuggestionFromShortcutProvider = SearchSuggestionProvider(
                components.search.searchEngineManager.getDefaultSearchEngine(this, engine.name),
                shortcutSearchUseCase,
                components.core.client,
                mode = SearchSuggestionProvider.Mode.MULTIPLE_SUGGESTIONS,
                icon = draw?.toBitmap()
            )
        }
    }

    private fun showSearchSuggestionProvider() {
        view.addProviders(searchSuggestionProvider!!)
    }

    private fun selectShortcutEngineSettings() {
        val directions = SearchFragmentDirections.actionSearchFragmentToSearchEngineFragment()
        Navigation.findNavController(view).navigate(directions)
    }

    private fun updateSearchWithVisibility(visible: Boolean) {
        search_with_shortcuts.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun updateView() = Consumer<AwesomeBarState> {
        if (engineDidChange(it)) {
            it.suggestionEngine?.let { newEngine ->
                setShortcutEngine(newEngine)
            }
        }

        if (shouldUpdateShortcutEnginePickerVisibility(it)) {
            if (it.showShortcutEnginePicker) {
                showShortcutEnginePicker()
                updateSearchWithVisibility(true)
            } else {
                hideShortcutEnginePicker()
                updateSearchWithVisibility(false)
                it.suggestionEngine?.also { showSearchSuggestionProvider() } ?: showSuggestionProviders()
            }
        }

        view.onInputChanged(it.query)
        state = it
    }

    private fun engineDidChange(newState: AwesomeBarState): Boolean {
        return state?.suggestionEngine != newState.suggestionEngine
    }

    private fun shouldUpdateShortcutEnginePickerVisibility(newState: AwesomeBarState): Boolean {
        return state?.showShortcutEnginePicker != newState.showShortcutEnginePicker
    }
}
