package com.example.nerevian.common

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
import com.example.nerevian.R
import com.example.nerevian.client.ClientHomeFragment
import com.example.nerevian.utils.SessionManager

class ProfileActivity : AppCompatActivity() {

    private lateinit var nameTxt: EditText
    private lateinit var lastNameTxt: EditText
    private lateinit var birthdateTxt: EditText
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

        val session = SessionManager(this)

        nameTxt = findViewById(R.id.et_name)
        lastNameTxt = findViewById(R.id.et_last_name)
        birthdateTxt = findViewById(R.id.et_birthdate)
        etId = findViewById(R.id.et_id)
        btnEditSave = findViewById(R.id.btn_edit_save)

        btnEditSave.setOnClickListener {
            if (isEditMode) {
                toggleEditMode(false)
            } else {
                toggleEditMode(true)
                nameTxt.requestFocus()
                nameTxt.setSelection(nameTxt.text.length)
                showKeyboard(nameTxt)
            }
        }
    }

    private fun toggleEditMode(enable: Boolean) {
        isEditMode = enable
        val fields = listOf(nameTxt, lastNameTxt, birthdateTxt, etId)
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