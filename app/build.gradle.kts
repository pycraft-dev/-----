/**
 * Без java.util.Properties: поддержка `=`, `:` (как в .properties), полноширинного `＝`,
 * пробелов/NBSP; комментарии `#` / `!`.
 */
fun parseKeyValueLines(text: String): Map<String, String> {
    val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
    fun trimW(s: String): String =
        s.trim { c ->
            c.isWhitespace() ||
                c == '\uFEFF' ||
                c == '\u00A0' ||
                c == '\u2007' ||
                c == '\u202F' ||
                c == '\u200B' ||
                c == '\u200C' ||
                c == '\u200D'
        }
    val map = linkedMapOf<String, String>()
    val delims = charArrayOf('=', ':', '\uFF1D') // ASCII =, :, fullwidth equals
    for (line in normalized.lineSequence()) {
        val t = trimW(line)
        if (t.isEmpty() || t.startsWith("#") || t.startsWith("!")) continue
        var best = -1
        for (d in delims) {
            val i = t.indexOf(d)
            if (i > 0 && (best == -1 || i < best)) best = i
        }
        if (best <= 0) continue
        val key = trimW(t.substring(0, best))
        val value = trimW(t.substring(best + 1))
        if (key.isNotEmpty()) map[key] = value
    }
    return map
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.enterprise.manufacturing"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.enterprise.manufacturing"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "1.0.10"
    }

    fun envSigningVar(name: String): String? =
        System.getenv(name)?.trim()?.takeIf { it.isNotEmpty() }

    val envStorePath = envSigningVar("ANDROID_KEYSTORE_PATH")
    val useEnvSigning =
        envStorePath != null && rootProject.file(envStorePath).isFile
    val keystorePropsFile = rootProject.file("keystore.properties")
    val useFileSigning = !useEnvSigning && keystorePropsFile.isFile
    val releaseSigningConfigured = useEnvSigning || useFileSigning

    signingConfigs {
        if (releaseSigningConfigured) {
            create("releaseSigning") {
                if (useEnvSigning) {
                    val f = rootProject.file(envStorePath!!)
                    storeFile = f
                    storePassword =
                        envSigningVar("ANDROID_KEYSTORE_PASSWORD")
                            ?: error("Задайте ANDROID_KEYSTORE_PASSWORD для CI")
                    keyAlias =
                        envSigningVar("ANDROID_KEY_ALIAS")
                            ?: error("Задайте ANDROID_KEY_ALIAS для CI")
                    keyPassword =
                        envSigningVar("ANDROID_KEY_PASSWORD")
                            ?: error("Задайте ANDROID_KEY_PASSWORD для CI")
                } else {
                    fun readPropsText(): String {
                        var primary =
                            keystorePropsFile.readText(Charsets.UTF_8).trimStart {
                                it == '\uFEFF' || it == '\u200B' || it == '\u200C' || it == '\u200D'
                            }
                        if (parseKeyValueLines(primary).isNotEmpty()) return primary
                        val bytes = keystorePropsFile.readBytes()
                        if (bytes.size >= 2) {
                            val b0 = bytes[0].toInt() and 0xFF
                            val b1 = bytes[1].toInt() and 0xFF
                            if (b0 == 0xFF && b1 == 0xFE) {
                                return keystorePropsFile.readText(Charsets.UTF_16LE).trimStart {
                                    it == '\uFEFF' || it == '\u200B'
                                }
                            }
                        }
                        return primary
                    }
                    val rawProps = readPropsText()
                    var kv = parseKeyValueLines(rawProps)
                    if (kv.isEmpty()) {
                        kv = parseKeyValueLines(String(keystorePropsFile.readBytes(), Charsets.ISO_8859_1))
                    }
                    if (kv.isEmpty()) {
                        val len = keystorePropsFile.length()
                        if (len == 0L) {
                            throw GradleException(
                                "keystore.properties на диске пустой (0 байт): ${keystorePropsFile.absolutePath}. " +
                                    "Gradle читает только сохранённый файл. Если в редакторе есть текст — нажмите **Ctrl+S** (или File → Save), " +
                                    "затем снова запустите сборку. Содержимое скопируйте из keystore.properties.example.",
                            )
                        }
                        val head =
                            keystorePropsFile.readBytes().take(64).joinToString(" ") {
                                (it.toInt() and 0xFF).toString(16).padStart(2, '0')
                            }
                        throw GradleException(
                            "keystore.properties: не найдено ни одной строки key=value. " +
                                "Файл: ${keystorePropsFile.absolutePath}, размер=$len байт. " +
                                "Начало файла (hex): $head. " +
                                "Нужны латинские ключи storeFile, storePassword, keyAlias, keyPassword и разделитель **=** (ASCII) или **:**. " +
                                "Сохраните как UTF-8. См. keystore.properties.example",
                        )
                    }
                    fun trimValue(s: String) =
                        s.trim { c ->
                            c.isWhitespace() ||
                                c == '\uFEFF' ||
                                c == '\u00A0' ||
                                c == '\u2007' ||
                                c == '\u202F'
                        }
                    fun req(key: String): String {
                        val direct = kv[key]?.let { trimValue(it) }.orEmpty()
                        val v =
                            direct.ifEmpty {
                                kv.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value?.let { trimValue(it) }
                                    .orEmpty()
                            }
                        if (v.isEmpty()) {
                            val keys = kv.keys.sorted().joinToString(", ")
                            throw GradleException(
                                "В keystore.properties нет значения для \"$key=\". " +
                                    "Файл: ${keystorePropsFile.absolutePath}. " +
                                    "Распознаны ключи: [$keys]. См. keystore.properties.example",
                            )
                        }
                        return v
                    }
                    val storeRel = req("storeFile")
                    val storeF = rootProject.file(storeRel)
                    if (!storeF.isFile) {
                        throw GradleException(
                            "Keystore не найден по пути из storeFile=\"$storeRel\" (от корня проекта). Ожидался файл: ${storeF.absolutePath}",
                        )
                    }
                    storeFile = storeF
                    storePassword = req("storePassword")
                    keyAlias = req("keyAlias")
                    keyPassword = req("keyPassword")
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("releaseSigning")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":sync"))
    implementation(project(":update"))
    implementation(project(":timesheet"))
    implementation(project(":drawings"))
    implementation(project(":defect"))
    implementation(project(":admin"))
    implementation(project(":auth"))
    implementation(project(":core"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    kapt(libs.androidx.hilt.compiler.work)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.tooling.preview)
}
