# Mod Template

The starting point for Elduin's Minecraft mods. Every mod in
[Elduin-Labs](https://github.com/Elduin-Labs) is created from this repo.

**Fabric only.** One source tree, built for more than one Minecraft version by
[Stonecutter](https://stonecutter.kikugie.dev/).

Adapted from [rotgruengelb/stonecutter-mod-template](https://github.com/rotgruengelb/stonecutter-mod-template),
with NeoForge and Forge removed.

## Minecraft versions

| version | Java |
|---|---|
| 1.21.11 | 21 |
| 26.2 | 25 |

Gradle downloads whichever JDK a version needs, so only one JDK has to be
installed locally.

## Making a mod from this

```bash
gh repo create Elduin-Labs/<slug> --public --template Elduin-Labs/mod-template --clone
cd <slug>
python3 setup.py
```

`setup.py` asks for the mod id, name, package and so on, rewrites
`stonecutter.properties.toml`, renames the mixin config and moves the Java
sources into the right package. Delete it afterwards.

## Building

```bash
./gradlew "Set active project to 1.21.11-fabric"
./gradlew "1.21.11-fabric:build"
```

Switching is optional: each version subproject regenerates its own sources, so
the jar is correct either way. It's there to keep the shared tree and the IDE in
the version you're reading. Never hand-edit `.sc_active_version` to do it —
Stonecutter tracks the tree's current form and that desyncs it.

To build everything:

```bash
./gradlew build
```

Jars land in `versions/<version>-fabric/build/libs/`.

## Adding a Minecraft version

Four places, and they must agree:

1. `settings.gradle.kts` — the Stonecutter version list
2. `stonecutter.properties.toml` — a `[fabric."<version>"]` block with the
   Minecraft, Fabric API and Mod Menu versions
3. `.github/workflows/release.yml` — the `minecraft` matrix, with the right Java
4. `CLAUDE.md` — the facts table

Every version you add is another full copy of Minecraft to download, decompile
and remap. Keep the list short.

## A note on memory

`gradle.properties` deliberately sets `org.gradle.parallel=false` and
`org.gradle.workers.max=2`. Building Minecraft is memory-hungry, and building
several versions at once on an 8 GB machine will exhaust RAM. Leave these alone.

## Releasing

Push a `v<version>` tag. `.github/workflows/release.yml` builds every version and
uploads one Modrinth file per Minecraft version, using the organization's
`MODRINTH_TOKEN` secret and the repo's `MODRINTH_PROJECT_ID` variable.

## Licence

MIT.
