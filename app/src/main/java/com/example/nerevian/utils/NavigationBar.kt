package com.example.nerevian.utils

import android.content.ClipData
import com.example.nerevian.R
import android.content.Context
import android.content.Intent
import android.media.RouteListingPreference
import android.provider.ContactsContract
import android.widget.ImageView
import com.example.nerevian.client.GameActivity
import com.example.nerevian.client.TrackerActivity
import com.example.nerevian.common.BaseHistoryActivity
import com.example.nerevian.common.HomePageActivity
import com.example.nerevian.common.ProfileActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class NavigationBar (private val context: Context){

    private val ROL_CLIENT = 1
    private val ROL_AGENT = 3

    fun setup(navView: BottomNavigationView){
        val preferences = context.getSharedPreferences("session", Context.MODE_PRIVATE)
        val roleId = preferences.getInt("rol_id", 0)

        if (roleId == ROL_AGENT){
            navView.menu.findItem(R.id.nav_games)?.isVisible = false
        }

        navView.setOnItemSelectedListener { item ->
            val targetActivity = when (item.itemId) {
                R.id.nav_home -> HomePageActivity::class.java
                R.id.nav_history -> BaseHistoryActivity::class.java
                R.id.nav_profile -> ProfileActivity::class.java
                R.id.nav_games -> GameActivity::class.java
                else -> null
            }

            if (targetActivity == null && item.itemId == R.id.nav_home) {
                 // Special case if HomePageActivity logic needs it, but it's handled by default usually.
            }

            targetActivity?.let {
                if (context::class.java != it) {
                    val intent = Intent(context, it)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    context.startActivity(intent)
                }
            }
            true
        }
    }
}