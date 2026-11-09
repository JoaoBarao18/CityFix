package pt.isec.amov.a2024136885.cityfix.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable fun AppNavHost(){
   val navController = rememberNavController()

   Scaffold() { innerPadding ->
      NavHost(
         navController = navController,
         startDestination = Route.Entrada,
         modifier = Modifier.padding(innerPadding)
      ) {
         composable<Route.Entrada> {
            EntradaContent(
               onContinueAsVisitor = {
                  navController.navigate(Route.OcorrenciasPublicas)
               }
            )
         }
         composable <Route.OcorrenciasPublicas>{
            Text("Ocorrencias publicas")
         }
      }
   }
}

@Composable
fun EntradaContent(onContinueAsVisitor: () -> Unit){
   Column() {
      Text("CityFix")
      Button(onClick = onContinueAsVisitor) {
         Text("Continuar como visitante")
      }
   }
}