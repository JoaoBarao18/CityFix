package pt.isec.amov.a2024136885.cityfix.ui.screens.publico

import pt.isec.amov.a2024136885.cityfix.R

// Dados fictícios para testar o painel e a lista pública.
internal fun ocorrenciasExemplo(packageName: String): List<OcorrenciaPublicaUi> = listOf(
    OcorrenciaPublicaUi(
        categoria = "Iluminação pública",
        situation = "Avaria na lâmpada",
        status = "Aberto",
        pathImg = "android.resource://$packageName/${R.drawable.img_welcome}",
        id = "ocorrencia_003",
        descricao= "Um cadeeiro da via publica nao esta a fucnioarn",
        latitude = 38.736946,
        longitude = -8.898

    )
)
