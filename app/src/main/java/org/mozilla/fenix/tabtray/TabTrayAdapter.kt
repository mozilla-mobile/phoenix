/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabtray

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.tab_tray_list_item.view.*
import mozilla.components.feature.media.state.MediaState
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.ui.SelectableListItemView
import org.mozilla.fenix.components.ui.SelectionHolder
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.increaseTapArea

/**
 * View that represents individual tab items
 */
class TabItemViewHolder(
    private val view: SelectableListItemView,
    private val interactor: TabTrayViewInteractor,
    private val selectionHolder: SelectionHolder<Tab>
) : RecyclerView.ViewHolder(view) {
    private var tab: Tab? = null
    private var mode: TabTrayFragmentState.Mode? = null

    init {
        view.displayAs(SelectableListItemView.ItemType.CLOSABLE_ITEM)
        view.accessoryView.setOnClickListener {
            if (mode is TabTrayFragmentState.Mode.Editing) return@setOnClickListener
            tab?.apply(interactor::closeButtonTapped)
        }

        view.playPauseButton.increaseTapArea(PLAY_PAUSE_BUTTON_EXTRA_DPS)

        view.playPauseButton.setOnClickListener {
            when (tab?.mediaState) {
                is MediaState.Playing -> {
                    it.context.components.analytics.metrics.track(Event.TabMediaPlay)
                    interactor.onPauseMediaClicked()
                }

                is MediaState.Paused -> {
                    it.context.components.analytics.metrics.track(Event.TabMediaPause)
                    interactor.onPlayMediaClicked()
                }
            }
        }
    }

    fun bind(tab: Tab, mode: TabTrayFragmentState.Mode) {
        this.tab = tab
        this.mode = mode
        view.title.text = tab.title
        view.url.text = tab.url
        view.loadFavicon(tab.url)
        view.setSelectionInteractor(tab, selectionHolder, interactor)
        view.changeSelected(tab in selectionHolder.selectedItems)
        updatePlayPauseButton(tab.mediaState)
        view.accessoryView.visibility = if (mode is TabTrayFragmentState.Mode.Normal) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
    }

    fun updatePlayPauseButton(mediaState: MediaState) {
        with(view.playPauseButton) {
            isVisible = mediaState is MediaState.Playing || mediaState is MediaState.Paused

            if (mediaState is MediaState.Playing) {
                contentDescription =
                    context.getString(R.string.mozac_feature_media_notification_action_pause)
                setImageDrawable(context.getDrawable(R.drawable.pause_with_background))
            } else {
                contentDescription =
                    context.getString(R.string.mozac_feature_media_notification_action_play)
                setImageDrawable(context.getDrawable(R.drawable.play_with_background))
            }
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.selectable_list_item
        private const val PLAY_PAUSE_BUTTON_EXTRA_DPS = 24
    }
}

/**
 * Adapter that helps facilitate the Tab Tray
 */
class TabTrayAdapter(
    private val interactor: TabTrayViewInteractor
) : RecyclerView.Adapter<TabItemViewHolder>(), SelectionHolder<Tab> {
    private var state = TabTrayFragmentState(listOf(), TabTrayFragmentState.Mode.Normal)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabItemViewHolder {
        val view = SelectableListItemView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        return TabItemViewHolder(view, interactor, this)
    }

    override fun getItemCount() = state.tabs.size

    override fun onBindViewHolder(holder: TabItemViewHolder, position: Int) {
        holder.bind(state.tabs[position], state.mode)
    }

    fun updateState(state: TabTrayFragmentState) {
        this.state = state
        notifyDataSetChanged()
    }

    override val selectedItems: Set<Tab>
        get() = state.mode.selectedTabs
}