package br.com.apolo.booster.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GainMathTest {

    @Test
    fun `100 por cento equivale a zero dB`() {
        assertEquals(0f, GainMath.percentToDb(100), 0.001f)
    }

    @Test
    fun `200 por cento equivale a 10 dB`() {
        assertEquals(10f, GainMath.percentToDb(200), 0.001f)
    }

    @Test
    fun `300 por cento equivale a 20 dB`() {
        assertEquals(20f, GainMath.percentToDb(300), 0.001f)
    }

    @Test
    fun `percentual fora da faixa e limitado`() {
        assertEquals(0f, GainMath.percentToDb(-50), 0.001f)
        assertEquals(20f, GainMath.percentToDb(900), 0.001f)
    }

    @Test
    fun `milibels acompanham a curva de dB`() {
        assertEquals(2000, GainMath.percentToMillibels(300))
        assertEquals(1500, GainMath.percentToMillibels(250))
    }

    @Test
    fun `teto do aparelho prevalece sobre o teto do app`() {
        assertEquals(1500, GainMath.clampToDevice(2000, 1500))
        assertEquals(2000, GainMath.clampToDevice(2000, 3000))
        assertEquals(0, GainMath.clampToDevice(2000, -10))
    }

    @Test
    fun `ganho em dB respeita o limite nativo`() {
        assertEquals(1200, GainMath.bandGainToLevel(12, -1500, 1500).toInt())
        assertEquals(900, GainMath.bandGainToLevel(12, -900, 900).toInt())
    }

    @Test
    fun `ganho negativo de banda vira nivel negativo`() {
        assertEquals(-600, GainMath.bandGainToLevel(-6, -1500, 1500).toInt())
    }

    @Test
    fun `fracao de volume e segura com maximo invalido`() {
        assertEquals(0f, GainMath.volumeFraction(3, 0), 0f)
        assertEquals(1f, GainMath.volumeFraction(15, 10), 0f)
        assertEquals(0.5f, GainMath.volumeFraction(5, 10), 0f)
    }

    @Test
    fun `curva e monotonica`() {
        var previous = -1f
        for (percent in 100..300) {
            val db = GainMath.percentToDb(percent)
            assertTrue(db >= previous)
            previous = db
        }
    }
}
