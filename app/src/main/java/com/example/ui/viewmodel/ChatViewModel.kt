package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GeminiRetrofitClient
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.Part
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ItemStatus
import com.example.data.repository.FinancialRepository
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class ChatSender {
    USER,
    ASSISTANT
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: ChatSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FinancialRepository(AppDatabase.getInstance(application))

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = ChatSender.ASSISTANT,
                text = "Olá! Sou seu Assistente Financeiro Inteligente. Estou aqui para ajudar com análises dos seus empréstimos, recebíveis, dívidas a pagar, fluxo de caixa e estratégias de negociação e cobrança.\n\nComo posso ajudar você hoje?"
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
    }

    fun clearChat() {
        _messages.value = listOf(
            ChatMessage(
                sender = ChatSender.ASSISTANT,
                text = "Conversa reiniciada. Como posso ajudar com suas finanças agora?"
            )
        )
        _errorMessage.value = null
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _isLoading.value) return

        val userMessage = ChatMessage(
            sender = ChatSender.USER,
            text = trimmed
        )
        val updatedList = _messages.value + userMessage
        _messages.value = updatedList
        _errorMessage.value = null
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    throw IllegalStateException(
                        "Chave de API do Gemini não configurada. Adicione sua GEMINI_API_KEY no painel de segredos (Secrets) do AI Studio."
                    )
                }

                // Gather current financial context to empower the assistant
                val financialContext = buildFinancialContextSummary()

                val systemInstructionText = """
                    Você é o Assistente Financeiro Inteligente do aplicativo Controle Financeiro.
                    Seu papel é atuar como consultor financeiro pessoal e comercial, especialista em:
                    - Gestão de empréstimos concedidos e juros a receber;
                    - Contas e vendas a receber de clientes;
                    - Dívidas e contas a pagar;
                    - Fluxo de caixa, saldo operacional e controle de inadimplência;
                    - Modelos de mensagens educadas de cobrança por WhatsApp ou telefone;
                    - Estratégias para quitar dívidas e economizar.

                    DADOS ATUAIS DO USUÁRIO NO APP:
                    $financialContext

                    DIRETRIZES:
                    - Responda de forma clara, prestativa, prática e objetiva em Português do Brasil.
                    - Quando o usuário perguntar sobre a situação atual dele, consulte os DADOS ATUAIS acima.
                    - Use formatação simples com tópicos e valores em R$ formatados.
                    - Se o usuário pedir um rascunho de cobrança, forneça um texto amigável e profissional pronto para copiar.
                """.trimIndent()

                // Construct multi-turn history from messages (excluding greetings or error messages)
                val conversationContents = updatedList
                    .filter { !it.isError }
                    .takeLast(10) // keep last 10 messages for token efficiency and high quality
                    .map { msg ->
                        Content(
                            role = if (msg.sender == ChatSender.USER) "user" else "model",
                            parts = listOf(Part(text = msg.text))
                        )
                    }

                val request = GenerateContentRequest(
                    contents = conversationContents,
                    systemInstruction = Content(
                        parts = listOf(Part(text = systemInstructionText))
                    ),
                    generationConfig = GenerationConfig(
                        temperature = 0.7f,
                        topP = 0.95f
                    )
                )

                val response = withContext(Dispatchers.IO) {
                    GeminiRetrofitClient.service.generateContent(
                        model = "gemini-3.8-flash",
                        apiKey = apiKey,
                        request = request
                    )
                }

                val replyText = response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text

                if (!replyText.isNullOrBlank()) {
                    _messages.value = _messages.value + ChatMessage(
                        sender = ChatSender.ASSISTANT,
                        text = replyText.trim()
                    )
                } else {
                    val blockReason = response.promptFeedback?.blockReason
                    val reasonMessage = if (blockReason != null) {
                        "A resposta foi bloqueada: $blockReason"
                    } else {
                        "Não foi possível obter uma resposta do assistente no momento."
                    }
                    _messages.value = _messages.value + ChatMessage(
                        sender = ChatSender.ASSISTANT,
                        text = reasonMessage,
                        isError = true
                    )
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Erro desconhecido ao comunicar com Gemini"
                _errorMessage.value = errorMsg
                _messages.value = _messages.value + ChatMessage(
                    sender = ChatSender.ASSISTANT,
                    text = "Erro: $errorMsg",
                    isError = true
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun buildFinancialContextSummary(): String {
        return try {
            val loans = repository.loans.first()
            val receivables = repository.receivables.first()
            val payables = repository.payables.first()

            val activeLoans = loans.filter { it.status != ItemStatus.PAID }
            val totalLoansBalance = activeLoans.sumOf { it.remainingBalance }
            val totalInterest = activeLoans.sumOf { it.calculatedInterest }

            val activeReceivables = receivables.filter { it.status != ItemStatus.PAID }
            val totalReceivablesBalance = activeReceivables.sumOf { it.remainingBalance }

            val activePayables = payables.filter { it.status != ItemStatus.PAID }
            val totalPayablesBalance = activePayables.sumOf { it.remainingBalance }

            val overdueLoans = loans.count { it.status == ItemStatus.OVERDUE }
            val overdueReceivables = receivables.count { it.status == ItemStatus.OVERDUE }
            val overduePayables = payables.count { it.status == ItemStatus.OVERDUE }

            val netBalance = (totalLoansBalance + totalReceivablesBalance) - totalPayablesBalance

            """
            - Empréstimos a receber ativos: ${activeLoans.size} contratos | Saldo: ${CurrencyFormatter.format(totalLoansBalance)} (Juros calculados: ${CurrencyFormatter.format(totalInterest)})
            - Outros recebíveis/vendas a receber ativos: ${activeReceivables.size} | Saldo: ${CurrencyFormatter.format(totalReceivablesBalance)}
            - Total geral a receber: ${CurrencyFormatter.format(totalLoansBalance + totalReceivablesBalance)}
            - Dívidas e contas a pagar ativas: ${activePayables.size} | Saldo: ${CurrencyFormatter.format(totalPayablesBalance)}
            - Saldo líquido projetado: ${CurrencyFormatter.format(netBalance)}
            - Itens com status VENCIDO: $overdueLoans empréstimo(s), $overdueReceivables recebível(is), $overduePayables conta(s) a pagar
            """.trimIndent()
        } catch (e: Exception) {
            "Informações financeiras indisponíveis no momento."
        }
    }
}
