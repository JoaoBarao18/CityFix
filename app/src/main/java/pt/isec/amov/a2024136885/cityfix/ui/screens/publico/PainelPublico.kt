package pt.isec.amov.a2024136885.cityfix.ui.screens.publico

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import okio.Path
import pt.isec.amov.a2024136885.cityfix.ui.theme.CityFIxTheme
import pt.isec.amov.a2024136885.cityfix.R

@Composable
fun CartaoEstatistica(
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier
){
   Card(modifier = modifier){
       Column(
           modifier = Modifier.padding(16.dp)
       ) {
           Text(text = titulo, style = MaterialTheme.typography.titleLarge)
           Text(text = valor, style = MaterialTheme.typography.bodySmall)
       }
   }
}


@Composable
fun Ocorrencia(situation: String,
               categoria: String,
               status: String,
               pathImg: String? = null
){
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically){
            Box(modifier = Modifier.size(64.dp)){
                if(pathImg.isNullOrBlank()){
                    Text("Sem Foto", modifier = Modifier.align(Alignment.Center))
                }else{
                    AsyncImage(
                        model = pathImg,
                        onError = { erro ->
                            Log.e("CityFixMiniatura", "Falha ao carregar a imagem", erro.result.throwable)
                        },
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = situation, style = MaterialTheme.typography.titleMedium)
                Text(text = categoria, style = MaterialTheme.typography.bodyMedium)
                Text(text = status, style = MaterialTheme.typography.labelMedium)
            }
        }

    }
}

data class OcorrenciaPublicaUi(
    val categoria: String,
    val situation: String,
    val status: String,
    val pathImg: String? = null
)

@Composable
fun PainelPublicoContent(ocorrencias: List<OcorrenciaPublicaUi>, onVerTodas: () -> Unit = {}){
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    )
    {
        Text(
            text = "CityFix",
            style = MaterialTheme.typography.headlineSmall
        )

        Row {
            CartaoEstatistica(
                titulo = "Tempo Médio",
                valor = "2,4 dias",
                modifier = Modifier.weight(1f)
            )
            CartaoEstatistica(
                titulo = "Ocorrencias",
                valor = "1 248",
                modifier = Modifier.weight(1f)
            )
            CartaoEstatistica(
                titulo = "Ativas",
                valor = "18%",
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ){
            Text(
                text = "Na sua cidade",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onVerTodas) {
                Text("Ver todas")
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ocorrencias.forEach { ocorrencia ->
                Ocorrencia(
                    situation = ocorrencia.situation,
                    categoria = ocorrencia.categoria,
                    status = ocorrencia.status,
                    pathImg = ocorrencia.pathImg
                )

            }
        }
    }
}



@Preview(showBackground = true)
@Composable
private fun CartaoEstatisitcaPreview(){
    CityFIxTheme(dynamicColor = false) {
        val packageName = LocalContext.current.packageName
        //CartaoEstatistica(titulo = "Tempo médio", valor = "2,4 dias")
        //PainelPublicoContent()
        //Ocorrencia("Avaria", "Iluminçaao","Aberto", pathImg = "android.resource://$packageName/${R.drawable.img_welcome}")

        PainelPublicoContent(
            ocorrencias = listOf(
                OcorrenciaPublicaUi(
                    categoria = "Iluminação pública",
                    situation = "Avaria",
                    status = "Aberto",
                    pathImg = "android.resource://$packageName/${R.drawable.img_welcome}"
                ),
                OcorrenciaPublicaUi(
                    categoria = "Vias públicas",
                    situation = "Dano",
                    status = "Em resolução"
                )
            )
        )
    }
}