package com.example.nerevian.client

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.nerevian.common.ProfileActivity
import com.example.nerevian.R
import com.example.nerevian.common.BaseHistoryActivity
import com.example.nerevian.utils.NavigationBar
import com.google.android.material.bottomnavigation.BottomNavigationView

class OrderHistoryClientActivity : BaseHistoryActivity() {

    private var isAcceptedExpanded = false
    private var isPendingExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.order_activity)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        setupSpecificLogic()
    }

    override fun setupSpecificLogic() {
        setupExpandableOrders()
        setupNavigation()
    }

    private fun setupExpandableOrders() {
        val acceptedHeader = findViewById<View>(R.id.order_accepted_header)
        val acceptedDetail = findViewById<LinearLayout>(R.id.order_detail_view)
        val btnTrack = findViewById<TextView>(R.id.btn_track)

        acceptedHeader.setOnClickListener {
            if (isAcceptedExpanded) { collapse(acceptedDetail) }
            else { expand(acceptedDetail) }
            isAcceptedExpanded = !isAcceptedExpanded
        }

        btnTrack.setOnClickListener {
            startActivity(Intent(this, TrackerActivity::class.java))
        }

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
        val navView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        NavigationBar(this).setup(navView)
    }
}