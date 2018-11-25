package com.programacionymas.myappointments.ui

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.programacionymas.myappointments.R
import com.programacionymas.myappointments.model.Appointment
import kotlinx.android.synthetic.main.activity_appointments.*

class AppointmentsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointments)

        val appointments = ArrayList<Appointment>()
        appointments.add(
            Appointment(1, "Médico Test", "12/12/2018", "3:00 PM")
        )
        appointments.add(
            Appointment(2, "Médico BB", "15/12/2018", "4:30 PM")
        )
        appointments.add(
            Appointment(3, "Médico CC", "17/12/2018", "7:00 AM")
        )

        rvAppointments.layoutManager = LinearLayoutManager(this) // GridLayoutManager
        rvAppointments.adapter = AppointmentAdapter(appointments)
    }
}
