# Скрипт публикации релиза на GitHub (`publish-release`)

Поднимает **`versionCode`** и **`versionName`** в **`app/build.gradle.kts`**, делает **commit**, **push** текущей ветки и **push тега** `v*` — в GitHub запускается workflow **`.github/workflows/release-bundle.yml`**, который собирает APK/AAB и выкладывает их в **Releases** (вместе с **`update.json`**, если шаг в workflow включён).

## Требования

- Установлены **git** и **GitHub**-доступ (`git push` настроен: SSH или credential).
- В репозитории настроены **Secrets** для Actions (см. **`docs/КОРПОРАТИВНЫЙ_РЕЛИЗ_CI.md`**).
- Запуск из корня проекта через **`scripts\publish-release.bat`** (скрипт сам переходит в корень).

## Запуск

Двойной щелчок **`publish-release.bat`** — **ничего вводить не нужно**, если в `app/build.gradle.kts` уже **`versionName`** вида **`M.m.p`** (например `1.0.5`):

Перед расчётом скрипт делает **`git fetch --tags origin`** и берёт **последний semver-тег** вида **`vM.m.p`** (как на GitHub Releases). База для следующего релиза:

- **`versionName` (следующий patch)** считается от **`max(то, что в gradle, версия из последнего тега)`** — если в файле ещё `1.0.1`, а на GitHub уже **`v1.0.5`**, следующий тег будет **`v1.0.6`**, а не `v1.0.2`.
- **`versionCode`** → **`max(gradle, versionCode из того же последнего тега в дереве app/build.gradle.kts)` + 1** (если в теге файла нет или не распарсился — как раньше, от локального gradle +1). Явное **`-VersionCode N`** по-прежнему задаёт код вручную.

Только локальный gradle и локальные теги (без сети): **`.\scripts\publish-release.ps1 -SkipGit -LocalOnly`** или **`-LocalOnly`** вместе с обычным запуском — **без** `git fetch`.

Если **`versionName`** не в формате **`M.m.p`**, скрипт спросит тег вручную (**`v1.0.6`** или **`1.0.6`**).

Можно явно задать тег (переопределяет авто-имя, **`versionCode` всё равно +1**, если не указан **`-VersionCode`**):

```powershell
.\scripts\publish-release.ps1 -Tag v2.0.0
```

Из PowerShell в корне репозитория:

```powershell
.\scripts\publish-release.ps1 -Tag v1.0.7 -VersionCode 9
```

Только правка `build.gradle.kts` без git:

```powershell
.\scripts\publish-release.ps1 -Tag v1.0.7 -SkipGit
```

## Если `git push` отклонён (non-fast-forward)

На GitHub есть коммиты, которых нет локально. Перед релизом один раз запустите с подтягиванием ветки (до правки `build.gradle.kts` скрипт сделает **`git pull --rebase origin <текущая-ветка>`** и заново прочитает версии из файла):

```powershell
.\scripts\publish-release.ps1 -PullRebase
```

Либо вручную:

```bash
git pull --rebase origin main
git push origin main
```

Если скрипт успел сделать **commit** с bump, но **push ветки** упал: тег ещё не создаётся — исправьте синхронизацию и выполните **`git push origin main`** (и при необходимости **`git push origin v…`** для тега, если он уже был создан вручную). Чтобы отменить только что созданный локальный коммит с bump и вернуть правку в `build.gradle.kts` в индекс: **`git reset --soft HEAD~1`**, затем **`git checkout -- app/build.gradle.kts`** при необходимости.

Если **не** использовать **`-PullRebase`**, при отстающей ветке скрипт после **`git fetch origin`** может вывести **предупреждение**, что **`origin/<ветка>` впереди** — это сигнал заранее сделать rebase, иначе снова будет отказ push.

## Если тег уже был на GitHub

Удалите тег на сервере и локально, затем снова:

```bash
git push origin :refs/tags/v1.0.6
git tag -d v1.0.6
```

## После успешного Actions

В приложении в **`UPDATE_MANIFEST_URL`** должен быть стабильный URL, например:

`https://github.com/pycraft-dev/-----/releases/latest/download/update.json`

Проверка обновлений — экран **«Обновления»** на рабочем столе приложения (одна кнопка).
