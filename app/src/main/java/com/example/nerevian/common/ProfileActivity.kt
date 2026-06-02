package com.example.nerevian.common

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class ProfileActivity : AppCompatActivity() {

    private val SERVER_HOST = "192.168.1.48"
    private val SERVER_PORT = 9090
    private val arxiusDir get() = File(filesDir, "arxius").also { it.mkdirs() }

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
        if (ok) uploadDni(tempCameraFile, "dni_photo.jpg", "image/jpeg")
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) result.data?.data?.let { uploadDniFromUri(it) }
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
        findViewById<Button>(R.id.btn_download_dni).setOnClickListener { downloadDni() }
        findViewById<Button>(R.id.btn_logout).setOnClickListener { logout() }

        NavigationBar(this).setup(findViewById<BottomNavigationView>(R.id.bottom_navigation))
    }

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

    private fun uploadDni(src: File, filename: String, mime: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val ok = uploadFileToServer(src, filename)
            withContext(Dispatchers.Main) {
                if (ok) {
                    dniPrefs.edit().putString("name", filename).putString("mime", mime).apply()
                    etId.setText(filename)
                    toast("DNI uploaded to server")
                } else {
                    toast("Upload failed — is the server running?")
                }
            }
        }
    }

    private fun uploadDniFromUri(uri: Uri) {
        val filename = contentResolver.query(uri, null, null, null, null)?.use { c ->
            val col = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (c.moveToFirst() && col >= 0) c.getString(col) else null
        } ?: uri.lastPathSegment ?: "dni_document"
        val mime = contentResolver.getType(uri) ?: "application/octet-stream"

        CoroutineScope(Dispatchers.IO).launch {
            val tempFile = File(cacheDir, "temp_upload_${System.currentTimeMillis()}")
            var ok = false
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                }
                ok = uploadFileToServer(tempFile, filename)
            } finally {
                tempFile.delete()
            }
            withContext(Dispatchers.Main) {
                if (ok) {
                    dniPrefs.edit().putString("name", filename).putString("mime", mime).apply()
                    etId.setText(filename)
                    toast("DNI uploaded to server")
                } else {
                    toast("Upload failed — is the server running?")
                }
            }
        }
    }

    private fun uploadFileToServer(file: File, filename: String): Boolean {
        return try {
            Socket(SERVER_HOST, SERVER_PORT).use { socket ->
                val out = socket.getOutputStream()
                val inp = socket.getInputStream()
                sendLine(out, filename)
                sendLine(out, "UPLOAD")
                sendLine(out, file.length().toString())
                FileInputStream(file).use { fis ->
                    var b = fis.read()
                    while (b != -1) {
                        out.write(b)
                        b = fis.read()
                    }
                }
                out.flush()
                readLine(inp) == "OK"
            }
        } catch (e: Exception) {
            android.util.Log.e("FileTransfer", "Upload error: ${e.javaClass.simpleName}: ${e.message}", e)
            false
        }
    }

    private fun downloadDni() {
        val filename = dniPrefs.getString("name", null) ?: return toast("No DNI uploaded yet")
        val mime     = dniPrefs.getString("mime", "*/*") ?: "*/*"
        val destFile = File(arxiusDir, filename)

        CoroutineScope(Dispatchers.IO).launch {
            val result = downloadFileFromServer(filename, destFile)
            withContext(Dispatchers.Main) {
                when (result) {
                    DownloadResult.OK       -> openFile(destFile, mime)
                    DownloadResult.NOT_FOUND -> toast("File not found on server")
                    DownloadResult.ERROR    -> toast("Download failed — is the server running?")
                }
            }
        }
    }

    private fun downloadFileFromServer(filename: String, destFile: File): DownloadResult {
        return try {
            Socket(SERVER_HOST, SERVER_PORT).use { socket ->
                val out = socket.getOutputStream()
                val inp = socket.getInputStream()
                sendLine(out, filename)
                sendLine(out, "DOWNLOAD")
                when (readLine(inp)) {
                    "NOT_FOUND" -> DownloadResult.NOT_FOUND
                    "EXISTS" -> {
                        val fileSize = readLine(inp).toLong()
                        FileOutputStream(destFile).use { fos ->
                            var received = 0L
                            while (received < fileSize) {
                                val b = inp.read()
                                if (b == -1) break
                                fos.write(b)
                                received++
                            }
                        }
                        DownloadResult.OK
                    }
                    else -> DownloadResult.ERROR
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FileTransfer", "Download error: ${e.javaClass.simpleName}: ${e.message}", e)
            DownloadResult.ERROR
        }
    }

    private fun sendLine(out: OutputStream, text: String) {
        out.write(text.toByteArray(Charsets.UTF_8))
        out.write('\n'.code)
        out.flush()
    }

    private fun readLine(inp: InputStream): String {
        val sb = StringBuilder()
        var b = inp.read()
        while (b != -1 && b != '\n'.code) {
            sb.append(b.toChar())
            b = inp.read()
        }
        return sb.toString()
    }

    private enum class DownloadResult { OK, NOT_FOUND, ERROR }

    private fun openFile(file: File, mime: String) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mime)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Open DNI"
            ))
        } catch (e: Exception) {
            toast("Cannot open file: ${e.message}")
        }
    }

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
