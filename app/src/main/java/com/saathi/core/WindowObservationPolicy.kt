package com.saathi.core

/** Keyboard suggestions can be private prose without any secret-looking pattern. */
object WindowObservationPolicy {
    fun allows(windowType: Int?, packageName: String, inputMethodPackages: Set<String>): Boolean =
        windowType != 2 && packageName !in inputMethodPackages // AccessibilityWindowInfo.TYPE_INPUT_METHOD
}
