This directory vendors the official `poishadow-all.jar` release asset from
`centic9/poi-on-android` release `5.2.5-4`.

Reason: the Android build depends on the shaded Apache POI jar, but this release
asset is not published as a resolvable Maven/JitPack artifact. Keeping the jar in
the repository makes CI reproducible.

Source:
https://github.com/centic9/poi-on-android/releases/tag/5.2.5-4
