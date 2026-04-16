package com.example.nerevian.common

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.nerevian.R
import com.example.nerevian.network.ApiService
import com.example.nerevian.utils.NavigationBar
import com.example.nerevian.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private val apiService = ApiService()
    private lateinit var session: SessionManager
    private lateinit var nameTxt: EditText
    private lateinit var lastNameTxt: EditText
    private lateinit var birthdateTxt: EditText
    private lateinit var etId: EditText
    private lateinit var btnEditSave: Button
    private lateinit var profileNameHeader: TextView
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_activity)

        session = SessionManager(this)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        nameTxt = findViewById(R.id.et_name)
        lastNameTxt = findViewById(R.id.et_last_name)
        etId = findViewById(R.id.et_id)
        btnEditSave = findViewById(R.id.btn_edit_save)
        profileNameHeader = findViewById(R.id.profile_name_header)

        loadUserData()

        btnEditSave.setOnClickListener {
            if (isEditMode) {
                saveProfile()
            } else {
                toggleEditMode(true)
                nameTxt.requestFocus()
                val inputManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                inputManager.showSoftInput(nameTxt, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        
        logout()
        setupNavigationBar()
    }

    private fun loadUserData() {
        nameTxt.setText(session.name)
        lastNameTxt.setText(session.lastName)
        etId.setText(session.email) // Show email as identification
        profileNameHeader.text = " ${session.name} ${session.lastName}"
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val date = String.format(Locale.getDefault(), "%02d-%02d-%d", selectedDay, selectedMonth + 1, selectedYear)
                birthdateTxt.setText(date)
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun saveProfile() {
        val name = nameTxt.text.toString().trim()
        val lastName = lastNameTxt.text.toString().trim()
        // val birthdate = birthdateTxt.text.toString()

        if (name.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, "Name and Last Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.updateProfile(session.token ?: "", name, lastName)
                
                withContext(Dispatchers.Main) {
                    if (response != null) {
                        session.updateUserInfo(name, lastName)
                        loadUserData()
                        toggleEditMode(false)
                        Toast.makeText(this@ProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProfileActivity, "Failed to update profile on the server", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun toggleEditMode(enable: Boolean) {
        isEditMode = enable
        nameTxt.isEnabled = enable
        lastNameTxt.isEnabled = enable
        
        if (enable) {
            btnEditSave.text = "SAVE CHANGES"
        } else {
            btnEditSave.text = "EDIT"
            val inputManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(nameTxt.windowToken, 0)
        }
    }

    private fun logout(){
        val btnLogout = findViewById<Button>(R.id.btn_logout)
        btnLogout.setOnClickListener {
            val token = session.token
            CoroutineScope(Dispatchers.IO).launch {
                if (token != null) {
                    apiService.logout(token)
                }
                withContext(Dispatchers.Main) {
                    session.logout()
                    val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
    
    private fun setupNavigationBar() {
        val navView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        NavigationBar(this).setup(navView)
    }

}