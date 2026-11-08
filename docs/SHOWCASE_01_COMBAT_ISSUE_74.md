# SHOWCASE-01 Combat — Issue #74: реальный мечевой контакт и школьные тактики

**Ветка:** `feature/showcase-combat-mode-fencing-v4-m1`. **База:** интегрированная Fabric QA5 PR #69 @ `440dfe07f67d4cce1a8fc10f2bd62a5a2574e137`. CharacterData **v4** и Origin v1 сохранены. Этот PR не является заменой исторического NeoForge `main`.

**Задание:** Issue #74; PR #70/GDD S07–S09. Этот проход реализует проверяемый вертикальный слой, а не обещает всю DMC/HEMA механику, полноценный клинковый sweep collider или готовый MNAE renderer.

## Архитектурный выбор

| Путь | Когда работает | Попадание и серверный ущерб | Риск |
| --- | --- | --- | --- |
| A. Ванильный ЛКМ | Исходный режим, блок, воздух, инструмент, GUI, непринятый сервером F6 | Непосредственный нативный hit; Minecraft mining без изменений | Совместимость высокая, нет hold/charge |
| B. Существующий `R` ActionTimeline | Предыдущий C2.1, 3 стадии | Server-selected Mob, старый WINDUP→ACTIVE один `player.attack` | Только Mob; отдельные сроки и stamina |
| C. Новый opt-in F6 Combat Mode **(выбран)** | Только подтверждённый сервером F6, меч в MAIN_HAND, клиентское наведение на Entity при первом нажатии | Сервер проверяет состояние, отсчитывает hold, списывает stamina, через 4/7 тиков выбирает target в 3.35 блока/конусе/LOS и вызывает **ровно один ванильный `player.attack(target)`** | Пока ограниченный конус, не движущийся клинковый хитбокс |

Режим **с выключенным серверным флагом** целиком сохраняет ванильный ЛКМ. F6 не включает перехват, пока сервер не отправит подтверждение `CombatModeStatus(enabled=true)`.

**Определение оружия:** для боевой ветки используется существующий `EquipmentRules.profile(actualMainhandId, MAIN_HAND).family()` и read-only `WeaponPresentationResolver` из QA5/#62. Меч в OFF_HAND, топор, кирка, булава, щит, лук и пустая основная рука не активируют новую атаку. Клиент не передаёт цель или хитовое время. Фактическая физическая рука остаётся в read-only контракте для MNAE; нет нового независимого справочника семейств.

## Инструкция проверки в реальном Minecraft 26.3

1. Собрать `./gradlew build` с Java 25. Проверить версию в Fabric launcher. Для **одиночной игры** добавить аргументы Java запуска игры `-Dmkm.combat.experimental=true -Dmkm.combat.qaLog=true`. Для **выделенного сервера** добавить те же аргументы перед `-jar` серверного запуска. Клиент и сервер должны использовать **одну и ту же тестовую сборку**.
2. В резервной копии тестового мира: `/give @s minecraft:iron_sword`, `/give @s minecraft:shield`, `/summon minecraft:zombie ~ ~ ~3` (GM/cheats для тестовых существ). Выбрать меч в MAIN_HAND, щит в OFFHAND. `/mkm fencing liechtenauer`; подождать 20 тиков.
3. Нажать **F6**. Строка на экране должна сказать `COMBAT MODE ON`; HUD снизу слева показывает режим. Нажать/отпустить ЛКМ **по живой сущности** в пределах досягаемости, удерживая менее 12 тиков — QUICK. Следить за `MKM QUICK`, серверным контактом через **4** тика после release. Затем держать меч ЛКМ 12–35 тиков — CHARGED: контакт через **7** тиков, заметное отбрасывание и краткий Slowness только при реальном damageTaken>0.
4. Для **защитника**: `/mkm fencing fiore`, щит в руке, дождаться смены, получить настоящий заблокированный удар. Сообщение `FIORE BLOCK | RIPOSTE READY` открывает **40-тиковое** окно. Переключиться на удар по врагу без смены школы; QUICK с положительным уроном применяет Weakness к противнику и небольшое отбрасывание. Это НЕ второй удар и не второй источник урона.
5. F6 → OFF, бейте обычным ЛКМ/ломайте камень, траву, древесину и руду киркой: сервер обязан вернуть стандартное поведение. Пока режим включён, клик по **BLOCK** не перехватывается даже с мечом; начало клика по Entity фиксирует только эту попытку, до отпускания нельзя позднее начать добычу блока.
6. `/mkm fencing destreza` + QUICK подтверждает короткий эффект Slowness на подтверждённой цели; `liechtenauer` CHARGED даёт усиленный толчок. В обеих школах посмотрите действие на двух игроках и на обычных мобах. Сравните F5, леворукость, пустую MAINHAND с мечом в OFFHAND, щит, переключение меча на кирку посреди удержания, GUI во время удержания и смерть/relog/dimension.
7. В логах с `mkm.combat.qaLog=true` ищите `MKM_COMBAT_QA commit`, `confirmed`, `resolve` по actor/target entity id. Для каждого принятого serial ожидается не более одного committed/resolved события; никакой MNAE-пакет урона не существует. Разница клиентских и серверных тиков/latency должна быть учтена; HUD-надпись `predicted` не является доказательством контакта.

## Серверные инварианты

- Только **опциональный** режим; запрет перехвата vanilla mining, GUI, offhand-only sword, иные предметы. F6 с отключённым флагом выдаёт отказ сервера и оставляет vanilla.
- C2S `CombatModeRequest` — boolean toggle; S2C `CombatModeStatus` — ACK; C2S `CombatStrikeIntent(phase, sequence)` принимает **только** PRESS(0), RELEASE(1), CANCEL(2), монотонный номер попытки. Никакого указателя на сущность, урона/координат/клиентского времени или предоставления опыта.
- На сервере удержание 0–11 тиков = QUICK (8 base stamina, +4 tick windup); 12–35 = CHARGED (22 base stamina, +7 tick windup), ожидаемая очередь запрещена при действующих R/Dodge, защите/использовании предмета, invalid world/player, кулдауне. Отказы не вызывают `player.attack`.
- Подтверждение контакта в `CombatEvents.afterDamage` допускает **только** реальный положительный неблокированный server healthDamage. Силовой множитель применяется одному серверно выбранному direct target, а не вторичным vanilla sweep-целям. Временный `resolving` и one-shot `hitConfirmed` исключают повторное применение вторичных эффектов в одной атаке.
- Обычный `CombatEvents.modifyIncoming` и единственный `LivingEntityCombatMixin.hurtServer` остаются владельцами скалирования. MNAE не наносит урон; клиентское действие не запускает `player.attack`.
- Важные ограничения: target сейчас определяется ограниченным **серверным конусом + LOS**, не по анимированной траектории клинка. Дальность не расширена за ванильную рекомендацию. Не реализованы анимационные phase ACK MNAE, 3–5 горячих специальных приёмов, directional parry, stagger knockdown, полная server-hurt visual spectator sync. Нельзя утверждать точную HEMA-хореографию или готовые R07 позы.

## Школы и CharacterData v4

Из PR #65 перенесены только чистые `FencingSchool`, `FencingSchoolState`, `FencingSchoolRules`, `FencingSchoolMotionCues` и выбор стойки. `EquipmentSnapshots` сохраняет `OriginModifierRules.snapshot` с выбором расы/класса QA5, добавляя **только** разрешённые сервером школьные дельты ПЕРЕД итоговым `PlayerModifierCalculator`. Никакого копирования старого CharacterData v3/Feature M2 поверх v4.

- **Лихтенауэр:** усиленное отбрасывание при подтверждённом CHARGED, pressure/stagger и дополнительная цена stamina.
- **Фиоре:** настоящий shield BLOCK → 40-tick RIPOSTE READY → первый подтверждённый QUICK добавляет Weakness и небольшой knockback; без блока и контакта эффекта нет.
- **Destreza:** подтверждённый QUICK ненадолго замедляет противника, менее дорогой dodge, отдельное MKM penetration (не пробивает ванильную броню).
- Выбор через `/mkm fencing [liechtenauer|fiore|destreza|none]`; 20 тиков подготовки, 80 тиков кулдаун, используется только SWORD в MAIN_HAND. Стойка owner-only synced, сбрасывается после смерти и перезахода, **не записывается в CharacterData**.

## Тесты и ограничения приёмки

CI выполняет JUnit для press/release, 12-tick charge, одноразового impact, packet loss/reorder/spam, TTL, GUI/mining fallback, target cone/LOS, v4 single final clamp, школьных gate-правил, а также прошлые QA5 тесты World/Enemy/Character/Performance. Скрипт `tools/combat/qa/showcase_authority_selfcheck.py` проверяет отсутствие клиентского урона и сохранение v4. К клиентскому boot / dedicated server boot нельзя приравнивать 2 реальных игрока.

**BLOCKERS до готового SHOWCASE-01:** контролируемый F5 60fps ролик, реальный PvP 2 игроков, Wi-Fi latency/reorder, подтверждённый knockback на мобе и PvP, тест Entity→BLOCK при удержании, offhand, Java25 на Windows, upgrade v3→v4 с реальным мировым сохранением, запуск рядом с Magic M4 и MNAE R07 без повторных регистраторов. Требуется провести эту матрицу на копии мира; не заявлять visual/gameplay acceptance без записи.

## Handoffs

**Integration QA / PR #69/#70:** cherry-pick из этого PR на QA5, не из старого #65 целиком. Files with conflicts: `MkmMod`, `MkmClient`, `EquipmentSnapshots`, `CombatEvents`, `CombatService`, `CombatNetworking`, `mkm.mixins.json`. Проверьте одну регистрацию payload каждого типа; Origin v1 и CharacterData v4 должны остаться без изменений. Режим выключен по умолчанию.

**Animation:** use existing WeaponPresentationResolver and read-only FencingSchoolMotionCues. F6 серверная задержка 4/7 тиков от RELEASE; MNAE клип пока не может считать клиентский LMB «hit». Для визуальной синхронизации нужен отдельный серверный phase ACK/наблюдатель; не использовать локальную временную подсказку как подтверждённое попадание. Никогда не добавлять MNAE damage event.

**Character:** school is transient stance, no persisted unlock. Future mentor/class eligibility server verifies before activation. No second calculator or XP write.

**World/AI:** effects only after actual server health damage. Mob attacks continue original vanilla/Enemy AI routes; no extra rewards or new target scans beyond bounded local contact query on each queued hit. Do not register a new AI damage hook.

**Magic:** preserve original AbilityService and magic school channels, no damage from combat pose events.
