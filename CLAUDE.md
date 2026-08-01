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

Current state (verified 2026-07-31):
- Standard Forge 1.20.1 MDK. `gradle.properties` is set: `mod_id=sereneregions`,
  `mod_group_id=net.demolutio.sereneregions`, `mod_authors=demolutio`, `mod_version=0.1-1.20.1`,
  `forge_version=47.4.21`.
- The example package **has been renamed** to `net/demolutio/sereneregions/`
  (`SereneRegions.java`, `Config.java`), but the **bodies are still the untouched MDK example** —
  `EXAMPLE_BLOCK`, `EXAMPLE_ITEM`, `EXAMPLE_TAB`, the "HELLO FROM COMMON SETUP" logging, the dirt
  block config. All of that is dead weight to be deleted/replaced.
- `src/main/resources/META-INF/mods.toml` **already declares** mandatory dependencies on
  `regions_unexplored` (`[0.5.6,)`) and `sereneseasons` (empty `versionRange`), both `ordering=AFTER`.
- **No tag JSON and no datagen exist yet.** `src/main/resources/data/` does not exist. This is where
  the actual work starts.
- `build.gradle` has **no RU/SS dependency declared** — the `dependencies` block is still all
  commented-out MDK examples, and `flatDir 'libs'` is commented out too.

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
