package com.andvl1.engrade

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View

class CardActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.card)

        val v = findViewById<View>(R.id.yellow_card)

        val red = intent.getBooleanExtra("red", false)

        if (red) {
            v.setBackgroundColor(Color.RED)
        } else {
            v.setBackgroundColor(Color.YELLOW)
        }
    }

    fun dismiss(view: View) {
        finish()
    }
}
