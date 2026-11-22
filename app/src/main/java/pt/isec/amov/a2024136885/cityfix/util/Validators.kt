package pt.isec.amov.a2024136885.cityfix.util

import android.util.Patterns
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object Validators {

    // Domínios autorizados para contas de Operador Municipal (Decidido por nos)
    val MUNICIPAL_OPERATOR_DOMAINS = setOf(
        "@cityfix.pt", //dominio para testes do projeto
        "cpcoimbra@psp.pt", //Comando Distrital de coimbra da psp
        "div.coimbra@psp.pt", // Divisao Policial e esquadras de coimbra
        "cbresqtr.coimbra@psp.pt", //Esquadra de transito e Coimbra
        "policia.municipal@cm-coimbra.pt", //Policia Municipal de Coimbra
        "cbs@cm-coimbra.pt", // Bombeiros sapadores de coimbra
        "ahbomvolcoimbra@gmail.com", //Bombeiros voluntarios de coimbra
        "delegacao.centro@inem.pt" //Delegacao Inem Centro
    )

    private val phoneRegex = Regex("^\\d{9}$")
    private val nifRegex = Regex("^\\d{9}$")

    fun validateName(name: String): String? {
        return if (name.isBlank()) "O nome é obrigatório." else null
    }

    // Usado para Munícipe (aceita qualquer e-mail válido, incluindo @gmail.com)
    fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> "O e-mail é obrigatório."
            !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() -> "E-mail inválido."
            else -> null
        }
    }

    // Usado apenas para Operador Municipal (exige domínio da autarquia / entidade autorizada)
    fun validateOperatorEmail(
        email: String,
        allowedDomains: Set<String> = MUNICIPAL_OPERATOR_DOMAINS
    ): String? {
        val baseError = validateEmail(email)
        if (baseError != null) return baseError

        val normalizedEmail = email.trim().lowercase()
        val isAuthorizedDomain = allowedDomains.any { domain ->
            val normalizedDomain = if (domain.startsWith("@")) {
                domain.lowercase()
            } else {
                "@${domain.lowercase()}"
            }
            normalizedEmail.endsWith(normalizedDomain)
        }

        return if (!isAuthorizedDomain) {
            "O e-mail do operador tem de pertencer a um domínio autorizado (${allowedDomains.joinToString(", ")})."
        } else {
            null
        }
    }

    fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "A palavra-passe é obrigatória."
            password.length < 8 -> "A palavra-passe deve ter pelo menos 8 caracteres."
            !password.any { it.isUpperCase() } -> "A palavra-passe deve conter pelo menos uma letra maiúscula."
            !password.any { it.isLowerCase() } -> "A palavra-passe deve conter pelo menos uma letra minúscula."
            !password.any { it.isDigit() } -> "A palavra-passe deve conter pelo menos um número."
            !password.any { !it.isLetterOrDigit() } -> "A palavra-passe deve conter pelo menos um caracter especial (ex: !@#$%)."
            else -> null
        }
    }

    fun validatePhone(phone: String): String? {
        val digitsOnly = phone.trim().replace(" ", "")
        return if (!phoneRegex.matches(digitsOnly)) {
            "Introduza um número de telefone válido (9 dígitos)."
        } else {
            null
        }
    }

    fun validateNifFormat(nif: String): String? {
        val trimmed = nif.trim()
        return if (!nifRegex.matches(trimmed)) {
            "O NIF deve conter exatamente 9 dígitos."
        } else {
            null
        }
    }

    /**
     * Verifica na coleção `utilizadores` se já existe algum registo com o mesmo NIF.
     * Retorna mensagem de erro se já existir, ou null se estiver disponível.
     */
    suspend fun validateUniqueNif(
        nif: String,
        firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    ): String? {
        val formatError = validateNifFormat(nif)
        if (formatError != null) return formatError

        val snapshot = firestore.collection("utilizadores")
            .whereEqualTo("nif", nif.trim())
            .limit(1)
            .get()
            .await()

        return if (!snapshot.isEmpty) {
            "Este NIF já se encontra registado no sistema."
        } else {
            null
        }
    }
}