package com.example.nerevian.agent

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.nerevian.R

import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.example.nerevian.network.ApiService
import com.example.nerevian.utils.NavigationBar
import com.example.nerevian.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class UpdateTrackingActivity : AppCompatActivity() {

    private val apiService = ApiService()
    private lateinit var session: SessionManager
    private var offerId: Int = -1

    private lateinit var tvOrderTitle: TextView
    private lateinit var tvCurrentStatus: TextView
    private lateinit var spinnerNewStatus: Spinner
    private lateinit var btnSave: TextView
    private lateinit var btnCancel: TextView

    private var statusList = mutableListOf<Pair<Int, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_tracking)
        
        session = SessionManager(this)
        offerId = intent.getIntExtra("offer_id", -1)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets

        }

        tvOrderTitle = findViewById(R.id.tv_order_title)
        tvCurrentStatus = findViewById(R.id.tv_current_status)
        spinnerNewStatus = findViewById(R.id.spinner_new_status)
        btnSave = findViewById(R.id.btn_save_changes)
        btnCancel = findViewById(R.id.btn_cancel)

        tvOrderTitle.text = "Order #$offerId"

        val navView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        navView.setOnItemSelectedListener(null)
        navView.selectedItemId = R.id.nav_history
        NavigationBar(this).setup(navView)

        loadTrackingData()

        btnCancel.setOnClickListener { finish() }
        btnSave.setOnClickListener { updateTracking() }
    }

    private fun loadTrackingData() {
        if (offerId == -1 || session.token == null) return

        CoroutineScope(Dispatchers.IO).launch {
            val currentResult = apiService.getCurrentTracking(session.token!!, offerId)
            val optionsResult = apiService.getTrackingOptions(session.token!!, offerId)
            
            withContext(Dispatchers.Main) {
                if (currentResult != null) {
                    val data = currentResult.optJSONObject("data")
                    val currentStepName = data?.optString("nom") ?: "N/A"
                    tvCurrentStatus.text = currentStepName
                }

                if (optionsResult != null) {
                    val allSteps = optionsResult.optJSONArray("data") ?: JSONArray()
                    statusList.clear()
                    val spinnerItems = mutableListOf<String>()
                    spinnerItems.add("Choose status")
                    
                    var currentStepId: Int = -1
                    if (currentResult != null) {
                        currentStepId = currentResult.optJSONObject("data")?.optInt("id", -1) ?: -1
                    }

                    var selectedIndex = 0
                    for (i in 0 until allSteps.length()) {
                        val step = allSteps.getJSONObject(i)
                        val id = step.getInt("id")
                        val name = step.getString("nom")
                        statusList.add(id to name)
                        spinnerItems.add(name)
                        
                        if (id == currentStepId) {
                            selectedIndex = i + 1 // +1 because of "Choose status"
                        }
                    }

                    val adapter = ArrayAdapter(this@UpdateTrackingActivity, android.R.layout.simple_spinner_item, spinnerItems)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerNewStatus.adapter = adapter
                    
                    if (selectedIndex > 0) {
                        spinnerNewStatus.setSelection(selectedIndex)
                    }
                }
            }
        }
    }

    private fun updateTracking() {
        val selectedPosition = spinnerNewStatus.selectedItemPosition
        if (selectedPosition <= 0) {
            Toast.makeText(this, "Please select a new status", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedStepId = statusList[selectedPosition - 1].first
        
        CoroutineScope(Dispatchers.IO).launch {
            val result = apiService.patchTracking(session.token!!, offerId, selectedStepId)
            
            withContext(Dispatchers.Main) {
                if (result != null) {
                    Toast.makeText(this@UpdateTrackingActivity, "Tracking updated successfully", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@UpdateTrackingActivity, "Failed to update tracking", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}