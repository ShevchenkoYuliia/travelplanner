package com.example.travelplanner.security

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BiometricManagerTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `checkAvailability returns unavailable without sensor`() {
        val manager = MockBiometricManager(
            availability = BiometricAvailability(false, BiometricType.None, "Немає датчика")
        )

        val availability = manager.checkAvailability()

        assertFalse(availability.isSupported)
        assertEquals(BiometricType.None, availability.type)
        assertEquals("Немає датчика", availability.message)
    }

    @Test
    fun `authenticate returns success with successful mock result`() = runTest(dispatcher) {
        val manager = MockBiometricManager(nextResult = BiometricAuthResult.Success)

        val result = manager.authenticate("test")

        assertEquals(BiometricAuthResult.Success, result)
        assertEquals(BiometricAuthState.Success, manager.state.value)
    }

    @Test
    fun `authenticate returns failed when user cancels`() = runTest(dispatcher) {
        val manager = MockBiometricManager(nextResult = BiometricAuthResult.UserCancelled)

        val result = manager.authenticate("test")

        assertEquals(BiometricAuthResult.UserCancelled, result)
        assertTrue(manager.state.value is BiometricAuthState.Failed)
    }

    @Test
    fun `authenticate returns system error from mock`() = runTest(dispatcher) {
        val manager = MockBiometricManager(
            nextResult = BiometricAuthResult.SystemError("Системна помилка")
        )

        val result = manager.authenticate("test")

        assertEquals(BiometricAuthResult.SystemError("Системна помилка"), result)
        assertEquals(BiometricAuthState.Failed("Системна помилка"), manager.state.value)
    }

    @Test
    fun `authenticate returns unavailable when sensor becomes unavailable`() = runTest(dispatcher) {
        val manager = MockBiometricManager(
            availability = BiometricAvailability(false, BiometricType.None, "Датчик недоступний"),
            nextResult = BiometricAuthResult.Success
        )

        val result = manager.authenticate("test")

        assertEquals(BiometricAuthResult.Unavailable("Датчик недоступний"), result)
        assertEquals(BiometricAuthState.Unavailable("Датчик недоступний"), manager.state.value)
    }

    @Test
    fun `isEnabledByUser reads saved value after restart`() {
        val preferences = InMemorySecurityPreferences()
        MockBiometricManager(preferences).setEnabledByUser(true)
        val restartedManager = MockBiometricManager(preferences)

        assertTrue(restartedManager.isEnabledByUser())
    }

    @Test
    fun `settings save and read without distortion`() {
        val preferences = InMemorySecurityPreferences()

        preferences.setBiometricEnabled(true)
        preferences.setLockAfterSeconds(123)

        assertTrue(preferences.isBiometricEnabled())
        assertEquals(123, preferences.lockAfterSeconds())
    }

    @Test
    fun `lock timeout is clamped to supported range`() {
        val preferences = InMemorySecurityPreferences()

        preferences.setLockAfterSeconds(3)
        assertEquals(10, preferences.lockAfterSeconds())

        preferences.setLockAfterSeconds(999)
        assertEquals(600, preferences.lockAfterSeconds())
    }

    @Test
    fun `ui state transitions to authenticating after authenticate call`() = runTest(dispatcher) {
        val manager = PausedBiometricManager()
        val viewModel = SecurityViewModel(manager, manager.preferences)

        viewModel.authenticateAsync("test")
        runCurrent()

        assertEquals(BiometricAuthState.Authenticating, viewModel.uiState.value.authState)
    }

    @Test
    fun `ui state transitions to success after successful authentication`() = runTest(dispatcher) {
        val manager = PausedBiometricManager()
        val viewModel = SecurityViewModel(manager, manager.preferences)

        viewModel.authenticateAsync("test")
        runCurrent()
        manager.complete(BiometricAuthResult.Success)
        runCurrent()

        assertEquals(BiometricAuthState.Success, viewModel.uiState.value.authState)
    }

    @Test
    fun `security view model refuses authentication when disabled by user`() = runTest(dispatcher) {
        val preferences = InMemorySecurityPreferences(biometricEnabled = false)
        val manager = MockBiometricManager(preferences)
        val viewModel = SecurityViewModel(manager, preferences)

        val result = viewModel.authenticate("test")

        assertEquals(BiometricAuthResult.Unavailable("Біометрія вимкнена в налаштуваннях"), result)
        assertEquals(
            BiometricAuthState.Unavailable("Біометрія вимкнена в налаштуваннях"),
            viewModel.uiState.value.authState
        )
    }

    @Test
    fun `mock can switch between different scenarios`() = runTest(dispatcher) {
        val manager = MockBiometricManager(nextResult = BiometricAuthResult.Success)

        assertEquals(BiometricAuthResult.Success, manager.authenticate("test"))

        manager.setNextResult(BiometricAuthResult.Failed("Не розпізнано"))
        assertEquals(BiometricAuthResult.Failed("Не розпізнано"), manager.authenticate("test"))
    }

    @Test
    fun `enabling biometrics updates centralized ui state`() {
        val preferences = InMemorySecurityPreferences()
        val manager = MockBiometricManager(preferences)
        val viewModel = SecurityViewModel(manager, preferences)

        viewModel.setBiometricEnabled(true)

        assertTrue(viewModel.uiState.value.biometricEnabled)
        assertTrue(manager.isEnabledByUser())
    }
}

private class PausedBiometricManager : BiometricManager {
    val preferences = InMemorySecurityPreferences(biometricEnabled = true)
    private val pending = CompletableDeferred<BiometricAuthResult>()
    private val _state = MutableStateFlow<BiometricAuthState>(BiometricAuthState.Idle)
    override val state: StateFlow<BiometricAuthState> = _state

    override fun checkAvailability(): BiometricAvailability =
        BiometricAvailability(true, BiometricType.Fingerprint)

    override suspend fun authenticate(reason: String): BiometricAuthResult {
        _state.value = BiometricAuthState.Authenticating
        val result = pending.await()
        _state.value = result.toState()
        return result
    }

    override fun isEnabledByUser(): Boolean = preferences.isBiometricEnabled()

    override fun setEnabledByUser(enabled: Boolean) {
        preferences.setBiometricEnabled(enabled)
    }

    fun complete(result: BiometricAuthResult) {
        pending.complete(result)
    }
}
