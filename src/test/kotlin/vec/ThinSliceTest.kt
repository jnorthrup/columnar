package vec

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList as flowToList // Alias to avoid conflict
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThinSliceTest {

    @Test
    fun `flow should emit items correctly`() = runBlocking {
        val testFlow: Flow<Int> = flowOf(1, 2, 33)
        val collectedItems = testFlow.flowToList() // Use alias

        assertEquals(3, collectedItems.size, "Flow should emit 3 items")
        assertEquals(1, collectedItems[0], "First item should be 1")
        assertEquals(33, collectedItems[2], "Third item should be 33")
    }

    @Test
    fun `clock system now should return current time`() {
        val currentTime = Clock.System.now()
        // Basic check: epoch seconds should be positive and significant (e.g., after year 2000)
        assertTrue(currentTime.epochSeconds > 946684800L, "Current time should be after 2000-01-01")
    }
}
