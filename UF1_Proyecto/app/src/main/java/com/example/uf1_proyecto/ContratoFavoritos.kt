package com.example.uf1_proyecto

import android.provider.BaseColumns

object ContratoFavoritos {
    const val NOMBRE_TABLA = "favoritos"

    object Columnas : BaseColumns {
        const val COLUMN_COD_NACIONAL = "codNacional"
    }
}