
package com.example.fridgefoodsaver

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Menghubungkan komponen dari XML
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        // Fungsi tombol Login
        btnLogin.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            when {
                email.isEmpty() -> {
                    etEmail.error = "Email wajib diisi"
                }

                password.isEmpty() -> {
                    etPassword.error = "Password wajib diisi"
                }

                else -> {
                    val intent = Intent(
                        this,
                        DashboardActivity::class.java
                    )
                    startActivity(intent)
                }
            }
        }

        // Fungsi menuju halaman Sign Up
        tvRegister.setOnClickListener {

            val intent = Intent(
                this,
                RegisterActivity::class.java
            )

            startActivity(intent)
        }
    }
}
