package com.example.nerevian.client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.res.ResourcesCompat
import com.example.nerevian.R
import com.example.nerevian.network.ApiService
import com.example.nerevian.utils.SessionManager
import com.example.nerevian.utils.NavigationBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.json.JSONArray
import org.json.JSONObject

class TrackerActivity : AppCompatActivity() {

    private val apiService = ApiService()
    private lateinit var stepsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tracker_activity)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        stepsContainer = findViewById(R.id.steps_container)

        val navView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        NavigationBar(this).setup(navView)

        loadTracking()
    }

    private fun loadTracking() {
        val session = SessionManager(this)
        val offerId = intent.getIntExtra("offer_id", -1)

        if (offerId == -1 || session.token == null) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val optionsResult = apiService.getTrackingOptions(session.token!!, offerId)
            val currentResult = apiService.getCurrentTracking(session.token!!, offerId)

            withContext(Dispatchers.Main) {
                if (optionsResult == null) {
                    return@withContext
                }

                val optionsArray = optionsResult.optJSONArray("data") ?: JSONArray()
                val currentData = currentResult?.optJSONObject("data")
                val currentId = currentData?.optInt("id", -1) ?: -1

                val steps = mutableListOf<String>()
                var currentStepIndex = -1

                for (i in 0 until optionsArray.length()) {
                    val step = optionsArray.getJSONObject(i)
                    val id = step.getInt("id")
                    val name = step.getString("nom")
                    steps.add(name)
                    if (id == currentId) {
                        currentStepIndex = i
                    }
                }

                renderSteps(steps, currentStepIndex)
            }
        }
    }

    private fun renderSteps(steps: List<String>, currentStepIndex: Int) {
        stepsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        steps.forEachIndexed { index, stepName ->
            val stepView = inflater.inflate(R.layout.item_tracking_step, stepsContainer, false)
            val circle = stepView.findViewById<ImageView>(R.id.step_circle)
            val label = stepView.findViewById<TextView>(R.id.step_label)

            label.text = stepName

            when {
                index < currentStepIndex -> {
                    // Completed steps
                    circle.setImageResource(R.drawable.tracking_dark_circle)
                    label.setTextColor(ContextCompat.getColor(this, R.color.darkestBlueMainText))
                    label.typeface = ResourcesCompat.getFont(this, R.font.jost_extralight)
                    label.alpha = 0.5f
                }
                index == currentStepIndex -> {
                    // Current active step
                    circle.setImageResource(R.drawable.tracking_dark_circle)
                    label.setTextColor(ContextCompat.getColor(this, R.color.darkestBlueMainText))
                    label.typeface = ResourcesCompat.getFont(this, R.font.jost_bold)
                    label.alpha = 1.0f
                }
                else -> {
                    // Future steps
                    circle.setImageResource(R.drawable.tracking_light_circle)
                    label.setTextColor(ContextCompat.getColor(this, R.color.darkestBlueMainText))
                    label.typeface = ResourcesCompat.getFont(this, R.font.jost_extralightitalic)
                    label.alpha = 0.5f
                }
            }

            stepsContainer.addView(stepView)

            if (index < steps.size - 1) {
                val arrowView = inflater.inflate(R.layout.item_tracking_arrow, stepsContainer, false)
                stepsContainer.addView(arrowView)
            }
        }
    }
}
