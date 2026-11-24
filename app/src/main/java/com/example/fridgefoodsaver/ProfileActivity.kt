package com.example.fridgefoodsaver

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fridgefoodsaver.notification.NotificationScheduler
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var switchReminder: Switch
    private lateinit var tvReminderStatus: TextView

    private val preferences by lazy {
        getSharedPreferences("fridge_settings", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        val ivProfileAvatar = findViewById<MaterialCardView>(R.id.ivProfileAvatar)
        val tvProfileName = findViewById<TextView>(R.id.tvProfileName)
        val tvProfileEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val tvAccountName = findViewById<TextView>(R.id.tvAccountName)
        val tvAccountEmail = findViewById<TextView>(R.id.tvAccountEmail)

        switchReminder = findViewById(R.id.switchReminder)
        tvReminderStatus = findViewById(R.id.tvReminderStatus)

        val btnLogout = findViewById<Button>(R.id.btnLogout)

        ivProfileAvatar.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#C8E6C9"))
            setStroke((2.5f * resources.displayMetrics.density).toInt(), Color.parseColor("#2E6B35"))
        }

        val user = auth.currentUser

        val name = user?.displayName
            ?: user?.email?.substringBefore("@")
            ?: "Pengguna"

        val email = user?.email ?: "-"

        tvProfileName.text = name
        tvProfileEmail.text = email
        tvAccountName.text = name
        tvAccountEmail.text = email

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

        setupBottomNavigation()

        btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_profile

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> true
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_inventory -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_add -> {
                    startActivity(Intent(this, AddFoodActivity::class.java))
                    true
                }
                R.id.nav_notification -> {
                    startActivity(Intent(this, NotificationActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
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
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Keluar dari Akun")
            .setMessage("Apakah kamu yakin ingin keluar?")
            .setNegativeButton("Batal") { d, _ ->
                d.dismiss()
            }
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
            .create()

        dialog.show()

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(Color.parseColor("#C62828"))
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
            ?.setTextColor(Color.parseColor("#3F7D44"))
    }
}