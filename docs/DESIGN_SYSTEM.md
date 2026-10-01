# Design system — core screens implemented, full system pending

[Editable Figma file](https://www.figma.com/design/vx2M28p625yZQTAzcTLlQj). Use only [authorized references](DESIGN_REFERENCES.md).

31 variables across three collections: 14 primitives, eight semantic colors with Light/Dark modes, nine spacing/radius values. Two component sets: Action (Primary/Secondary/Disabled) and Content (Feature/Row/Card). Fourteen editable frames cover Welcome, Home, Practice, Setup, Session, Settings and Privacy in light/dark. Light and dark Home plus component sheet were visually reviewed. Other frames require individual visual checks. Figma ledgers contain exact IDs.

| Role | Light | Dark |
|---|---|---|
| Background | #FFFFFF | #131513 |
| Surface | #FFFFFF | #202420 |
| Text | #171A17 | #F6F6F6 |
| Primary | #087900 | #A4EE99 |
| On primary | #FFFFFF | #131513 |
| Muted | #596259 | #B8C3B6 |
| Border | #DCE3DA | #3E493C |
| Feature | #D7FFD4 | #253D22 |

Compose implementation: ui/SaathiApp.kt, ui/Copy.kt, ui/Preferences.kt. Android system fonts support device fallback; Figma uses Inter. Current screen typography is 34/42, 28/36, 22/30, 17/26 and 16/24. Existing Figma text styles still require reconciliation with the new screen ramp. Figma code syntax names SaathiTokens as a planned mapping; that class and Code Connect do not exist. Primitive scopes are empty; semantic/layout scopes are explicit.

Android buttons center text; Figma actions currently left-align it. Figma Home is a full scrolling composition; Android keeps navigation at the bottom. Feature radii/padding differ between generic Figma Content components and the screen-specific Android feature. These are documented fidelity gaps, not completed parity. Complete component alignment before claiming an exact reproduction.

Android screens include English/Hindi/Hinglish copy, theme persistence, optional haptics and reduced motion. Locale review, all-screen contrast/TalkBack validation, Figma language/large-text variants, intake/error/completion/overlay frames, prototypes and exact source motion remain pending. No unverified font assets were bundled.


## Navigation addition

Figma now has36 variables (five navigation sizing tokens added), plus Navigation/Dock24:86 and Navigation/PracticeTabs24:189. Each has three selected states in two themes, editable labels and matching original icon drawings. Twenty-four Smart Animate links preview selection. These are easing previews; Compose implements real springs, velocity stretch and live pager tracking. Twelve existing light/dark frames now use dock instances. Both component sheets and integrated Home were visually reviewed. Full Practice frame layout still reflects the earlier design and needs the new tabs composed into it.

Compose mapping: navigation/GlassNavigation.kt; PracticeNavigation.kt; NavigationIcons.kt; NavigationMetrics.kt; NavigationEnvironment.kt. Metrics tokens map to actual constants except pillRadius, which maps to RoundedCornerShape(50). Insets/font scaling determine Android heights; no screenshot pixel sizes are hardcoded. Figma uses Inter while Android uses system fonts; Settings drawing differs slightly (outlined Figma control circles vs filled Android circles). No Code Connect claim.

Static text contrast pairs: green on light feature, muted text on white, mint on dark feature and muted text on dark surface were calculated and meet4.5:1; this is not an exhaustive composited-glass accessibility audit. Controls retain selected semantics and text weight changes. See MOTION_AND_HAPTICS.md for blur restrictions, performance evidence and remaining checks.

## Liquid Glass Android extension

The latest user-authorized header/button references now drive shared Android material tokens and reusable components. See GLASS_UI.md for dimensions, sampling, states and fallback policy. Android controls have changed; the existing Figma file has not yet been synchronized to this extension. No new Figma component IDs are implied.
