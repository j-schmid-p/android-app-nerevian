package com.example.nerevian.client

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.nerevian.common.ProfileActivity
import com.example.nerevian.R

class ClientHomeFragment : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.client_home_fragment)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        setupNavigation()
    }

    private fun setupNavigation() {
        val navHome = findViewById<ImageView>(R.id.nav_home)
        val navHistory = findViewById<ImageView>(R.id.nav_history)
        val navProfile = findViewById<ImageView>(R.id.nav_profile)

        navHome.setOnClickListener {
            // Already here
        }

        navHistory.setOnClickListener {
            val intent = Intent(this, OrderHistoryClientActivity::class.java)
            startActivity(intent)
            finish()
        }

        navProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}