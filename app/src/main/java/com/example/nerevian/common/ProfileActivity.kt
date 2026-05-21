package com.example.nerevian.common

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {

    private val apiService = ApiService()
    private lateinit var session: SessionManager
    private lateinit var nameTxt: EditText
    private lateinit var lastNameTxt: EditText
    private lateinit var etId: EditText
    private lateinit var btnEditSave: Button
    private lateinit var profileNameHeader: TextView
    private var isEditMode = false

    private val dniPrefs by lazy { getSharedPreferences("profile_prefs", MODE_PRIVATE) }
    private val tempCameraFile get() = File(cacheDir, "temp_dni.jpg")

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) saveDni(tempCameraFile, "dni_photo.jpg", "image/jpeg")
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) result.data?.data?.let { saveDniFromUri(it) }
    }

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) openCamera() else toast("Camera permission denied")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_activity)

        session = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, 0)
            insets
        }

        nameTxt           = findViewById(R.id.et_name)
        lastNameTxt       = findViewById(R.id.et_last_name)
        etId              = findViewById(R.id.et_id)
        btnEditSave       = findViewById(R.id.btn_edit_save)
        profileNameHeader = findViewById(R.id.profile_name_header)

        loadUserData()

        btnEditSave.setOnClickListener { if (isEditMode) saveProfile() else enterEditMode() }
        findViewById<Button>(R.id.btn_upload_dni).setOnClickListener { showDniDialog() }
        findViewById<Button>(R.id.btn_download_dni).setOnClickListener { openDni() }
        findViewById<Button>(R.id.btn_logout).setOnClickListener { logout() }

        NavigationBar(this).setup(findViewById<BottomNavigationView>(R.id.bottom_navigation))
    }

    // ── DNI ──────────────────────────────────────────────────────────────────

    private fun showDniDialog() {
        AlertDialog.Builder(this)
            .setTitle("Upload DNI")
            .setItems(arrayOf("Take Photo", "Choose from Gallery")) { _, i ->
                if (i == 0) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                        openCamera()
                    else
                        cameraPermission.launch(Manifest.permission.CAMERA)
                } else {
                    galleryLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "image/*"))
                        addCategory(Intent.CATEGORY_OPENABLE)
                    })
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openCamera() {
        cameraLauncher.launch(FileProvider.getUriForFile(this, "$packageName.provider", tempCameraFile))
    }

    private fun saveDni(src: File, name: String, mime: String) {
        try {
            src.copyTo(File(filesDir, name), overwrite = true)
            dniPrefs.edit().putString("name", name).putString("mime", mime).apply()
            etId.setText(name)
            toast("DNI saved")
        } catch (e: Exception) { toast("Error saving DNI: ${e.message}") }
    }

    private fun saveDniFromUri(uri: Uri) {
        val name = contentResolver.query(uri, null, null, null, null)?.use { c ->
            val col = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (c.moveToFirst() && col >= 0) c.getString(col) else null
        } ?: uri.lastPathSegment ?: "dni_document"
        val mime = contentResolver.getType(uri) ?: "application/octet-stream"
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(File(filesDir, name)).use { output -> input.copyTo(output) }
            }
            dniPrefs.edit().putString("name", name).putString("mime", mime).apply()
            etId.setText(name)
            toast("DNI saved")
        } catch (e: Exception) { toast("Error saving DNI: ${e.message}") }
    }

    private fun openDni() {
        val name = dniPrefs.getString("name", null) ?: return toast("No DNI saved yet")
        val file = File(filesDir, name)
        if (!file.exists()) return toast("DNI file not found")
        val mime = dniPrefs.getString("mime", "*/*") ?: "*/*"
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, mime); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) },
                "Open DNI"
            ))
        } catch (e: Exception) { toast("Cannot open file: ${e.message}") }
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    private fun loadUserData() {
        nameTxt.setText(session.name)
        lastNameTxt.setText(session.lastName)
        profileNameHeader.text = " ${session.name} ${session.lastName}"
        etId.setText(dniPrefs.getString("name", null) ?: session.email)
    }

    private fun enterEditMode() {
        isEditMode = true
        nameTxt.isEnabled = true
        lastNameTxt.isEnabled = true
        btnEditSave.text = "SAVE CHANGES"
        nameTxt.requestFocus()
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(nameTxt, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun exitEditMode() {
        isEditMode = false
        nameTxt.isEnabled = false
        lastNameTxt.isEnabled = false
        btnEditSave.text = "EDIT"
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(nameTxt.windowToken, 0)
    }

    private fun saveProfile() {
        val name = nameTxt.text.toString().trim()
        val lastName = lastNameTxt.text.toString().trim()
        if (name.isEmpty() || lastName.isEmpty()) return toast("Name and Last Name cannot be empty")
        CoroutineScope(Dispatchers.IO).launch {
            val ok = runCatching { apiService.updateProfile(session.token ?: "", name, lastName) }.getOrNull()
            withContext(Dispatchers.Main) {
                if (ok != null) { session.updateUserInfo(name, lastName); loadUserData(); exitEditMode(); toast("Profile updated") }
                else toast("Failed to update profile")
            }
        }
    }

    private fun logout() {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { apiService.logout(session.token ?: "") }
            withContext(Dispatchers.Main) {
                session.logout()
                startActivity(Intent(this@ProfileActivity, LoginActivity::class.java)
                    .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
                finish()
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
