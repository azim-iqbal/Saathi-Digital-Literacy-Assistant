# Saathi vector identity

The previously generated two-part S mark is now rebuilt as flat, smooth vector contours. This is derived from the user's approved visual source in saathi-v1, not a new logo direction. The original raster files are preserved.

- saathi-symbol-green.svg: #087900, transparent background.
- saathi-symbol-mint.svg: #A4EE99, transparent background.
- saathi-symbol-white.svg: white, transparent background.
- Android `ic_saathi_mark.xml`: same contour geometry in the app header.
- Android `ic_saathi_launcher_foreground.xml`: mint geometry centered within the adaptive-icon safe region, over #131513.
- Android `ic_saathi_splash.xml`: enlarged, mask-safe centered logo for startup.
- Android13+ monochrome launcher layer supports themed icons.

Contours are traced from the two dominant alpha components of the generated master, simplified and fitted into25 closed cubic segments across the two contours. All exports share the same paths. `vectorize.py` reproduces them using Pillow and NumPy; it does not alter the master. Tiny raster artifacts and color texture are intentionally removed. SVG paths contain no bitmap, remote dependency or font. The name in the Android header remains live text for clarity and scaling.

Old density-specific PNG launcher files remain preserved; supported Android versions select the new anydpi adaptive vector resource. Small-size recognition, alternate OEM masks and themed-icon colors need device review beyond the emulator mask export.
