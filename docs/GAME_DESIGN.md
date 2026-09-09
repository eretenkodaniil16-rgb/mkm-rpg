# Game Design

## Vision

MKM is a large-scale RPG conversion of Minecraft. It should preserve the strengths of Minecraft as an explorable, systemic world while adding a much deeper role-playing layer: persistent character builds, active abilities, meaningful quests, dialogue choices, factions, authored encounters, progression and bosses.

The intended result is closer to an action-RPG/immersive RPG implemented inside Minecraft than to a turn-based CRPG rules transplant.

## Design pillars

### 1. Character identity

A character should be defined by more than equipment. Attributes, level progression, class/build choices, skills, abilities, status effects, reputation and quest decisions should create mechanically distinct play styles.

### 2. Real-time Minecraft combat foundation

Combat remains real-time and preserves Minecraft's core interaction model: player movement, physical aiming/hit validation, normal attacks, projectiles, blocking and positioning remain the foundation. MKM adds RPG depth on top through player modifiers, stamina, weapon archetypes, active/passive abilities, damage types, stagger, statuses, enemy roles and counterplay.

MKM does not use D&D-style initiative or turn order for ordinary combat. A physically valid Minecraft hit should not be converted into a hidden accuracy/evasion miss roll.

### 3. Consequential interaction

Dialogue, checks, faction standing and quest state should influence available options and outcomes. The player should be able to solve some situations through combat and others through investigation, social choices, exploration or build-specific capabilities.

### 4. Systemic world, authored RPG content

Minecraft's sandbox systems remain useful, but important encounters, dungeons, NPCs, quests and bosses may be authored deliberately. Procedural systems should support authored content rather than replace it.

### 5. Long-form progression

Character growth should support extended campaigns. New power should expand decision space rather than only increase numerical damage and health.

### 6. Multiplayer-safe foundations

Even when a feature is initially tested in single-player, it must not rely on assumptions that make dedicated-server or multiplayer support structurally impossible.

## Current high-level system map

The project is expected to grow toward these domains, introduced only when needed:

- character state and progression;
- primary attributes and derived player modifiers;
- real-time combat rules and damage pipeline;
- active/passive abilities and magic;
- classes/builds and perks;
- equipment and loot progression;
- NPCs and combat AI;
- dialogue;
- quests and world-state flags;
- factions and reputation;
- encounters, dungeons and bosses;
- client HUD/UI and presentation;
- persistence, networking and data definitions.

The accepted player-modifier vocabulary is tracked in `PLAYER_MODIFIERS.md`. Exact primary attributes and formulas remain a separate design step.

## First vertical slice

The first playable RPG slice should be intentionally small but end-to-end. A candidate slice contains:

- one player progression path;
- one active ability;
- one meaningful item/equipment interaction;
- one NPC;
- one hostile enemy archetype;
- one dialogue;
- one short quest with at least one state transition;
- one small encounter area;
- one reward/progression outcome;
- complete save/load and multiplayer-safe state synchronization for the features used.

The purpose is to test integration between systems before large content production begins.

## Non-goals for the foundation phase

The foundation milestone does not attempt to:

- replace Minecraft's renderer;
- replace Minecraft combat with a turn-based D&D combat loop;
- implement every RPG system up front;
- create a universal game framework unrelated to MKM's current needs;
- build large amounts of content before persistence/networking boundaries are proven;
- promise compatibility with every other mod at the expense of core design.

## Open design questions

These are deliberately unresolved and should be answered through prototypes and design work rather than assumptions:

- final primary attribute set and attribute-to-modifier formulas;
- classless vs class-based progression, or a hybrid;
- stamina tuning and which advanced actions consume it;
- final magic resource model, spell schools and elemental taxonomy;
- ability resource/cooldown model;
- dialogue presentation and check mechanics;
- death/respawn consequences;
- world structure and campaign progression;
- how strongly survival/crafting systems integrate with RPG progression;
- scope of procedural vs handcrafted dungeons and encounters.
