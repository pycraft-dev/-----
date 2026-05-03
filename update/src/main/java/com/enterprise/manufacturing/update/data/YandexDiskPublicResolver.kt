package com.enterprise.manufacturing.update.data

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URLEncoder

/**
 * Публичные страницы Диска (`disk.yandex.ru/i/…`, `…/d/…`, `yadi.sk/…`) отдают HTML;
 * прямой файл получают через [Yandex Disk API](https://yandex.ru/dev/disk/api/concepts/public.html).
 */
internal object YandexDiskPublicResolver {

    private val hosts = setOf("disk.yandex.ru", "disk.yandex.com", "yadi.sk")

    /**
     * `true`, если нужен запрос к `cloud-api.yandex.net`, иначе обычный GET по [originalUrl].
     */
    fun shouldResolve(originalUrl: String): Boolean {
        val url = originalUrl.toHttpUrlOrNull() ?: return false
        if (url.host !in hosts) return false
        if (url.encodedPath.startsWith("/client/")) return false
        return url.encodedPath.contains("/i/") || url.encodedPath.contains("/d/")
    }

    /**
     * URL метода `GET /v1/disk/public/resources/download` — в теле ответа поле `href` на временную прямую загрузку.
     *
     * Для ссылки на **папку** (`/d/…`) в корне шаринга ожидается файл **`update.json`**
     * (как в [документации Яндекса](https://yandex.ru/dev/disk/api/reference/public.html) — параметр `path`).
     */
    fun buildPublicDownloadMetaRequestUrl(publicPageUrl: String): String {
        val url = publicPageUrl.toHttpUrlOrNull() ?: error("yandex_bad_public_url")
        val enc = URLEncoder.encode(publicPageUrl, Charsets.UTF_8.name())
        val sb =
            StringBuilder("https://cloud-api.yandex.net/v1/disk/public/resources/download?public_key=")
                .append(enc)
        if (url.encodedPath.contains("/d/")) {
            sb.append("&path=").append(URLEncoder.encode("/update.json", Charsets.UTF_8.name()))
        }
        return sb.toString()
    }
}
