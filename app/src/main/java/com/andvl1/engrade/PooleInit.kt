package com.andvl1.engrade

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PooleInit : AppCompatActivity() {

    private var amount = 6
    private var mName1 :TextView?     = null
    private var mName2 :TextView?     = null
    private var mName3 :TextView?     = null
    private var mName4 :TextView?     = null
    private var mName5 :TextView?     = null
    private var mName6 :TextView?     = null
    private var mName7 :TextView?     = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_poule_init)

        mName1 = findViewById(R.id.name1)
        mName2 = findViewById(R.id.name2)
        mName3 = findViewById(R.id.name3)
        mName4 = findViewById(R.id.name4)
        mName5 = findViewById(R.id.name5)
        mName6 = findViewById(R.id.name6)
        mName7 = findViewById(R.id.name7)
    }

    fun selectAmount(view: View) {
        val items = arrayOf<CharSequence>("5", "6", "7")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose fencers amount") // TODO добавить эти фразы в strings с переводом
            .setItems(items) { _, which ->
                amount = items[which].toString().toInt()
                when (amount) {
                    5 -> {
                        mName6!!.visibility = View.INVISIBLE
                        mName7!!.visibility = View.INVISIBLE
                    }
                    6 -> {
                        mName6!!.visibility = View.VISIBLE
                        mName7!!.visibility = View.INVISIBLE
                    }
                    7 -> {
                        mName6!!.visibility = View.VISIBLE
                        mName7!!.visibility = View.VISIBLE
                    }
                }
            }
            .show()
    }

    fun moveToNext(view: View) {
        if (!checkAmount()) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Some fields are empty")
                .setMessage("You cannot move further until some fields are empty")
                .setNeutralButton("Ok") { _, _ -> }
                .show()
        } else {
            val intent = Intent(applicationContext, Table::class.java)
            val poole = arrayListOf(mName1!!.text.toString(), mName2!!.text.toString(),
                mName3!!.text.toString(), mName4!!.text.toString(), mName5!!.text.toString())
            if (amount == 6) {
                poole.add(mName6!!.text.toString())
            } else if (amount == 7) {
                poole.add(mName6!!.text.toString())
                poole.add(mName7!!.text.toString())
            }
            intent.putExtra("names", poole)
            intent.putExtra("amount", amount)
            startActivity(intent)
        }
    }

    private fun checkAmount():Boolean {
        Log.d("check", "enter")
        if (mName1!!.text.toString() == ""|| mName2!!.text.toString() == "" || mName3!!.text.toString() == "" || mName4!!.text.toString() == "" || mName5!!.text.toString() == "") {
            Log.d("check", "false")
            return false
        }
        when(amount) {
            6 -> {
                if (mName6!!.text.toString() == ""){
                    Log.d("check", "false")
                    return false
                }
            }
            7 -> {
                if (mName6!!.text.toString() == "" || mName7!!.text.toString() == "") {
                    Log.d("check", "false")
                    return false
                }
            }
        }
        Log.d("check", "true")
        return true
    }
}
