package com.example.nerevian.agent

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
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
import org.json.JSONObject

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
        val initialStatus = intent.getStringExtra("current_status")

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
        if (initialStatus != null) {
            tvCurrentStatus.text = initialStatus
        }

        val navView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
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
                android.util.Log.d("UpdateTracking", "Current Result Raw: $currentResult")
                
                // 1. Update Current Status ONLY if the API returned a valid step
                // This prevents overwriting the Intent data with "No tracking set" on 404 errors
                val stepObj = currentResult?.optJSONObject("tracking_step")
                var currentStepId = -1
                if (stepObj != null) {
                    val currentStepName = stepObj.optString("nom") ?: "No tracking set"
                    tvCurrentStatus.text = currentStepName
                    currentStepId = stepObj.optInt("id", -1)
                }

                if (optionsResult != null) {
                    android.util.Log.d("UpdateTracking", "Options Result Raw: $optionsResult")
                    
                    // 2. Parse Options from "tracking_steps" key
                    val stepsArray = optionsResult.optJSONArray("tracking_steps")
                    
                    statusList.clear()
                    val spinnerItems = mutableListOf<String>()
                    
                    if (stepsArray != null && stepsArray.length() > 0) {
                        var selectedIndex = 0
                        for (i in 0 until stepsArray.length()) {
                            val step = stepsArray.getJSONObject(i)
                            val id = step.getInt("id")
                            val name = step.getString("nom")
                            statusList.add(id to name)
                            spinnerItems.add(name)
                            
                            if (id == currentStepId) {
                                selectedIndex = i
                            }
                        }

                        val adapter = object : ArrayAdapter<String>(this@UpdateTrackingActivity, android.R.layout.simple_spinner_item, spinnerItems) {
                            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                                val v = super.getView(position, convertView, parent) as TextView
                                v.setTextColor(getColor(R.color.darkBlueTitle))
                                return v
                            }

                            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                                val v = super.getDropDownView(position, convertView, parent) as TextView
                                v.setTextColor(getColor(R.color.darkBlueTitle))
                                return v
                            }
                        }
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerNewStatus.adapter = adapter
                        
                        if (selectedIndex >= 0) {
                            spinnerNewStatus.setSelection(selectedIndex)
                        }
                    } else {
                        Toast.makeText(this@UpdateTrackingActivity, "No tracking options available for this Incoterm", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateTracking() {
        val selectedPosition = spinnerNewStatus.selectedItemPosition
        if (selectedPosition < 0) {
            Toast.makeText(this, "Please select a new status", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedStepPair = statusList[selectedPosition]
        val selectedStepId = selectedStepPair.first
        
        CoroutineScope(Dispatchers.IO).launch {
            // 1. Update Tracking Step
            val trackingResult = apiService.updateTrackingStep(session.token!!, offerId, selectedStepId)
            
            if (trackingResult != null) {
                // 2. Sync Offer Status based on Step ID
                val newStatusId = com.example.nerevian.utils.TrackingManager.getStatusForStepId(selectedStepId)
                if (newStatusId != -1) {
                    apiService.updateOfferStatus(session.token!!, offerId, newStatusId)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UpdateTrackingActivity, "Tracking updated successfully", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UpdateTrackingActivity, "Failed to update tracking. Check API permissions.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}