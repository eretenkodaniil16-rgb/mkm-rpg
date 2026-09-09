# Roadmap

This roadmap is ordered by architectural risk and integration value, not by content volume. Each milestone should leave the repository in a buildable state.

## 0.0.1 — Foundation

Goal: establish a trustworthy technical baseline before RPG content work.

Implemented on the foundation branch:

- project identity (`mkm`, package namespace, metadata);
- minimal common bootstrap with no template/demo content;
- CI build on push and pull request;
- build artifact upload after successful CI;
- dedicated-server-safe code boundary;
- architecture, game-design and agent guidance documents;
- persistence mechanism selected: NeoForge player data attachment;
- server-to-client Character Core synchronization selected: owner-only attachment synchronization;
- first Character Core persistence prototype implemented to exercise the chosen boundaries.

Still required before closing the foundation milestone:

- verify client development startup and in-game commands;
- verify save/exit/reload persistence;
- verify death/respawn copy behavior in a running game;
- verify dedicated server startup;
- add focused automated tests where the first stable domain rules justify them.

Exit criterion: a clean technical skeleton that can support Character Core without rewriting the bootstrap.

## 0.0.2 — Character Core

Goal: prove persistent, synchronized RPG state and then turn the current prototype into a tested gameplay foundation.

Current prototype scope already present:

- player RPG state container;
- total experience and derived level;
- prototype Strength, Dexterity and Vitality attributes;
- save/load persistence through a versioned attachment schema;
- owner-only server-to-client synchronization;
- `/mkm stats`, `/mkm xp add` and `/mkm xp set` commands;
- first read-only Character Sheet client interface.

Current design work:

- accepted player-modifier vocabulary is documented in `PLAYER_MODIFIERS.md`;
- real-time Minecraft combat is the foundation; ordinary combat will not use initiative/turn order;
- magic is planned as a first-class path and already has reserved player modifier categories for spell power, mana, magical defense and support magic.

Next Character Core work:

- runtime verification on client and dedicated server;
- progression/serialization tests;
- define the final primary attribute set;
- map primary attributes to the accepted player modifiers;
- decide how attribute points/build choices are acquired;
- evaluate schema migration mechanics before replacing the prototype STR/DEX/VIT format;
- update Character Sheet after the attribute model is accepted.

Avoid committing to final numerical formulas until the attribute-to-modifier mapping is reviewed.

## 0.0.3 — Combat Core

Goal: add RPG depth while preserving Minecraft's real-time combat loop and avoiding a monolithic combat manager.

Candidate scope:

- authoritative player modifier snapshot and aggregation rules;
- real-time damage context and calculation pipeline around valid Minecraft combat interactions;
- physical offense/defense values and mitigation;
- slashing, piercing and blunt physical damage categories;
- stagger/poise boundary;
- stamina boundary for advanced actions rather than normal attacks;
- critical/backstab/weak-point condition hooks without mandatory hidden accuracy/evasion rolls;
- status/effect application boundary;
- server-authoritative combat events;
- diagnostics/testing hooks.

Weapon-specific modifiers, weapon mastery and detailed equipment rules should build on this core after the player modifier model is stable.

## 0.0.4 — Ability and Magic Framework

Goal: support active/passive abilities and the first spell end-to-end without bypassing server authority.

Candidate scope:

- stable ability/spell identifiers;
- eligibility and targeting;
- stamina/mana/resource and cooldown representation;
- server validation/execution;
- client request and feedback;
- spell damage/healing/barrier hooks into the shared player modifier model;
- first magic penetration/resistance path;
- data definition where justified;
- UI exposure for the prototype ability/spell.

Final spell schools and elemental taxonomy should be defined through gameplay/lore design rather than assumed in advance.

## 0.0.5 — First RPG Vertical Slice

Goal: integrate systems into a small playable scenario rather than expanding frameworks in isolation.

Target content:

- one build/progression path;
- one active ability or spell;
- one equipment interaction;
- one NPC;
- one enemy archetype;
- one dialogue;
- one short quest;
- one small encounter location;
- one meaningful reward/progression outcome.

The vertical slice must survive save/load and work with the project's multiplayer authority model.

## After the first vertical slice

Only after the slice is reviewed should the project scale into larger domains such as:

- class/build trees and perk systems;
- deeper spell schools and magic systems;
- equipment/loot progression and weapon mastery;
- faction and reputation systems;
- richer dialogue/check mechanics;
- quest graphs and world-state consequences;
- enemy roles and advanced AI;
- handcrafted/procedural encounters and dungeons;
- bosses;
- full HUD/character screens;
- balancing/content pipelines;
- migration tooling and compatibility policy for public releases.

## Milestone discipline

A milestone is not complete merely because code exists. Completion requires relevant build/test evidence, updated documentation for durable design decisions and no known dedicated-server regressions in its scope.
