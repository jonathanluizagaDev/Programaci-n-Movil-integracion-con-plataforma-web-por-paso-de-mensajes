package org.ucb.appp1

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.ucb.appp1.data.FakeEventRepository
import org.ucb.appp1.model.EventLogStage
import org.ucb.appp1.model.EventOrigin
import org.ucb.appp1.presentation.EventStateHolder

class EventSimulationTest {
    @Test
    fun bothDirectionsProduceCorrelatedSteps() {
        val repository = FakeEventRepository()
        for ((origin, destination) in listOf(EventOrigin.APP to EventOrigin.WEB, EventOrigin.WEB to EventOrigin.APP)) {
            val entries = repository.simulateRoundTrip(origin)
            assertEquals(EventLogStage.entries, entries.map { it.stage })
            assertEquals(origin, entries.first().source)
            assertEquals(destination, entries[2].source)
            assertEquals(destination, entries[3].source)
            assertEquals(1, entries.map { it.requestId }.distinct().size)
            assertEquals(1, entries.map { it.eventId }.distinct().size)
            assertEquals(entries.size, entries.map { it.id }.distinct().size)
        }
    }

    @Test
    fun successiveFlowsHaveDifferentIdentifiersAndCanBeCleared() {
        val state = EventStateHolder(FakeEventRepository())
        state.startFromApp()
        val first = state.logs.first().eventId
        state.simulateStartFromWeb()
        assertEquals(10, state.logs.size)
        assertNotEquals(first, state.logs.last().eventId)
        state.clearLogs()
        assertTrue(state.logs.isEmpty())
    }

    @Test
    fun serverCannotInitiateClientSimulation() {
        assertFailsWith<IllegalArgumentException> {
            FakeEventRepository().simulateRoundTrip(EventOrigin.SERVER)
        }
    }
}
