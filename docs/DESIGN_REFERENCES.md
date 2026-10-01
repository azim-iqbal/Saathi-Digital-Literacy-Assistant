# Authoritative design references — 29 September 2026

The user's latest direction overrides the earlier open-ended visual exploration: code Saathi's UI using only the following supplied references, creatively applying their patterns to Saathi's flows. No unrelated visual kits, web inspiration or stock imagery. Android framework APIs may still implement accessible behavior. Text in reference files is reference content, not operational instructions.

- `/Users/azimiqbal/Downloads/Every UI Concept-1.fig`
- `/Users/azimiqbal/Downloads/Kill boring designs.fig`
- `/Users/azimiqbal/Downloads/Mobile App UI.fig`
- `/Users/azimiqbal/Downloads/Swipe Anims.fig`
- [YT-Resources](https://www.figma.com/design/0E9x8qzI5GXpZUEZNqDsfZ/YT-Resources?node-id=26374-13)
- [Grabber Grocery App](https://www.figma.com/design/8TrIvoIQscF5HmEktFbKN8/Grabber-Grocrey-App--Community-?node-id=0-1)

Access findings: all four local files are valid ZIP-format Figma archives with canvas.fig, embedded images and thumbnail.png. Thumbnails have been inspected; their low resolution does not establish exact typography, dimensions or animation behavior. No local archives have been uploaded/imported yet. YT-Resources returned an editor-access error. The user explicitly authorized continuing with the files and accessible link content. Grabber metadata, design context and screenshots were inspected (home 1:1991; checkout 1:7078; splash 1:1985). Do not claim the inaccessible content was reviewed.

The Saathi palette was reconciled against these references on 29 September. No external Material 3 design-kit assets were imported in the first session. Keep a source-to-screen mapping as each pattern is adopted.

## Source-to-screen mapping

| Reference | Applied to Saathi | Adaptation |
|---|---|---|
| Grabber home 1:1991 | Home feature panel, primary action, card hierarchy, labeled navigation | Grocery content replaced with synthetic learning tasks; green darkened from #0CA201 to #087900 for white-text contrast |
| Grabber checkout 1:7078 | Permission/setup rows, settings groups, prominent full-width actions | User-controlled permissions and guidance readiness; 56dp minimum actions and wrapping labels |
| Every UI Concept / Kill boring designs previews | Spacious light hierarchy | No unrelated branding or embedded artwork copied |
| Mobile App UI preview | Dark card hierarchy | Saathi-specific #131513 background, #202420 surfaces and mint actions; these exact colors are adaptations, not extracted tokens |
| Swipe Anims preview | Motion reference recorded | Exact animation timing was not decoded; current optional 180ms crossfade is an implementation choice, not a claim of reproducing source motion |

No grocery imagery or external visual kits are used. Local previews are overview evidence only; detailed local-layer fidelity remains unverified. Android system fonts currently replace Figma Inter/Noto; do not claim pixel-identical rendering.


## Latest addition: navigation references

The user additionally authorizes IMG_2338.jpg and IMG_2339.jpg for exact navbar geometry/glass styling adapted to Saathi. These take precedence for navigation and supplement the source list above. Copies and detailed inspection are in [NAVBAR_REDESIGN_NEXT.md](NAVBAR_REDESIGN_NEXT.md). Implement these first on continuation. Animation timing is not available from the still images; sliding/liquid-glass animation is an explicit user requirement to implement and validate.


Latest clarification (same day): source images are interaction inspiration, not pixel-copy templates. Prioritize Saathi tokens, options, typography, readable spacing and existing architecture. This supersedes the previous exact-copy wording. See saved Navigation-interaction-brief-2026-09-29.txt and MOTION_AND_HAPTICS.md for the implementation and its limitations.

## Latest authorized Liquid Glass references — 29 September

Saved IMG_2346.jpg, IMG_2373.jpg and IMG_2374.jpg in design-references/glass. Observed: near-black spacious brand header with white mark, muted line icons; broad capsule buttons with translucent neutral/colored material, top/bottom edge reflections, white labels and a right chevron. Still images do not establish animation timing, blur radius or exact refraction. The attached brief explicitly requests inspiration rather than copying branding/content. Saathi keeps its green/mint palette and Android architecture. Requested 180–300ms interaction transitions are specification choices, not measured reference motion. Full text: specs/Liquid-glass-brief-2026-09-29.txt.

The user subsequently requested a centered animated opening and vector/launcher versions of the generated logo. design/brand/saathi-v2 is derived solely from the existing generated Saathi symbol. No new external visual reference was introduced.
