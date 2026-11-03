# MKM RPG — бесплатный Blender/AI-конвейер (draft)

Scope: инструменты подготовки ресурсов для Fabric 26.3, **не** изменение игрового рендера, боевой логики, сохранений или клипов MNAE. Реальное воспроизведение блокируется задачами [Issue #43](https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/43).

## Подобранные бесплатные компоненты

| Компонент | Использование | Условия |
| --- | --- | --- |
| [Blender](https://www.blender.org/) | Редактор моделей, ригов и анимаций | Свободное ПО; версию Blender сверить с имеющимися скриптами |
| [MCP for Blender](https://github.com/ahujasid/mcp-for-blender) | Управление Blender из Codex/Claude | Третий сторонний open-source MCP; исполняет Python в Blender |
| [Rigify](https://docs.blender.org/manual/en/latest/addons/rigging/rigify/introduction.html) | Управляющий риг, IK/FK | Бесплатное расширение Blender |
| [Retarget](https://extensions.blender.org/add-ons/retarget/) | Перенос захваченного движения на риг | Бесплатно, только Blender 5.0+ |
| [Adobe Mixamo](https://www.mixamo.com/) | Референсные и исходные движения | Бесплатная учётная запись Adobe; проверять лицензию |
| [Rokoko Studio Starter](https://www.rokoko.com/pricing) | Видеозахват движения | Бесплатный лимит Vision AI, FBX экспорт; собственные персонажи/ретаргетинг — платная функция |
| [cc-blender-skill](https://github.com/RobLe3/cc-blender-skill) | Готовые AI-skills для Blender | MIT, набор из 30 навыков; требуется локальная установка и проверка источника |

Ни один из внешних настольных продуктов не может быть автоматически установлен на пользовательский ноутбук через GitHub PR. Этот PR содержит установочную процедуру и локальные MKM-specific skills. Интеграция настоящего Blender MCP требует запуска на ПК.

## Безопасное подключение в Windows

Открой PowerShell в локальной копии репозитория:

    python tools/animation/setup/check_toolchain.py
    pwsh -File tools/animation/setup/Setup-FreeBlenderToolchain.ps1

По умолчанию эти команды только проверяют наличие инструментов. Для установки uv (если отсутствует):

    pwsh -File tools/animation/setup/Setup-FreeBlenderToolchain.ps1 -InstallUv

Для установки Blender MCP add-on и регистрации в Codex (без замены существующей записи):

    pwsh -File tools/animation/setup/Setup-FreeBlenderToolchain.ps1 -Apply

Альтернатива вручную после установки uv:

    uvx mcp-for-blender install-addon
    codex mcp add blender --env BLENDER_HOST=localhost --env BLENDER_PORT=9876 --env BLENDER_MCP_SAFE_MODE=1 --env DISABLE_TELEMETRY=true -- uvx mcp-for-blender

После этого в Blender: Edit > Preferences > Add-ons > Interface: MCP for Blender, активировать; N > MCP for Blender > Start MCP Server, если сервер ещё не запущен. Перезапустить Codex. Команда codex mcp list подтверждает регистрацию, но **не** полноценную работу сокета.

Важное ограничение безопасности: сервер Blender MCP без аутентификации, с возможностью выполнения Python. Оставить адрес только localhost, не публиковать TCP 9876 в сеть, делать резервные копии .blend. Safe mode — дополнительная проверка скриптов, а не полноценная изоляция.

Rigify активировать в расширениях Blender. Retarget установить через [официальный каталог](https://extensions.blender.org/add-ons/retarget/) в Blender 5.0+. Для Mixamo и Rokoko требуется отдельная регистрация на их официальных сайтах. Платные тарифы и загрузка закрытых видео не предусмотрены.

## Готовые skills для Codex / Claude

Рекомендуемые навыки из [RobLe3/cc-blender-skill](https://github.com/RobLe3/cc-blender-skill), после проверки их исходного кода:

    npx skills add https://github.com/RobLe3/cc-blender-skill --skill text-to-blender
    npx skills add https://github.com/RobLe3/cc-blender-skill --skill blender-modeling
    npx skills add https://github.com/RobLe3/cc-blender-skill --skill blender-animation
    npx skills add https://github.com/RobLe3/cc-blender-skill --skill reference-to-3d
    npx skills add https://github.com/RobLe3/cc-blender-skill --skill animation-quality-gate
    npx skills add https://github.com/RobLe3/cc-blender-skill --skill quality-refinement-autoloop
    npx skills add https://github.com/RobLe3/cc-blender-skill --skill blender-export

Требуются Node.js и установленный Skills CLI (npx загружает пакет при запуске); интерфейс может запросить выбор ассистента и область установки. Не выполнять неизвестные скрипты без проверки. Готовые навыки не гарантируют корректность Minecraft-экспорта.

Три новые проектные инструкции для Codex размещены в .agents/skills: mkm-reference-modeler, mkm-combat-animator и mkm-animation-qa. Для Claude Code их можно после проверки скопировать в .claude/skills на локальной машине.

## Пилот: один удар мечом

1. Сохранить исходный референс и .blend с отдельными мешами оружия, скелетом, локтями, коленями, костями крепления клинка и опорными маркерами стоп.
2. Сделать только один цикл: подготовка, перенос веса, ускорение, контакт, продолжение и возврат в стойку.
3. Отрендерить не менее девяти кадров и отдельно проверить путь кончика меча, расстояние рукоять–кисть, пересечение головы и скольжение стоп.
4. Использовать существующий tools/animation/export_blender_to_mkm.py только для ограниченного шестикостного формата. Изгибы локтей/коленей нельзя выдавать за поддерживаемые при экспорте в шесть ванильных частей.
5. После устранения Issue #43 проверить фактические кадры Minecraft F5, две настройки F8 в идентичном состоянии, сервер и SHA JAR; результат Blender не засчитывается как игровой PASS.

## Не допускать

Никаких замен боевого движка, новых damage events, root-motion gameplay, изменений CharacterData, покупки подписок, переноса секретов в GitHub или слияния draft без игрового визуального теста.
