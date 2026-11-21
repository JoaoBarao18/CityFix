package pt.isec.amov.a2024136885.cityfix

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pt.isec.amov.a2024136885.cityfix.domain.location.estaDentroRaio

// Pontos de Coimbra junto ao limite, para detetar trocas de latitude e longitude.
@RunWith(AndroidJUnit4::class)
class LocationHandlerTest {
    @Test
    fun centroDaPracaPermiteAcesso() {
        assertTrue(estaDentroRaio(-8.4239, 40.2078))
    }

    @Test
    fun pontoDentroDoLimitePermiteAcesso() {
        // Cerca de 9 995 metros a este do centro combinado.
        assertTrue(estaDentroRaio(-8.3065, 40.2078))
    }

    @Test
    fun pontoForaDoLimiteRecusaAcesso() {
        // Cerca de 10 003 metros a este do centro combinado.
        assertFalse(estaDentroRaio(-8.3064, 40.2078))
    }

    @Test
    fun lisboaRecusaAcesso() {
        assertFalse(estaDentroRaio(-9.1393, 38.7223))
    }
}
