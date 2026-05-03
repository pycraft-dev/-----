# Manufacturing Enterprise

Android‑приложение для производственного предприятия: учёт дефектов, чертежи (PDF/DWG), табель времени, общий и личный чат, админ‑панель пользователей и фоновая «синхронизация» (заглушки под будущий сервер). Интерфейс на **Jetpack Compose**, DI — **Hilt**, данные — **Room** и локальная сессия.

<details>
<summary><strong>English</strong></summary>

Android app for manufacturing workflows: defect tracking, drawings (PDF/DWG), timesheet, team and direct chat, user admin, and background sync stubs for a future backend. **Jetpack Compose**, **Hilt**, **Room**, local session.

</details>

---

## Скриншоты / демо

| Экран | Путь или ссылка |
|-------|------------------|
| Плейсхолдер входа | `docs/screenshots/login.png` *(добавьте после съёмки)* |
| Чат | `docs/screenshots/chat.png` |
| Чертежи | `docs/screenshots/drawings.png` |

При необходимости создайте папку `docs/screenshots/` и вставьте сюда маркированные PNG или ссылки на ролик.

---

## Установка и запуск (исходники)

**Требования:** Android Studio Hedgehog или новее, JDK 17, Android SDK 34.

1. Клонируйте репозиторий.
2. Откройте корень Gradle‑проекта (`ManufacturingEnterprise`) в Android Studio.
3. Дождитесь синхронизации зависимостей.
4. Запуск на эмуляторе или устройстве: **Run → Run ’app’** (конфигурация модуля `:app`).
5. Сборка APK вручную (из корня проекта, если используете Gradle Wrapper):
   - отладочная: `./gradlew :app:assembleDebug`
   - релизная: `./gradlew :app:assembleRelease` *(настройте подпись в `app/build.gradle.kts`)*  

Итоговый APK обычно лежит в `app/build/outputs/apk/`.

<details>
<summary><strong>English — Install & build</strong></summary>

**Requirements:** Recent Android Studio, JDK 17, SDK 34.

1. Clone the repo.  
2. Open the project root in Android Studio.  
3. Sync Gradle.  
4. Run the `:app` configuration on an emulator or device.  
5. Optional CLI: `./gradlew :app:assembleDebug` or `assembleRelease` (configure signing for release).

</details>

### Первый вход (bootstrap)

При пустой базе создаётся локальная учётная запись для разработки:

- Логин: `admin_1`  
- Пароль: `admin123`

Сразу после первого запуска **смените пароль через админ‑модуль** или скорректируйте константы в исходниках перед промышленным использованием. Подробности см. модуль `:auth`.

---

## Документация

| Тема | Где искать |
|------|------------|
| Инструкция для пользователя без кода | [README_КЛИЕНТУ.md](README_КЛИЕНТУ.md) |
| Онлайн личный чат (PostgreSQL через Supabase) | [supabase/README.md](supabase/README.md) и SQL [`supabase/migrations/`](supabase/migrations/) |
| Модули и границы ответственности | раздел «Архитектура» ниже |
| Безопасность | локальная аутентификация по хешу пароля; текст личного чата может отправляться в Supabase если заданы `SUPABASE_*` в `local.properties`. Политики RLS в образце миграции открытые — только для тестов |
| Фоновые задачи | `EnterpriseSyncScheduler`, WorkManager |

Для производственной эксплуатации потребуются: политика паролей, резервное копирование БД, при необходимости — свой сервер синхронизации вместо заглушек.

<details>
<summary><strong>English — Documentation map</strong></summary>

End‑user overview: [README_КЛИЕНТУ.md](README_КЛИЕНТУ.md). Optional Supabase direct chat: configure `local.properties` and run SQL migrations; sample RLS is permissive — tighten before production.

</details>

---

## Архитектура проекта

```
ManufacturingEnterprise (root)
├── app/          — точка входа, Navigation, сборка приложения
├── core/         — БД Room, навигация, чат, Supabase (ключи через BuildConfig)
├── auth/         — локальная сессия DataStore + вход по пользователям Room
├── admin/        — админ‑панель пользователей
├── defect/       — брак / дефекты
├── drawings/     — чертежи, PDF-превью, обсуждение по версии
├── timesheet/    — учёт времени (ручной ввод длительности)
├── sync/         — WorkManager и заглушки синхронизации
├── update/       — канал проверки обновлений (Retrofit / заглушка)
└── supabase/     — SQL для облака (PostgreSQL), инструкция по проекту
```

**Стек:** Kotlin, Compose, Navigation-Compose, Hilt, Room, DataStore, Coroutines/Flow, Supabase PostgREST, WorkManager.

---

## Контакты разработчика

📬 **Контакты разработчика**

**Вова** \| pycraft-dev  
Python‑разработчик • Современные GUI‑приложения • Автоматизация

- 📧 [pycraft-dev@21051992.ru](mailto:pycraft-dev@21051992.ru)  
- 💬 Telegram: [@Pycraftdev](https://t.me/Pycraftdev)  
- 💼 [Kwork](https://www.kwork.ru)  
- 🐙 [GitHub](https://github.com)

💡 Нужно похожее приложение под ваши задачи? Напишите — обсудим!

---

## Лицензия

Проект распространяется по лицензии MIT — см. файл [LICENSE](LICENSE).
