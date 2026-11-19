package com.example.fridgefoodsaver

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etRegisterEmail)
        val etPassword = findViewById<EditText>(R.id.etRegisterPassword)
        val etConfirmPassword =
            findViewById<EditText>(R.id.etConfirmPassword)

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        btnRegister.setOnClickListener {

            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword =
                etConfirmPassword.text.toString()

            when {
                name.isEmpty() -> {
                    etName.error = "Nama wajib diisi"
                }

                email.isEmpty() -> {
                    etEmail.error = "Email wajib diisi"
                }

                !android.util.Patterns.EMAIL_ADDRESS
                    .matcher(email).matches() -> {
                    etEmail.error = "Format email tidak valid"
                }

                password.length < 6 -> {
                    etPassword.error =
                        "Password minimal 6 karakter"
                }

                confirmPassword.isEmpty() -> {
                    etConfirmPassword.error =
                        "Konfirmasi password wajib diisi"
                }

                password != confirmPassword -> {
                    etConfirmPassword.error =
                        "Password tidak sama"
                }

                else -> {
                    Toast.makeText(
                        this,
                        "Form registrasi valid",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        tvLogin.setOnClickListener {
            finish()
        }
    }
}
