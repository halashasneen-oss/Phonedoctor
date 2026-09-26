package com.phonedoctor.app.ui.common

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Binds a [ViewBinding] to a fragment's current view lifecycle.
 *
 * A Fragment can survive while its view is destroyed and recreated on the
 * Navigation back stack. The delegate therefore validates that a cached
 * binding belongs to the Fragment's *current* root view before reusing it.
 * This prevents listeners/state from being applied to a stale, detached view.
 */
class FragmentViewBindingDelegate<T : ViewBinding>(
    private val fragment: Fragment,
    private val bind: (View) -> T
) : ReadOnlyProperty<Fragment, T> {

    private var binding: T? = null
    private var observedViewOwner: LifecycleOwner? = null

    private val viewLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            if (observedViewOwner === owner) {
                binding = null
                observedViewOwner = null
            }
        }
    }

    private val ownerObserver = Observer<LifecycleOwner?> { owner ->
        observedViewOwner?.lifecycle?.removeObserver(viewLifecycleObserver)
        observedViewOwner = owner
        owner?.lifecycle?.addObserver(viewLifecycleObserver)
    }

    init {
        fragment.viewLifecycleOwnerLiveData.observeForever(ownerObserver)
        fragment.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    fragment.viewLifecycleOwnerLiveData.removeObserver(ownerObserver)
                    observedViewOwner?.lifecycle?.removeObserver(viewLifecycleObserver)
                    observedViewOwner = null
                    binding = null
                }
            }
        )
    }

    override fun getValue(
        thisRef: Fragment,
        property: KProperty<*>
    ): T {
        val currentView = thisRef.requireView()
        val existing = binding

        if (existing != null && existing.root === currentView) {
            return existing
        }

        return bind(currentView).also {
            binding = it
        }
    }
}

fun <T : ViewBinding> Fragment.viewBinding(
    bind: (View) -> T
): FragmentViewBindingDelegate<T> =
    FragmentViewBindingDelegate(this, bind)
