package com.example.nerevian.common

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.nerevian.R
import com.example.nerevian.agent.AgentHomeFragment
import com.example.nerevian.client.ClientHomeFragment
import com.example.nerevian.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit

class LoginActivity : AppCompatActivity() {

    private val apiService = ApiService()
    private val PREFERENCE_NAME = "session"
    private lateinit var loginBtn: Button
    private val ROL_CLIENT = 1
    private val ROL_AGENT = 3


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.login_activity)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loginBtn = findViewById<Button>(R.id.loginButton)

        checkSession()

        loginBtn.setOnClickListener { validateInputs() }
    }

    private fun checkSession() {
        val preferences = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val savedRolId = preferences.getInt("rol_id", -1)
        val savedToken = preferences.getString("token", null)

        if (savedRolId != -1 && savedToken != null) { goToUserActivity(savedRolId) }
    }

    private fun validateInputs () {
        val email = findViewById<EditText>(R.id.emailInput).text.toString().trim()
        val password = findViewById<EditText>(R.id.passwordInput).text.toString().trim()

        if (password.isEmpty() || email.isEmpty()){
            Toast.makeText(this, "Please enter both your email and password",
                Toast.LENGTH_SHORT).show()
        }
        else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(this, "Please enter a valid email",
                Toast.LENGTH_SHORT).show()
        } else {
            callAPI(email, password)
        }
    }

    private fun callAPI(email: String, password: String) {
        loginBtn.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val loginResult = apiService.login(email, password)

            if (loginResult == null || !loginResult.optBoolean("success", false)) {
                withContext(Dispatchers.Main) {
                    loginBtn.isEnabled = true
                    val msg = loginResult?.optString("message", "Invalid credentials") ?: "Connection error"
                    Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_SHORT).show()
                }
            } else {
                val token = loginResult.getString("token")
                val meResult = apiService.getMe(token)

                withContext(Dispatchers.Main) {
                    loginBtn.isEnabled = true
                    if (meResult == null) {
                        Toast.makeText(this@LoginActivity, "Could not load user data", Toast.LENGTH_SHORT).show()
                    } else {
                        getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit()
                            .putString("token", token)
                            .putInt("rol_id", meResult.getInt("rol_id"))
                            .putInt("user_id", meResult.getInt("id"))
                            .putString("nom", meResult.optString("nom"))
                            .putString("cognoms", meResult.optString("cognoms"))
                            .putString("correu", meResult.optString("correu"))
                            .apply()

                        goToUserActivity(meResult.getInt("rol_id"))
                    }
                }
            }
        }
    }

    private fun goToUserActivity(role : Int) {

         /*val intent = when (role) {
            ROL_CLIENT -> Intent(this, ClientHomeFragment::class.java)
            ROL_AGENT -> Intent(this, AgentHomeFragment::class.java)

            else -> {
                getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                    .edit { clear() } //TODO mirar si aixo del clear esta be
                Toast.makeText(this,
                    "You cannot login to this app with the user role you hold",
                    Toast.LENGTH_SHORT).show()
                return
            }
        }*/

        val intent = Intent(this, HomePageActivity::class.java)

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}