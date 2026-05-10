## Supabase: онлайн-чат для Manufacturing Enterprise

Текст, голос и вложения личного чата уходят в **Postgres** (`direct_messages`) и **Storage** (bucket **`chat-files`**). Второе устройство подтягивает их при открытом диалоге (опрос ~5 с).

### 1. Создать проект

1. [Supabase Dashboard](https://supabase.com/dashboard) → **New project**.
2. Сохраните **Project URL** и **anon public** ключ (**Settings → API**).

### 2. Применить схему

**Вариант A — SQL Editor в Dashboard**

Выполните миграции **по порядку имени файла**:

1. [`migrations/20260106120000_direct_messages.sql`](migrations/20260106120000_direct_messages.sql) — таблица `direct_messages`.
2. [`migrations/20260503130000_enterprise_users.sql`](migrations/20260503130000_enterprise_users.sql) — каталог `enterprise_users`.
3. [`migrations/20260510140000_direct_messages_storage.sql`](migrations/20260510140000_direct_messages_storage.sql) — колонки вложений + bucket **`chat-files`** и политики Storage.

Если шаг 1 уже выполнялся раньше, его можно пропустить; шаг 3 нужен для **онлайн-файлов и голоса**.

Скопируйте содержимое каждого файла в **SQL Editor** и выполните (**Run**).

**Вариант B — CLI**

```bash
supabase link --project-ref <your-project-ref>
supabase db push
```

### 3. Настройка `local.properties` (корень Android-проекта)

Файл лежит рядом с `settings.gradle.kts` и **не коммитится** в Git (в `.gitignore`). После правок обязательно **пересоберите** приложение (**Build → Rebuild Project** или `./gradlew assembleDebug`).

Добавьте **ровно эти ключи** (без кавычек вокруг значений, без пробела вокруг `=`):

```properties
SUPABASE_URL=https://xxxxxxxx.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Проверьте:

| Что | Как должно быть |
|-----|------------------|
| `SUPABASE_URL` | Из **Settings → API → Project URL**, без слэша в конце (лишний слэш не критичен, но лучше без). |
| `SUPABASE_ANON_KEY` | Длинная строка **anon public** из того же экрана, целиком одна строка. |
| Сборка | После сохранения `local.properties` выполните clean/rebuild, иначе старый пустой `BuildConfig` останется в кэше. |
| Устройства | На **всех** APK для теста чата — **одинаковые** URL и ключ, один проект Supabase. |

Если ключи пустые, клиент Supabase не создаётся: сообщения остаются только локально (`PENDING`).

### 4. Пользователи и логины

После миграции `enterprise_users` на экране **Синхронизация** используйте «Загрузить пользователей с сервера»; при создании пользователя в админке запись уходит в облако (`upsert` по **login**).

**Логины** в локальной БД и в `enterprise_users` должны совпадать — от них строится ключ диалога `conversation_key`.

### Ошибка при `ALTER PUBLICATION supabase_realtime`

Если SQL падает на строке с **realtime**, удалите эту строку из скрипта первой миграции и выполните остальное; для чата через polling realtime не обязателен.

### ⚠️ Безопасность

Политики **RLS** и **Storage** в миграциях рассчитаны на **быстрые тесты** (anon может читать/писать `chat-files` и строки в `direct_messages`). Для продакшена: **Supabase Auth**, узкие RLS, приватный bucket и **signed URLs** вместо публичного bucket.

### Поведение клиента

- Обновление списка в открытом чате: **polling ~5 с** (можно позже заменить на Realtime).
- Транскрипт голоса после распознавания пока **только локально** в Room; синхронизация поля `transcript` на сервер не реализована.
