package com.andvl1.engrade


import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import androidx.fragment.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle
import android.view.View

/**
 * Created by ethan on 7/23/14.
 */
class CardAlertFragment : DialogFragment() {
    private var mViewId: Int = 0
    private lateinit var mListener: CardAlertListener

    val title: String
        get() {
            when (mViewId) {
                R.id.yellowCardButton -> return "" + R.string.yellow_card_dialog
                R.id.redCardButton -> return "" + R.string.red_card_dialog
            }

            return "Neither yellow nor red card."
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewId = arguments!!.getInt("view.getId()", mViewId)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        val builder = AlertDialog.Builder(activity)
        var whichArray = -1
        val text = arrayOf(
            arrayOf(resources.getString(R.string.yellow_card_dialog), "0"),
            arrayOf(resources.getString(R.string.red_card_dialog), "1")
        )

        when (mViewId) {
            R.id.yellowCardButton -> whichArray = 0
            R.id.redCardButton -> whichArray = 1
        }

        val index = whichArray
        builder.setTitle(text[whichArray][0])
            .setItems(R.array.fencer_names) { dialog, which ->
                //TODO: switch to ListAdapter to allow for dynamic names
// The 'which' argument contains the index position of the selected item
                val mainActivity = activity as MainActivity
                mainActivity.giveCard(which, Integer.parseInt(text[index][1]))
            }

        return builder.create()
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        try {
            // Instantiate the CardAlertListener so we can send events to the host
            mListener = activity as CardAlertListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException("$activity must implement CardAlertListener")
        }

    }

    interface CardAlertListener {
        fun onDialogClick(dialogFragment: DialogFragment, fencer: Int, cardType: Int)
        //        public void onDialogClickBottom(DialogFragment dialogFragment, int fencer, int cardType);
    }

    companion object {

        fun newInstance(view: View): CardAlertFragment {
            val cardAlertFragment = CardAlertFragment()

            val args = Bundle()
            args.putInt("view.getId()", view.id)
            cardAlertFragment.arguments = args

            return cardAlertFragment
        }
    }
}
