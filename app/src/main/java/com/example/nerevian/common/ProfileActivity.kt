package com.example.nerevian.common

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var btnUploadDni: Button
    private lateinit var btnDownloadDni: Button
    private lateinit var profileNameHeader: TextView
    private var isEditMode = false

    private val pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult
                handleDniUpload(uri)
            }
        }

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

        nameTxt        = findViewById(R.id.et_name)
        lastNameTxt    = findViewById(R.id.et_last_name)
        etId           = findViewById(R.id.et_id)
        btnEditSave    = findViewById(R.id.btn_edit_save)
        btnUploadDni   = findViewById(R.id.btn_upload_dni)
        btnDownloadDni = findViewById(R.id.btn_download_dni)
        profileNameHeader = findViewById(R.id.profile_name_header)

        loadUserData()

        btnEditSave.setOnClickListener {
            if (isEditMode) {
                saveProfile()
            } else {
                toggleEditMode(true)
                nameTxt.requestFocus()
                nameTxt.requestFocus()
                val inputManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                inputManager.showSoftInput(nameTxt, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        btnUploadDni.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "image/*"))
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            pickFileLauncher.launch(intent)
        }

        btnDownloadDni.setOnClickListener {
            downloadDni()
        }

        logout()
        setupNavigationBar()
    }

    private fun loadUserData() {
        nameTxt.setText(session.name)
        lastNameTxt.setText(session.lastName)
        etId.setText(session.email)
        profileNameHeader.text = " ${session.name} ${session.lastName}"
    }

    private fun saveProfile() {
        val name     = nameTxt.text.toString().trim()
        val lastName = lastNameTxt.text.toString().trim()

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
        nameTxt.isEnabled    = enable
        lastNameTxt.isEnabled = enable

        if (enable) {
            btnEditSave.text = "SAVE CHANGES"
        } else {
            btnEditSave.text = "EDIT"
            val inputManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(nameTxt.windowToken, 0)
        }
    }
    private fun handleDniUpload(uri: Uri) {
        val token = session.token
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired, please log in again", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fileName = resolveFileName(uri) ?: "dni_document"
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

                val fileSizeBytes = resolveFileSize(uri)
                val maxBytes = 10 * 1024 * 1024L // 10 MB
                if (fileSizeBytes > maxBytes) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ProfileActivity,
                            "File is too large. Maximum size is 10 MB.",
                            Toast.LENGTH_LONG
                                      ).show()
                    }
                    return@launch
                }

                val tempFile = File(cacheDir, fileName)
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                }

                withContext(Dispatchers.Main) {
                    btnUploadDni.isEnabled = false
                    btnUploadDni.text      = "Uploading…"
                }

                val response = apiService.uploadDni(token, tempFile, mimeType)
                tempFile.delete()

                withContext(Dispatchers.Main) {
                    btnUploadDni.isEnabled = true
                    btnUploadDni.text      = "UPLOAD DNI"

                    if (response != null && response.optString("message").contains("success", ignoreCase = true)) {
                        val docName = response.optJSONObject("document")?.optString("name") ?: fileName
                        etId.setText(docName)
                        Toast.makeText(this@ProfileActivity, "DNI uploaded successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorMsg = response?.optString("message") ?: "Unknown error"
                        Toast.makeText(this@ProfileActivity, "Upload failed: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnUploadDni.isEnabled = true
                    btnUploadDni.text      = "UPLOAD DNI"
                    Toast.makeText(this@ProfileActivity, "Upload error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun downloadDni() {
        val token = session.token
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired, please log in again", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.Main) {
                    btnDownloadDni.isEnabled = false
                    btnDownloadDni.text = "Downloading…"
                }

                // Paso 1: metadata
                val metaResponse = apiService.getDniMetadata(token)

                // Log para ver qué devuelve exactamente el servidor
                android.util.Log.d("DNI", "Meta response: $metaResponse")

                val document = metaResponse?.optJSONObject("document")
                val downloadUrl = document?.optString("download_url")
                val docName = document?.optString("name") ?: "dni_document"

                if (downloadUrl.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        btnDownloadDni.isEnabled = true
                        btnDownloadDni.text = "DOWNLOAD DNI"
                        Toast.makeText(
                            this@ProfileActivity,
                            "No DNI uploaded yet",
                            Toast.LENGTH_SHORT
                                      ).show()
                    }
                    return@launch  // ← ahora sí cancela toda la coroutine
                }

                // Paso 2: descarga
                val downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: cacheDir
                val destFile = File(downloadsDir, docName)

                try {
                    apiService.downloadDniFile(downloadUrl, destFile)
                } catch (downloadEx: Exception) {
                    // URL expirada → reintento
                    val retryUrl = apiService.getDniMetadata(token)
                        ?.optJSONObject("document")
                        ?.optString("download_url")
                    if (!retryUrl.isNullOrEmpty()) {
                        apiService.downloadDniFile(retryUrl, destFile)
                    } else {
                        throw downloadEx
                    }
                }

                // Paso 3: abre el fichero
                withContext(Dispatchers.Main) {
                    btnDownloadDni.isEnabled = true
                    btnDownloadDni.text = "DOWNLOAD DNI"
                    openDownloadedFile(destFile)
                    Toast.makeText(
                        this@ProfileActivity,
                        "DNI downloaded: $docName",
                        Toast.LENGTH_SHORT
                                  ).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnDownloadDni.isEnabled = true
                    btnDownloadDni.text = "DOWNLOAD DNI"
                    Toast.makeText(
                        this@ProfileActivity,
                        "Download error: ${e.message}",
                        Toast.LENGTH_LONG
                                  ).show()
                }
            }
        }
    }

    private fun openDownloadedFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                file
                                                )
            val mimeType = contentResolver.getType(uri) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Open DNI"))
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resolveFileName(uri: Uri): String? {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
        return uri.lastPathSegment
    }

    private fun resolveFileSize(uri: Uri): Long {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0) return cursor.getLong(idx)
            }
        }
        return 0L
    }

    private fun logout() {
        val btnLogout = findViewById<Button>(R.id.btn_logout)
        btnLogout.setOnClickListener {
            val token = session.token
            CoroutineScope(Dispatchers.IO).launch {
                if (token != null) { apiService.logout(token) }
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