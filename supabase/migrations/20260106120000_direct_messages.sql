-- Личный чат (онлайн). Логины совпадают с users.login на каждом устройстве.
-- ⚠ DEV: политики RLS ниже допускают любой доступ с анонимным ключом.
-- Перед продакшеном замените на Supabase Auth + строгие RLS или вызов только через Edge Function.

CREATE TABLE IF NOT EXISTS public.direct_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_key TEXT NOT NULL,
    sender_login TEXT NOT NULL,
    recipient_login TEXT NOT NULL,
    body TEXT NOT NULL,
    message_type TEXT NOT NULL DEFAULT 'TEXT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_direct_messages_conv_created
    ON public.direct_messages (conversation_key, created_at ASC);

ALTER TABLE public.direct_messages ENABLE ROW LEVEL SECURITY;

-- ⚠ Открытые правила только для черновых тестов между устройствами.
CREATE POLICY "direct_messages_allow_read"
    ON public.direct_messages FOR SELECT
    USING (true);

CREATE POLICY "direct_messages_allow_insert"
    ON public.direct_messages FOR INSERT
    WITH CHECK (true);

GRANT SELECT, INSERT ON public.direct_messages TO anon, authenticated;

-- Realtime для будущих подписок. Если уже добавлено — строка упадёт, можно удалить её из скрипта.
ALTER PUBLICATION supabase_realtime ADD TABLE public.direct_messages;
