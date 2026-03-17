package com.example.nerevian

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ProfileActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etBirthdate: EditText
    private lateinit var etId: EditText
    private lateinit var btnEditSave: Button
    
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_activity)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        etName = findViewById(R.id.et_name)
        etLastName = findViewById(R.id.et_last_name)
        etBirthdate = findViewById(R.id.et_birthdate)
        etId = findViewById(R.id.et_id)
        btnEditSave = findViewById(R.id.btn_edit_save)

        btnEditSave.setOnClickListener {
            if (isEditMode) {
                toggleEditMode(false)
            } else {
                toggleEditMode(true)
                etName.requestFocus()
                etName.setSelection(etName.text.length)
                showKeyboard(etName)
            }
        }

        setupNavigation()
    }

    private fun setupNavigation() {
        val navHome = findViewById<ImageView>(R.id.nav_home)
        val navProfile = findViewById<ImageView>(R.id.nav_profile)

        navHome.setOnClickListener {
            val intent = Intent(this, HomePageActivity::class.java)
            startActivity(intent)
            finish()
        }

        navProfile.setOnClickListener {
            // Already here
        }
    }

    private fun toggleEditMode(enable: Boolean) {
        isEditMode = enable
        val fields = listOf(etName, etLastName, etBirthdate, etId)
        fields.forEach { it.isEnabled = enable }
        if (enable) {
            btnEditSave.text = "SAVE CHANGES"
        } else {
            btnEditSave.text = "EDIT"
            hideKeyboard()
        }
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }
}
