package io.celox.stupidisco.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStateTest {

    @Test
    fun `default state has correct values`() {
        val state = AppState()
        assertTrue(state.status is AppStatus.Ready)
        assertEquals("", state.transcript)
        assertEquals("", state.partialTranscript)
        assertEquals("", state.answer)
        assertFalse(state.isAnswerComplete)
        assertEquals(0, state.questionCount)
    }

    @Test
    fun `copy preserves unchanged fields`() {
        val original = AppState(
            status = AppStatus.Recording,
            transcript = "Hello",
            answer = "World",
            questionCount = 3
        )
        val copied = original.copy(status = AppStatus.Thinking)

        assertTrue(copied.status is AppStatus.Thinking)
        assertEquals("Hello", copied.transcript)
        assertEquals("World", copied.answer)
        assertEquals(3, copied.questionCount)
    }

    @Test
    fun `AppStatus Ready is singleton`() {
        assertTrue(AppStatus.Ready === AppStatus.Ready)
    }

    @Test
    fun `AppStatus Recording is singleton`() {
        assertTrue(AppStatus.Recording === AppStatus.Recording)
    }

    @Test
    fun `AppStatus Thinking is singleton`() {
        assertTrue(AppStatus.Thinking === AppStatus.Thinking)
    }

    @Test
    fun `AppStatus Error carries message`() {
        val error = AppStatus.Error("Connection failed")
        assertEquals("Connection failed", error.message)
    }

    @Test
    fun `AppStatus Error equality`() {
        val e1 = AppStatus.Error("msg")
        val e2 = AppStatus.Error("msg")
        assertEquals(e1, e2)
    }

    @Test
    fun `copy with all fields changed`() {
        val state = AppState().copy(
            status = AppStatus.Error("err"),
            transcript = "t",
            partialTranscript = "p",
            answer = "a",
            isAnswerComplete = true,
            questionCount = 5
        )
        assertTrue(state.status is AppStatus.Error)
        assertEquals("t", state.transcript)
        assertEquals("p", state.partialTranscript)
        assertEquals("a", state.answer)
        assertTrue(state.isAnswerComplete)
        assertEquals(5, state.questionCount)
    }
}
