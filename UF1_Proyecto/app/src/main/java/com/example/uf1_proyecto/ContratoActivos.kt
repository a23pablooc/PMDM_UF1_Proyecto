package com.example.uf1_proyecto

import android.provider.BaseColumns

object ContratoActivos {
    const val NOMBRE_TABLA = "Activos"

    object Columnas : BaseColumns {
        const val _ID = "nombre"
        const val COLUMN_INICIO = "fechaInicio"
        const val COLUMN_FIN = "fechaFin"
        const val COLUMN_HORARIO = "horario"
    }
}