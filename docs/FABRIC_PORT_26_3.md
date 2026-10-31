# Fabric 26.3 migration

Target: Minecraft 26.3, Fabric Loader 0.20.1, Fabric API 0.165.0+26.3, Loom 1.19-SNAPSHOT, Java 25.

This branch is a **separate platform port** from NeoForge 26.2; no earlier PR is merged or overwritten.

- The seven RPG attributes, modifier definitions, formula calculus, and pure unit tests retain their existing Java domain model.
- Player CharacterData is attached via Fabric Data Attachment API, persistent and copied on death, with owner-only synchronization. Existing NeoForge save attachments are **not automatically migrated** because the persistence formats differ. Back up worlds and migrate explicitly later.
- Server-authoritative stamina is nonpersistent and synced only to its owner.
- Dodge networking uses Fabric's payload registry, not NeoForge payload registration.
- Combat melee modifiers require one small Fabric Mixin because Fabric's high-level ALLOW_DAMAGE callback can cancel but cannot change the amount.
- Poise uses server living entity callbacks and remains provisional; full animation/AI stagger still requires work.

Runtime checklist: load mod and Fabric API on Minecraft 26.3; join multiplayer; /mkm ping and /mkm stats; open K sheet; right-click Codex; dodge G+movement; heavy Shift+hit; poise behavior; save/load; death copy; dedicated-server startup; world backup and migration.

## Self-contained player installation (Fabric Loader only)

Beginning with `0.0.3-fabric.2`, MKM uses Fabric Loom's official `include(implementation(...))` to ship the **complete upstream Fabric API distribution** inside the release JAR. That distribution itself contains its versioned Fabric API module JARs. Loader understands and resolves all nested metadata; the API is not copied into our Java packages, forked, shaded or impersonated.

Players install only Fabric Loader for Minecraft 26.3 and the single `mkm-rpg-0.0.3-fabric.2.jar`. **Do not install Fabric API separately for MKM alone.** If other mods require a newer version of Fabric API, Fabric Loader may select the compatible external version; regular Fabric dependency compatibility requirements still apply. Fabric Loader remains mandatory. The embedded API doesn't guarantee compatibility with arbitrary mod packs.

The build and release CI check the outer `fabric.mod.json` nested JAR entries, find one embedded `fabric-api` distribution and check that it itself contains child modules. This catches an incorrectly packaged binary before release. API bytes come from FabricMC without modification; Apache-2.0 licensing information is included as a third-party notice and license copy.

The compiled game runtime still requires Minecraft 26.3 and Java 25; build tools and dependencies remain necessary **for developers compiling from source**, not for players installing the JAR.
