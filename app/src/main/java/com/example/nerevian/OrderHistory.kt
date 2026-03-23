package com.example.nerevian

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat

class OrderHistory : AppCompatActivity() {

    private var isAcceptedExpanded = false
    private var isPendingExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.order_activity)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        setupExpandableOrders()
        setupNavigation()
    }

    private fun setupExpandableOrders() {
        // Accepted Order
        val acceptedHeader = findViewById<View>(R.id.order_accepted_header)
        val acceptedDetail = findViewById<LinearLayout>(R.id.order_detail_view)
        val btnTrack = findViewById<TextView>(R.id.btn_track)

        acceptedHeader.setOnClickListener {
            if (isAcceptedExpanded) collapse(acceptedDetail) else expand(acceptedDetail)
            isAcceptedExpanded = !isAcceptedExpanded
        }

        btnTrack.setOnClickListener {
            val intent = Intent(this, TrackerActivity::class.java)
            startActivity(intent)
        }

        // Pending Order
        val pendingHeader = findViewById<View>(R.id.order_pending_header)
        val pendingDetail = findViewById<LinearLayout>(R.id.order_pending_detail)
        pendingHeader.setOnClickListener {
            if (isPendingExpanded) collapse(pendingDetail) else expand(pendingDetail)
            isPendingExpanded = !isPendingExpanded
        }
    }

    private fun expand(v: View) {
        v.visibility = View.VISIBLE
        v.alpha = 0f
        v.translationY = -20f
        v.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun collapse(v: View) {
        v.animate()
            .alpha(0f)
            .translationY(-20f)
            .setDuration(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction { v.visibility = View.GONE }
            .start()
    }

    private fun setupNavigation() {
        val navHome = findViewById<ImageView>(R.id.nav_home)
        val navHistory = findViewById<ImageView>(R.id.nav_history)
        val navProfile = findViewById<ImageView>(R.id.nav_profile)

        navHome.setOnClickListener {
            startActivity(Intent(this, HomePageActivity::class.java))
            finish()
        }
        navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }
}
