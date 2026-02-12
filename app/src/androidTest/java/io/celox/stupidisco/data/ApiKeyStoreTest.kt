package io.celox.stupidisco.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApiKeyStoreTest {

    private lateinit var store: ApiKeyStore

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Clear any existing keys
        context.getSharedPreferences("stupidisco_keys", 0).edit().clear().apply()
        store = ApiKeyStore(context)
    }

    @Test
    fun hasKeys_returnsFalse_whenNoKeysSaved() {
        assertFalse(store.hasKeys())
    }

    @Test
    fun getKeys_returnEmpty_whenNoKeysSaved() {
        assertEquals("", store.getDeepgramKey())
        assertEquals("", store.getAnthropicKey())
    }

    @Test
    fun saveKeys_thenRetrieve() {
        store.saveKeys("dg-test-key", "ant-test-key")
        assertEquals("dg-test-key", store.getDeepgramKey())
        assertEquals("ant-test-key", store.getAnthropicKey())
    }

    @Test
    fun hasKeys_returnsTrue_afterSavingBothKeys() {
        store.saveKeys("dg-key", "ant-key")
        assertTrue(store.hasKeys())
    }

    @Test
    fun saveKeys_overwrites_previousKeys() {
        store.saveKeys("old-dg", "old-ant")
        store.saveKeys("new-dg", "new-ant")
        assertEquals("new-dg", store.getDeepgramKey())
        assertEquals("new-ant", store.getAnthropicKey())
    }

    @Test
    fun hasKeys_returnsFalse_whenOnlyOneKeySaved() {
        store.saveKeys("dg-key", "")
        assertFalse(store.hasKeys())
    }
}
