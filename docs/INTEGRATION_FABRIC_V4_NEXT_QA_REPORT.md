# MKM RPG — независимый интеграционный аудит следующей Fabric-версии

**Статус: DRAFT QA. НЕ публичный релиз. НЕ игровая или визуальная приёмка.**
Minecraft 26.3 / Fabric 0.20.1 / Java 25. Переходная ветка: integration/fabric-v4-next-qa-gates-v1.
Основа: PR #30 @ 43cf6b0755a02f56b6a5d61366ccfd3b12c856a0 → PR #45 @ 57d5a0160437caf65b67b62868559a8da815a12a. Версия тестовой сборки 0.0.6-fabric.0-qa.1.

## 1. Реальный граф зависимостей

| PR | SHA head | Фактическая база | Решение |
|---|---|---|---|
| #30 | 43cf6b07 | Fabric C3.7 | Историческая контрольная интеграция: Character v3, Combat D3, Magic M1, World E1; не изменять. |
| #35 | f6c18934 | Character M2 #29 → M1 #15 | Goldens и протокол v4; перенесены реальные тестовые v3 JSON, не весь Character code заново. |
| #36 | c3fa6ebc | PR #30 | Combat input и canonical read-only WeaponPresentationResolver; перенести чистые контракты, не экспериментальный shield/LMB Mixin. |
| #39 | e79250d4 | World E2 #26 → E1 #18 | Реализация World E2.1; уже выборочно интегрирована в #45. Вторичное наложение затрёт Character и World. |
| #42 | 737490ed | PR #30 | Magic M3, но Character v3; отдельная адаптация на v4 обязательна. |
| #44 | eeeae560 | MNAE #41 | Client resolver + C3 math; v2 персонаж; не ставить как обновление v3/v4. |
| #45 | 57d5a016 | PR #30 | Выбранная основа Character v4 + World E2.1; зелёный отдельный CI. |
| #46 | 94a6ddff | MNAE #41 | Другой client resolver и рендер 6 ModelPart; конкурирует с #44, v2; не слепо объединять. |
| #49 | 579ff5b3 | PR #30 | Документы и отдельный workflow static QA; не проверяет игру. |
| #53 | 5cae0c0d | дизайн #52 → #51 → #50 | 17 костей Blender, девять реальных превью; последний CI action_required, ноль выполненных jobs. |

Сравнение коммитов подтвердило: PR #45 на 11 коммитов впереди PR #30 и ни на одном позади; #42 и #45 ДИВЕРГИРОВАЛИ. #44 и #46 имеют общую базу #41, но конфликтующие изменения в анимации. #49 — автономная документация.

**Порядок:** PR #30 сохраняется → использовать PR #45 как отдельную тестовую v4 основу → переносить только единый боевой read-only resolver → отдельный Magic M3 v4-safe → после настоящей видеопроверки только опциональный MNAE. Все ветки draft, исторический NeoForge main неприкосновенен.

## 2. Дублирование WeaponPresentationResolver

**Вариант A:** оставить два источника истины — Combat классифицирует по EquipmentRules и ID, MNAE по ItemTags. Риск: топор превращается в меч, а пустая mainhand + offhand sword ошибочно изображается как безоружный клип. Тесты разных модулей не гарантируют一致ие. Отклонён.

**Вариант B (выбран):** Combat-owned com.eretenkodaniil.mkm.combat.presentation.WeaponPresentationResolver определяет actual MAIN_HAND/OFF_HAND, физическую LEFT/RIGHT руку и семейство через EquipmentRules; а MNAE имеет только MnaeRComboPresentationRoute для выбора существующего косметического клипа. Никакого второго классификатора. В тестах: меч, топор, кирка, булава, лук, щит, сторонний неизвестный ID, левая рука, offhand sword, F8, комбо 1/2/3 и старые перекаты. Для неаттестованной offhand-ситуации — VANILLA (без косметического клипа), но серверные попадания/урон не меняются.

**Граница готовности:** новые pure Java классы и тесты включены в ветку. Они ПОКА НЕ подключают новый рендерер MNAE. PR #36 оставлен отдельным; его shield experiment, LMB Mixin, stamina и изменения CombatEvents НЕ перенесены. Future modded weapon categories должны расширять единую Equipment classifier, а не вводить третью систему.

## 3. CharacterData v3→v4: процедура и реальные гарантии

- Старые #41/#44/#46 содержат v2; #30 и #42 — v3 (XP, 7 характеристик, earned и 7 allocated points); #45 — v4 (+ claimed_reward_ids).
- Код CharacterData v4 использует ALLOCATION_SCHEMA_VERSION=3 и НЕ обнуляет v3 купленные очки. В старых v1/v2 расход не был сохранён, поэтому migration там другая. Не менять порог версии 3.
- Серверный CharacterRewardRules.grantOnce атомарно изменяет XP + список receipt в ОДНОМ заменяемом Character attachment; Ford Quest и World encounter persisted отдельно и НЕ образуют ACID-транзакцию.
- Порядок: receipt перед quest REWARDED; старый reward_xp_floor читается, но не пополняет XP. Уже выполненный старый квест БЕЗ receipt = MANUAL_REVIEW, а не повторная выплата.
- Клиент получает XP/характеристики/кредиты, но не серверный список claimed_reward_ids. Future unknown schema должен запрещать мутации. Восстановление v4→v3 без полной копии недопустимо.

**Порядок реального эксперимента с миром:**
1. Закрыть Minecraft и сервер, убедиться в прекращении записи на диск.
2. Сохранить ВСЮ папку мира вместе с playerdata, data, region, level.dat и Fabric world attachments. Ветка содержит offline инструмент:
   python3 tools/integration/backup_world_preflight.py --world "/путь/к/миру" --output "/отдельная/папка/world-before-v4.zip" --server-stopped
   Флаг подтверждает оператор, скрипт НЕ определяет сам работающий процесс. Он запрещает перезапись, копию внутри мира, небезопасные ссылки, проверяет CRC и SHA256 каждого файла.
3. Распаковать полную копию в НОВУЮ тестовую папку, не изменяя оригинал. Зафиксировать SHA256 ZIP и старый JAR.
4. Запустить сервер и ОБА клиента с идентичным QA JAR 0.0.6-fabric.0-qa.1 и Fabric API. Убедиться в отсутствии старых mkm JAR.
5. Сравнить реальные записи до/после: XP, семь характеристик, earned/spent/available, все 7 allocations, размер и содержимое receipt (сервер), UUID игроков; relog/death/restart. Не подменять серверный факт значением HUD.
6. Два игрока: PER10/PER12, общий NPC Ильра, независимые raider UUID, чужой убийца, правильный владелец quest, задержка/подделка сетевого запроса, журнал J, выдача и повторная выдача награды после restart. 
7. Выгрузка/загрузка чанка, телепорт и смерть рандомного противника offline owner, partial save-cut player/world, проверка отсутствия двойного spawn. Удалённая/непрогруженная сущность НЕ свидетельствует о смерти.
8. Откат только остановкой сервера, восстановлением полного старого ZIP И старого JAR одновременно. Нельзя запускать v3 JAR в уже мигрированном v4 мире.

**Автоматическая проверка:** исходные GOLDEN v3 JSON из #35 (включая 14 уже потраченных points, административный STR=63 и старое максимальное XP=650000) проверяются в IntegratedV3ToV4UpgradeTest через настоящий CharacterData.CODEC v4. Проверены receipt replay, lossless fields, old rewarded/no receipt, future schema reject, malformed allocation reject. MultiOwnerEncounterStressContractTest моделирует 256 владельцев и две независимые receipt history, но НЕ запускает 256 реальных Minecraft мобов/чанков. Python backup preflight содержит 5 тестов.

## 4. Проверка #45, CI и пакетирование

У #45 уже есть World E2.1 с PER12, валидированными выбором/дистанцией NPC, per-owner UUID, replay-resistant dialogue session, J journal, immutable registry (MAX_ACTIVE 4096), tombstone. Существуют JUnit модели двух владельцев и codec roundtrips. До этого этапа настоящие два клиента, реальный chunk-unload, NBT из пользовательского мира, server power-cut и GUI visual QA НЕ выполнялись.

**Подтверждённая ошибка:** исходный #45 сохранил версию 0.0.5-fabric.4 и старый Windows installer JAR несмотря на смену схемы сохранения и сетевого протокола. В QA ветке введены 0.0.6-fabric.0-qa.1 и архив MKM_Fabric_26.3_Integration_V4_QA_ONLY.zip; README и установщик предупреждают про backup и невозможность downgrade. Никакой публичный релиз не создавался.

Промежуточный экспериментальный CI: Java25 Gradle/JUnit, Python tests, server + Xvfb client boot, оригинальные animation selfchecks и сборка Windows успешно прошли, **но два workflow были завершены FAIL из-за неверного пути для переименованного ZIP** в последнем upload-artifact. Это исправлено отдельным коммитом 2ffda270 (workflow теперь указывает на новое QA имя ZIP). Для приёмки обязателен зелёный FULL workflow на финальном SHA, а не перенос статуса со старых SHA.

## 5. PR #53 / GitHub Actions action_required

На head 5cae0c0dae1daf1bc7cfe07945af1fb67ea741b8 два workflow 42418646285 и 42418646166 имеют completed/action_required, **нулевое количество jobs**, actor/trigger github-actions[bot]. На предыдущем пользовательском 17e84e2c оба workflow 42418585027 и 42418585019 SUCCESS и реально выполняли Blender/QA.

Причина согласно документированному GitHub поведению: auto-commit в PR инициирован Actions через GITHUB_TOKEN, в итоге PR workflow требует одобрения (https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/trigger-a-workflow). Это системный approval gate, НЕ ошибка Blender и не доказательство успешных последних прогонов. Пользователь с write-доступом должен посмотреть изменённые workflows на PR #53 и нажать Awaiting approval → Approve workflows to run, затем дождаться зелёного результата именно SHA 5cae0c0d. Наличие .blend, 9 PNG, manifest.json в Git не заменяет CI.

## 6. 17-костный Blender-риг и MNAE

**Вариант A:** ретаргетировать 17 костей в шесть Vanilla ModelPart без нового рендерера. Дешевле, но локти, колени, кисть, пятки, вложенные pivot, IK контакт и надёжное положение рукояти необратимо теряются. Только компромиссный C3/vanilla fallback.

**Вариант B (предпочтителен, но НЕ реализован здесь):** опциональный косметический 17-bone articulated player renderer/overlay, сохранив серверный Combat/Collision/Hitbox и vanilla предметы. Пример — сначала отдельный манекен со статическими High Outer/Mid/Low guard poses, потом работающий socket ItemInHandLayer, потом движения и наблюдатели. Не переписывать CombatService или всю игру.

В экспортёре фиксировать rest/bind matrices, Blender +X-right/-Y-forward/+Z-up → Minecraft ModelPart basis (перемещение model pixels, согласованные pivot, scale и quaternion order); тестировать Blender world hand/grip/tip координаты на кадрах 1/20/40. Blender сцена имеет рост 1.8 authoring units, прямое соответствие блокам нужно КАЛИБРОВАТЬ. Опубликовать только data/renders отдельным PR, никакой функциональной зависимости PAL. Контроль: 60fps F5 в реальном Minecraft с фронтального/бокового/заднего вида, броня, щит, offhand, left arm, GUI scale, 2 players и отсутствующие head intersections. Состояние R06: approved authoring baseline, game_mnae_status NOT_TESTED.

## 7. Расширяемость рас, классов, мобов

**Вариант А:** добавлять новые race/class/skill arrays напрямую в CharacterData v4, часто повышая schemaVersion и рискуя ledger XP. **Вариант Б (предпочтителен):** отдельный версионированный persisted компонент mkm:character_build с идентификаторами расы/класса и выбранными способностями, явно мигрируемый; одно вычисление PlayerModifierSnapshot + EquipmentSnapshots со стабильным порядком эффектов и без двойного начисления предметов. Не создавать пустые классы, пока не появится реальный feature owner. Параметры race/class описывать в валидируемых JSON с namespace/version, а не клонировать шаблоны в каждом Player NBT.

Для мобов: versioned mkm:mob_archetype definition, identity UUID, faction/role/AI, rig profile, data-driven spawn pools; chunk lifecycle event and per-chunk spatial index, а не глобальные сканы каждым мобом каждый тик. NPC visual event не должен порождать урон или AI переход. Границы server authority/serialized packets, ограниченные per-player observation events.

## 8. Стратегия массового QA

Текущий автоматический уровень A: golden migration, Reward once, 256 immutable owner reservations, weapon selection/F8 policy и backup ZIP; source+SHA anchored Java25 CI. Это НЕ тест Minecraft TPS.

Следующий уровень B: Fabric GameTests/dedicated harness с 2 владельцами, наборами 50/200/500/1000 сущностей, расстояниями между 1/4/16/64 чанками, hostile idle/chase/pathfinding, загрузкой/выгрузкой и телепортами, рестартом и произвольными точками падения сохранения. Проверить UUID, no duplicate spawn/XP, trust boundary wrong owner, bounded tombstone, никакой клиентский косметический event не меняет урон.

Собирать метрики warm и worst-case MSPT p50/p95/p99, TPS, heap high water, GC pause, allocation rate, chunk tickets, количество AI scans, кб/сек packets по игрокам, peak active jobs и профиль серверной сети. Целевой серверный бюджет 20 TPS = 50 мс на tick (не утверждение текущих результатов). Сравнение без мода и с модом на одном world seed, Java 25, heap и дистанции. 30–60 минут soak, бэкпрешер при превышении бюджета. Registry immutable Map.copyOf при добавлении/удалении O(n); MAX_ACTIVE=4096, для миллионов сущностей нужна ограниченная partition/spatial registry с данными для unloaded chunks.

Уровень C: настоящий Windows клиент и dedicated server, F5/GUI first-person и 2 реальных клиента, видеоподтверждение пользователя. Ни CI с Xvfb, ни Blender-превью, ни POSE APPLIED не доказывают визуальное качество.

## 9. Доказательства и решение руководителю

Проверенные отдельные зеленые CI на HEAD:
- PR #30: https://github.com/eretenkodaniil16-rgb/mkm-rpg/actions/runs/41263128504
- PR #35: https://github.com/eretenkodaniil16-rgb/mkm-rpg/actions/runs/41271525973
- PR #36: https://github.com/eretenkodaniil16-rgb/mkm-rpg/actions/runs/41271510725
- PR #39: https://github.com/eretenkodaniil16-rgb/mkm-rpg/actions/runs/41271245756
- PR #42: https://github.com/eretenkodaniil16-rgb/mkm-rpg/actions/runs/41271496271
- PR #44: https://github.com/eretenkodaniil16-rgb/mkm-rpg/actions/runs/41271518069
- PR #45: https://github.com/eretenkodaniil16-rgb/mkm-rpg/actions/runs/41271658267
- PR #46: https://github.com/eretenkodaniil16-rgb/mkm-rpg/actions/runs/41271942883
- PR #49 docs-only: https://github.com/eretenkodaniil16-rgb/mkm-rpg/actions/runs/41748758174
- PR #53 HEAD requires approval: https://github.com/eretenkodaniil16-rgb/mkm-rpg/actions/runs/42418646285

**Решение:** изолированную ветку следующей QA-версии допускать к JUnit/CI, но НЕ объединять в main, не выпускать JAR и не объявлять готовность игрового сценария. Следующий точный шаг после полного зелёного CI на текущем коммите — полная резервная копия, затем v3→v4 на клоне мира с двумя реальными клиентами и протоколом сравнения. После приёмки — отдельный v4-safe Magic M3 PR, затем отдельный опциональный 17-bone MNAE spike с настоящим F5.
