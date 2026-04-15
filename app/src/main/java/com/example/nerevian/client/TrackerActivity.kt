package com.example.nerevian.client

import android.content.Intent
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
import com.example.nerevian.common.HomePageActivity
import com.example.nerevian.common.BaseHistoryActivity
import com.example.nerevian.common.ProfileActivity
import com.example.nerevian.network.ApiService
import com.example.nerevian.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrackerActivity : AppCompatActivity() {

    private val apiService = ApiService()
    private lateinit var stepsContainer: LinearLayout

    private val allSteps = listOf(
        "Pickup at origin",
        "Arrival at consolidation warehouse",
        "Export customs clearance",
        "Departure from origin port/airport",
        "In international transit",
        "Arrival at destination port/airport",
        "Import customs clearance",
        "Out for delivery (Last mile)",
        "Delivered to final customer"
    )

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

        setupNavigation()
        loadTracking()
    }

    private fun loadTracking() {
        val session = SessionManager(this)
        val offerId = intent.getIntExtra("offer_id", -1)

        if (offerId == -1 || session.token == null) {
            // Si no hi ha oferta, mostrem els passos sense cap actiu
            renderSteps(currentStepIndex = -1)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val result = apiService.getTracking(session.token!!, offerId)

            withContext(Dispatchers.Main) {
                if (result == null) {
                    renderSteps(currentStepIndex = -1)
                    return@withContext
                }

                // La API retorna l'últim pas completat
                // Busquem quin índex correspon al tracking_step_id actual
                val trackingArray = result.optJSONArray("data")
                val lastCompletedOrder = if (trackingArray != null && trackingArray.length() > 0) {
                    // Agafem l'últim element (el més avançat)
                    var maxOrder = 0
                    for (i in 0 until trackingArray.length()) {
                        val step = trackingArray.getJSONObject(i)
                        val ordre = step.optJSONObject("step")?.optInt("ordre") ?: step.optInt("ordre", 0)
                        if (ordre > maxOrder) maxOrder = ordre
                    }
                    maxOrder
                } else {
                    0
                }

                // ordre va de 1 a 9, index va de 0 a 8
                renderSteps(currentStepIndex = lastCompletedOrder - 1)
            }
        }
    }

    private fun renderSteps(currentStepIndex: Int) {
        stepsContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        allSteps.forEachIndexed { index, stepName ->
            // Afegir el pas
            val stepView = inflater.inflate(R.layout.item_tracking_step, stepsContainer, false)
            val circle = stepView.findViewById<ImageView>(R.id.step_circle)
            val label = stepView.findViewById<TextView>(R.id.step_label)

            label.text = stepName

            when {
                index < currentStepIndex -> {
                    circle.setImageResource(R.drawable.tracking_dark_circle)
                    label.setTextColor(ContextCompat.getColor(this, R.color.darkestBlueMainText))
                    label.typeface = ResourcesCompat.getFont(this, R.font.jost_extralight)
                }
                index == currentStepIndex -> {
                    circle.setImageResource(R.drawable.tracking_light_circle)
                    circle.setImageResource(R.drawable.tracking_dark_circle)
                    label.setTextColor(ContextCompat.getColor(this, R.color.darkestBlueMainText))
                    label.typeface = ResourcesCompat.getFont(this, R.font.jost_semibold)
                }
                else -> {
                    circle.setImageResource(R.drawable.tracking_light_circle)
                    label.setTextColor(ContextCompat.getColor(this, R.color.darkestBlueMainText))
                    label.typeface = ResourcesCompat.getFont(this, R.font.jost_extralightitalic)
                }
            }

            stepsContainer.addView(stepView)

            if (index < allSteps.size - 1) {
                val arrowView = inflater.inflate(R.layout.item_tracking_arrow, stepsContainer, false)
                stepsContainer.addView(arrowView)
            }
        }
    }


    private fun setupNavigation() {
        val navHome = findViewById<ImageView>(R.id.nav_home)
        val navHistory = findViewById<ImageView>(R.id.nav_history)
        val navProfile = findViewById<ImageView>(R.id.nav_profile)

        navHome.setOnClickListener {
            startActivity(Intent(this, HomePageActivity::class.java))
            finish()
        }

        navHistory.setOnClickListener {
            startActivity(Intent(this, BaseHistoryActivity::class.java))
            finish()
        }

        navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }
}