package com.programacionymas.myappointments.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.programacionymas.myappointments.R
import com.programacionymas.myappointments.io.ApiService
import com.programacionymas.myappointments.model.User
import com.programacionymas.myappointments.util.PreferenceHelper
import com.programacionymas.myappointments.util.PreferenceHelper.get
import com.programacionymas.myappointments.util.PreferenceHelper.set
import com.programacionymas.myappointments.util.toast
import kotlinx.android.synthetic.main.activity_menu.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MenuActivity : AppCompatActivity() {

    private val apiService by lazy {
        ApiService.create()
    }

    private val preferences by lazy {
        PreferenceHelper.defaultPrefs(this)
    }

    private val authHeader by lazy {
        val jwt = preferences["jwt", ""]
        "Bearer $jwt"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        val storeToken = intent.getBooleanExtra("store_token", false)
        if (storeToken)
            storeToken()

        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        btnProfile.setOnClickListener {
            editProfile()
        }

        btnCreateAppointment.setOnClickListener {
            createAppointment(it)
        }

        btnMyAppointments.setOnClickListener {
            val intent = Intent(this, AppointmentsActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun createAppointment(view: View) {
        val call = apiService.getUser(authHeader)
        call.enqueue(object: Callback<User> {
            override fun onFailure(call: Call<User>, t: Throwable) {
                toast(t.localizedMessage)
            }

            override fun onResponse(call: Call<User>, response: Response<User>) {
                if (response.isSuccessful) {
                    val user = response.body()
                    val phoneLength = user?.phone?.length ?: 0

                    if (phoneLength >= 6) {
                        val intent = Intent(this@MenuActivity, CreateAppointmentActivity::class.java)
                        startActivity(intent)
                    } else {
                        Snackbar.make(view, R.string.you_need_a_phone, Snackbar.LENGTH_LONG).show()
                    }
                }
            }

        })
    }

    private fun editProfile() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
    }

    private fun storeToken() {
        val deviceToken = FirebaseMessaging.getInstance().token.result

        if (deviceToken == null) {
            Log.d(TAG, "We don't store the device token as we got a null value")
            return
        }

        val call = apiService.postToken(authHeader, deviceToken)
        call.enqueue(object: Callback<Void> {
            override fun onFailure(call: Call<Void>, t: Throwable) {
                toast(t.localizedMessage)
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Token registrado correctamente")
                } else {
                    Log.d(TAG, "Hubo un problema al registrar el token")
                }
            }
        })
    }

    private fun performLogout() {
        val call = apiService.postLogout(authHeader)
        call.enqueue(object: Callback<Void> {
            override fun onFailure(call: Call<Void>, t: Throwable) {
                toast(t.localizedMessage)
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                clearSessionPreference()

                val intent = Intent(this@MenuActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        })
    }

    private fun clearSessionPreference() {
        preferences["jwt"] = ""
    }

    companion object {
        private const val TAG = "MenuActivity"
    }
}
