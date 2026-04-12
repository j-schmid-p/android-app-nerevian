package com.example.nerevian.common

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.nerevian.R
import com.example.nerevian.utils.NavigationBar
import com.google.android.material.bottomnavigation.BottomNavigationView

abstract class BaseHistoryActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupCommonUI()
        setupSpecificLogic()
    }

    private fun setupCommonUI(){
        val navView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        NavigationBar(this).setup(navView)
    }

    abstract fun setupSpecificLogic()
}