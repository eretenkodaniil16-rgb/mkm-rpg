# MKM RPG — Issue #73, R07 17-kost MNAE on QA5 CharacterData v4

**Статус:** технический вертикальный прототип, draft. `#73`/SHOWCASE-01 `#71`, PR `#70`; DO NOT MERGE TO HISTORICAL NEOFORGE MAIN. Исходная база: QA5 PR #69, SHA `440dfe07f67d4cce1a8fc10f2bd62a5a2574e137`, CharacterData v4. Предыдущая анимационная работа: старый v2-стек PR #46 / #63. **Не переносить его CharacterData, сетевые регистрации или MkmClient wholesale.**

## G0 / архитектурное решение

Сравнили: (1) механическое объединение PR63 (небезопасно: v2→v4 downgrade и несовместимые клиенты); (2) **выделенный cosmetic-only порт артефактов и источника R07 на HEAD QA5** (выбрано); (3) собственный полный PlayerRenderer с armor/first-person/item layers (избыточный риск до теста одного удара).

Из #63 перенесены только `Vec3`, `Quat`, `ArticulatedClipV3`, `ArticulatedAvatarLayer`, Blender-exporter и чистые тесты; новый `ArticulatedMode` использует v4-совместимые существующие `LocalCombatAnimation`, `ActionTimeline`, `EquipmentRules`. `MkmClient` изменён адресными добавлениями F8/F9 и слоя, **сохраняя все v4 обработки Allocation, Origin, Renewal, Ford journal и Combat**.

## Неприкосновенный оригинальный R06

- Аннотированный тег `mkm-guards-r06-approved` -> peeled commit `5cae0c0dae1daf1bc7cfe07945af1fb67ea741b8`.
- Binary `MKM_Three_Sword_Guards_R06.blend` SHA256 `024d2f85e0bef85ec3bb493d0011ceaf7c147c41131048635db76d7305c05581`.
- R07 — только производная Mid Guard frame20, исходные девять static PNG и три стойки неизменны.
- `.github/workflows/mnae-r07-v4-vertical.yml` извлекает **тот же бинарный .blend из тега** (проверяет SHA), запускает Blender и коммитит версионированный JSON только в отдельную feature-ветку. `R07` можно собрать и без Blender после фиксации ресурса в репозитории.
- Авторский файл R07 и девять диагностических PNG доступны в Blender CI artifact, source script `tools/animation/r07/export_r07_blender.py`. Правка старого `.blend` запрещена.

## Преобразования / причины потери движения

| Путь | Координаты / метод | Прежняя ошибка | R07 v4 |
|---|---|---|---|
| Blender authoring | +X вправо, −Y вперёд, +Z вверх; 17 bones, armature, wrist IK | R02 использовал independent sword quaternion | Финальная R06 кисть и bound sword |
| Export v3 | world-pivot `(x*s,24-z*s,y*s)`, `s=24/1.8`; `R_game=B R_blender B^-1`, xyzw quaternion | C3 вращал только 6 частей, не экспонировал локти и колени | 43 IK-evaluated world samples, 17 joint locals/rest hierarchy |
| Runtime clip | Sampling `t=(phase elapsed)/14`, `impact=5/14 = frame16` | Старый профайлер неверно относил любую вещь в sword | `MnaEWeaponVisualPolicy` через `EquipmentRules`, sword-only v3 |
| Avatar layer | По 17 `ModelPart` с собственными мировыми `pivot+q`; rigid cubes; atlas 64×64 | VanillaAvatarAdapter терял иерархию, сводя к six-part | Действительные отдельные локти, колени, кисти, стопы |
| ItemInHandLayer | `translateToHand` заменён только для MAIN_HAND sword в MNAE17; рукоять из bone-local socket | Старый ItemInHandLayer вращал оружие относительно vanilla-arm, меч за головой | Сокет теперь отдельный, 43-frame золотые проверки, **финальная игровая калибровка по видео ещё отсутствует** |
| First person / armor / shield | Ванильные слои | Нет безопасного общего skinning | Намеренный контролируемый fallback до F5 проверок |
| Left-handed Steve/Alex | Перестановка R↔L, отражение X/quaternion | Корректность скина/UV неизвестна | Тестируется математически, визуальный gate ещё открыт |

**Важное техническое уточнение:** R07 использует world-space кости, уже вычисленные Blender после IK. Старый `Skeleton.world()` не включён в клиентский hot path. Такой выбор исключает второй ошибочный проход parent TRS, но пока ограничивает формат одним источником 17 костей. Quat→Euler в ModelPart проверяется тестами на всех 731 joint frames.

## Визуальное управление

- **F8 выключена, F9 выключена:** C3.7 baseline (в QA5 C3.8 не интегрирована).
- **F8 включена, F9 выключена:** MNAE **R07 six-part projection**, производная того же R07; это НЕ старый MNAE M1 и так и подписано в HUD.
- **F8 включена, F9 включена:** новый 17-костный AvatarLayer, authored Mid Guard в покое и все 43 кадра при первом R-комбо. При отсутствии клипа/слоя переключение не должно скрывать ванильную модель.
- Недопустимые состояния: топор/кирка/лук, оружие только в offhand, щит/предмет в offhand, броня, первое лицо, отсутствие земли, другие шаги комбо → явный conservative fallback без C3 мечевой импровизации.
- При F8 включена, G-уклонение использует **три типа**: forward tuck, lateral step, backward hop. Нет обязательного полного вращения всего игрока на 360°. Остальные диагонали принадлежат соответствующей группе, серверная дистанция/коллизии не меняются.

## Проверки и неустранённые риски

- `ArticulatedClipV3Test`: 17 bones, 43 real frames, полный Blender→Java socket XYZ golden всех кадров, 14 серверных тиков, impact tick5, parser 1MiB bound.
- `R07CoordinateParityTest`: quaternion→ModelPart Euler, зеркальная кисть/клинок, раздельные локти/колени.
- `MnaEWeaponVisualPolicyTest`: sword vs axe/pickaxe/bow/shield/offhand.
- `EvasionChoreographyTest`: три различные направления без 2π, ограниченные непрерывные кривые.
- CI: Java25, Gradle/JUnit, asset SHA, dedicated server boot, client bootstrap under Xvfb. Версия `0.0.6-fabric.1-r07-v4`; JAR с Fabric API, frozen v4 schema.
- `tools/animation/r07/ci_f5_video.py` и отдельный GitHub Actions video capture эксперимент: создаёт disposable world, пытается Quick Play и X11 60fps F5. **Если реальный мир/предмет/действие не подтверждены, артефакт НЕ проходит визуальную приёмку.**
- **ОТКРЫТЫ:** (a) фактический grip/head/armor collision на пикселях ItemInHandLayer, (b) реальный 60fps пользовательский F5 A/B/C, Steve/Alex skin UV и left-handed, (c) coop наблюдатель — owner TIMELINE не транслируется удалённым игрокам, (d) безопасная локальная защита от всех конфликтов RenderLayer порядка, (e) переходы во время прыжка/сдвига и контакт стоп с разным рельефом, (f) finish/pose approval для трёх уклонений.

## Ручной QA в отдельном мире

Поставить **только новый** JAR версии `0.0.6-fabric.1-r07-v4`, убедиться, что мир новый/резервный; Minecraft 26.3 Fabric Java25, нажать F5. Снять броню, освободить OFF_HAND, взять minecraft:iron_sword основной рукой, на земле сравнить F8/F9 + R на одной камере (F8 off; F8 on/F9 off; F8 on/F9 on) и записать 60fps. Проверить idle Mid Guard, frame contact, конец восстановления, движение, левую основную руку. Затем отдельно щит/offhand, топор, кирку, броню, первое лицо, relog и смену измерения; любое исчезновение персонажа/меча — **P0 STOP**. Для dual-player оценить только fallback — observer MNAE **PENDING**.

## Запреты

Нет изменений CharacterData v4, CombatService/DamageSource/ActionTimelineRules, hitbox/physical movement/stamina/XP/quest/magic/world. Нет PAL/GeckoLib. Не переносить ветку PR63 над QA5; только cherry-pick отдельных cosmetic изменений. Не объединять в historical NeoForge `main`. Важнее реальный видео-гейт, чем успешный Blender CI.
