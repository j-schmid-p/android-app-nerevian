package com.example.nerevian.common

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.nerevian.R
import com.example.nerevian.agent.OrderHistoryAgentFragment
import com.example.nerevian.client.OrderHistoryClientFragment
import com.example.nerevian.utils.NavigationBar
import com.google.android.material.bottomnavigation.BottomNavigationView

class BaseHistoryActivity : AppCompatActivity() {

    private val ROL_CLIENT = 1
    private val ROL_AGENT = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page) // Using same layout with fragment_container
        
        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val navView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        NavigationBar(this).setup(navView)
        navView.selectedItemId = R.id.nav_history

        loadHistoryFragment()
    }

    private fun loadHistoryFragment() {
        val rolId = getSharedPreferences("session", Context.MODE_PRIVATE).getInt("rol_id", -1)

        val fragment = when (rolId) {
            ROL_CLIENT -> OrderHistoryClientFragment()
            ROL_AGENT -> OrderHistoryAgentFragment()
            else -> OrderHistoryClientFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
