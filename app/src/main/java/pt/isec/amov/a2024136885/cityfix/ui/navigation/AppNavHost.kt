// Liga os ecrãs e verifica se o visitante está dentro do
// raio de 10 km antes de abrir as ocorrências
package pt.isec.amov.a2024136885.cityfix.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import pt.isec.amov.a2024136885.cityfix.ui.screens.operator.ValidateUsersScreen
import pt.isec.amov.a2024136885.cityfix.ui.screens.profile.ProfileEditScreen
import pt.isec.amov.a2024136885.cityfix.ui.screens.publico.DetalheOcorrenciaPublicaContent
import pt.isec.amov.a2024136885.cityfix.ui.screens.publico.ListaOcorrenciasPublicas
import pt.isec.amov.a2024136885.cityfix.ui.screens.publico.PainelPublicoContent
import pt.isec.amov.a2024136885.cityfix.ui.screens.publico.ocorrenciasExemplo
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.AuthViewModel
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.AuthViewModelFactory
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.OperatorViewModel
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.OperatorViewModelFactory
import pt.isec.amov.a2024136885.cityfix.ui.screens.operator.ManageCategoriesScreen
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.CategoryViewModel
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.CategoryViewModelFactory
import pt.isec.amov.a2024136885.cityfix.data.repository.CategoryRepository
import pt.isec.amov.a2024136885.cityfix.util.NotificationHelper
import kotlin.math.round
import pt.isec.amov.a2024136885.cityfix.ui.screens.operador.DetalheOperacionalScreen
import pt.isec.amov.a2024136885.cityfix.ui.screens.operador.PainelOperacionalScreen
import pt.isec.amov.a2024136885.cityfix.ui.screens.municipe.CriarOcorrenciaRoute
import pt.isec.amov.a2024136885.cityfix.ui.screens.municipe.DetalheMinhaOcorrenciaRoute
import pt.isec.amov.a2024136885.cityfix.ui.screens.municipe.MinhasOcorrenciasRoute
import pt.isec.amov.a2024136885.cityfix.ui.screens.municipe.SugestaoImpacto
import pt.isec.amov.a2024136885.cityfix.ui.screens.profile.ProfileEditScreen
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.PainelOperacionalViewModel
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.AuthViewModel
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.AuthViewModelFactory
import pt.isec.amov.a2024136885.cityfix.util.NotificationHelper
import pt.isec.amov.a2024136885.cityfix.data.repository.OcorrenciaRespository
import kotlinx.coroutines.flow.catch
import pt.isec.amov.a2024136885.cityfix.ui.screens.publico.DetalhePublicoRoute
import pt.isec.amov.a2024136885.cityfix.ui.screens.publico.ListaPublicaRoute
import pt.isec.amov.a2024136885.cityfix.ui.screens.publico.PainelPublicoRoute
import pt.isec.amov.a2024136885.cityfix.ui.viewmodel.PainelPublicoViewModel

@Composable
fun AppNavHost() {
   val navController = rememberNavController()
   val context = LocalContext.current
   val painelPublicoViewModel: PainelPublicoViewModel = viewModel()
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
   val operador = authUiState.currentUser?.takeIf {
      authUiState.isLoginSuccessful && it.perfil == "operador" && it.validado
   }
   val municipe = authUiState.currentUser?.takeIf {
      authUiState.isLoginSuccessful && it.perfil == "municipe" && it.validado
   }

   LaunchedEffect(municipe?.uid) {
      val uid = municipe?.uid ?: return@LaunchedEffect
      OcorrenciaRespository().observarMinhas(uid)
         .catch { Log.w("CityFixProximidade", "Falha ao observar avisos", it) }
         .collect { ocorrencias ->
            ocorrencias.forEach { ocorrencia ->
               val instante = ocorrencia.proximidadeEm
               if (ocorrencia.estado == "em_resolucao" && instante != null) {
                  notificationHelper.mostrarProximidadeSeNova(uid, ocorrencia.id, instante)
               }
            }
         }
   }

   // Transição automática para o ecrã de desafio MFA quando gerado o código
   LaunchedEffect(authUiState.pendingMfaUid) {
      authUiState.pendingMfaUid?.let { uid ->
         navController.navigate(Route.MfaChallenge(uid = uid))
      }
   }

   // Transição para as ocorrências após validação completa de login + MFA
   LaunchedEffect(authUiState.isLoginSuccessful) {
      if (authUiState.isLoginSuccessful) {
         val destino = if (operador != null) Route.PainelOperacional else Route.OcorrenciasPublicas
         navController.navigate(destino) {
            popUpTo(Route.Entrada) { inclusive = true }
         }
      }
   }

   // Setup do OperatorViewModel
   val operatorViewModel: OperatorViewModel = viewModel(
      factory = OperatorViewModelFactory(userRepository)
   )
   val operatorUiState by operatorViewModel.uiState.collectAsState()

   // Setup do CategoryViewModel
   val categoryRepository = remember { CategoryRepository() }
   val categoryViewModel: CategoryViewModel = viewModel(
      factory = CategoryViewModelFactory(categoryRepository)
   )
   val categoryUiState by categoryViewModel.uiState.collectAsState()

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
            .addOnSuccessListener { location ->
               aCheckarLocalizacao = false
               if (location == null) {
                  Log.d("CityFixLocation", "Posicao indisponivel")
                  mensagemVisitor = R.string.visitor_location_unavailable
               } else {
                  Log.d("CityFixLocation", "Lat=${location.latitude}, Lon=${location.longitude}")

                  if (estaDentroRaio(longitude = location.longitude, latitude = location.latitude)) {
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
            var credenciais by remember { mutableStateOf<Pair<String, String>?>(null) }
            var notificacoesRecusadas by remember { mutableStateOf(false) }
            val permissaoNotificacoes = rememberLauncherForActivityResult(
               ActivityResultContracts.RequestPermission()
            ) { concedida ->
               val login = credenciais
               credenciais = null
               notificacoesRecusadas = !concedida
               if (concedida && login != null) authViewModel.initiateLogin(login.first, login.second)
            }
            LoginScreen(
               uiState = if (notificacoesRecusadas) {
                  authUiState.copy(errorMessage = stringResource(R.string.auth_notification_permission))
               } else authUiState,
               onLoginClick = { email, pass ->
                  notificacoesRecusadas = false
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                     ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                     PackageManager.PERMISSION_GRANTED
                  ) {
                     credenciais = email to pass
                     permissaoNotificacoes.launch(Manifest.permission.POST_NOTIFICATIONS)
                  } else {
                     authViewModel.initiateLogin(email, pass)
                  }
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

         composable<Route.ValidateUsers> {
            ValidateUsersScreen(
               uiState = operatorUiState,
               onApproveClick = { uid -> operatorViewModel.approveMunicipe(uid) },
               onBackClick = { navController.popBackStack() },
               onClearFeedback = { operatorViewModel.clearFeedback() }
            )
         }

         composable<Route.ManageCategories> {
            ManageCategoriesScreen(
               uiState = categoryUiState,
               onAddCategory = { nome -> categoryViewModel.addCategory(nome) },
               onEditCategory = { id, novoNome -> categoryViewModel.editCategory(id, novoNome) },
               onToggleCategoryStatus = { id, ativa -> categoryViewModel.toggleCategoryStatus(id, ativa) },
               onBackClick = { navController.popBackStack() },
               onClearFeedback = { categoryViewModel.clearFeedback() }
            )
         }

         composable<Route.OcorrenciasPublicas> {
            PainelPublicoContent(
               ocorrencias = ocorrenciasPublicas,
               currentUser = authUiState.currentUser,
               onVerTodas = { navController.navigate(Route.ListarOcorrenciasPublicas) },
               onAbrirOcorrencia = { id -> navController.navigate(Route.DetalheOcorrenciaPublica(id)) },
               onPerfilClick = { navController.navigate(Route.ProfileEdit) },
               onValidateUsersClick = { navController.navigate(Route.ValidateUsers) },
               onManageCategoriesClick = { navController.navigate(Route.ManageCategories) },
               onLogoutClick = {
                  if (authUiState.currentUser != null) {
                     authViewModel.logout()
                  }
                  navController.navigate(Route.Entrada) {
                     popUpTo(Route.Entrada) { inclusive = true }
                  }
               }
            )
         }

         composable<Route.ListarOcorrenciasPublicas> {
            ListaOcorrenciasPublicas(
               ocorrencias = ocorrenciasPublicas,
               onAbrirOcorrencia = { id ->
                  navController.navigate(Route.DetalheOcorrenciaPublica(id))
               },
               onVoltar = { navController.popBackStack() }
            )
         }

         composable<Route.DetalheOcorrenciaPublica> { entrada ->
            val rota = entrada.toRoute<Route.DetalheOcorrenciaPublica>()
            val ocorrencia = ocorrenciasPublicas.firstOrNull {
               it.id == rota.id
         composable<Route.PainelOperacional> {
            if (operador != null) {
               val painelViewModel: PainelOperacionalViewModel = viewModel()
               PainelOperacionalScreen(
                  operadorId = operador.uid,
                  viewModel = painelViewModel,
                  onAbrirOcorrencia = { id -> navController.navigate(Route.DetalheOperacional(id)) }
               )
            } else {
               LaunchedEffect(Unit) { navController.navigate(Route.Entrada) }
            }
         }

         composable<Route.DetalheOperacional> { entrada ->
            if (operador != null) {
               val painel = remember(entrada) { navController.getBackStackEntry(Route.PainelOperacional) }
               val painelViewModel: PainelOperacionalViewModel = viewModel(viewModelStoreOwner = painel)
               DetalheOperacionalScreen(
                  operadorId = operador.uid,
                  id = entrada.toRoute<Route.DetalheOperacional>().id,
                  viewModel = painelViewModel,
                  onVoltar = { navController.popBackStack() }
               )
            } else {
               LaunchedEffect(Unit) { navController.navigate(Route.Entrada) }
            }
         }

         composable <Route.OcorrenciasPublicas>{
            PainelPublicoRoute(
               vm = painelPublicoViewModel,
               onVerTodas = { navController.navigate(Route.ListarOcorrenciasPublicas) },
               onAbrir = { id -> navController.navigate(Route.DetalheOcorrenciaPublica(id)) },
               onCriar = municipe?.let { { navController.navigate(Route.CriarOcorrencia) } },
               onMinhas = municipe?.let { { navController.navigate(Route.MinhasOcorrencias) } },
               visitante = !authUiState.isLoginSuccessful
            )
            SugestaoImpacto(
               detImpactosAtiva = municipe?.detecaoImpactos == true,
               onAbrirOcorrencia = {
                  navController.navigate(Route.CriarOcorrencia)
               }
            )
         }

         composable<Route.MinhasOcorrencias> {
            if (municipe != null) {
               MinhasOcorrenciasRoute(
                  autorId = municipe.uid,
                  onAbrir = { id -> navController.navigate(Route.DetalheMinhaOcorrencia(id)) },
                  onVoltar = { navController.popBackStack() }
               )
            } else {
               LaunchedEffect(Unit) { navController.popBackStack() }
            }
         }

            if (ocorrencia != null) {
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
         composable<Route.DetalheMinhaOcorrencia> { entrada ->
            if (municipe != null) {
               DetalheMinhaOcorrenciaRoute(
                  autorId = municipe.uid,
                  id = entrada.toRoute<Route.DetalheMinhaOcorrencia>().id,
                  onVoltar = { navController.popBackStack() }
               )
            } else {
               LaunchedEffect(Unit) { navController.popBackStack() }
            }
         }

         composable<Route.CriarOcorrencia> {
            if (municipe != null) {
               CriarOcorrenciaRoute(autorId = municipe.uid)
            } else {
               LaunchedEffect(Unit) {
                  navController.popBackStack()
               }
            }
         }

         composable<Route.ListarOcorrenciasPublicas> {
            ListaPublicaRoute(
               vm = painelPublicoViewModel,
               onAbrir = { id -> navController.navigate(Route.DetalheOcorrenciaPublica(id)) },
               onVoltar = { navController.popBackStack() }
            )
         }

         composable<Route.DetalheOcorrenciaPublica> { entrada ->
            DetalhePublicoRoute(
               vm = painelPublicoViewModel,
               id = entrada.toRoute<Route.DetalheOcorrenciaPublica>().id,
               onVoltar = { navController.popBackStack() }
            )
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