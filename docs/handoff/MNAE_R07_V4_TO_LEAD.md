# Состояние для главного: MKM Showcase-01 animation #73

На современной QA5 v4 базе выполнен изолированный cosmetic порт R07 (17 joints, 43 samples, one first-combo sword, sword attached to Mid Guard through origin-preserving 17-bone export). Прежний PR63 привязан к CharacterData v2; не использован целиком. Native client and Character v4 integrations сохранены; версия нового тестового JAR `0.0.6-fabric.1-r07-v4`.

F8 обеспечивает сравнение C3.7 и R07 six-part projection (не старой MNAE M1), F9 выбирает независимый R07 17-bone layer, idle Mid Guard без мгновенного появления после начала удара. Первоначальный ошибочный sword fallback для axe/pickaxe/offhand устранён. Для F8 нового 3-категорийного dodge есть forward tuck/lateral step/back hop вместо полного 360° вращения жёсткой модели. Все они косметические.

No damage, stamina, hitbox, XP, world state or save changes. Второй игрок видит vanilla/C3, pending MNAE observer sync. Фактический ItemInHandLayer/Steve-Alex/armor и пользовательская F5 запись **не приняты** до подтверждения. Есть экспериментальная CI-автоматизация реального Xvfb Quick Play мира и видеозахвата, её успех нужно проверить отдельно.

План: (1) реальные F5 и 43-frame pixel measurements; (2) held-item post-transform calibration + Alex/Steve UV + armor profile; (3) remote visual clip/phase S2C with UUID/sequence, dimension/relog expiration; (4) contact-aware dodges and stable movement; (5) только после этого расширять библиотеку ударов. DO NOT MERGE MAIN.
