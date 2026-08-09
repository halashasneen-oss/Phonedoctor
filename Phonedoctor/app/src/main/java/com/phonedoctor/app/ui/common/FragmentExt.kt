package com.phonedoctor.app.ui.common

import androidx.fragment.app.Fragment
import com.phonedoctor.app.PhoneDoctorApp
import com.phonedoctor.app.ServiceLocator

/** Every fragment reaches shared repositories through the app-level [ServiceLocator]. */
fun Fragment.serviceLocator(): ServiceLocator =
    (requireActivity().application as PhoneDoctorApp).serviceLocator
