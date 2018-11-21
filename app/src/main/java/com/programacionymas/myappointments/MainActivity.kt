package com.programacionymas.myappointments

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.programacionymas.myappointments.PreferenceHelper.get
import com.programacionymas.myappointments.PreferenceHelper.set
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val snackBar by lazy {
        Snackbar.make(mainLayout, R.string.press_back_again, Snackbar.LENGTH_SHORT)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // shared preferences
        // SQLite
        // files

        val preferences = PreferenceHelper.defaultPrefs(this)
        if (preferences["session", false])
            goToMenuActivity()

        btnLogin.setOnClickListener {
            // validates
            createSessionPreference()
            goToMenuActivity()
        }

        tvGoToRegister.setOnClickListener {
            Toast.makeText(this, getString(R.string.please_fill_your_register_data), Toast.LENGTH_SHORT).show()

            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun createSessionPreference() {
        val preferences = PreferenceHelper.defaultPrefs(this)
        preferences["session"] = true
    }

    private fun goToMenuActivity() {
        val intent = Intent(this, MenuActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if (snackBar.isShown)
            super.onBackPressed()
        else
            snackBar.show()
    }
}
