package com.phonedoctor.app.ui.common

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Binds a [ViewBinding] to a fragment's view lifecycle, clearing the
 * reference in onDestroyView to avoid leaking the view hierarchy.
 */
class FragmentViewBindingDelegate<T : ViewBinding>(
    private val fragment: Fragment,
    private val bind: (View) -> T
) : ReadOnlyProperty<Fragment, T> {

    private var binding: T? = null

    init {
        fragment.viewLifecycleOwnerLiveData.observeForever { owner ->
            owner?.lifecycle?.addObserver(object : LifecycleEventObserver {
                override fun onStateChanged(source: androidx.lifecycle.LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        binding = null
                    }
                }
            })
        }
    }

    override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
        val existing = binding
        if (existing != null) return existing
        val newBinding = bind(thisRef.requireView())
        binding = newBinding
        return newBinding
    }
}

fun <T : ViewBinding> Fragment.viewBinding(bind: (View) -> T): FragmentViewBindingDelegate<T> =
    FragmentViewBindingDelegate(this, bind)
