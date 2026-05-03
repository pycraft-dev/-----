## Supabase: онлайн-чат для Manufacturing Enterprise

### 1. Создать проект

1. [Supabase Dashboard](https://supabase.com/dashboard) → **New project**.
2. Сохраните **Project URL** и **anon public** ключ (**Settings → API**).

### 2. Применить схему

**Вариант A — SQL Editor в Dashboard**

Выполните миграции **по порядку имени файла** (или объедините в один скрипт):

1. [`migrations/20260106120000_direct_messages.sql`](migrations/20260106120000_direct_messages.sql) — личный чат `direct_messages`.
2. [`migrations/20260503130000_enterprise_users.sql`](migrations/20260503130000_enterprise_users.sql) — каталог пользователей `enterprise_users` (синхронизация логинов между устройствами).

Скопируйте содержимое каждого файла в **SQL Editor** и выполните.

**Вариант B — CLI**

```bash
supabase link --project-ref <your-project-ref>
supabase db push
```

(Нужна установленная [Supabase CLI](https://supabase.com/docs/guides/cli).)

### 3. Подключить Android

В **`local.properties`** в корне **Android**-дерева Gradle (рядом с `settings.gradle.kts`) добавьте:

```properties
SUPABASE_URL=https://ВАШ_ПРОЕКТ.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJI...
```

Пересоберите проект — значения попадут в `BuildConfig` модуля `:core`.

На всех устройствах **одинаковые** `SUPABASE_URL` и `SUPABASE_ANON_KEY`. После применения миграции `enterprise_users` можно подтянуть каталог с экрана **Синхронизация** («Загрузить пользователей с сервера»); при создании пользователя в админке запись также уходит в облако (`upsert` по `login`).

**Логины** в локальной БД и в таблице `enterprise_users` должны совпадать (поле **login** при создании пользователя).

### Ошибка при `ALTER PUBLICATION supabase_realtime`

На части проектов таблица уже добавлена в realtime. Если SQL падает на этой строке — удалите её и выполните остальной скрипт; для тестового чата достаточно `SELECT`/`INSERT`.

### ⚠️ Безопасность

Политики в миграции **намеренно открыты** для быстрых тестов. Для боя: включите **Supabase Auth**, храните `sender_login` только из JWT, сузьте **RLS**.

### Ограничения текущей клиентской реализации

- В облако уходит только **TEXT** сообщения личного чата (голос/файлы — пока локально).
- Обновление списка: **polling** каждые ~5 с; можно заменить на Realtime без смены таблицы.
