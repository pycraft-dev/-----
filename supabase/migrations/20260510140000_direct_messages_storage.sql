-- Вложения личного чата: путь в Storage + метаданные (голос / файл).
-- Публичный bucket для dev: любой с anon-ключом может читать/писать объекты в этом bucket.
-- Перед продом: закрыть bucket, подписанные URL через Edge Function или Auth.

ALTER TABLE public.direct_messages
    ADD COLUMN IF NOT EXISTS attachment_storage_path TEXT,
    ADD COLUMN IF NOT EXISTS attachment_mime TEXT,
    ADD COLUMN IF NOT EXISTS attachment_display_name TEXT,
    ADD COLUMN IF NOT EXISTS voice_duration_ms BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS transcript TEXT NOT NULL DEFAULT '';

-- Bucket для файлов чата (50 MiB на объект; типы не ограничиваем).
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES ('chat-files', 'chat-files', true, 52428800, NULL)
ON CONFLICT (id) DO UPDATE
SET public       = EXCLUDED.public,
    file_size_limit = EXCLUDED.file_size_limit;

-- Политики storage.objects (если уже есть — переименуйте или удалите дубликаты вручную).
DROP POLICY IF EXISTS "chat_files_public_read" ON storage.objects;
CREATE POLICY "chat_files_public_read"
    ON storage.objects FOR SELECT
    USING (bucket_id = 'chat-files');

DROP POLICY IF EXISTS "chat_files_anon_insert" ON storage.objects;
CREATE POLICY "chat_files_anon_insert"
    ON storage.objects FOR INSERT
    WITH CHECK (bucket_id = 'chat-files');

DROP POLICY IF EXISTS "chat_files_anon_update" ON storage.objects;
CREATE POLICY "chat_files_anon_update"
    ON storage.objects FOR UPDATE
    USING (bucket_id = 'chat-files')
    WITH CHECK (bucket_id = 'chat-files');

DROP POLICY IF EXISTS "chat_files_anon_delete" ON storage.objects;
CREATE POLICY "chat_files_anon_delete"
    ON storage.objects FOR DELETE
    USING (bucket_id = 'chat-files');
