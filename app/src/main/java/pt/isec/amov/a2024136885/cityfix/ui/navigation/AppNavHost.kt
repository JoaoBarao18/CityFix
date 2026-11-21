// Liga os ecrãs e verifica se o visitante está dentro do
// raio de 10 km antes de abrir as ocorrências
package pt.isec.amov.a2024136885.cityfix.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import pt.isec.amov.a2024136885.cityfix.R
import pt.isec.amov.a2024136885.cityfix.data.repository.AuthRepository
import pt.isec.amov.a2024136885.cityfix.data.repository.UserRepository
import pt.isec.amov.a2024136885.cityfix.domain.location.estaDentroRaio
import pt.isec.amov.a2024136885.cityfix.ui.screens.auth.LoginScreen
import pt.isec.amov.a2024136885.cityfix.ui.screens.auth.MfaChallengeScreen
import pt.isec.amov.a2024136885.cityfix.ui.screens.auth.RegisterScreen
import pt.isec.amov.a2024136885.cityfix.ui.screens.municipe.CriarOcorrenciaRoute
import pt.isec.amov.a2024136885.cityfix.ui.screens.municipe.SugestaoImpacto
import pt.isec.amov.a2024136885.cityfix.ui.screens.profile.ProfileEditScreen
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.AuthViewModel
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.AuthViewModelFactory
import pt.isec.amov.a2024136885.cityfix.util.NotificationHelper
import pt.isec.amov.a2024136885.cityfix.ui.screens.publico.DetalheOcorrenciaPublicaContent
import pt.isec.amov.a2024136885.cityfix.ui.screens.publico.ListaOcorrenciasPublicas
import pt.isec.amov.a2024136885.cityfix.ui.screens.publico.PainelPublicoContent
import pt.isec.amov.a2024136885.cityfix.ui.screens.publico.ocorrenciasExemplo
import kotlin.math.round

@Composable
fun AppNavHost() {
   val navController = rememberNavController()
   val context = LocalContext.current
   val ocorrenciasPublicas = ocorrenciasExemplo(context.packageName)
   var aCheckarLocalizacao by remember { mutableStateOf(false) }
   var mensagemVisitor by remember { mutableStateOf<Int?>(null) }
   val localCliente = remember(context) {
      LocationServices.getFusedLocationProviderClient(context)
   }

   // Setup do AuthViewModel preservando os teus repositórios
   val notificationHelper = remember(context) { NotificationHelper(context) }
   val authRepository = remember(notificationHelper) { AuthRepository(notificationHelper = notificationHelper) }
   val userRepository = remember { UserRepository() }
   val authViewModel: AuthViewModel = viewModel(
      factory = AuthViewModelFactory(authRepository, userRepository)
   )
   val authUiState by authViewModel.uiState.collectAsState()
   val utilizador = authUiState.currentUser
   val municipeValidado = authUiState.isLoginSuccessful &&
      utilizador?.perfil == "municipe" && utilizador.validado

   // Transição automática para o ecrã de desafio MFA quando gerado o código
   LaunchedEffect(authUiState.pendingMfaUid) {
      authUiState.pendingMfaUid?.let { uid ->
         navController.navigate(Route.MfaChallenge(uid = uid))
      }
   }

   // Transição para as ocorrências após validação completa de login + MFA
   LaunchedEffect(authUiState.isLoginSuccessful) {
      if (authUiState.isLoginSuccessful) {
         navController.navigate(Route.OcorrenciasPublicas) {
            popUpTo(Route.Entrada) { inclusive = true }
         }
      }
   }

   val locationpermission = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions()
   ) { permissions ->
      val percisa = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
      val aprox = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

      val resultado = when {
         percisa -> "Percisa"
         aprox -> "Aproximada"
         else -> "Sem permisao"
      }

      Log.d("CityFixLocation", resultado)

      if (
         ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
         ) == PackageManager.PERMISSION_GRANTED ||
         ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
         ) == PackageManager.PERMISSION_GRANTED
      ) {
         val prioridade = if (percisa) {
            Priority.PRIORITY_HIGH_ACCURACY
         } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
         }
         aCheckarLocalizacao = true
         localCliente.getCurrentLocation(prioridade, null)
            .addOnSuccessListener {
               location ->
               aCheckarLocalizacao = false
               if (location == null) {
                  Log.d("CityFixLocation", "Posicao indisponivel")
                  mensagemVisitor = R.string.visitor_location_unavailable
               } else {
                  Log.d("CityFixLocation", "Lat=${location.latitude}, Lon=${location.longitude}")

                  if(estaDentroRaio(longitude = location.longitude, latitude = location.latitude)){
                     navController.navigate(Route.OcorrenciasPublicas)
                  } else {
                     mensagemVisitor = R.string.visitor_outside_radius
                  }
               }
            }.addOnFailureListener { erro ->
               aCheckarLocalizacao = false
               Log.e("CityFixLocation", "Falha ao obter a posição", erro)
               mensagemVisitor = R.string.visitor_location_unavailable
            }
      } else {
         mensagemVisitor = R.string.visitor_location_permission_denied
      }
   }

   Scaffold { innerPadding ->
      NavHost(
         navController = navController,
         startDestination = Route.Entrada,
         modifier = Modifier.padding(innerPadding)
      ) {
         composable<Route.Entrada> {
            EntradaContent(
               mensagemVisitor = mensagemVisitor,
               aChecarLocalizacao = aCheckarLocalizacao,
               onContinueAsVisitor = {
                  mensagemVisitor = null
                  locationpermission.launch(
                     arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                     )
                  )
               },
               onNavigateToLogin = {
                  authViewModel.clearMessages()
                  navController.navigate(Route.Login)
               }
            )
         }

         composable<Route.Login> {
            LoginScreen(
               uiState = authUiState,
               onLoginClick = { email, pass ->
                  authViewModel.initiateLogin(email, pass)
               },
               onNavigateToRegister = {
                  authViewModel.clearMessages()
                  navController.navigate(Route.Register)
               },
               onForgotPasswordClick = { email ->
                  authViewModel.sendPasswordReset(email)
               },
               onAnonymousClick = {
                  navController.popBackStack(Route.Entrada, inclusive = false)
               },
               onClearError = {
                  authViewModel.clearMessages()
               }
            )
         }

         composable<Route.MfaChallenge> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.MfaChallenge>()
            MfaChallengeScreen(
               uid = args.uid,
               uiState = authUiState,
               onVerifyClick = { uid, code ->
                  authViewModel.submitMfaCode(uid, code)
               },
               onCancelClick = {
                  authViewModel.clearMessages()
                  navController.popBackStack()
               }
            )
         }

         composable<Route.Register> {
            RegisterScreen(
               uiState = authUiState,
               onRegisterMunicipe = { nome, email, pass, nif, tel, lat, lng, impactos ->
                  authViewModel.registerMunicipe(nome, email, pass, nif, tel, lat, lng, impactos)
               },
               onRegisterOperador = { nome, email, pass, nif, tel ->
                  authViewModel.registerOperador(nome, email, pass, nif, tel)
               },
               onNavigateBack = {
                  authViewModel.clearMessages()
                  navController.popBackStack()
               },
               onClearError = {
                  authViewModel.clearMessages()
               }
            )
         }

         composable<Route.ProfileEdit> {
            LaunchedEffect(Unit) {
               authViewModel.fetchCurrentUser()
            }
            ProfileEditScreen(
               uiState = authUiState,
               onSaveClick = { novoTelefone, novosImpactos ->
                  authViewModel.updateProfile(
                     telefone = novoTelefone,
                     fotoUrl = null,
                     impactos = novosImpactos
                  )
               },
               onBackClick = {
                  authViewModel.clearMessages()
                  navController.popBackStack()
               }
            )
         }

         composable <Route.OcorrenciasPublicas>{
            PainelPublicoContent(
               ocorrencias = ocorrenciasPublicas,
               onVerTodas = {navController.navigate(Route.ListarOcorrenciasPublicas)},
               onAbrirOcorrencia = {id ->  navController.navigate(Route.DetalheOcorrenciaPublica(id)) }
            )
            SugestaoImpacto(
               detImpactosAtiva = municipeValidado && utilizador?.detecaoImpactos == true,
               onAbrirOcorrencia = {
                  navController.navigate(Route.CriarOcorrencia)
               }
            )
         }

         composable<Route.CriarOcorrencia> {
            if (municipeValidado) {
               CriarOcorrenciaRoute()
            } else {
               LaunchedEffect(Unit) {
                  navController.popBackStack()
               }
            }
         }

         composable<Route.ListarOcorrenciasPublicas>{
            ListaOcorrenciasPublicas(
               ocorrencias = ocorrenciasPublicas,
               onAbrirOcorrencia = { id ->
                  navController.navigate(Route.DetalheOcorrenciaPublica(id))
               },
               onVoltar = { navController.popBackStack() }
            )
         }

         composable<Route.DetalheOcorrenciaPublica>{
            entrada ->
            val rota = entrada.toRoute<Route.DetalheOcorrenciaPublica>()
            val ocorrencia = ocorrenciasPublicas.firstOrNull(){
               it.id == rota.id
            }

            if(ocorrencia != null){
               DetalheOcorrenciaPublicaContent(
                  categoria = ocorrencia.categoria,
                  situacao = ocorrencia.situation,
                  status = ocorrencia.status,
                  descricao = ocorrencia.descricao,
                  latitudeAproximada = round(ocorrencia.latitude * 10) / 10,
                  longitudeAproximada = round(ocorrencia.longitude * 10) / 10,
                  pathImg = ocorrencia.pathImg,
                  pathVideo = ocorrencia.pathVideo,
                  onVoltar = { navController.popBackStack() },
               )
            }
         }
      }
   }
}

@Composable
fun EntradaContent(
   mensagemVisitor: Int?,
   aChecarLocalizacao: Boolean,
   onContinueAsVisitor: () -> Unit,
   onNavigateToLogin: () -> Unit
) {
   Column(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally
   ) {
      Text(
         text = "CityFix",
         style = MaterialTheme.typography.headlineLarge,
         color = MaterialTheme.colorScheme.primary,
         fontWeight = FontWeight.Bold
      )
      Card(
         modifier = Modifier.fillMaxWidth(),
         colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
         )
      ) {
         Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
         ) {
            Image(
               painter = painterResource(R.drawable.img_welcome),
               contentDescription = null,
               modifier = Modifier.size(80.dp)
            )
            Text(
               text = stringResource(R.string.welcome_description),
               style = MaterialTheme.typography.titleMedium,
               modifier = Modifier.weight(1f)
            )
         }
      }

      Button(
         onClick = onContinueAsVisitor,
         enabled = !aChecarLocalizacao,
         modifier = Modifier.fillMaxWidth()
      ) {
         Text(
            stringResource(
               if (aChecarLocalizacao) {
                  R.string.checking_location
               } else {
                  R.string.continue_as_visitor
               }
            )
         )
      }

      OutlinedButton(
         onClick = onNavigateToLogin,
         modifier = Modifier.fillMaxWidth()
      ) {
         Text("Sign In / Register")
      }

      Text(
         text = stringResource(R.string.visitor_radius_note),
         style = MaterialTheme.typography.bodySmall,
         color = MaterialTheme.colorScheme.onSurfaceVariant,
         textAlign = TextAlign.Center,
         modifier = Modifier.fillMaxWidth()
      )

      if (mensagemVisitor != null) {
         Text(stringResource(mensagemVisitor))
      }
   }
}