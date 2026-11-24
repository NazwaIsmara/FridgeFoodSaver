package com.example.fridgefoodsaver

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGoToRegister: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        etEmail = findViewById(R.id.etLoginEmail)
        etPassword = findViewById(R.id.etLoginPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvGoToRegister = findViewById(R.id.tvGoToRegister)

        btnLogin.setOnClickListener {
            loginUser()
        }

        tvGoToRegister.setOnClickListener {
            startActivity(
                Intent(this, RegisterActivity::class.java)
            )
        }
    }

    override fun onStart() {
        super.onStart()

        // Periksa apakah pengguna masih memiliki sesi login
        if (::auth.isInitialized && auth.currentUser != null) {
            openDashboard()
        }
    }

    private fun loginUser() {

        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        if (email.isEmpty()) {
            etEmail.error = "Email wajib diisi"
            etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Format email tidak valid"
            etEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            etPassword.error = "Password wajib diisi"
            etPassword.requestFocus()
            return
        }

        setLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                setLoading(false)

                if (task.isSuccessful) {

                    Toast.makeText(
                        this,
                        "Login berhasil!",
                        Toast.LENGTH_SHORT
                    ).show()

                    openDashboard()

                } else {

                    Toast.makeText(
                        this,
                        getFirebaseErrorMessage(task.exception),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun setLoading(isLoading: Boolean) {

        btnLogin.isEnabled = !isLoading
        tvGoToRegister.isEnabled = !isLoading

        btnLogin.text = if (isLoading) {
            "Memproses..."
        } else {
            "Masuk"
        }
    }

    private fun openDashboard() {

        val intent = Intent(this, DashboardActivity::class.java)

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    private fun getFirebaseErrorMessage(exception: Exception?): String {

        val code = (exception as? FirebaseAuthException)?.errorCode

        return when (code) {

            "ERROR_INVALID_EMAIL" ->
                "Format email tidak valid."

            "ERROR_USER_NOT_FOUND" ->
                "Akun tidak ditemukan."

            "ERROR_WRONG_PASSWORD",
            "ERROR_INVALID_CREDENTIAL" ->
                "Email atau password salah."

            "ERROR_USER_DISABLED" ->
                "Akun telah dinonaktifkan."

            "ERROR_TOO_MANY_REQUESTS" ->
                "Terlalu banyak percobaan. Coba lagi nanti."

            "ERROR_NETWORK_REQUEST_FAILED" ->
                "Koneksi internet bermasalah."

            else ->
                "Login gagal. Periksa email, password, dan koneksi internet."
        }
    }
}
