package com.example

import com.example.data.api.Candidate
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerateContentResponse
import com.example.data.api.Part
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiChatUnitTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testGenerateContentRequestSerialization() {
        val request = GenerateContentRequest(
            contents = listOf(
                Content(role = "user", parts = listOf(Part(text = "Qual meu saldo?"))),
                Content(role = "model", parts = listOf(Part(text = "Seu saldo é R$ 1.500,00.")))
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "Você é um consultor financeiro."))
            )
        )

        val serialized = json.encodeToString(request)
        assertTrue(serialized.contains("Qual meu saldo?"))
        assertTrue(serialized.contains("consultor financeiro"))
        assertTrue(serialized.contains("user"))
        assertTrue(serialized.contains("model"))
    }

    @Test
    fun testGenerateContentResponseDeserialization() {
        val sampleResponseJson = """
            {
                "candidates": [
                    {
                        "content": {
                            "role": "model",
                            "parts": [
                                {
                                    "text": "Aqui estão dicas para cobrar clientes de forma amigável."
                                }
                            ]
                        },
                        "finishReason": "STOP"
                    }
                ]
            }
        """.trimIndent()

        val response = json.decodeFromString<GenerateContentResponse>(sampleResponseJson)
        assertNotNull(response.candidates)
        assertEquals(1, response.candidates?.size)
        val candidate = response.candidates?.first()
        assertEquals("Aqui estão dicas para cobrar clientes de forma amigável.", candidate?.content?.parts?.firstOrNull()?.text)
        assertEquals("STOP", candidate?.finishReason)
    }
}
