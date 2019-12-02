package com.andvl1.engrade

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

import java.util.Objects

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Objects.requireNonNull(actionBar!!).setDisplayHomeAsUpEnabled(true)
        // Display the fragment as the main content.


        val fm : FragmentManager = FragmentActivity().supportFragmentManager
        fm.beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
//        fragmentManager.beginTransaction()
//            .replace(android.R.id.content, SettingsFragment())
//            .commit()
    }
}