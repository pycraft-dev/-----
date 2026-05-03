-- Синхронизация учётных записей между устройствами (логин = первичный ключ).
-- ⚠ DEV: RLS как у direct_messages — только для тестов; для боя — Auth + строгие политики.

CREATE TABLE IF NOT EXISTS public.enterprise_users (
    login TEXT PRIMARY KEY,
    password_hash TEXT NOT NULL,
    full_name TEXT NOT NULL,
    position TEXT NOT NULL DEFAULT '',
    group_key TEXT NOT NULL,
    role TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.enterprise_users ENABLE ROW LEVEL SECURITY;

CREATE POLICY "enterprise_users_allow_select"
    ON public.enterprise_users FOR SELECT
    USING (true);

CREATE POLICY "enterprise_users_allow_insert"
    ON public.enterprise_users FOR INSERT
    WITH CHECK (true);

CREATE POLICY "enterprise_users_allow_update"
    ON public.enterprise_users FOR UPDATE
    USING (true)
    WITH CHECK (true);

GRANT SELECT, INSERT, UPDATE ON public.enterprise_users TO anon, authenticated;
