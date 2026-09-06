# Roadmap

This roadmap is ordered by architectural risk and integration value, not by content volume. Each milestone should leave the repository in a buildable state.

## 0.0.1 — Foundation

Goal: establish a trustworthy technical baseline before RPG content work.

- project identity (`mkm`, package namespace, metadata);
- minimal common bootstrap with no template/demo content;
- CI build on push and pull request;
- dedicated-server-safe code boundary;
- architecture, game-design and agent guidance documents;
- verify client development startup;
- verify dedicated server startup;
- choose and document persistence mechanism for RPG player data;
- choose and document networking pattern for player-state synchronization;
- add the first minimal test infrastructure where useful.

Exit criterion: a clean technical skeleton that can support Character Core without rewriting the bootstrap.

## 0.0.2 — Character Core

Goal: prove persistent, synchronized RPG state.

Initial scope:

- player RPG state container;
- level and experience;
- a small attribute set used only for the prototype;
- deterministic derived-stat calculation;
- save/load persistence;
- server-to-client synchronization;
- `/mkm` debug/admin commands for inspection and controlled mutation;
- schema/version strategy;
- tests for progression and serialization rules.

Avoid committing to the final progression model until the prototype produces useful gameplay feedback.

## 0.0.3 — Combat Core

Goal: introduce RPG combat rules without making the entire game depend on a monolithic combat manager.

Candidate scope:

- damage context and calculation pipeline;
- derived offense/defense values;
- critical/mitigation rules for the prototype;
- status/effect application boundary;
- server-authoritative combat events;
- diagnostics/testing hooks.

## 0.0.4 — Ability Framework

Goal: support one active and one passive ability end-to-end.

Candidate scope:

- stable ability identifiers;
- eligibility and targeting;
- resource/cooldown representation;
- server validation/execution;
- client request and feedback;
- data definition where justified;
- UI exposure for the prototype ability.

## 0.0.5 — First RPG Vertical Slice

Goal: integrate systems into a small playable scenario rather than expanding frameworks in isolation.

Target content:

- one build/progression path;
- one active ability;
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
- deeper ability/spell systems;
- equipment/loot progression;
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
