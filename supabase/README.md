## Supabase: онлайн-чат для Manufacturing Enterprise

### 1. Создать проект

1. [Supabase Dashboard](https://supabase.com/dashboard) → **New project**.
2. Сохраните **Project URL** и **anon public** ключ (**Settings → API**).

### 2. Применить схему

**Вариант A — SQL Editor в Dashboard**

Скопируйте содержимое файла [`migrations/20260106120000_direct_messages.sql`](migrations/20260106120000_direct_messages.sql) и выполните в разделе **SQL Editor**.

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

На обоих устройствах **логины пользователей** в локальной БД должны совпадать (то же поле **login**, что при создании пользователя в приложении).

### Ошибка при `ALTER PUBLICATION supabase_realtime`

На части проектов таблица уже добавлена в realtime. Если SQL падает на этой строке — удалите её и выполните остальной скрипт; для тестового чата достаточно `SELECT`/`INSERT`.

### ⚠️ Безопасность

Политики в миграции **намеренно открыты** для быстрых тестов. Для боя: включите **Supabase Auth**, храните `sender_login` только из JWT, сузьте **RLS**.

### Ограничения текущей клиентской реализации

- В облако уходит только **TEXT** сообщения личного чата (голос/файлы — пока локально).
- Обновление списка: **polling** каждые ~5 с; можно заменить на Realtime без смены таблицы.
