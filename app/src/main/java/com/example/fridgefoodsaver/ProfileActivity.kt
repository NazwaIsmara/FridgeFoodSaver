package com.example.fridgefoodsaver

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.fridgefoodsaver.notification.NotificationScheduler

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var switchReminder: Switch
    private lateinit var tvReminderStatus: TextView
    private lateinit var tvAvatar: TextView

    private val preferences by lazy {
        getSharedPreferences("fridge_settings", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        val tvBack = findViewById<TextView>(R.id.tvBack)
        val tvProfileName = findViewById<TextView>(R.id.tvProfileName)
        val tvProfileEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val tvAccountName = findViewById<TextView>(R.id.tvAccountName)
        val tvAccountEmail = findViewById<TextView>(R.id.tvAccountEmail)

        tvAvatar = findViewById(R.id.tvAvatar)
        switchReminder = findViewById(R.id.switchReminder)
        tvReminderStatus = findViewById(R.id.tvReminderStatus)

        val btnLogout = findViewById<Button>(R.id.btnLogout)

        val user = auth.currentUser

        val name = user?.displayName
            ?: user?.email?.substringBefore("@")
            ?: "Pengguna"

        val email = user?.email ?: "-"

        tvProfileName.text = name
        tvProfileEmail.text = email
        tvAccountName.text = name
        tvAccountEmail.text = email

        tvAvatar.text = name
            .trim()
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "F" }

        switchReminder.isChecked = preferences.getBoolean(
            "reminder_enabled",
            true
        )

        updateReminderStatus(switchReminder.isChecked)

        switchReminder.setOnCheckedChangeListener { _, isChecked ->

            preferences.edit()
                .putBoolean("reminder_enabled", isChecked)
                .apply()

            if (isChecked) {

                NotificationScheduler.scheduleDailyNotification(this)

                Toast.makeText(
                    this,
                    "Pengingat kedaluwarsa diaktifkan",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                NotificationScheduler.cancelDailyNotification(this)

                Toast.makeText(
                    this,
                    "Pengingat kedaluwarsa dinonaktifkan",
                    Toast.LENGTH_SHORT
                ).show()
            }

            updateReminderStatus(isChecked)
        }

        tvBack.setOnClickListener {
            finish()
        }

        btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun updateReminderStatus(isEnabled: Boolean) {

        tvReminderStatus.text = if (isEnabled) {
            "● Pengingat sedang aktif"
        } else {
            "● Pengingat sedang nonaktif"
        }

        tvReminderStatus.setTextColor(
            getColor(
                if (isEnabled) {
                    R.color.sage_green
                } else {
                    R.color.expiry_red
                }
            )
        )
    }

    private fun showLogoutDialog() {

        AlertDialog.Builder(this)
            .setTitle("Keluar dari Akun")
            .setMessage("Apakah kamu yakin ingin keluar?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Keluar") { _, _ ->

                auth.signOut()

                val intent = Intent(
                    this,
                    LoginActivity::class.java
                )

                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                startActivity(intent)
                finish()
            }
            .show()
    }
}
