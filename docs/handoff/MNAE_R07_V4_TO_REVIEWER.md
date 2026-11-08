# Технический handoff: Issue73 / QA5-v4 / MNAE R07

Сравнивайте PR HEAD с базой `integration/fabric-v4-world-origins-enemy-perf-qa5` SHA 440dfe07. Подтвердите, что CharacterData.java, CombatService, DamageSource, ActionTimelineRules, EquipmentSnapshots, MkmMod, network registration и сохранения **не изменены**. Единственные корректировки `MkmClient` — два бинда F8/F9, одно client-only render layer registration, reset debug при disconnect. Не вносить всю v2-версию клиента PR63.

Проверить `ArticulatedClipV3` на несовпадение с frozen `mkm-guards-r06-approved`, фактическим .blend SHA256, схемой координат, 17 joints/43 frames/14ticks/impact=5/14. Проверить `R07CoordinateParityTest` и все 43 удержания grip/tip; это данные model-space, **не доказательство правильной реальной геометрии vanilla ItemInHandLayer**.

Критические renderer invariants: один и только один MNAE17 layer, отсутствие double C3 rotate во время R07, skipDraw restore на входе `setupAnim` следующего кадра, видимое fallback тело при noneligible и отсутствии resource, нет JUnit/Blender references в серверном класс-лоадере. Провести F5 in-world с натуральным мечом и реально включёнными F8/F9, измерить расстояние от головы/рукояти, убедиться в руке/луке/щите/топоре и выборе главной руки. Offhand, броня, первое лицо должны быть safe fallback, а не невидимыми.

Смена мира/dimension/relog: пока нет observer S2C, UI только local; spectator gate отмечен pending. В двух игроках должен остаться vanilla/C3 fallback, без утечки приватного TIMELINE клиента на стороннего персонажа.

Три уклонения реализованы **только косметически в F8**, проверка terrain contact/foot support обязательна. Удар R07 — только combo1, остальные варианты legacy.

Непосредственная процедура испытания и QA-артефакты в `docs/MNAE_R07_V4_ISSUE73_QA.md`. Не объявлять «AAA» и не объединять до реальных кадров. Не разрешать upgrade исторического v2 JAR в v4 world.
