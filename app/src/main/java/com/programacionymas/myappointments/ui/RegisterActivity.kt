package com.programacionymas.myappointments.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.programacionymas.myappointments.R
import com.programacionymas.myappointments.io.ApiService
import com.programacionymas.myappointments.io.response.LoginResponse
import com.programacionymas.myappointments.util.PreferenceHelper
import com.programacionymas.myappointments.util.PreferenceHelper.set
import com.programacionymas.myappointments.util.toast
import kotlinx.android.synthetic.main.activity_register.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private val apiService by lazy {
        ApiService.create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        tvGoToLogin.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        btnConfirmRegister.setOnClickListener {
            performRegister()
        }
    }

    private fun performRegister() {
        val name = etRegisterName.text.toString().trim()
        val email = etRegisterEmail.text.toString().trim()
        val password = etRegisterPassword.text.toString()
        val passwordConfirmation = etRegisterPasswordConfirmation.text.toString()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || passwordConfirmation.isEmpty()) {
            toast(getString(R.string.error_register_empty_fields))
            return
        }

        if (password != passwordConfirmation) {
            toast(getString(R.string.error_register_passwords_do_not_match))
            return
        }

        val call = apiService.postRegister(name, email, password, passwordConfirmation)
        call.enqueue(object: Callback<LoginResponse> {
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                toast(t.localizedMessage)
            }

            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    if (loginResponse == null) {
                        toast(getString(R.string.error_login_response))
                        return
                    }
                    if (loginResponse.success) {
                        createSessionPreference(loginResponse.jwt)
                        toast(getString(R.string.welcome_name, loginResponse.user.name))
                        goToMenuActivity()
                    } else {
                        toast(getString(R.string.error_register_validation))
                    }
                } else {
                    toast(getString(R.string.error_register_validation))
                }
            }
        })
    }

    private fun createSessionPreference(jwt: String) {
        val preferences = PreferenceHelper.defaultPrefs(this)
        preferences["jwt"] = jwt
    }

    private fun goToMenuActivity() {
        val intent = Intent(this, MenuActivity::class.java)
        startActivity(intent)
        finish()
    }
}
