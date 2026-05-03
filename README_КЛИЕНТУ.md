# Инструкция пользователя — Manufacturing Enterprise

Краткая памятка: **скачали приложение → установили → вошли → пользуетесь**. Без технических подробностей.

---

## 1. Установка

1. Установите **APK** или приложение из **Google Play / внутреннего теста** — так, как выдал разработчик или администратор.  
2. При запросе Android разрешите только то, о чём спрашивает приложение (камера, микрофон, файлы — если используете фото брака, голос в чате или вложения).  
3. Откройте иконку **Manufacturing Enterprise** на главном экране.

<details>
<summary><strong>English — Installation</strong></summary>

Install the APK or store build provided by your admin. Grant permissions when prompted. Open the Manufacturing Enterprise icon.

</details>

---

## 2. Вход

1. Введите **логин** и **пароль**, которые выдал администратор.  
2. Нажмите кнопку входа.

Если не проходит вход — проверьте раскладку клавиатуры и Caps Lock. Обратитесь к администратору за учётной записью.

> **Первый запуск с «пустой» базой:** может использоваться тестовый вход `admin_1` / `admin123` только до настройки реальных пользователей. Не используйте его в бою.

<details>
<summary><strong>English — Sign in</strong></summary>

Enter login and password from your admin. For empty dev databases a bootstrap account may exist — replace with real users for production.

</details>

---

## 3. Основные разделы (кнопки и экраны)

После входа вы попадаете в главное меню приложения (навигация зависит от вашей роли).

| Что нужно сделать | Куда зайти |
|-------------------|------------|
| Посмотреть или оформить **брак** | раздел брака / дефектов |
| Открыть **чертежи**, загрузить файл | чертежи |
| Отметить **время работы** | учёт времени: что сделано + часы и минуты |
| **Написать коллеге** | список чатов → личный диалог |

Кнопка **«Назад»** вверху или системная стрелка Android возвращает на предыдущий экран.

<details>
<summary><strong>English — Main sections</strong></summary>

Defects, drawings, timesheet (manual duration), chats — navigate from the main menu according to role. Use the app bar back action or system back.

</details>

---

## 4. Чат и сообщения

- Введите текст в поле внизу и отправьте.  
- Голосовые и файлы — по кнопкам рядом с полем ввода (если они включены в вашей сборке).

**Важно:** в текущей версии данные и сообщения хранятся **на устройстве**. Чтобы «увидеть» переписку с другого телефона, нужна настройка сервера у администратора. На одном устройстве можно войти под разными пользователями по очереди для проверки.

<details>
<summary><strong>English — Chat</strong></summary>

Text in the bottom field; voice/file if available. Data is device-local unless a sync server is configured by your organization.

</details>

---

## 5. Скриншоты интерфейса

Вставьте сюда изображения или ссылки:

- Вход: `docs/screenshots/login.png`  
- Главный экран: `docs/screenshots/home.png`  
- Чат: `docs/screenshots/chat.png`  

*(Папку `docs/screenshots` можно создать при подготовке материалов для заказчика.)*

---

## 6. Частые вопросы

**Не открывается файл чертежа.**  
Проверьте формат (например, PDF) и что файл не повреждён. Попробуйте выбрать документ ещё раз.

**Нет интернета — работает ли приложение?**  
Большинство функций рассчитано на **локальную** работу. Обновления и будущая «облачная» переписка зависят от настроек вашей организации.

**Кому писать по доработкам?**  
Разработчику — контакты в основном [README.md](README.md) (раздел «Контакты разработчика»).

<details>
<summary><strong>English — FAQ</strong></summary>

Drawing won’t open: check format (e.g. PDF). App works offline for most local features. For features and support see contacts in README.md.

</details>

---

## Лицензия

Программное обеспечение распространяется на условиях MIT; текст лицензии: [LICENSE](LICENSE).
