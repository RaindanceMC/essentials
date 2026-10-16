# RaindanceMC/essentials

- **`essentials-module`** — Rain's Flexible Modules — Essentials: The boring stuff players immediately expect: /spawn, AFK, private messages, /seen, join and quit lines, and a nickname. Homes, warps, teleport requests, random teleport and going back are their own modules already — this is what is left once those are taken out.
- **`essentials-standalone`** — Rain's Essentials: Rain's Essentials, as a plugin of its own. Nothing here but a build file and a descriptor: the essentials module and the standard wrapper are shaded together, and that is the whole plugin.

Part of [RaindanceMC](https://github.com/RaindanceMC); built on [RainsCore](https://github.com/RaindanceMC/RainsCore) and the shared parent in [framework](https://github.com/RaindanceMC/framework). Split out of `rains-flexible-modules` on 16.10.2026, history kept.

## Build

```
mvn install
```

Needs Java 25 and read access to `packages.tstieh.de` (server ids `packages-releases` / `packages-snapshots` in `~/.m2/settings.xml`).

## Plugins

- `essentials-standalone/target/RainsEssentials-1.28.2.jar` — also published as `de.raindancer.modules:essentials-standalone:1.28.2`

## Versions

A module's version is a property in this repository's root `pom.xml`, a plugin's is `plugin.version` in its own pom. CI publishes every version that is not in the registry yet on the next push to master.
