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
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etRegisterEmail)
        etPassword = findViewById(R.id.etRegisterPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)

        btnRegister.setOnClickListener {
            registerUser()
        }

        tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun registerUser() {

        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (name == "") {
            etName.error = "Nama wajib diisi"
            etName.requestFocus()
            return
        }

        if (email == "") {
            etEmail.error = "Email wajib diisi"
            etEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Format email tidak valid"
            etEmail.requestFocus()
            return
        }

        if (password == "") {
            etPassword.error = "Password wajib diisi"
            etPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            etPassword.error = "Password minimal 6 karakter"
            etPassword.requestFocus()
            return
        }

        if (confirmPassword == "") {
            etConfirmPassword.error = "Konfirmasi password wajib diisi"
            etConfirmPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            etConfirmPassword.error = "Password tidak sama"
            etConfirmPassword.requestFocus()
            return
        }

        createFirebaseAccount(name, email, password)
    }

    private fun createFirebaseAccount(
        name: String,
        email: String,
        password: String
    ) {

        setLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val user = auth.currentUser

                    if (user != null) {

                        val profileUpdates =
                            UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build()

                        user.updateProfile(profileUpdates)
                            .addOnCompleteListener { profileTask ->

                                setLoading(false)

                                if (profileTask.isSuccessful) {
                                    Toast.makeText(
                                        this,
                                        "Registrasi berhasil!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Akun dibuat. Nama profil belum tersimpan.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                                openDashboard()
                            }

                    } else {

                        setLoading(false)

                        Toast.makeText(
                            this,
                            "Akun dibuat, silakan login.",
                            Toast.LENGTH_LONG
                        ).show()

                        openDashboard()
                    }

                } else {

                    setLoading(false)

                    Toast.makeText(
                        this,
                        getFirebaseErrorMessage(task.exception),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun setLoading(isLoading: Boolean) {

        btnRegister.isEnabled = !isLoading
        tvLogin.isEnabled = !isLoading

        if (isLoading) {
            btnRegister.text = "Creating Account..."
        } else {
            btnRegister.text = "Create Account"
        }
    }

    private fun openDashboard() {

        val intent = Intent(this, DashboardActivity::class.java)

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    private fun getFirebaseErrorMessage(exception: Exception?): String {

        val message: String

        if (exception != null) {
            message = exception.message ?: ""
        } else {
            message = ""
        }

        return when {

            message.contains("already in use", ignoreCase = true) ->
                "Email sudah terdaftar. Silakan login."

            message.contains("invalid email", ignoreCase = true) ->
                "Format email tidak valid."

            message.contains("weak password", ignoreCase = true) ->
                "Password terlalu lemah. Gunakan minimal 6 karakter."

            message.contains("network", ignoreCase = true) ->
                "Koneksi internet bermasalah."

            else ->
                "Registrasi gagal. Periksa data dan koneksi internet."
        }
    }
}
