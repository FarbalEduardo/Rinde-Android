package com.farbalapps.rinde.domain.moderation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentModeratorTest {

    private val moderator = ContentModerator()

    @Test
    fun `analyzeText returns Allowed for legitimate content`() {
        val title = "Oferta de Aceite"
        val desc = "Aceite de maravilla 2x1 en el pasillo central."
        
        val result = moderator.analyzeText(title, desc)
        
        assertTrue("El contenido legítimo debería ser aceptado", result is ContentModerator.ModerationResult.Allowed)
    }

    @Test
    fun `analyzeText rejects explicit forbidden keyword`() {
        val title = "Vendo cachorro"
        val desc = "Hermoso cachorro busca hogar."
        
        val result = moderator.analyzeText(title, desc)
        
        assertTrue("Debería rechazar la venta de animales", result is ContentModerator.ModerationResult.Rejected)
        val reason = (result as ContentModerator.ModerationResult.Rejected).reason
        assertTrue(reason.contains("prohíbe la venta de animales"))
    }

    @Test
    fun `analyzeText rejects forbidden substance keyword`() {
        val title = "Hierba mágica"
        val desc = "Vendo marihuana de calidad."
        
        val result = moderator.analyzeText(title, desc)
        
        assertTrue("Debería rechazar sustancias ilícitas", result is ContentModerator.ModerationResult.Rejected)
    }

    @Test
    fun `analyzeText rejects via Regex patterns regardless of case`() {
        val title = "VENTA DE ANIMALES"
        val desc = "Cualquier texto aquí."
        
        val result = moderator.analyzeText(title, desc)
        
        assertTrue("Debería rechazar patrones detectados por Regex", result is ContentModerator.ModerationResult.Rejected)
    }

    @Test
    fun `analyzeText handles edge cases like empty strings`() {
        val result = moderator.analyzeText("", "")
        assertTrue(result is ContentModerator.ModerationResult.Allowed)
    }
}
