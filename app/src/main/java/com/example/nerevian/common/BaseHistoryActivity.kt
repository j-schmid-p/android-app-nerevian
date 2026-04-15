package com.example.nerevian.common

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.nerevian.R
import com.example.nerevian.agent.OrderHistoryAgentFragment
import com.example.nerevian.client.OrderHistoryClientFragment
import com.example.nerevian.network.ApiService
import com.example.nerevian.utils.NavigationBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.example.nerevian.utils.SessionManager
import kotlinx.coroutines.*
import org.json.JSONObject

class BaseHistoryActivity : AppCompatActivity() {

    private val ROL_CLIENT = 1
    private val ROL_AGENT = 3

    private val apiService = ApiService()
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)
        
        session = SessionManager(this)

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

    fun refreshHistory() {
        loadHistoryFragment()
    }

    private fun loadHistoryFragment() {
        val rolId = session.rolId

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
