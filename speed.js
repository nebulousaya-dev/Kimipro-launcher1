// speed.js — пример скрипта для KimiPro Launcher (KP API v1)
// Кинь файл в папку /sdcard/KimiPro/scripts/ и включи в приложении.

// --- создаём модуль ---
const speed = kp.module("Speed", "MOVEMENT");

// --- добавляем настройки ---
speed.add(kp.setting.slider("Скорость", 1.5, 0.1, 5.0, 0.1));
speed.add(kp.setting.mode("Режим", ["Vanilla", "Strafe"]));
speed.add(kp.setting.state("Авто-прыжок", false));
speed.add(kp.setting.text("Префикс", "Введите текст", "hi"));
speed.add(kp.setting.button("Тест", () => kp.toast("Кнопка нажата")));

// --- события модуля ---
speed.onEnable(() => {
    kp.toast("Speed включён");
    kp.data.set("speed_enabled", true);
});

speed.onDisable(() => {
    kp.toast("Speed выключен");
    kp.data.set("speed_enabled", false);
});

speed.onTick(() => {
    const v = speed.get("Скорость").get();
    kp.log("Speed =", v, "| режим =", speed.get("Режим").get());

    // игровые функции работают после подключения GameBridge.Impl
    // if (kp.game.isInGame()) {
    //     kp.game.setPosition(kp.game.playerX(), kp.game.playerY(), kp.game.playerZ());
    // }
});

// --- глобальные хуки скрипта ---
kp.on("enable", () => kp.log("скрипт запущен"));
kp.on("tick", () => { /* вызывается каждый тик */ });

// --- чтение сохранённых данных ---
const enabledBefore = kp.data.get("speed_enabled", false);
kp.log("speed_enabled =", enabledBefore);
