# Liquid Glass UI — implementation handoff

Latest source: [user brief](specs/Liquid-glass-brief-2026-09-29.txt). References saved in design-references/glass/IMG_2346.jpg, IMG_2373.jpg and IMG_2374.jpg. These authorize the new visual direction; existing navigation images and earlier Figma references remain authorized. Reference shapes/reflections are observed; our motion and material values are proposed implementation choices.

## Implemented

- 1 October continuation: main shell and assistant share `SaathiColors` and `SaathiBrand`; existing main palette/header are unchanged. Assistant mode chips, content width and reduced-motion policy now align with the shell. Existing glass components retained, with an opaque assistant fallback. See TEST_RESULTS.md for sampled light/dark evidence.

- Shared Compose `GlassPanel`, `GlassButton`, `GlassChevron`, `GlassTokens` and environment in app/src/main/java/com/saathi/ui/glass. No new dependencies or architecture replacement.
- Primary and secondary full-width actions, compact header actions, Practice CTA, privacy confirmation buttons, feature/task/settings cards, language/theme choices, switches and task input container use the material system. Radio/switch affordances retain standard Android semantics.
- Broad pills: 64dp minimum height; compact actions: 48dp. Text wraps and grows rather than being ellipsized. Chevron mirrors for RTL. Content column caps at720dp; floating navigation caps at600dp. Header wraps at narrow/large-text sizes.
- Shared green/mint identity, white primary labels, top-to-bottom translucent tints,1dp edge reflection,2dp depth,18dp backdrop blur on eligible devices. Focused buttons have2dp rings. Press scales to97.5%, hover lifts1dp, with220ms transitions; reduced motion removes animation. Pointer/keyboard behavior still needs device verification.
- Dedicated decorative background recording prevents feedback loops: controls sample Saathi's ambient background, never themselves, adjacent content, input text or external app pixels. This is a frosted material, not a physical refraction shader. Subtle backgrounds make the glass less obvious than the photographic references.
- Opaque fallback when reduced transparency is enabled, API<31, low RAM, power saver, high contrast or unsupported acceleration. Dialogs intentionally use an opaque material because their coordinates belong to a separate window. Labels/icons stay sharp. Disabled controls remain disabled and visibly muted.
- Existing generated S symbol is integrated into the header, with theme tint and a live-text Saathi wordmark. Raster master preserved. Follow-up now uses traced vector geometry in the header and launcher; see LAUNCH_EXPERIENCE.md.
- Both native synthetic practice buttons use `applySaathiGlass`, a matching pill/ripple/focus/disabled **opaque fallback**. Their IDs, listeners and form validation remain intact.
- Floating dock and sculpted category selectors retain their prior dedicated motion and shapes. The dock now samples the same decorative background.

## Interactive-element inventory and remaining scope

| Surface | Result / remaining work |
| --- | --- |
| Compose welcome/home/intake/setup/session/privacy actions | Shared glass buttons |
| Compose header Back/language | Compact shared glass buttons |
| Practice category action | Shared glass button, existing test tag preserved |
| Privacy dialog | Shared glass buttons; intentional opaque surface |
| Settings choices/switches and action cards | Glass containers, native semantics and readable controls |
| Bottom dock/category tabs | Existing custom navigation preserved |
| Floating assistant / live intake | Shared glass buttons and panels, mode chips, branded native edge bubble. New panel localization, Figma parity and device accessibility checks remain pending; see LIVE_ASSISTANT.md. |
| Native DemoBillPayActivity payment/home buttons | Styled opaque fallback; full native form/card/theme redesign and new visual verification pending |
| LegacyTaskActivity ImageButtons, language/reply chips and system language dialog | Historical activity has no route from current Compose shell; still old styling. Audit found these explicitly; do not claim every repository control is migrated. Inspect whether this retained activity should be retired or brought into the new system before changing it. |
| TermsActivity | Historical standalone native text page; full visual parity pending |

## Next phase

1. Review final screenshots and test results. Physical-device frame timing, blur-on/off comparison, TalkBack, keyboard focus/hover, older APIs, power/contrast fallbacks, RTL, tablet/landscape and native fixture interaction remain to verify. Existing software-emulator timing is poor; do not call the UI production-performance-ready.
2. Finish the native synthetic fixture and retained legacy-surface decision above. Preserve target IDs and sensitive-input semantics.
3. Mirror these new controls/material tokens, logo/header and screen layouts into the existing Figma file. Read skills and docs/figma-state.json first; no Figma writes occurred during this phase. Existing Figma screens still show the earlier card/button system.
4. Test live permission revocation/attachment failure and continue the broader execution plan. Backend/cloud and microphone expansion remain disabled/pending.

The Android app is the only platform implemented. The brief's desktop/web wording is interpreted as responsive Android window sizing, not a newly created web product. No desktop/tablet verification claim.
