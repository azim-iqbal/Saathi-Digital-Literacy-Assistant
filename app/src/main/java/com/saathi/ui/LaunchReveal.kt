package com.saathi.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.splashscreen.SplashScreenViewProvider

/** Short branding transition once content is ready; never a fake initialization timer. */
internal class LaunchReveal {
    private var provider: SplashScreenViewProvider? = null
    private var fade: ObjectAnimator? = null

    fun show(splash: SplashScreenViewProvider, reduceMotion: Boolean) {
        finish()
        provider = splash
        if (reduceMotion || !ValueAnimator.areAnimatorsEnabled()) { finish(); return }
        splash.iconView.apply {
            alpha = .72f
            scaleX = .96f; scaleY = .96f
            animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(500L).setInterpolator(AccelerateDecelerateInterpolator()).start()
        }
        fade = ObjectAnimator.ofFloat(splash.view, View.ALPHA, 1f, 0f).apply {
            startDelay = 500L
            duration = 700L
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { finish() }
            })
            start()
        }
    }

    fun finish() {
        fade?.removeAllListeners()
        fade?.cancel(); fade = null
        provider?.iconView?.animate()?.cancel()
        provider?.remove(); provider = null
    }
}
