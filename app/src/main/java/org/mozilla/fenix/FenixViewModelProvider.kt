package org.mozilla.fenix

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import org.mozilla.fenix.mvi.ViewState
import org.mozilla.fenix.mvi.Change
import org.mozilla.fenix.mvi.UIComponentViewModelBase
import org.mozilla.fenix.mvi.UIComponentViewModelProvider

object FenixViewModelProvider {
    fun <S : ViewState, C : Change, T : UIComponentViewModelBase<S, C>>create(
        fragment: Fragment,
        modelClass: Class<T>,
        viewModelCreator: () -> T): UIComponentViewModelProvider<S, C> {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return viewModelCreator() as T
            }
        }

        return object : UIComponentViewModelProvider<S, C> {
            override fun fetchViewModel(): T {
                return ViewModelProviders.of(fragment, factory).get(modelClass)
            }

        }
    }
}
