package com.example.nerevian.common

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Consumer
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.nerevian.R
import com.example.nerevian.agent.HomePageAgentActivity
import com.example.nerevian.client.HomePageClientActivity
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.login_activity)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val loginBtn = findViewById<Button>(R.id.loginButton)

        checkSession()

        loginBtn.setOnClickListener { validateInputs() }
    }

    private fun checkSession() {
        val preferences = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val savedUserType = preferences.getInt("user_type", -1)
        val savedToken = preferences.getString("token", null)

        if (savedUserType != -1 && savedToken != null)
        {
            goToUserActivity(savedUserType)
            return
        }
    }

    private fun validateInputs () {
        val emailTxt = findViewById<EditText>(R.id.emailInput)
        val passwordTxt = findViewById<EditText>(R.id.passwordInput)

        val email = emailTxt.text.toString().trim()
        val password = passwordTxt.text.toString().trim()


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

    private fun callAPI(email: String, password: String){
        loginBtn.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val result = apiService.login(email, password)

            withContext(Dispatchers.Main){
                loginBtn.isEnabled = true

                if (result == null){
                    Toast.makeText(this@LoginActivity, "Connection error",
                        Toast.LENGTH_SHORT).show()

                } else if (!result.optBoolean("success", false)){
                    Toast.makeText(this@LoginActivity, result.optString(
                        "message", "Invalid credentials"),
                        Toast.LENGTH_SHORT).show()

                } else {
                    //TODO mirar si "rol" es correcte
                    val userRole = result.getInt("rol_id")
                    val token = result.getString("token")

                    getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit()
                        .putInt("rol_id", userRole)
                        .putString("token", token)
                        .apply()

                    goToUserActivity(userRole)
                }

            }
        }
    }

    private fun goToUserActivity(role : Int) {

        val intent = when (role) {
            1 -> Intent(this, HomePageClientActivity::class.java)
            3 -> Intent(this, HomePageAgentActivity::class.java)

            else -> {
                getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
                    .edit { clear() } //TODO mirar si aixo del clear esta be
                Toast.makeText(this,
                    "You cannot login to this app with the user role you hold",
                    Toast.LENGTH_SHORT).show()
                return
            }
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}