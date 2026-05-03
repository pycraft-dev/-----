# Корпоративные обновления + GitHub: пошагово «для чайников»

Здесь два направления в одной цепочке:

1. **Код и автосборка** — приватный репозиторий **GitHub**, при нажатии кнопки (или теге `v*`) собирается подписанный **AAB**.
2. **«Обновить всем»** — готовый **AAB** загружается в **Google Play** (корпоративный / закрытый трек), а **MDM** (например, **Microsoft Intune** + **Managed Google Play**) раздаёт и принудительно обновляет приложение на телефонах сотрудников.

В проекте уже настроено:

- подпись **release** из секретов CI **или** из файла `keystore.properties` локально;
- workflow **`.github/workflows/release-bundle.yml`** — сборка **AAB** и выкладка артефакта в GitHub Actions.

---

## Часть A. Один раз: ключ подписи (keystore)

Подпись нужна, чтобы Android и Google Play считали приложение **одним и тем же** при обновлениях.

### Шаг A1. Установите JDK (если ещё нет)

На ПК разработчика нужен **JDK 17** (как в проекте). Проверка в терминале:

```bash
java -version
```

### Шаг A2. Создайте файл ключа

В **корне проекта** (рядом с `settings.gradle.kts`) выполните:

```bash
keytool -genkeypair -v -storetype PKCS12 -keystore upload-keystore.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

Вас спросят пароли и данные организации — **запомните пароль** и **alias** (мы использовали `upload`).

Файл **`upload-keystore.jks`** появится в корне. Он **секретный** — в GitHub **не коммитить** (уже в `.gitignore` для типичных имён; при другом имени добавьте строку в `.gitignore`).

### Шаг A3. Локальная сборка AAB на своём ПК (проверка)

1. Скопируйте `keystore.properties.example` → **`keystore.properties`** в корне.
2. Заполните пути и пароли (как в примере, `storeFile=upload-keystore.jks` если файл в корне).
3. **Обязательно сохраните файл на диск** (**Ctrl+S** / File → Save). Gradle читает только сохранённый файл; если на диске **0 байт**, в редакторе вы просто не сохранили изменения.
4. В `app/build.gradle.kts` перед релизом увеличьте **`versionCode`** (целое число, каждый релиз **+1**), при необходимости **`versionName`**.
5. В корне проекта:

```bash
./gradlew bundleRelease
```

На Windows: `gradlew.bat bundleRelease`.

Готовый файл: **`app/build/outputs/bundle/release/app-release.aab`**.

Если Gradle ругается на подпись — проверьте путь `storeFile`, пароли и что **`keystore.properties` сохранён** (не только открыт в редакторе).

---

## Часть B. GitHub: репозиторий и секреты

### Шаг B1. Создайте **приватный** репозиторий

На [github.com](https://github.com) → **New repository** → выберите **Private** → создайте пустой репозиторий.

### Шаг B2. Залейте код

На своём ПК (один из вариантов):

```bash
cd /путь/к/проекту
git init
git remote add origin https://github.com/ВАШ_ЛОГИН/ВАШ_РЕПО.git
git add .
git commit -m "Initial import"
git branch -M main
git push -u origin main
```

Не коммитьте **`keystore.properties`** и **`.jks`** — они в `.gitignore`.

### Шаг B3. Секреты для GitHub Actions

Откройте репозиторий → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**.

Создайте **четыре** секрета:

| Имя секрета | Что положить |
|-------------|----------------|
| `RELEASE_KEYSTORE_BASE64` | Содержимое keystore в **Base64** одной строкой (см. ниже). |
| `ANDROID_KEYSTORE_PASSWORD` | Пароль хранилища (тот же, что при `keytool`). |
| `ANDROID_KEY_ALIAS` | Alias ключа, например **`upload`**. |
| `ANDROID_KEY_PASSWORD` | Пароль ключа (часто совпадает с паролем хранилища). |

**Как получить Base64 keystore:**

- **Windows (PowerShell):**

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("upload-keystore.jks")) | Set-Clipboard
```

Вставьте из буфера в значение секрета `RELEASE_KEYSTORE_BASE64`.

- **macOS / Linux:**

```bash
base64 -w0 upload-keystore.jks   # Linux
base64 upload-keystore.jks       # macOS (без -w0)
```

Скопируйте вывод в секрет.

---

## Часть C. Запуск сборки на GitHub

### Шаг C1. Ручной запуск

1. Репозиторий → вкладка **Actions**.
2. Слева выберите workflow **«Release AAB»**.
3. **Run workflow** → ветка **main** → **Run workflow**.

### Шаг C2. Запуск по тегу (удобно для версий)

Локально:

```bash
git tag v1.0.1
git push origin v1.0.1
```

Workflow сработает на push тега вида **`v*`** (например `v1.0.1`).

### Шаг C3. Скачать AAB

Когда workflow **зелёный** → откройте запуск → внизу раздел **Artifacts** → скачайте **`manufacturing-enterprise-aab`**. Внутри архива — **`.aab`** для загрузки в Play Console.

Если сборка **красная** — откройте лог шага **«Сборка bundleRelease»**; частые причины: не заданы секреты, неверный пароль/alias, не увеличен `versionCode` относительно уже загруженного в Play.

---

## Часть D. Google Play и «обновить всем» (MDM)

Кратко, без привязки к одному вендору:

1. Зарегистрируйте **Google Play Developer** аккаунт (разовый взнос по правилам Google).
2. Создайте приложение с тем же **`applicationId`**, что в проекте: **`com.enterprise.manufacturing`**.
3. Включите **Play App Signing** (рекомендуется).
4. Загрузите первый **AAB** в **Internal testing** или **Closed testing**.
5. Подключите организацию к **Google Workspace** и **Managed Google Play** (если ещё не сделано).
6. В **MDM** (например, **Intune**): добавьте приложение из **Managed Play**, назначьте группам устройств, включите политику **обязательного приложения** / **принудительного обновления** (формулировки зависят от консоли MDM).

Дальше цикл такой: **увеличили `versionCode` в проекте** → **GitHub Actions собрал AAB** → **загрузили новый AAB в тот же трек Play** → **MDM разослал обновление**.

Официальные справки (на английском, но с картинками):

- [Загрузка приложения в Play Console](https://support.google.com/googleplay/android-developer/answer/9859152)
- [Рабочий профиль и управляемые приложения (Google)](https://support.google.com/work/android/answer/6190239)

---

## Чеклист перед первым «боевым» релизом

- [ ] `versionCode` в `app/build.gradle.kts` больше, чем у уже опубликованной версии в Play.
- [ ] Секреты в GitHub заданы, workflow **Release AAB** проходит зелёным.
- [ ] AAB проверен на тестовом треке Play перед массовым назначением в MDM.
- [ ] Сотрудникам выданы **рабочие** логины/пароли (экран администратора в приложении или отдельная политика компании), тестовый `admin_1` / `admin123` только для пустой базы.

---

## Если что-то непонятно

- **Локально не собирается release** — сначала добейтесь успешного `bundleRelease` с `keystore.properties`, потом переносите те же пароли в секреты GitHub.
- **Play отклоняет AAB** — читайте текст ошибки в консоли (часто версия, подпись, целевой API).
- **MDM** настраивает ИТ-отдел компании; разработчик обычно передаёт только **AAB** и номер версии.
