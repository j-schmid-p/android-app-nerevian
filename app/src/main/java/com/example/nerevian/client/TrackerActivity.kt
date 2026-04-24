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

        loadTracking()
        setupNavigationBar()
    }

    private fun loadTracking() {
        val session = SessionManager(this)
        val offerId = intent.getIntExtra("offer_id", -1)


        if (offerId != -1 || session.token != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val optionsResult = apiService.getTrackingOptions(session.token!!, offerId)
                val currentResult = apiService.getCurrentTracking(session.token!!, offerId)

                withContext(Dispatchers.Main) {
                    if (optionsResult == null) {
                        return@withContext
                    }

                    val optionsArray = optionsResult.optJSONArray("tracking_steps") ?: JSONArray()
                    val currentStepObj = currentResult?.optJSONObject("tracking_step")
                    val currentId = currentStepObj?.optInt("id", -1) ?: -1

                    // Create a list of pairs (id, name) to sort them correctly
                    val stepsList = mutableListOf<Pair<Int, String>>()
                    for (i in 0 until optionsArray.length()) {
                        val step = optionsArray.getJSONObject(i)
                        stepsList.add(Pair(step.getInt("id"), step.getString("nom")))
                    }

                    // Sort steps by ID: lowest ID first as requested
                    stepsList.sortBy { it.first }

                    val stepsNames = mutableListOf<String>()
                    var currentStepIndex = -1

                    stepsList.forEachIndexed { index, pair ->
                        stepsNames.add(pair.second)
                        if (pair.first == currentId) {
                            currentStepIndex = index
                        }
                    }

                    renderSteps(stepsNames, currentStepIndex)
                }
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
                    // Completed steps: Dark circle, light italic font
                    circle.setImageResource(R.drawable.tracking_dark_circle)
                    circle.layoutParams.width = (60 * resources.displayMetrics.density).toInt()
                    circle.layoutParams.height = (60 * resources.displayMetrics.density).toInt()
                    
                    label.setTextColor(ContextCompat.getColor(this, R.color.darkestBlueMainText))
                    label.typeface = ResourcesCompat.getFont(this, R.font.jost_extralightitalic)
                    label.textSize = 22f
                }
                index == currentStepIndex -> {
                    // Current active step: Dark circle, BOLD font, larger text
                    circle.setImageResource(R.drawable.tracking_dark_circle)
                    // Make current circle even larger
                    circle.layoutParams.width = (80 * resources.displayMetrics.density).toInt()
                    circle.layoutParams.height = (80 * resources.displayMetrics.density).toInt()
                    
                    label.setTextColor(ContextCompat.getColor(this, R.color.darkestBlueMainText))
                    label.typeface = ResourcesCompat.getFont(this, R.font.jost_bold)
                    label.textSize = 28f
                }
                else -> {
                    // Future steps: Light circle, light italic font
                    circle.setImageResource(R.drawable.tracking_light_circle)
                    circle.layoutParams.width = (60 * resources.displayMetrics.density).toInt()
                    circle.layoutParams.height = (60 * resources.displayMetrics.density).toInt()
                    
                    label.setTextColor(ContextCompat.getColor(this, R.color.darkestBlueMainText))
                    label.typeface = ResourcesCompat.getFont(this, R.font.jost_extralightitalic)
                    label.textSize = 22f
                }
            }

            stepsContainer.addView(stepView)

            // Add an arrow after every step EXCEPT the last one
            if (index < steps.size - 1) {
                val arrowView = inflater.inflate(R.layout.item_tracking_arrow, stepsContainer, false)
                stepsContainer.addView(arrowView)
            }
        }
    }

    private fun setupNavigationBar() {
        val navView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        NavigationBar(this).setup(navView)
    }
}
