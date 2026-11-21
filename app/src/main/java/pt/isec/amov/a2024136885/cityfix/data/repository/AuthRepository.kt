package pt.isec.amov.a2024136885.cityfix.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pt.isec.amov.a2024136885.cityfix.data.model.User
import pt.isec.amov.a2024136885.cityfix.util.NotificationHelper
import kotlin.random.Random

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val notificationHelper: NotificationHelper? = null
) {

    val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Passo 1 do Login: Valida email e password.
     * Gera o código MFA, envia via Notificação Local e guarda expiração de 5 minutos.
     */
    suspend fun initiateLogin(email: String, password: String): Result<String> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val uid = authResult.user?.uid ?: return Result.failure(Exception("Utilizador não encontrado."))

            // Gerar código de 6 dígitos
            val mfaCode = String.format("%06d", Random.nextInt(100000, 999999))
            val expiresAt = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutos

            // Guardar temporariamente no Firestore
            firestore.collection("utilizadores")
                .document(uid)
                .update(
                    mapOf(
                        "mfaCode" to mfaCode,
                        "mfaExpiresAt" to expiresAt
                    )
                ).await()

            // Disparar notificação local
            notificationHelper?.showMfaCodeNotification(mfaCode)

            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Passo 2 do Login: Valida o código MFA de 6 dígitos introduzido pelo utilizador.
     */
    suspend fun verifyMfaCode(uid: String, inputCode: String): Result<Boolean> {
        return try {
            val doc = firestore.collection("utilizadores").document(uid).get().await()
            val user = doc.toObject(User::class.java)
                ?: return Result.failure(Exception("Utilizador não encontrado no sistema."))

            val currentTime = System.currentTimeMillis()

            if (user.mfaExpiresAt == null || currentTime > user.mfaExpiresAt) {
                return Result.failure(Exception("O código de validação expirou. Faça login novamente."))
            }

            if (user.mfaCode != inputCode.trim()) {
                return Result.failure(Exception("Código incorreto. Verifique a notificação."))
            }

            // Limpa o código para não poder ser reutilizado
            firestore.collection("utilizadores")
                .document(uid)
                .update(
                    mapOf(
                        "mfaCode" to null,
                        "mfaExpiresAt" to null
                    )
                ).await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Registo de Munícipe (Fica pendente de validação do NIF pela autarquia)
     */
    suspend fun registerMunicipe(
        nome: String,
        email: String,
        password: String,
        nif: String,
        telefone: String,
        latitude: Double,
        longitude: Double,
        detecaoImpactos: Boolean
    ): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Erro ao criar conta de autenticação."))

            val user = User(
                uid = firebaseUser.uid,
                nome = nome.trim(),
                email = email.trim(),
                nif = nif.trim(),
                telefone = telefone.trim(),
                perfil = "municipe",
                validado = false,
                latitude = latitude,
                longitude = longitude,
                detecaoImpactos = detecaoImpactos,
                criadoEm = System.currentTimeMillis()
            )

            firestore.collection("utilizadores")
                .document(firebaseUser.uid)
                .set(user)
                .await()

            firebaseUser.sendEmailVerification()

            Result.success(user)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Este e-mail já está registado."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Registo de Operador Municipal (Fica logo ativo segundo o enunciado)
     */
    suspend fun registerOperador(
        nome: String,
        email: String,
        password: String,
        nif: String,
        telefone: String
    ): Result<User> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Erro ao criar conta."))

            val user = User(
                uid = firebaseUser.uid,
                nome = nome.trim(),
                email = email.trim(),
                nif = nif.trim(),
                telefone = telefone.trim(),
                perfil = "operador",
                validado = true, // Operador fica logo ativo
                criadoEm = System.currentTimeMillis()
            )

            firestore.collection("utilizadores")
                .document(firebaseUser.uid)
                .set(user)
                .await()

            Result.success(user)
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Este e-mail já está registado."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUserProfile(): Result<User?> {
        val uid = currentUserId ?: return Result.success(null)
        return try {
            val snapshot = firestore.collection("utilizadores").document(uid).get().await()
            Result.success(snapshot.toObject(User::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }
}