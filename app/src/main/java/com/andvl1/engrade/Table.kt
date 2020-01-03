package com.andvl1.engrade

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TableLayout

class Table : AppCompatActivity() {

    private var mTable: TableLayout? = null
    private lateinit var names: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_table)

        mTable = findViewById(R.id.pooleTable)
        val intent = intent
        mTable!!.isStretchAllColumns = true
        names =  intent.getStringArrayListExtra("names")!!
    }
}
