package br.com.apolo.booster.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqPresetsTest {

    @Test
    fun `plano e silencio em todas as bandas`() {
        val gains = EqPresets.gains(EqPresets.FLAT, 5)
        assertTrue(gains.all { it == 0 })
    }

    @Test
    fun `quantidade de ganhos acompanha as bandas do aparelho`() {
        assertEquals(3, EqPresets.gains(EqPresets.ROCK, 3).size)
        assertEquals(10, EqPresets.gains(EqPresets.ROCK, 10).size)
    }

    @Test
    fun `aparelho sem bandas nao recebe curva`() {
        assertTrue(EqPresets.gains(EqPresets.BASS, 0).isEmpty())
    }

    @Test
    fun `preset de graves reforca a primeira banda`() {
        assertTrue(EqPresets.gains(EqPresets.BASS, 5).first() > 0)
    }

    @Test
    fun `todos os ganhos ficam na faixa de doze dB`() {
        for (preset in 0..4) {
            EqPresets.gains(preset, 8).forEach { gain ->
                assertTrue("gain $gain fora da faixa", gain in -12..12)
            }
        }
    }
}
