ВНИМАНИЕ: ЭКСПЕРИМЕНТАЛЬНАЯ QA-СБОРКА. Showcase Character M5: переговоры человека, руны дворфа, защита воина V, оберег мага V, урок Ильры + QA5 Encounter/Enemy/Performance.
НЕ содержит PR58/60/63/65. НЕ ПУБЛИЧНЫЙ РЕЛИЗ.
Закройте Minecraft/сервер и создайте резервную копию ВСЕГО мира, включая playerdata и data.
CharacterData переходит с v3 на v4. Нельзя открывать обновлённый мир старым JAR.
Откат возможен только восстановлением СТАРОГО JAR И ПОЛНОЙ исходной копии мира.

MKM RPG — автоматическая установка для Windows
================================================
Minecraft Java Edition: 26.3
Fabric Loader: 0.20.1
MKM: 0.0.7-fabric.1 (Fabric API встроен в JAR)

1. Установите официальный Minecraft Launcher и один раз запустите Minecraft 26.3.
2. Полностью закройте Minecraft и лаунчер.
3. Распакуйте ZIP-архив целиком в обычную папку. Не запускайте файлы прямо из архива.
4. Дважды щёлкните Install_MKM.cmd.
5. Установщик проверит JAR MKM, скачает официальный Fabric Installer 1.1.2
   с maven.fabricmc.net, проверит его SHA-256, установит Fabric Loader и
   скопирует MKM в папку mods.
6. Откройте Minecraft Launcher и выберите профиль fabric-loader-0.20.1-26.3.

Оптимизационные моды Sodium/Lithium/C2ME/Moonrise отдельно не включены;\nсм. docs/MKM_HYBRID_PERFORMANCE_ENGINE_M0.md.\nFabric API отдельно скачивать не нужно: он уже включён в MKM.
Сам Minecraft, официальная учётная запись и Minecraft Launcher НЕ входят в пакет.
Для первой установки нужен интернет. Установщик не требует прав администратора.
Установщик использует официальный Windows Fabric Installer EXE, для которого
обычно не требуется отдельная системная установка Java.

Старая сборка MKM при необходимости перемещается в mods\.mkm-backup\<дата>.
Другие моды и миры установщик не удаляет.
Перед переносом мира с NeoForge создайте резервную копию — сохранения MKM
между NeoForge и Fabric ещё не мигрируются автоматически.

Если игра установлена в нестандартной папке:
  powershell -NoProfile -ExecutionPolicy Bypass -File .\Install_MKM.ps1 -GameDir "D:\Games\.minecraft"

Этот установщик предназначен для ОФИЦИАЛЬНОГО Minecraft Launcher на Windows.
Для Prism/Modrinth/CurseForge используйте встроенную установку Fabric в лаунчере;
затем просто добавьте приложенный JAR MKM в папку mods вашего экземпляра.

Источники:
https://fabricmc.net/use/
https://wiki.fabricmc.net/install
https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.1.2/
https://github.com/eretenkodaniil16-rgb/mkm-rpg

Ограничения: это установщик, не самозагружаемый JAR-мод. Fabric Loader должен
быть установлен раньше запуска MKM. Runtime-тест на Windows в игре ещё не выполнен.
