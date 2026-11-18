# KimiPro Launcher

Лаунчер с собственным GUI, менеджером модулей, JS-движком (Rhino) и новым **KP API v1** для скриптов.
Архитектурно схож с Variable Launcher, но с чистым переосмысленным API.

## Сборка APK

1. Установите [Android Studio](https://developer.android.com/studio) (Hedgehog+).
2. Откройте папку проекта — Studio сама докачает Gradle и SDK.
3. **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. APK появится в `app/build/outputs/apk/debug/app-debug.apk` (или release после создания подписи:
   **Build → Generate Signed Bundle/APK**).

Из консоли: `gradlew assembleDebug` (Linux/macOS) или `gradlew.bat assembleDebug` (Windows).

## Установка и использование

1. Установите APK на Android 7.0+.
2. Скрипты кладите в `/sdcard/KimiPro/scripts/*.js`
   (на Android 10+ если папка недоступна — используется
   `/sdcard/Android/data/com.kimipro.launcher/files/scripts/`).
3. В приложении: вкладка **«Скрипты»** — включайте/выключайте скрипты.
4. Вкладка **«Модули»** — тап по модулю открывает настройки.
5. Кнопка **«Меню»** — плавающее оверлей-меню поверх игры (нужно разрешение «поверх других приложений»).

## KP API v1 — справка для скриптов

Глобальный объект `kp` доступен в каждом скрипте.

### Утилиты
| Функция | Описание |
|---|---|
| `kp.version` | версия API |
| `kp.log(...args)` | лог в logcat (тег `KimiPro`) |
| `kp.toast(text)` | тост на экране |
| `kp.tickRate(20)` | тиков в секунду (по умолчанию 20) |
| `kp.on("enable" \| "disable" \| "tick", fn)` | глобальные хуки скрипта |

Также можно объявлять глобальные функции `onEnable()`, `onDisable()`, `onTick()` — они вызовутся автоматически.

### Модули
```js
const mod = kp.module("Speed", "MOVEMENT"); // категории: COMBAT, MOVEMENT, PLAYER, MISC, OTHER
mod.isActive();          // bool
mod.setActive(true);     // включить
mod.toggle();
mod.bind("R");           // привязка клавиши
mod.getBind();
mod.add(kp.setting.slider("Скорость", 1.5, 0.1, 5.0, 0.1));
mod.get("Скорость");     // настройка по имени
mod.settings();          // массив настроек
mod.onEnable(fn);  mod.onDisable(fn);  mod.onTick(fn);
```

### Настройки
```js
kp.setting.slider(name, default, min, max, step)
kp.setting.mode(name, ["a", "b"])
kp.setting.state(name, defaultBool)
kp.setting.text(name, hint, defaultText)
kp.setting.button(name, fn)

const s = kp.setting.slider("Speed", 1.0, 0.1, 5.0, 0.1);
s.get();  s.set(2.5);  s.onChange(v => kp.log(v));  s.show(false);
```

### Данные (сохраняются между запусками)
```js
kp.data.set("key", "value");   // string | number | boolean
kp.data.get("key", defaultValue);
kp.data.remove("key");
kp.data.all();
```

### Игровой мост (kp.game)
```js
kp.game.isInGame();
kp.game.playerX(); kp.game.playerY(); kp.game.playerZ();
kp.game.yaw(); kp.game.pitch(); kp.game.health();
kp.game.setPosition(x, y, z);
kp.game.setRotation(yaw, pitch);
kp.game.sendChat("text");
kp.game.chatMsg("только для тебя");
```

Чтобы `kp.game.*` реально работал, реализуйте `GameBridge.Impl` на стороне клиента
(`com.kimipro.launcher.game.GameBridge`) — это точка интеграции с игрой, аналог
`LocalPlayer`/`Level`/`Player` из Variable Launcher. Без неё методы кидают `IllegalStateException`.

```kotlin
GameBridge.impl = object : GameBridge.Impl {
    override fun get(key: String): Any? = /* x, y, z, yaw, pitch, health */
    override fun setPosition(x: Double, y: Double, z: Double) { /* ... */ }
    override fun setRotation(yaw: Double, pitch: Double) {}
    override fun sendChat(text: String) {}
    override fun display(text: String) {}
    override fun isInGame(): Boolean = true
}
```

## Структура проекта

```
app/src/main/java/com/kimipro/launcher/
├── MainActivity.kt          — главный GUI (вкладки Скрипты/Модули/Меню)
├── ScriptsAdapter.kt        — список скриптов
├── ModulesAdapter.kt        — список модулей
├── SettingsDialog.kt        — редактор настроек модуля
├── App.kt                   — Application
├── prefs/Prefs.kt           — хранилище kp.data
├── module/
│   ├── Module.kt            — Module, Setting (slider/mode/state/text/button), Category
│   └── ModuleManager.kt     — реестр модулей
├── overlay/MenuService.kt   — плавающее оверлей-меню
├── game/GameBridge.kt       — мост к клиенту игры
└── script/
    ├── Engine.kt            — Rhino-контексты скриптов, хуки
    ├── KPApi.kt             — регистрация объекта kp (KP API v1)
    ├── JsFn.kt              — обёртки JS-функций
    ├── HookBus.kt           — тикер 20/с
    └── ScriptManager.kt     — загрузка/запуск скриптов
examples/speed.js            — пример скрипта
```
