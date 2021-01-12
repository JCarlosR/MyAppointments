package com.programacionymas.myappointments.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.programacionymas.myappointments.R
import com.programacionymas.myappointments.io.ApiService
import com.programacionymas.myappointments.io.response.SimpleResponse
import com.programacionymas.myappointments.model.Doctor
import com.programacionymas.myappointments.model.Schedule
import com.programacionymas.myappointments.model.Specialty
import com.programacionymas.myappointments.util.PreferenceHelper
import com.programacionymas.myappointments.util.PreferenceHelper.get
import com.programacionymas.myappointments.util.toast
import kotlinx.android.synthetic.main.activity_create_appointment.*
import kotlinx.android.synthetic.main.card_view_step_one.*
import kotlinx.android.synthetic.main.card_view_step_three.*
import kotlinx.android.synthetic.main.card_view_step_two.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList

class CreateAppointmentActivity : AppCompatActivity() {

    private val apiService: ApiService by lazy {
        ApiService.create()
    }

    private val preferences by lazy {
        PreferenceHelper.defaultPrefs(this)
    }

    private val selectedCalendar = Calendar.getInstance()
    private var selectedTimeRadioBtn: RadioButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_appointment)

        btnNext.setOnClickListener {
            if (etDescription.text.toString().length < 3) {
                etDescription.error = getString(R.string.validate_appointment_description)
            } else {
                // continue to step 2
                cvStep1.visibility = View.GONE
                cvStep2.visibility = View.VISIBLE
            }
        }

        btnNext2.setOnClickListener {
            when {
                etScheduledDate.text.toString().isEmpty() ->
                    etScheduledDate.error = getString(R.string.validate_appointment_date)

                selectedTimeRadioBtn == null ->
                    Snackbar.make(createAppointmentLinearLayout,
                        R.string.validate_appointment_time, Snackbar.LENGTH_SHORT).show()

                else -> {
                    // continue to step 3
                    showAppointmentDataToConfirm()
                    cvStep2.visibility = View.GONE
                    cvStep3.visibility = View.VISIBLE
                }
            }
        }

        btnConfirmAppointment.setOnClickListener {
            performStoreAppointment()
        }

        loadSpecialties()
        listenSpecialtyChanges()
        listenDoctorAndDateChanges()
    }

    private fun performStoreAppointment() {
        btnConfirmAppointment.isClickable = false

        val jwt = preferences["jwt", ""]
        val authHeader = "Bearer $jwt"
        val description = tvConfirmDescription.text.toString()
        val specialty = spinnerSpecialties.selectedItem as Specialty
        val doctor = spinnerDoctors.selectedItem as Doctor
        val scheduledDate = tvConfirmDate.text.toString()
        val scheduledTime = tvConfirmTime.text.toString()
        val type = tvConfirmType.text.toString()

        val call = apiService.storeAppointment(
            authHeader, description,
            specialty.id, doctor.id,
            scheduledDate, scheduledTime,
            type
        )
        call.enqueue(object: Callback<SimpleResponse> {
            override fun onFailure(call: Call<SimpleResponse>, t: Throwable) {
                toast(t.localizedMessage)
                btnConfirmAppointment.isClickable = true
            }

            override fun onResponse(call: Call<SimpleResponse>, response: Response<SimpleResponse>) {
                if (response.isSuccessful) {
                    toast(getString(R.string.create_appointment_success))
                    finish()
                } else {
                    toast(getString(R.string.create_appointment_error))
                    btnConfirmAppointment.isClickable = true
                }
            }
        })
    }

    private fun listenDoctorAndDateChanges() {
        // doctors
        spinnerDoctors.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(adapter: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val doctor = adapter?.getItemAtPosition(position) as Doctor
                loadHours(doctor.id, etScheduledDate.text.toString())
            }
        }

        // scheduled date
        etScheduledDate.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                val doctor = spinnerDoctors.selectedItem as Doctor
                loadHours(doctor.id, etScheduledDate.text.toString())
            }

        })
    }

    private fun loadHours(doctorId: Int, date: String) {
        if (date.isEmpty()) {
            return
        }

        val call = apiService.getHours(doctorId, date)
        call.enqueue(object: Callback<Schedule> {
            override fun onFailure(call: Call<Schedule>, t: Throwable) {
                Toast.makeText(this@CreateAppointmentActivity, getString(R.string.error_loading_hours), Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<Schedule>, response: Response<Schedule>) {
                if (response.isSuccessful) {
                    val schedule = response.body()
                    // Toast.makeText(this@CreateAppointmentActivity, "morning: ${schedule?.morning?.size}, afternoon: ${schedule?.afternoon?.size}", Toast.LENGTH_SHORT).show()

                    // val hours = arrayOf("3:00 PM", "3:30 PM", "4:00 PM", "4:30 PM")
                    schedule?.let {
                        tvSelectDoctorAndDate.visibility = View.GONE

                        val intervals = it.morning + it.afternoon
                        val hours = ArrayList<String>()
                        intervals.forEach { interval ->
                            hours.add(interval.start)
                        }
                        displayIntervalRadios(hours)
                    }
                }
            }

        })
        // Toast.makeText(this, "doctor: $doctorId, date: $date", Toast.LENGTH_SHORT).show()
    }

    private fun loadSpecialties() {
        val call = apiService.getSpecialties()
        call.enqueue(object: Callback<ArrayList<Specialty>> {
            override fun onFailure(call: Call<ArrayList<Specialty>>, t: Throwable) {
                Toast.makeText(this@CreateAppointmentActivity, getString(R.string.error_loading_specialties), Toast.LENGTH_SHORT).show()
                finish()
            }

            override fun onResponse(call: Call<ArrayList<Specialty>>, response: Response<ArrayList<Specialty>>) {
                if (response.isSuccessful) { // [200...300)
                    response.body()?.let {
                        val specialties = it.toMutableList()
                        spinnerSpecialties.adapter = ArrayAdapter(this@CreateAppointmentActivity, android.R.layout.simple_list_item_1, specialties)
                    }

                }
            }
        })
    }

    private fun listenSpecialtyChanges() {
        spinnerSpecialties.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {

            }

            override fun onItemSelected(adapter: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val specialty = adapter?.getItemAtPosition(position) as Specialty
                loadDoctors(specialty.id)
            }
        }
    }

    private fun loadDoctors(specialtyId: Int) {
        val call = apiService.getDoctors(specialtyId)
        call.enqueue(object: Callback<ArrayList<Doctor>> {
            override fun onFailure(call: Call<ArrayList<Doctor>>, t: Throwable) {
                Toast.makeText(this@CreateAppointmentActivity, getString(R.string.error_loading_doctors), Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<ArrayList<Doctor>>, response: Response<ArrayList<Doctor>>) {
                if (response.isSuccessful) { // [200...300)
                    response.body()?.let {
                        val doctors = it.toMutableList()
                        spinnerDoctors.adapter = ArrayAdapter(this@CreateAppointmentActivity, android.R.layout.simple_list_item_1, doctors)
                    }
                }
            }
        })
    }

    private fun showAppointmentDataToConfirm() {
        tvConfirmDescription.text = etDescription.text.toString()
        tvConfirmSpecialty.text = spinnerSpecialties.selectedItem.toString()

        val selectedRadioBtnId = radioGroupType.checkedRadioButtonId
        val selectedRadioType = radioGroupType.findViewById<RadioButton>(selectedRadioBtnId)

        tvConfirmType.text = selectedRadioType.text.toString()

        tvConfirmDoctorName.text = spinnerDoctors.selectedItem.toString()
        tvConfirmDate.text = etScheduledDate.text.toString()
        tvConfirmTime.text = selectedTimeRadioBtn?.text.toString()
    }

    fun onClickScheduledDate(v: View?) {
        val year = selectedCalendar.get(Calendar.YEAR)
        val month = selectedCalendar.get(Calendar.MONTH)
        val dayOfMonth = selectedCalendar.get(Calendar.DAY_OF_MONTH)

        val listener = DatePickerDialog.OnDateSetListener { datePicker, y, m, d ->
            // Toast.makeText(this, "$y-$m-$d", Toast.LENGTH_SHORT).show()
            selectedCalendar.set(y, m, d)

            etScheduledDate.setText(
                resources.getString(
                    R.string.date_format,
                    y,
                    (m+1).twoDigits(),
                    d.twoDigits()
                )
            )
            etScheduledDate.error = null
        }

        // new dialog
        val datePickerDialog = DatePickerDialog(this, listener, year, month, dayOfMonth)

        // set limits
        val datePicker = datePickerDialog.datePicker
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        datePicker.minDate = calendar.timeInMillis // +1
        calendar.add(Calendar.DAY_OF_MONTH, 29)
        datePicker.maxDate = calendar.timeInMillis // +30

        // show dialog
        datePickerDialog.show()
    }

    private fun displayIntervalRadios(hours: ArrayList<String>) {
        selectedTimeRadioBtn = null
        radioGroupLeft.removeAllViews()
        radioGroupRight.removeAllViews()

        if (hours.isEmpty()) {
            tvNotAvailableHours.visibility = View.VISIBLE
            return
        }

        tvNotAvailableHours.visibility = View.GONE

        // val hours = arrayOf("3:00 PM", "3:30 PM", "4:00 PM", "4:30 PM")
        var goToLeft = true

        hours.forEach {
            val radioButton = RadioButton(this)
            radioButton.id = View.generateViewId()
            radioButton.text = it

            radioButton.setOnClickListener { view ->
                selectedTimeRadioBtn?.isChecked = false

                selectedTimeRadioBtn = view as RadioButton?
                selectedTimeRadioBtn?.isChecked = true
            }

            if (goToLeft)
                radioGroupLeft.addView(radioButton)
            else
                radioGroupRight.addView(radioButton)
            goToLeft = !goToLeft
        }
    }

    private fun Int.twoDigits()
            = if (this>=10) this.toString() else "0$this"

    override fun onBackPressed() {
        when {
            cvStep3.visibility == View.VISIBLE -> {
                cvStep3.visibility = View.GONE
                cvStep2.visibility = View.VISIBLE

            }
            cvStep2.visibility == View.VISIBLE -> {
                cvStep2.visibility = View.GONE
                cvStep1.visibility = View.VISIBLE

            }
            cvStep1.visibility == View.VISIBLE -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(getString(R.string.dialog_create_appointment_exit_title))
                builder.setMessage(getString(R.string.dialog_create_appointment_exit_message))
                builder.setPositiveButton(getString(R.string.dialog_create_appointment_exit_positive_btn)) { _, _ ->
                    finish()
                }
                builder.setNegativeButton(getString(R.string.dialog_create_appointment_exit_negative_btn)) { dialog, _ ->
                    dialog.dismiss()
                }

                val dialog = builder.create()
                dialog.show()
            }
        }

    }
}
