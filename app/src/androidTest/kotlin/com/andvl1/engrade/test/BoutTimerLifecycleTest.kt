package com.andvl1.engrade.test

import android.app.PendingIntent
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.andvl1.engrade.data.SettingsRepository
import com.andvl1.engrade.domain.model.BoutConfig
import com.andvl1.engrade.domain.model.Weapon
import com.andvl1.engrade.platform.NotificationHelper
import com.andvl1.engrade.platform.SoundManager
import com.andvl1.engrade.ui.bout.BoutEvent
import com.andvl1.engrade.ui.bout.DefaultBoutComponent
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.stop
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Детерминированный тест бага M4: таймер боя не должен продолжать тикать
 * когда приложение уходит в фон (lifecycle переходит в STOPPED).
 *
 * Тест работает на уровне компонента — без UI, без Activity.
 * Эмулирует: запуск таймера → жизненный цикл onStop → проверка что таймер встал.
 */
class BoutTimerLifecycleTest {

    @Test
    fun timerPausesWhenLifecycleStops() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Фиксированный конфиг — не зависим от DataStore / настроек пользователя.
        // periodLengthMs = 180_000 (3 минуты), mode 5, сабля.
        val testConfig = BoutConfig(
            mode = 5,
            weapon = Weapon.SABRE,
            periodLengthMs = 180_000L,
            breakLengthMs = 60_000L,
            priorityLengthMs = 60_000L,
            showDoubleTouchButton = true,
            anywhereToStart = true
        )

        val soundManager = SoundManager(context)
        val notificationHelper = NotificationHelper(context)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, Class.forName("com.andvl1.engrade.EnGardeActivity")),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val lifecycle = LifecycleRegistry()
        val componentContext = DefaultComponentContext(lifecycle = lifecycle)

        var component: DefaultBoutComponent? = null

        // Компоненты Decompose обязаны создаваться на главном потоке.
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            // Поднимаем lifecycle до RESUMED перед созданием компонента —
            // иначе init{} блок уйдёт в INITIALIZED состоянии.
            lifecycle.resume()

            component = DefaultBoutComponent(
                componentContext = componentContext,
                settingsRepository = SettingsRepository(context),
                soundManager = soundManager,
                notificationHelper = notificationHelper,
                notificationPendingIntent = pendingIntent,
                overrideConfig = testConfig,
                onNavigateToSettings = {}
            )
        }

        val boutComponent = component!!

        // Ждём инициализацию движка (coroutine в init{} настраивает BoutEngine).
        Thread.sleep(300)

        // Запускаем таймер на главном потоке (компонент использует Dispatchers.Main).
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            boutComponent.onEvent(BoutEvent.TimerClicked)
        }

        // Ждём несколько тиков чтобы убедиться что таймер реально пошёл.
        Thread.sleep(400)

        // Проверяем что таймер запущен.
        val stateBeforeStop = boutComponent.state.value
        assert(stateBeforeStop.isTimerRunning) {
            "Ожидалось isTimerRunning=true после TimerClicked, но получили false. " +
                "Возможно инициализация компонента ещё не завершена."
        }

        // Фиксируем оставшееся время ДО ухода в фон.
        val timeBeforeStop = boutComponent.state.value.timeRemainingMs

        // Эмулируем уход приложения в фон — Activity onStop.
        // doOnStop { pauseTimer() } должен сработать.
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            lifecycle.stop()
        }

        // Даём время на обработку события lifecycle.
        Thread.sleep(100)

        // ПРОВЕРКА 1: таймер должен быть остановлен сразу после onStop.
        val stateAfterStop = boutComponent.state.value
        assertFalse(
            "FAIL M4: isTimerRunning=${stateAfterStop.isTimerRunning} после lifecycle.stop(). " +
                "Ожидалось false — doOnStop { pauseTimer() } не сработал.",
            stateAfterStop.isTimerRunning
        )

        // Фиксируем время сразу после остановки.
        val timeAfterStop = boutComponent.state.value.timeRemainingMs

        // Ждём 1 секунду — за это время таймер НЕ должен продвинуться.
        Thread.sleep(1000)

        // ПРОВЕРКА 2: timeRemainingMs не изменился за время ожидания в фоне.
        val timeAfterWait = boutComponent.state.value.timeRemainingMs
        assertEquals(
            "FAIL M4: timeRemainingMs изменился пока приложение в фоне. " +
                "До ожидания: ${timeAfterStop}мс, после 1с ожидания: ${timeAfterWait}мс. " +
                "Таймер продолжает тикать несмотря на lifecycle.stop().",
            timeAfterStop,
            timeAfterWait
        )

        soundManager.release()
    }
}
