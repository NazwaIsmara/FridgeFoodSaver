package com.example.fridgefoodsaver

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val tvBack = findViewById<TextView>(R.id.tvBack)
        val tvProfileName = findViewById<TextView>(R.id.tvProfileName)
        val tvProfileEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val tvAccountName = findViewById<TextView>(R.id.tvAccountName)
        val tvAccountEmail = findViewById<TextView>(R.id.tvAccountEmail)
        val switchReminder = findViewById<Switch>(R.id.switchReminder)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // Data contoh profil
        val name = intent.getStringExtra("user_name")
            ?: "Pengguna Fridge Food Saver"

        val email = intent.getStringExtra("user_email")
            ?: "pengguna@email.com"

        tvProfileName.text = name
        tvProfileEmail.text = email
        tvAccountName.text = name
        tvAccountEmail.text = email

        tvBack.setOnClickListener {
            finish()
        }

        // Status pengingat masih berupa pengaturan tampilan.
        // Penyimpanan preferensi akan ditambahkan kemudian.

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Keluar dari Akun")
                .setMessage("Apakah kamu yakin ingin keluar?")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Keluar") { _, _ ->
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .show()
        }
    }
}
