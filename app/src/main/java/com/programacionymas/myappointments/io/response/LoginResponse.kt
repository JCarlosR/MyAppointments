package com.programacionymas.myappointments.io.response

import com.programacionymas.myappointments.model.User

data class LoginResponse(val success: Boolean, val user: User, val jwt: String)