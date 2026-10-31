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
