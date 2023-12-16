package com.example.uf1_proyecto

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View.OnClickListener
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.children
import com.example.uf1_proyecto.databinding.DialogAddActiveMedBinding
import com.example.uf1_proyecto.databinding.TimePickerLayoutBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class AddActiveMedDialog(
    private val context: Context,
    private val listener: OnDataEnteredListener
) {

    interface OnDataEnteredListener {
        fun onDataEntered(medicamento: Medicamento)
    }

    private var _binding: DialogAddActiveMedBinding? =
        DialogAddActiveMedBinding.inflate(LayoutInflater.from(context))
    private val binding get() = _binding!!

    private var _pillboxViewModel: PillboxViewModel? = null
    private val pillboxViewModel get() = _pillboxViewModel!!

    private val inputCodNacional: EditText = binding.codNacional
    private val inputNombre: EditText = binding.nombre
    private val inputFavorite: CheckBox = binding.saveAsFavorite
    private val inputFechaInicio: TextView = binding.dateStart
    private val inputFechaFin: TextView = binding.dateEnd
    private var fichaTecnica: String? = null
    private var prospecto: String? = null

    private val alertDialog: AlertDialog = AlertDialog.Builder(context)
        .setView(binding.root)
        .setTitle(context.getString(R.string.add_medicament))
        .setPositiveButton(context.getString(R.string.accept), null)
        .setNegativeButton(context.getString(R.string.cancel), null)
        .create()

    init {
        _pillboxViewModel = PillboxViewModel.getInstance(context)

        alertDialog.setOnShowListener {
            setupPositiveButton()
        }

        addTimePicker(false)

        binding.btnSearch.setOnClickListener {
            if (inputCodNacional.text.isNullOrBlank()) {
                return@setOnClickListener
            }

            val searchingToast = Toast.makeText(
                context, context.getString(R.string.searching), Toast.LENGTH_LONG
            ).also { it.show() }

            GlobalScope.launch(Dispatchers.Main) {
                val codNacional = inputCodNacional.text.toString().split(".")[0].trim()

                val medicamento = pillboxViewModel.searchMedicamento(codNacional)

                withContext(Dispatchers.Main) {
                    searchingToast.cancel()
                    if (medicamento == null) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.codNacional_not_found),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        inputNombre.setText(medicamento.nombre)
                        fichaTecnica = medicamento.fichaTecnica
                        prospecto = medicamento.prospecto
                    }
                }
            }

        }

        binding.btnHelp.setOnClickListener {
            Toast.makeText(context, context.getString(R.string.codNacional_help), Toast.LENGTH_LONG)
                .show()
        }

        binding.btnAddTimer
            .setOnClickListener { addTimePicker(true) }

        val addDatePickerListener = OnClickListener { view ->
            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, monthOfYear, dayOfMonth ->
                    when (view.id) {
                        R.id.btn_date_picker1 -> inputFechaInicio.text =
                            DateTimeUtils.millisToDate(
                                DateTimeUtils.createDate(
                                    year, monthOfYear, dayOfMonth
                                )
                            )

                        R.id.btn_date_picker2 -> inputFechaFin.text = DateTimeUtils.millisToDate(
                            DateTimeUtils.createDate(
                                year, monthOfYear, dayOfMonth
                            )
                        )
                    }
                },
                // Establece la fecha actual como predeterminada
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH),
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            )

            datePickerDialog.show()
        }

        binding.btnDatePicker1.setOnClickListener(addDatePickerListener)
        binding.btnDatePicker2.setOnClickListener(addDatePickerListener)

        inputFechaInicio.text = DateTimeUtils.getTodayAsString()
        inputFechaFin.text = DateTimeUtils.getTodayAsString()

    }

    private fun validateForm(): Boolean {
        if (inputNombre.text.isNullOrBlank()) {
            Toast.makeText(context, context.getString(R.string.empty_name), Toast.LENGTH_LONG)
                .show()
            return false
        }

        if ((DateTimeUtils.dateToMillis(inputFechaInicio.text.toString()) > DateTimeUtils.dateToMillis(
                inputFechaFin.text.toString()
            ) || (DateTimeUtils.dateToMillis(inputFechaFin.text.toString()) < DateTimeUtils.getTodayAsMillis()))
        ) {
            Toast.makeText(
                context, context.getString(R.string.invalid_date), Toast.LENGTH_LONG
            ).show()
            return false
        }

        if (getSchedule().isEmpty()) {
            Toast.makeText(
                context, context.getString(R.string.no_schedule), Toast.LENGTH_LONG
            ).show()
            return false
        }

        if (getSchedule().size < binding.scheduleLayout.childCount) {
            Toast.makeText(
                context, context.getString(R.string.invalid_schedule), Toast.LENGTH_LONG
            ).show()
            return false
        }

        return true
    }

    private fun getSchedule(): Set<Long> {
        val horario = sortedSetOf<Long>()

        for (child in (binding.scheduleLayout.children)) {
            val time = child.findViewById<TextView>(R.id.timer_hour).text.toString()
            horario.add(DateTimeUtils.timeToMillis(time))
        }

        return horario
    }

    private fun addTimePicker(showAfterAdd: Boolean) {
        val timerBinding = TimePickerLayoutBinding.inflate(LayoutInflater.from(context), binding.scheduleLayout, true)

        timerBinding.timerHour.text = DateTimeUtils.millisToTime(-3600000) // 0:00:00 - 12:00:00 AM

        timerBinding.timePicker.setOnClickListener {
            val timePickerDialog = TimePickerDialog(
                context, { _, hourOfDay, minute ->
                    val time = DateTimeUtils.millisToTime(
                        DateTimeUtils.createTime(
                            hourOfDay, minute
                        )
                    )
                    timerBinding.timerHour.text = time
                },
                // Establece las 00:00 como hora predeterminada
                0,
                0,
                DateTimeUtils.is24TimeFormat(context)
            )

            timePickerDialog.show()
        }

        timerBinding.deleteTimer.setOnClickListener {
            binding.scheduleLayout.removeView(timerBinding.root)
        }

        if (showAfterAdd) {
            timerBinding.timePicker.performClick()
        }
    }

    private fun setupPositiveButton() {
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (!validateForm()) {
                return@setOnClickListener
            }

            val nombre = inputNombre.text.toString()
            var codNacional = inputCodNacional.text.toString().split(".")[0].trim()
            if (codNacional.isBlank()) {
                codNacional = "-1"
            }
            val fichaTecnica = fichaTecnica
            val prospecto = prospecto
            val fechaInicio = DateTimeUtils.dateToMillis(inputFechaInicio.text.toString())
            val fechaFin = DateTimeUtils.dateToMillis(inputFechaFin.text.toString())
            val horario = getSchedule()
            val isFavorite = inputFavorite.isChecked

            alertDialog.dismiss()
            val builder = MedicamentoBuilder()
                .setNombre(nombre)
                .setCodNacional(codNacional.toInt())
                .setFichaTecnica(fichaTecnica ?: "")
                .setProspecto(prospecto ?: "")
                .setFechaInicio(fechaInicio)
                .setFechaFin(fechaFin)
                .setHorario(horario)
                .setFavorito(isFavorite)
            listener.onDataEntered(builder.build())
        }
    }

    fun show() {
        alertDialog.show()
    }

}