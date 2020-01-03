package com.andvl1.engrade

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView

class Table : AppCompatActivity() {

    private var mTable: TableLayout? = null
    private lateinit var names: ArrayList<String>
    var mName1: TextView? = null
    var mName2: TextView? = null
    var mName3: TextView? = null
    var mName4: TextView? = null
    var mName5: TextView? = null
    var mName6: TextView? = null
    var mName7: TextView? = null
    var amount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_table)

        findViews()
        val intent = intent
        mTable!!.isStretchAllColumns = true
        names =  intent.getStringArrayListExtra("names")!!
        amount = intent.getIntExtra("amount", 5)
        mName1!!.text = names[0]
        mName2!!.text = names[1]
        mName3!!.text = names[2]
        mName4!!.text = names[3]
        mName5!!.text = names[4]
        when (amount) {
            5 -> {
                setSixthInvisible()
                setSeventhInvisible()
            }
            6 -> {
                mName6!!.text = names[5]
                setSeventhInvisible()
            }
            7 -> {
                mName6!!.text = names[5]
                mName7!!.text = names[6]
            }
        }
    }

    private fun setSixthInvisible() {
        findViewById<TableRow>(R.id.athRow6).visibility = View.INVISIBLE
        findViewById<TextView>(R.id.ath6).visibility = View.INVISIBLE
        findViewById<TextView>(R.id.b16).visibility = View.INVISIBLE
        findViewById<TextView>(R.id.b26).visibility = View.INVISIBLE
        findViewById<TextView>(R.id.b36).visibility = View.INVISIBLE
        findViewById<TextView>(R.id.b46).visibility = View.INVISIBLE
        findViewById<TextView>(R.id.b56).visibility = View.INVISIBLE
    }

    private fun setSeventhInvisible() {
        findViewById<TableRow>(R.id.athRow7).visibility = View.INVISIBLE
        findViewById<TextView>(R.id.ath7).visibility = View.INVISIBLE
        findViewById<TextView>(R.id.b17).visibility = View.INVISIBLE
        findViewById<TextView>(R.id.b27).visibility = View.INVISIBLE
        findViewById<TextView>(R.id.b37).visibility = View.INVISIBLE
        findViewById<TextView>(R.id.b47).visibility = View.INVISIBLE
        findViewById<TextView>(R.id.b57).visibility = View.INVISIBLE
        findViewById<TextView>(R.id.b67).visibility = View.INVISIBLE
    }

    private fun findViews() {
        mTable = findViewById(R.id.pooleTable)
        mName1 = findViewById(R.id.athName1)
        mName2 = findViewById(R.id.athName2)
        mName3 = findViewById(R.id.athName3)
        mName4 = findViewById(R.id.athName4)
        mName5 = findViewById(R.id.athName5)
        mName6 = findViewById(R.id.athName6)
        mName7 = findViewById(R.id.athName7)
    }
}
