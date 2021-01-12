package com.programacionymas.myappointments.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.programacionymas.myappointments.R
import com.programacionymas.myappointments.io.ApiService
import com.programacionymas.myappointments.model.User
import com.programacionymas.myappointments.util.PreferenceHelper
import com.programacionymas.myappointments.util.PreferenceHelper.get
import com.programacionymas.myappointments.util.toast
import kotlinx.android.synthetic.main.activity_profile.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {

    private val apiService: ApiService by lazy {
        ApiService.create()
    }

    private val preferences by lazy {
        PreferenceHelper.defaultPrefs(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val jwt = preferences["jwt", ""]

        val authHeader = "Bearer $jwt"

        val call = apiService.getUser(authHeader)

        call.enqueue(object : Callback<User> {
            override fun onFailure(call: Call<User>, t: Throwable) {
                toast(t.localizedMessage)
            }

            override fun onResponse(call: Call<User>, response: Response<User>) {
                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null)
                        displayProfileData(user)
                }
            }
        })

        /*
        Handler().postDelayed({
            displayProfileData()
        }, 3000)
        */
    }

    private fun displayProfileData(user: User) {
        etName.setText(user.name)
        etPhone.setText(user.phone)
        etAddress.setText(user.address)

        progressBarProfile.visibility = View.GONE
        linearLayoutProfile.visibility = View.VISIBLE

        btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    private fun saveProfile() {
        val name = etName.text.toString()
        val phone = etPhone.text.toString()
        val address = etAddress.text.toString()

        if (name.length < 4) {
            inputLayoutName.error = getString(R.string.error_profile_name)
            return
        }

        val jwt = preferences["jwt", ""]
        val authHeader = "Bearer $jwt"

        val call = apiService.postUser(authHeader, name, phone, address)
        call.enqueue(object: Callback<Void> {
            override fun onFailure(call: Call<Void>, t: Throwable) {
                toast(t.localizedMessage)
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    toast(getString(R.string.profile_success_message))
                    finish()
                }
            }
        })
    }
}
