# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

**Serene Regions** is a Forge **1.20.1** compatibility mod (`mod_id = serene_regions`). Its
sole purpose is to make **Regions Unexplored** (RU) crops, saplings and seeds respond to
**Serene Seasons** (SS) seasons — i.e. give each RU crop/sapling the season(s) in which it is
allowed to grow, and categorize each RU biome so seasons behave sensibly there.

This repo is a **standard Forge 1.20.1 MDK** that has **not been customized yet**. The Java
source is still the untouched MDK example (`src/main/java/com/example/examplemod/ExampleMod.java`,
package `com.example.examplemod`, `mod_group_id=com.example.examplemod`). Only `gradle.properties`
mod metadata (`mod_id`, `mod_name`, `mod_authors`) has been set. **Actual compat work has not
started.** When beginning, expect to rename/replace the example package and class.

Two upstream mods are **cloned into this repo as reference source only** — they are not part of the
build:
- `SereneSeasons/` — multi-loader (common/forge/fabric), namespace `sereneseasons`
- `REGIONS_UNEXPLORED_FORGE/` — single-loader Forge, namespace `regions_unexplored`, `mod_version 0.5.6`

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
  Forge gametests under namespace `serene_regions` (run config already wires
  `forge.enabledGameTestNamespaces`).
- First-run gotcha: ForgeGradle decompiles Minecraft and needs the `-Xmx3G` already set in
  `gradle.properties`.

### Making RU + SS available in the dev environment

Nothing in `build.gradle`'s `dependencies` block references RU or SS yet. Adding the tags alone is
enough to *build* the jar, but to **test** in-game the two mods must be present at runtime so their
block/item/biome IDs resolve. Either build jars from the cloned repos, or (simpler) drop RU + SS +
their deps (SS needs GlitchCore; RU needs TerraBlender) into a `./libs` flat-dir repo and declare
them with `fg.deobf(...)` / `runtimeOnly`. Decide this before writing any Java that imports SS/RU
classes.

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
