# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## HOW TO WORK WITH THIS USER — read first

This is the user's **first Minecraft mod, and it is a deliberate learning project**. He is training
his ability to produce code from a blank editor. **Do not write his code for him.** By default,
operate in the `socratic-debugging` register:

- No code, no pseudocode, no step-by-step decomposition of his problem. Decomposition is the skill
  he is missing — handing it over is the main failure mode, and it feels helpful, which is why it's
  a trap.
- Require an attempt before helping with a blocker. If he's stuck, ask **how long**; under 30
  minutes, send him back.
- You *may*: ask questions, explain general concepts/mechanics, point at where to look, and review
  code he wrote (name what's wrong; never write the fix).
- He may push for the answer when tired. Refuse in one sentence, no moralizing, and get back to
  work. The exception is if he **explicitly** says he's leaving training mode for real shipping
  work — make him say it plainly rather than sliding out by degrees.
- This file itself is context **for Claude only** — he says he doesn't read it. Keeping it accurate
  is your job, but don't treat "it's written in CLAUDE.md" as something he already knows.
- He is a Brazilian Portuguese speaker who wants his **English corrected**: at the end of replies,
  briefly flag sentence-structure/semantic errors (comma splices, dropped subjects/objects,
  question inversion, misplaced `only`). Keep it short and below the substantive answer.

## What this project is

**Serene Regions** is a Forge **1.20.1** compatibility mod (`mod_id = sereneregions`, no
underscore). Its sole purpose is to make **Regions Unexplored** (RU) crops, saplings and seeds
respond to **Serene Seasons** (SS) seasons — i.e. give each RU crop/sapling the season(s) in which
it is allowed to grow, and categorize each RU biome so seasons behave sensibly there.

Current state (verified 2026-08-15): **feature-complete and release-ready.** `./gradlew build`
produces a clean 12 KB `sereneregions-1.0-1.20.1.jar`; git is clean and pushed to
`git@github.com:luisfelipefeyh/SereneRegions.git` (branch `master`).

- **All 13 tag files are written and verified in-game.** Under
  `src/main/resources/data/sereneseasons/tags/`:
  `blocks/{spring,summer,autumn,winter,year_round,unbreakable_infertile}_crops.json`,
  `items/{spring,summer,autumn,winter,year_round}_crops.json`, and
  `worldgen/biome/{tropical,blacklisted}_biomes.json`. All 33 non-potted RU saplings plus
  `duskmelon` and `salmonberry_bush`; every ID diffed against RU's registrations. Tropical is 12
  biomes (bayou and old_growth_bayou were deliberately removed — SS leaves vanilla `swamp` out of
  tropical too); blacklisted is 7 fully-underground biomes.
- **`lesser_color_change_biomes` is declined, not pending.** User decided against it. Do not
  re-propose.
- **The MDK example Java is gone.** `SereneRegions.java` is ~30 lines: `@Mod` class, `MOD_ID`,
  `LOGGER`, and one `FMLLoadCompleteEvent` listener logging "Serene Regions is loaded!".
  `Config.java` was deleted along with `registerConfig`. Note the listener is registered on the
  **mod event bus** via `modEventBus.addListener(this::...)` — subscribing lifecycle events on
  `MinecraftForge.EVENT_BUS` fails silently, which cost a debugging round.
- **`mods.toml` is finished**: `license="MIT"`, `logoFile="image.png"`, `logoBlur=false`,
  `displayTest="IGNORE_SERVER_VERSION"`, dependencies on `regions_unexplored` `[0.5.6,)` and
  `sereneseasons` `[9.1,)` both `ordering=AFTER`, `side="BOTH"`. No `issueTrackerURL` (optional,
  still absent).
- **`displayTest="IGNORE_SERVER_VERSION"` is correct and verified** — the MDK's own comment block
  documents it as the server-only-mod value. (I initially got this backwards; the docs settle it.)
  Verified empirically: a client without the jar joins a server with it, and tooltips/fertility/
  tropical biomes all work, because tags are synced server→client. Note `displayTest` does **not**
  control which side the mod loads on, and singleplayer still needs the jar locally.
- **`build.gradle` slimmed to 95 lines**: `publishing`/`maven-publish`, the manifest attributes
  block, and the mixin `annotationProcessor` were all removed. The `tasks.named('jar')` wrapper was
  kept for `finalizedBy 'reobfJar'`. `runs { }` keeps `client`, `server`, `data`; `gameTestServer`
  and the `forge.enabledGameTestNamespaces` properties were removed.
- `gradle.properties`: `mod_version=1.0-1.20.1`, `mod_license=MIT`,
  `mod_authors=demolutio, luisaava`, `mapping_channel=parchment`
  (`2023.09.03-1.20.1` — dev-time only, `reobfJar` means it does not affect the shipped jar).
- Root is clean: `README.md` (written this session, user-edited), `LICENSE.txt` (MIT),
  `build.gradle`, `gradle.properties`, `CLAUDE.md`. `README.txt`, `CREDITS.txt` and `changelog.txt`
  are deleted.
- `src/main/resources/image.png` is Luisa's logo (776×526, **not square** despite being described
  as such — a separate square export is still needed for Modrinth/CurseForge project icons).

Not yet done: an in-game look at the logo in the Mods screen; a real-instance smoke test of *this*
jar (an earlier build was verified on a Modrinth instance); the GitHub release tag and the
Modrinth/CurseForge uploads.

### Tag design decisions already made (do not re-litigate)

- **Hand-written JSON now, datagen refactor later** as a deliberate two-phase plan. Phase 2 keeps
  the hand-written files around so the generated output can be `diff`ed against them as a
  correctness oracle. Datagen phase 2 **will** require a compile dependency on RU (`implementation
  fg.deobf`, replacing `runtimeOnly`) — that is the entire point of doing it, since the benefit is
  compile-time symbol resolution, not scale.
- **Design philosophy: match SS's vanilla precedent.** Single-season tags are within precedent
  (SS gives vanilla `jungle_sapling`/`acacia_sapling` summer-only, `cherry_sapling` spring-only,
  `dark_oak_sapling` autumn-only).
- Untagged blocks are already fertile spring/summer/autumn, so tags only ever *restrict*; the tag
  files are the **exceptions**, not an exhaustive list.
- Nether trees (`brimwood`, `cobalt`) are deliberately tagged summer — seasons don't run in the
  Nether, so this only governs a sapling carried to the overworld.
- `duskmelon` is deliberately **winter-only** ("creature of the cold"). Note `ModFertility` line 72
  makes everything below y=48 without sky access unconditionally fertile, so the tag only governs
  surface farming.
- Item-tag entries are `salmonberry` and `duskmelon_slice`, not the block names — both are
  `FoodItemWithBlock`, i.e. the plantable item. Item tags drive **only** the "fertile in: …"
  tooltip, never growth.

### The tag-reference footgun that cost a debugging session (2026-08-11)

An unresolvable `#namespace:tag` reference **destroys the entire tag**, not just that entry. All
eight season files briefly contained `#regions_unexplored:year_round_crops`, a tag RU does not
define. Result: `sereneseasons:{spring,summer,autumn,winter}_crops` all failed to load *completely*,
taking SS's own vanilla entries with them. `ModFertility.populate()` then built empty sets, so
`ModFertility.isCrop(state)` returned false for **every block in the game** — no growth gating
anywhere, no tooltips anywhere. It presents as "Serene Seasons is fundamentally broken."

The evidence was an `ERROR`-level line from `net.minecraft.tags.TagLoader` in the run log the whole
time: `Couldn't load tag sereneseasons:summer_crops as it is missing following references: ...`.

Two takeaways: **(a)** season files should NOT re-declare `#sereneseasons:year_round_crops` — SS's
own files already contribute it to the merged tag; **(b)** `{"id": "...", "required": false}` is
what converts a fatal reference into a skipped entry, which is a separate question from whether the
mod dependency is hard.

Two upstream mods are **cloned into this repo as reference source only** — they are gitignored
(`/SereneSeasons/`, `/REGIONS_UNEXPLORED_FORGE/`) and are not part of the build:
- `SereneSeasons/` — multi-loader (common/forge/fabric), namespace `sereneseasons`, on branch
  **`1.20.1`**
- `REGIONS_UNEXPLORED_FORGE/` — single-loader Forge, namespace `regions_unexplored` (Java root
  package `net.regions_unexplored`), `mod_version 0.5.6`, on branch **`1.20.1_FINAL`**

Read those trees to look up exact block/item/biome IDs and mechanics; do not edit them.

## Build & run commands

Run from the repo root (the Serene Regions MDK, **not** the cloned subfolders):

```bash
./gradlew build              # compile + build the mod jar (build/libs/)
./gradlew genIntellijRuns    # generate IntelliJ run configs (run once after import)
./gradlew genEclipseRuns     # Eclipse equivalent
./gradlew runClient          # launch a dev client with the mod loaded
./gradlew runServer          # launch a dev dedicated server
./gradlew runData            # run data generators -> src/generated/resources
./gradlew runGameTestServer  # run registered gametests, then exit
./gradlew --refresh-dependencies   # refresh cached deps if IDE libs go missing
./gradlew clean              # reset build outputs (does not touch source)
```

- Toolchain: **Java 17**. Mappings: `official` (Mojang), version `1.20.1`.
- There is no unit-test suite; verification is done in-game via `runClient`/`runServer`, or with
  Forge gametests under namespace `sereneregions` (all three run configs in `build.gradle` already
  wire `forge.enabledGameTestNamespaces` to `mod_id`).
- First-run gotcha: ForgeGradle decompiles Minecraft and needs the `-Xmx3G` already set in
  `gradle.properties`.

### Dev environment: RU + SS at runtime (SOLVED — do not re-litigate)

`runClient` boots with all four mods loaded as of 2026-08-04. The setup, and *why* each piece is
there, because every part of it was hard-won:

- `repositories { maven { url "https://cursemaven.com" } }` — CurseMaven is a community Maven façade
  over CurseForge. Coordinates are `curse.maven:<slug>-<projectId>:<fileId>`. TODO (flagged, not
  done): restrict it with `content { includeGroup "curse.maven" }` so Gradle stops asking cursemaven
  about every unresolved artifact.
- Four `runtimeOnly fg.deobf(...)` deps: `sereneseasons-291874:8246702`,
  `regionsunexplored-659110:5558225`, `terrablender-563928:6290448`, `glitchcore-955399:5787839`.
  **`runtimeOnly`, not `implementation`** — the mod is data-only and compiles against neither.
  SS needs GlitchCore, RU needs TerraBlender; CurseMaven serves no usable POM, so transitive
  resolution does **not** happen — every dependency-of-a-dependency must be declared by hand.
- `id 'org.spongepowered.mixin' version '0.7.+'` in `plugins` — **required**, and non-obvious.
  Without it, GlitchCore's mixins fail at boot with `InvalidInjectionException: could not find any
  targets matching 'Lnet/minecraft/client/gui/GuiGraphics;m_280497_(...)'`. Cause: published mod
  jars carry an SRG-named refmap; `fg.deobf` remaps their *bytecode* into the dev mappings but
  leaves the *refmap* untouched, so Mixin looks up SRG names in a named-mapping runtime. MixinGradle
  fixes it by injecting `mixin.env.remapRefMap=true` and `mixin.env.refMapRemappingFile=<output of
  createSrgToMcp>` into the run configs. This bites *any* mixin-bearing mod pulled from CurseMaven.
- `annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'` was added in the same change but
  is almost certainly inert (it generates refmaps for the project's *own* mixins, of which there are
  none). User was asked to test removing it — outcome unknown.

### `mods.toml` version ranges — the `""` footgun

`versionRange = ""` matches **nothing**, despite the Forge docs claiming "an empty string matches any
version". Verified against `maven-artifact-3.9.1`: `VersionRange.createFromVersionSpec("")` yields
zero restrictions, and `containsVersion(...)` iterates restrictions and returns `false` by default.
`IModInfo.UNBOUNDED` — FML's fallback when the field is *omitted* — is defined as
`createFromVersionSpec("")`, so omitting it is equally broken. The check lives in
`ModSorter#verifyDependencyVersions`. Current values: RU `[0.5.6,)`, SS `[9.1,)`.

## How the compat actually works (the important architecture)

### Serene Seasons gates growth entirely through tags + a randomTick mixin

This is the key insight that shapes the whole mod: **SS does not hard-code crop lists. It reads
tags.** So most of Serene Regions is a **datapack** (tag JSON), not Java.

Mechanism (see `SereneSeasons/common/src/main/java/sereneseasons/`):
- `mixin/MixinBlockStateBase.java` injects at `HEAD` of `BlockBehaviour.BlockStateBase#randomTick`
  (cancellable) and calls `SeasonalCropGrowthHandler.onCropGrowth`. **Any block that random-ticks**
  (crops *and* vanilla-style saplings, which grow on random tick) is therefore gated.
- `season/SeasonalCropGrowthHandler.java`: if the block is a "crop" (in any season crop tag) and
  **not** fertile right now, it cancels the growth tick (behavior configurable: grow-slowly /
  can't-grow / break). Bonemeal is intercepted the same way via a `PlayerInteractEvent`.
- `init/ModFertility.java`: builds the fertile-season sets from tags and decides fertility in
  `isCropFertile(...)`. The biome logic there is what makes biome categorization matter:
  - Biome in `infertile_biomes` → **never** fertile.
  - Biome in `blacklisted_biomes` (or seasons disabled / dimension not whitelisted) → **always**
    fertile (seasons ignored).
  - Biome in `tropical_biomes` → summer crops (and unlisted crops) grow **year-round**; no winter.
  - Otherwise, if the biome is too cold to rain (`biome.warmEnoughToRain(pos)` is false, i.e.
    temperature < 0.15) → **only** winter crops grow.
  - Otherwise fertility follows the current season vs the crop's season tag(s).
  - Below `undergroundFertilityLevel` (default y=48) and not seeing sky → always fertile.
  - **Crops not listed in any season tag are treated as fertile in all non-winter seasons** — so
    only add a crop to tags if you want to *restrict* it.

### The tags Serene Regions must produce

All authored under **`src/main/resources/data/sereneseasons/tags/...`** (SS's own namespace; SS
merges additively when `"replace": false`). Mirror SS's own layout exactly — see
`SereneSeasons/common/src/main/resources/data/sereneseasons/tags/` for reference files.

- **Block tags** (`tags/blocks/{spring,summer,autumn,winter,year_round}_crops.json`) — the RU crop
  and **sapling** *blocks* to gate. Each season tag already includes `#sereneseasons:year_round_crops`,
  so adding a block to `year_round_crops` makes it fertile every season. Defined in
  `init/ModTags.java`.
- **Item tags** (`tags/items/{...}_crops.json`) — the matching **seed/sapling items**. These are
  **only used for the "fertile in: …" tooltip** (`ModFertility.setupTooltips`), not for growth. Keep
  them in sync with the block tags for good UX.
- **Biome tags** (`tags/worldgen/biome/{tropical,blacklisted,infertile,lesser_color_change}_biomes.json`)
  — categorize RU biomes. `lesser_color_change_biomes` only affects foliage-color shifting, not
  growth.

A block/item can be in **multiple** season tags to allow several growing seasons.

Whether Serene Regions needs *any* Java at all is a design choice: a data-only Forge mod still needs
`mods.toml` (declaring the mod + `dependencies` on `sereneseasons` and `regions_unexplored`) and a
`pack.mcmeta`; a `@Mod` entry class is optional unless you add runtime logic. Datagen (`runData`)
via a `TagsProvider` is a good way to author these tags instead of hand-writing JSON.

## Regions Unexplored content to map

Namespace: **`regions_unexplored`**. Everything is registered in Java, not JSON, so grep the source
for exact IDs.

- **Blocks & their IDs**: `REGIONS_UNEXPLORED_FORGE/.../block/RuBlocks.java` (~1900 lines; the
  registered name strings, e.g. `"maple_sapling"`, live here — `BlockRegistry.java` only holds
  helper methods).
- **Saplings** (~33 non-potted, e.g. `maple_sapling`, `baobab_sapling`, `palm_sapling`,
  `redwood_sapling`, `cypress_sapling`, …): all extend vanilla `SaplingBlock`
  (`world/level/block/plant/sapling/RuSaplingBlock.java`) → random-tick growth → gate them via the
  crop block tags. Ignore the `potted_*` variants (flower pots don't grow). A few use special
  growers: `BrimSaplingBlock`, `CactusSaplingBlock`, `NetherSaplingBlock`.
- **Actual growing crops**: `duskmelon` (`plant/food/DuskmelonBlock.java`, `BushBlock` +
  `BonemealableBlock`) and `salmonberry_bush` (`plant/food/SalmonBerryBushBlock.java`, sweet-berry
  style, random ticks). **`barley` is NOT a growing crop** — it's a decorative `RuDoublePlantBlock`
  (copied from sunflower); don't tag it as a crop. Cave/nether "plants" like `glister_bulb`,
  `glistering_sprout`, `prismoss_sprout` are decorative too.
- **Biomes** (~71): `data/worldgen/biome/RuBiomes.java` holds the `ResourceKey<Biome>` list; climate
  (`.temperature(...)`, `.hasPrecipitation(...)`) that determines cold/tropical classification is in
  `data/worldgen/biome/builder/*.java` (e.g. `TaigaBiomes`, `FrozenBiomes`, `AridBiomes`,
  `TropicalBiomes`-style groupings). Use temperature to decide `tropical_biomes` (hot) vs
  cold/winter-only vs normal; put caves/oceans/rivers in `blacklisted_biomes` as SS does for vanilla.

## Gotchas

- Serene Seasons is **multi-loader**; only read `common/` and `forge/` — the mechanics live in
  `common/src/main/java/sereneseasons/`.
- SS tag folders for 1.20.1 are `tags/blocks`, `tags/items`, `tags/worldgen/biome` (plural
  `blocks`/`items`). Match these paths so the merge lands on the right tag.
- Seasons only apply in **whitelisted dimensions** (default: overworld) — expect no seasonal effect
  in Nether/End, so RU nether/end plants don't need season tags.
