package com.example.nerevian.utils

import com.example.nerevian.R
import android.content.Context
import android.content.Intent
import com.example.nerevian.client.GameActivity
import com.example.nerevian.common.BaseHistoryActivity
import com.example.nerevian.common.HomePageActivity
import com.example.nerevian.common.ProfileActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class NavigationBar(private val context: Context) {

    fun setup(navView: BottomNavigationView) {
        val session = SessionManager(context)
        val roleId = session.rolId

        if (roleId == SessionManager.ROL_AGENT) {
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

            targetActivity?.let {
                if (context::class.java != it) {
                    val intent = Intent(context, it)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    context.startActivity(intent)
                }
            }
            true
        }

        navView.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.nav_history) {
                (context as? BaseHistoryActivity)?.refreshHistory()
            }
        }
    }
}
