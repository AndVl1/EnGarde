package com.andvl1.engrade

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceManager
import android.view.animation.Animation
import android.media.Ringtone
import android.os.Vibrator
import android.os.CountDownTimer
import android.view.MenuItem
import java.util.*
import android.content.SharedPreferences
import android.graphics.Color
import android.view.WindowManager
import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.animation.AlphaAnimation
import android.content.Intent
import android.content.res.Configuration
import android.media.RingtoneManager
import android.media.effect.Effect
import android.os.VibrationEffect
import android.util.Log
import android.view.Menu
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.cengalabs.flatui.views.FlatButton
import com.cengalabs.flatui.views.FlatSeekBar
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import java.lang.Exception

class MainActivity : AppCompatActivity(), CardAlertFragment.CardAlertListener{
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 200)
    private lateinit var leftFencer: Fencer
    private lateinit var rightFencer: Fencer
    private var mTimeRemaining: Long = 0
    private var mPeriodLength: Long = 0
    private var mBreakLength: Long = 0
    private var mPriorityLength: Long = 0
    private var mStartVibrationPattern: LongArray? = null
    private var mEndVibrationPattern: LongArray? = null
    private var mPreviousTimesArray: LongArray? = null
    private var mPeriodNumber: Int = 0
    private var mNextSectionType: Int = 0
    private var mMode: Int = 0
    private var mWeapon: Int = 0 // 0 - sabre, 1 - foil/epee
    private var mMaxPeriods: Int = 0
    private var mRecentActionArray: IntArray? = null
    private var mPreviousPeriodNumbersArray: IntArray? = null
    private var mPreviousSectionTypesArray: IntArray? = null
    private var mTimer: TextView? = null
    private var mLeftScoreView: TextView? = null
    private var mRightScoreView: TextView? = null
    private var mPeriodView: TextView? = null
    private var mLeftWinnerView: TextView? = null
    private var mRightWinnerView: TextView? = null
    private var mLeftPenaltyIndicator: ImageView? = null
    private var mLeftPriorityIndicator: ImageView? = null
    private var mRightPenaltyIndicator: ImageView? = null
    private var mRightPriorityIndicator: ImageView? = null
    private var mTimerRunning: Boolean = false
    private var mInPeriod: Boolean = false
    private var mInBreak: Boolean = false
    private var mInPriority: Boolean = false
    private var mShowDouble: Boolean = false
    private var mBlackBackground: Boolean = false
    private var mIsOver: Boolean = false
    private var mCountDownTimer: CountDownTimer? = null
    private var mVibrator: Vibrator? = null
    private var mRinger: Ringtone? = null
    private var mBlink: Animation? = null
    private var mRecentActions: ArrayDeque<Int>? = null
    private var mPreviousPeriodNumbers: ArrayDeque<Int>? = null
    private var mPreviousSectionTypes: ArrayDeque<Int>? = null
    private var mPreviousTimes: ArrayDeque<Long>? = null
    private var mActionUndo: MenuItem? = null
    private var mMainLayout: RelativeLayout? = null
    private var mSnackBar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        var state: Bundle? = savedInstanceState
        super.onCreate(state)
        setContentView(R.layout.main_activity)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        leftFencer = Fencer()
        rightFencer = Fencer()

        mTimer = findViewById(R.id.timer)
        mLeftScoreView = findViewById(R.id.scoreOne)
        mRightScoreView = findViewById(R.id.scoreTwo)
        mLeftWinnerView = findViewById(R.id.winnerViewLeft)
        mLeftWinnerView!!.visibility = View.INVISIBLE
        mRightWinnerView = findViewById(R.id.winnerViewRight)
        mRightWinnerView!!.visibility = View.INVISIBLE
        mPeriodView = findViewById(R.id.periodView)
        mLeftPenaltyIndicator = findViewById(R.id.penaltyCircleViewLeft)
        mLeftPriorityIndicator = findViewById(R.id.priorityCircleViewLeft)
        mRightPenaltyIndicator = findViewById(R.id.penaltyCircleViewRight)
        mRightPriorityIndicator = findViewById(R.id.priorityCircleViewRight)
        mMainLayout = findViewById(R.id.mainLayout)

        if (state == null) state = Bundle()

        mPeriodLength = state.getLong("mPeriodLength", 3 * 60 * 1000)
        mBreakLength = state.getLong("mBreakLength", 1 * 60 * 1000)
        mPriorityLength = state.getLong("mPriorityLength", 1 * 60 * 1000)
        mTimeRemaining = state.getLong("mTimeRemaining", mPeriodLength)
        mPreviousTimesArray = state.getLongArray("mPreviousTimesArray")
        mPreviousPeriodNumbersArray = state.getIntArray("mPreviousPeriodNumbersArray")
        mPreviousSectionTypesArray = state.getIntArray("mPreviousSectionTypesArray")
        mTimerRunning = state.getBoolean("mTimerRunning", false)
        mPeriodNumber = state.getInt("mPeriodNumber", 1)
        mMode = state.getInt("mMode", 5)
        mShowDouble = state.getBoolean("mShowDouble", true)
        mBlackBackground = state.getBoolean("mBlackBackground", false)
        mInPeriod = state.getBoolean("mInPeriod", true)
        mInBreak = state.getBoolean("mInBreak", false)
        mInPriority = state.getBoolean("mInPriority", false)
        mRecentActionArray = state.getIntArray("mRecentActionArray")
        mIsOver = state.getBoolean("mIsOver", false)

        updateViews()
        loadSettings()

        if (mRecentActionArray == null)
            mRecentActions = ArrayDeque(0)
        else
            for (action in mRecentActionArray!!)
                mRecentActions!!.push(action)

        if (mPreviousTimesArray == null)
            mPreviousTimes = ArrayDeque(0)
        else
            for (time in mPreviousTimesArray!!)
                mPreviousTimes!!.push(time)

        if (mPreviousPeriodNumbersArray == null)
            mPreviousPeriodNumbers = ArrayDeque(0)
        else
            for (sectionType in mPreviousPeriodNumbersArray!!)
                mPreviousPeriodNumbers!!.push(sectionType)

        if (mPreviousSectionTypesArray == null)
            mPreviousSectionTypes = ArrayDeque(0)
        else
            for (sectionType in mPreviousSectionTypesArray!!)
                mPreviousSectionTypes!!.push(sectionType)

        // set-up blinking animation used when mTimer is paused TODO: make animation better (no fade)
        mBlink = AlphaAnimation(0.0f, 1.0f)
        mBlink!!.duration = 1000
        mBlink!!.startOffset = 0
        mBlink!!.repeatCount = Animation.INFINITE
        mBlink!!.repeatMode = Animation.START_ON_FIRST_FRAME

        // used to signal to user that mTimeRemaining has expired
        mVibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        mStartVibrationPattern = longArrayOf(0, 50, 100, 50)
        mEndVibrationPattern = longArrayOf(
            0, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50,
            500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50,
            100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50
        )

        var mAlert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        if (mAlert == null) { // mAlert is null, using backup
            mAlert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            // just in case
            if (mAlert == null) {
                // mAlert backup is null, using 2nd backup
                mAlert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
        }

        mRinger = RingtoneManager.getRingtone(applicationContext, mAlert)
        mSnackBar = Snackbar.make(mMainLayout!!, "", BaseTransientBottomBar.LENGTH_SHORT)
    }

    override fun onResume() {
        super.onResume()

        // reload recent actions, previous times, etc.
        if (mRecentActionArray == null)
            mRecentActions = ArrayDeque(0)
        else
            for (action in mRecentActionArray!!)
                mRecentActions!!.push(action)

        if (mPreviousTimesArray == null)
            mPreviousTimes = ArrayDeque(0)
        else
            for (time in mPreviousTimesArray!!)
                mPreviousTimes!!.push(time)

        if (mPreviousPeriodNumbersArray == null)
            mPreviousPeriodNumbers = ArrayDeque(0)
        else
            for (sectionType in mPreviousPeriodNumbersArray!!)
                mPreviousPeriodNumbers!!.push(sectionType)

        if (mPreviousSectionTypesArray == null)
            mPreviousSectionTypes = ArrayDeque(0)
        else
            for (sectionType in mPreviousSectionTypesArray!!)
                mPreviousSectionTypes!!.push(sectionType)

        loadSettings()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putLong("mTimeRemaining", mTimeRemaining)
        outState.putLong("mPeriodLength", mPeriodLength)
        outState.putLong("mPriorityLength", mPriorityLength)
        outState.putBoolean("mTimerRunning", mTimerRunning)
        outState.putInt("mPeriodNumber", mPeriodNumber)
        outState.putLong("mBreakLength", mBreakLength)
        outState.putInt("mMode", mMode)
        outState.putBoolean("mShowDouble", mShowDouble)
        outState.putBoolean("mBlackBackground", mBlackBackground)
        outState.putBoolean("mInPeriod", mInPeriod)
        outState.putBoolean("mInBreak", mInBreak)
        outState.putBoolean("mInPriority", mInPriority)

        mRecentActionArray = IntArray(mRecentActions!!.size)
        for (i in mRecentActions!!.size - 1 downTo 0) {
            mRecentActionArray!![i] = mRecentActions!!.pop()
        }
        outState.putIntArray("mRecentActionArray", mRecentActionArray)

        mPreviousTimesArray = LongArray(mPreviousTimes!!.size)
        if (!mPreviousTimes.isNullOrEmpty()) {
            for (i in mPreviousTimes!!.size - 1 downTo 0) {
                mPreviousTimesArray!![i] = mPreviousTimes!!.pop()
            }
        }
        outState.putLongArray("mPreviousTimesArray", mPreviousTimesArray)

        mPreviousPeriodNumbersArray = IntArray(mPreviousPeriodNumbers!!.size)
        if (!mPreviousPeriodNumbers.isNullOrEmpty()) {
            for (i in mPreviousPeriodNumbers!!.size - 1 downTo 0)
                mPreviousPeriodNumbersArray!![i] = mPreviousPeriodNumbers!!.pop()
        }
        outState.putIntArray("mPreviousPeriodNumbersArray", mPreviousPeriodNumbersArray)

        mPreviousSectionTypesArray = IntArray(mPreviousSectionTypes!!.size)
        if (!mPreviousSectionTypes.isNullOrEmpty()) {
            for (i in mPreviousSectionTypes!!.size downTo 0)
                mPreviousSectionTypesArray!![i] = mPreviousSectionTypes!!.pop()
        }
        outState.putIntArray("mPreviousSectionTypesArray", mPreviousSectionTypesArray)
    }

    fun resetAll(menuItem: MenuItem) { // onClick for action_reset
        resetScores()
        if (mTimeRemaining != mPeriodLength) resetTime()
        resetPriority()
        resetCards()
        resetPeriod()
        resetRecentActions()
        updateAll()
        resetOver()
        resetWinner()
    }

    private fun resetWinner() {
        mLeftWinnerView!!.visibility = View.INVISIBLE
        mRightWinnerView!!.visibility = View.INVISIBLE
        leftFencer.takeWinner(rightFencer.score)
        rightFencer.takeWinner(leftFencer.score)
    }

    private fun resetOver() {
        mIsOver = false
    }

    private fun updateAll() {
        updateViews()
        updateUndoButton()
        updateOver()
        updateWinner()
    }

    private fun updateWinner() =// make a TextView with "Winner" appear above winner's score
        if (mIsOver) {
            mIsOver = if (leftFencer.isWinner && !rightFencer.isWinner) {
                mLeftWinnerView!!.visibility = View.VISIBLE
                mRightWinnerView!!.visibility = View.INVISIBLE
                true
            } else if (rightFencer.isWinner && !leftFencer.isWinner) {
                mRightWinnerView!!.visibility = View.VISIBLE
                mLeftWinnerView!!.visibility = View.INVISIBLE
                true
            } else {
                mLeftWinnerView!!.visibility = View.INVISIBLE
                mRightWinnerView!!.visibility = View.INVISIBLE
                false
            }
        } else {
            mLeftWinnerView!!.visibility = View.INVISIBLE
            mRightWinnerView!!.visibility = View.INVISIBLE
            mIsOver = false
        }

    private fun updateOver() {
        mIsOver = leftFencer.score >= mMode || rightFencer.score >= mMode ||
                (mTimeRemaining == (0).toLong() && (mInPriority ||
                        leftFencer.score > rightFencer.score ||
                        rightFencer.score < leftFencer.score))
    }

    private fun resetRecentActions() {
        mRecentActions = ArrayDeque(0)
    }

    private fun resetPeriod() {
        mPeriodNumber = 1
        mInPeriod = true
        mInPriority = false
        mInBreak = false
    }

    private fun resetCards() {
        leftFencer.takeYellowCard()
        leftFencer.takeRedCard()
        rightFencer.takeYellowCard()
        rightFencer.takeRedCard()
        updatePenaltyIndicators()
    }

    private fun resetPriority() {
        mInPriority = false
        leftFencer.resetPriority()
        rightFencer.resetPriority()
        mLeftPriorityIndicator!!.visibility = View.INVISIBLE
        mRightPriorityIndicator!!.visibility = View.INVISIBLE
    }

    private fun resetTime() {
        mTimeRemaining = mPeriodLength
        mTimer!!.text = mTimeRemaining.toString()
        mTimer!!.setTextColor(Color.WHITE)
        updateTimer(mTimeRemaining)
        mTimerRunning = false
        mRinger!!.stop()
        mVibrator!!.cancel()
        mTimer!!.clearAnimation()
    }

    fun openSettings(menuItem: MenuItem) {
        val settingsIntent = Intent(applicationContext, SettingsActivity::class.java)
        startActivity(settingsIntent)
    }

    fun undoMostRecent(menuItem: MenuItem) {
        if (mRecentActions!!.isNotEmpty())
            undoAction(mRecentActions!!.pop())
        else
            Toast.makeText(applicationContext, "Already removed all actions", Toast.LENGTH_LONG/2).show()
    }

    private fun undoAction(action: Int?) {
        pauseTimer()
//        Toast.makeText(applicationContext, "$action number", Toast.LENGTH_SHORT).show()
        when (action) {
            0 -> {
                subScore(leftFencer)
                if (leftFencer.isWinner) leftFencer.takeWinner(rightFencer.score)
                updateWinner()
                updateViews()
                updateUndoButton()
            }
            1 -> {
                subScore(rightFencer)
                if (rightFencer.isWinner) rightFencer.takeWinner(leftFencer.score)
                updateWinner()
                updateViews()
                updateUndoButton()
            }
            2 -> {
                subBothScores()
                if (leftFencer.isWinner) leftFencer.takeWinner(rightFencer.score)
                else if (rightFencer.isWinner) rightFencer.takeWinner(leftFencer.score)
                updateViews()
                updateUndoButton()
            }
            3 -> {
                leftFencer.takeYellowCard()
                updatePenaltyIndicators()
            }
            4 -> {
                leftFencer.takeRedCard()
                subScore(rightFencer)
                updateViews()
                updateUndoButton()
            }
            5 -> {
                rightFencer.takeYellowCard()
                updatePenaltyIndicators()
            }
            6 -> {
                rightFencer.takeRedCard()
                subScore(leftFencer)
                updateViews()
                updateUndoButton()
            }
            7 -> {
                pauseTimer()
                mInPriority = false
                val previousTime = mPreviousTimes!!.pop()
                startTimer(previousTime)

                mNextSectionType = mPreviousSectionTypes!!.pop()
                mPeriodNumber = mPreviousPeriodNumbers!!.pop()

                if (mMaxPeriods == 1) {
                    mInPeriod = true
                    mPeriodNumber = 1
                } else {
                    if (mNextSectionType == 0) {
                        mInPeriod = true
                        mInBreak = false
                    } else if (mNextSectionType == 1) {
                        mInPeriod = false
                        mInBreak = true
                    }
                }

                onClickTimer(mTimer!!)
                mTimeRemaining = previousTime
                updateTimer(mTimeRemaining)
                pauseTimer()
                resetPriority()
                resetOver()
                updateTimer(mTimeRemaining)
                updateViews()
            }
        }
    }

    private fun startTimer(time: Long?) {
        if (!mIsOver) {
            mTimer!!.clearAnimation()
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_SIGNAL_OFF)
            mTimer!!.setTextColor(Color.WHITE)
//            mVibrator!!.vibrate(mStartVibrationPattern, -1)
            mVibrator!!.vibrate(200)
//            mVibrator!!.vibrate(VibrationEffect.createOneShot(5, 10))
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // keep screen awake when timer is running
            mCountDownTimer = object : CountDownTimer(time!!, 10) {
                override fun onTick(millisUntilFinished: Long) {
                    updateTimer(millisUntilFinished)
                    mTimeRemaining = millisUntilFinished
                }

                override fun onFinish() {
                    endSection()
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // turn screen-awake off when timer is expired
                }
            }.start()
            mTimerRunning = true
        }
    }

    @SuppressLint("SetTextI18n")
    private fun endSection() {
        mTimer!!.text = "0:00.00"
        mTimeRemaining = 0
        mTimer!!.setTextColor(resources.getColor(R.color.red_timer, applicationContext.theme)) // change timer to red
        // mTimer!!.setTextColor(resources.getColor(R.color.red_timer)); // change timer to red
        mTimer!!.animation = mBlink
//        mVibrator!!.vibrate(mEndVibrationPattern, -1)
        mVibrator!!.vibrate(3000)
//        mVibrator!!.vibrate(VibrationEffect.createOneShot(200, 30))
        mRinger!!.play()
        mTimerRunning = false

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // disable keep screen awake

        // Determine if the bout is in regulation time or overtime
        if (mInPriority) { // overtime
            if (leftFencer.score == rightFencer.score) {
                if (leftFencer.hasPriority() && !rightFencer.hasPriority()) { // left fencer won by priority
                    leftFencer.addScore()
                    leftFencer.makeWinner(rightFencer.score)
                    rightFencer.makeLoser(leftFencer.score)
                    mIsOver = true
                } else if (rightFencer.hasPriority() && !leftFencer.hasPriority()) { // right fencer won by priority
                    rightFencer.addScore()
                    rightFencer.makeWinner(leftFencer.score)
                    leftFencer.makeLoser(rightFencer.score)
                    mIsOver = true
                }
            } else {
                if (leftFencer.score > rightFencer.score) { // left fencer won in priority by a touch
                    leftFencer.makeWinner(rightFencer.score)
                    rightFencer.makeLoser(leftFencer.score)
                    mIsOver = true
                } else if (leftFencer.score < rightFencer.score) { // right fencer won in priority by a touch
                    leftFencer.makeLoser(rightFencer.score)
                    rightFencer.makeWinner(leftFencer.score)
                    mIsOver = true
                }
            }
        } else { // regulation time
            if (mPeriodNumber < mMaxPeriods) { // next period will also be regulation time
                if (mInPeriod) {
                    mNextSectionType = 1
                    mInPeriod = false
                } else if (mInBreak) {
                    mNextSectionType = 0
                    mInBreak = false
                }
            } else if (leftFencer.score > rightFencer.score) { // left fencer won in regulation time
                leftFencer.makeWinner(rightFencer.score)
                rightFencer.makeLoser(leftFencer.score)
                mIsOver = true
            } else if (leftFencer.score < rightFencer.score) { // right fencer won in regulation time
                leftFencer.makeLoser(rightFencer.score)
                rightFencer.makeWinner(leftFencer.score)
                mIsOver = true
            } else if (leftFencer.score == rightFencer.score) { // scores tied; go to priority
                mInBreak = false
                mInPeriod = mInBreak
                mNextSectionType = 2
            }
        }
    }

    private fun subScore(fencer: Fencer) {
        fencer.subtractScore()
    }

    private fun subBothScores() {
        leftFencer.subtractScore()
        rightFencer.subtractScore()
    }

    fun skipSection(menuItem: MenuItem) {
        if (mIsOver) {
            mPreviousTimes!!.push(mTimeRemaining)
            when {
                mInPriority -> mPreviousSectionTypes!!.push(2)
                mInBreak -> mPreviousSectionTypes!!.push(1)
                mInPeriod -> mPreviousSectionTypes!!.push(0)
            }
            mPreviousPeriodNumbers!!.push(mPeriodNumber)
            endSection()
            mRinger!!.stop()
            mVibrator!!.cancel()
            mRecentActions!!.push(7)
            if (!mInPeriod)
                showSnackbar(
                    resources.getString(R.string.toast_skipped),
                    "",
                    resources.getString(R.string.toast_period),
                    ""
                )
            else
                showSnackbar(
                    resources.getString(R.string.toast_skipped),
                    "",
                    resources.getString(R.string.toast_break),
                    ""
                )
        } else {
            pauseTimer()
            if (mInPriority)
                showSnackbar(
                    resources.getString(R.string.toast_unable), "",
                    resources.getString(R.string.toast_skip), resources.getString(R.string.toast_priority)
                )
        }
    }

    private fun resetScores() {
        leftFencer.resetScore()
        rightFencer.resetScore()
        if (mTimerRunning)
            mCountDownTimer!!.cancel()
        updateScores()
    }

    override fun onDialogClick(dialogFragment: DialogFragment, fencer: Int, cardType: Int) {
        giveCard(fencer, cardType)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.items, menu)
        mActionUndo = menu!!.findItem(R.id.action_undo)
        updateUndoButton()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id: Int = item.itemId
        return id == R.id.action_settings || super.onOptionsItemSelected(item)
    } // CODE HERE

    private fun updateUndoButton() {
        mActionUndo!!.isVisible = !mRecentActions.isNullOrEmpty()
    }

    fun onClickTimer(view: View) {
        if (mInPeriod || mInBreak || mInPriority) // if in a time section, start/stop
            countDown()
        else { // if in between sections, get ready for next one
            mVibrator!!.cancel()
            mRinger!!.stop()
            mTimer!!.setTextColor(Color.WHITE)
            mTimer!!.clearAnimation()
            when (mNextSectionType) {
                0 -> { // period
                    mTimeRemaining = mPeriodLength
                    nextPeriod()
                    mInPeriod = true
                }
                1 -> { // break
                    mTimeRemaining = mBreakLength
                    mInBreak = true
                }
                2 -> { // priority
                    mTimeRemaining = mPriorityLength
                    mInPriority = true
                    determinePriority()
                }
            }
            updateAll()
        }
    }

    private fun determinePriority() {
        val r = Math.random()
        if (r > .5) {
            leftFencer.givePriority()
            mLeftPriorityIndicator!!.visibility = View.VISIBLE
        } else {
            rightFencer.givePriority()
            mRightPriorityIndicator!!.visibility = View.VISIBLE
        }
    }

    private fun nextPeriod() {
        mPeriodNumber++
        updatePeriod()
    }

    private fun countDown() {
        mRinger!!.stop()
        mVibrator!!.cancel()
        if (mTimerRunning) pauseTimer()
        else startTimer(mTimeRemaining)
    }

    fun showCardDialog(view: View) {
        pauseTimer()
        val man = this.supportFragmentManager
        val cardDialog = CardAlertFragment.newInstance(view)
        cardDialog.show(man, "Penalty Card")
    }

    fun addScore(view: View) { // onClick for score textViews
        if (mIsOver) {
            showSnackbar(
                resources.getString(R.string.toast_unable), resources.getString(R.string.toast_give),
                resources.getString(R.string.toast_touch), resources.getString(R.string.toast_winner_determined)
            )
        } else {
            pauseTimer()
            when (view.id) {
                R.id.scoreOne -> {
                    leftFencer.addScore()
                    mRecentActions!!.push(0)
                    showSnackbar(
                        resources.getString(R.string.toast_gave), "", resources.getString(R.string.toast_touch),
                        resources.getString(R.string.toast_left)
                    )
                    if (mWeapon == 0 && leftFencer.score == 8 && rightFencer.score < 8) {
                        mTimeRemaining = mBreakLength
                        mInBreak = true
                    }
                    mIsOver = if (leftFencer.score >= mMode || mInPriority) {
                        leftFencer.makeWinner(rightFencer.score)
                        true
                    } else false
                }
                R.id.scoreTwo -> {
                    rightFencer.addScore()
                    mRecentActions!!.push(1)
                    showSnackbar(
                        resources.getString(R.string.toast_gave), "", resources.getString(R.string.toast_touch),
                        resources.getString(R.string.toast_right)
                    )
                    if (mWeapon == 0 && rightFencer.score == 8 && leftFencer.score < 8) {
                        mTimeRemaining = mBreakLength
                        mInBreak = true
                    }
                    mIsOver = if (rightFencer.score >= mMode || mInPriority) {
                        rightFencer.makeWinner(rightFencer.score)
                        true
                    } else false
                }
                R.id.doubleTouchButton -> {
                    if (leftFencer.score == rightFencer.score &&
                        leftFencer.score == mMode - 1
                    ) {
                        showSnackbar(
                            resources.getString(R.string.toast_unable),
                            "",
                            resources.getString(R.string.toast_give),
                            resources.getString(R.string.toast_touch)
                        )
                    } else {
                        leftFencer.addScore()
                        rightFencer.addScore()
                        mRecentActions!!.push(2)
                        showSnackbar(
                            resources.getString(R.string.toast_gave), "", resources.getString(R.string.toast_double),
                            resources.getString(R.string.toast_touch)
                        )
                        mIsOver = when {
                            leftFencer.score >= mMode -> {
                                leftFencer.makeWinner(rightFencer.score)
                                true
                            }
                            rightFencer.score >= mMode -> {
                                rightFencer.makeWinner(leftFencer.score)
                                true
                            }
                            else -> false
                        }
                    }
                }
            }
            updateAll()
        }
    }

    private fun loadSettings() {
        val mSharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        mWeapon = try {
            Integer.parseInt(mSharedPreferences.getString("pref_weapon", "0")!!)
        } catch (e : Exception) {
            val wp = mSharedPreferences.getString("pref_weapon", "0")!!
            if (wp == "сабля") {
                0
            } else {
                1
            }
        }
        mMode = Integer.parseInt(mSharedPreferences.getString("pref_mode", "5")!!)
        when  {
            mMode == 5 -> mMaxPeriods = 1
            mMode == 15 && mWeapon == 1 -> mMaxPeriods = 3
            mMode == 15 && mWeapon == 0 -> mMaxPeriods = 2
        }
        mShowDouble = mSharedPreferences.getBoolean("pref_show_double", true)

//        if (mShowDouble) findViewById(R.id.doubleTouchButton).setVisibility(View.VISIBLE)
//        else findViewById(R.id.doubleTouchButton).setVisibility(View.INVISIBLE)
        if (mShowDouble) findViewById<View>(R.id.doubleTouchButton).visibility = View.VISIBLE
        else findViewById<View>(R.id.doubleTouchButton).visibility = View.INVISIBLE

        val mAnywhereToStart: Boolean = mSharedPreferences.getBoolean("pref_anywhere_to_start", true)

        val cardLayout: View = findViewById(R.id.cardLayout)
        if (mAnywhereToStart) {
            mMainLayout!!.setOnClickListener { view -> onClickTimer(view) }
            cardLayout.setOnClickListener { view -> onClickTimer(view) }

        } else {
            mMainLayout!!.setOnClickListener { pauseTimer() }
            cardLayout.setOnClickListener { pauseTimer() }
        }

    }

    private fun pauseTimer() {
        toneGenerator.stopTone()
        mRinger!!.stop()
        mVibrator!!.cancel()
        if (mTimerRunning) {
//            mVibrator!!.vibrate(VibrationEffect.createOneShot(5, 10))
            mVibrator!!.vibrate(200)
            mCountDownTimer!!.cancel()
            mTimerRunning = false

            //only blink if time remaining < time in section
            if (mTimeRemaining != (60 * 1000).toLong() && mTimeRemaining != (3 * 60 * 1000).toLong() ||
                mTimeRemaining == (60 * 1000).toLong() && !(mInBreak || mInPriority) ||
                mTimeRemaining == (3 * 60 * 1000).toLong() && !mInPeriod
            )
                mTimer!!.startAnimation(mBlink)

            if (!mTimerRunning)
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // disable keep screen awake
        }
    }

    private fun updateViews() {
        updatePeriod()
        updateScores()
        updatePenaltyIndicators()
        updateTimer(mTimeRemaining)
        updatePriorityIndicators()
    }

    @SuppressLint("SetTextI18n")
    private fun updatePeriod() {
        when {
            mInPeriod -> mPeriodView!!.text = resources.getString(R.string.period) + " " + mPeriodNumber
            mInBreak -> mPeriodView!!.text = resources.getString(R.string.rest) + " " + mPeriodNumber
            mInPriority -> mPeriodView!!.text = resources.getString(R.string.priority)
        }
    }

    // METHODS FOR SCORES
    private fun updateScores() {
        mLeftScoreView!!.text = leftFencer.score.toString()
        mRightScoreView!!.text = rightFencer.score.toString()
    }

    private fun updatePenaltyIndicators() { // update penalty indicator views
        when {
            leftFencer.hasRedCard() -> {
                mLeftPenaltyIndicator!!.setColorFilter(Color.RED)
                mLeftPenaltyIndicator!!.visibility = View.VISIBLE
            }
            leftFencer.hasYellowCard() -> {
                mLeftPenaltyIndicator!!.setColorFilter(Color.YELLOW)
                mLeftPenaltyIndicator!!.visibility = View.VISIBLE
            }
            else -> mLeftPenaltyIndicator!!.visibility = View.INVISIBLE
        }

        when {
            rightFencer.hasRedCard() -> {
                mRightPenaltyIndicator!!.setColorFilter(Color.RED)
                mRightPenaltyIndicator!!.visibility = View.VISIBLE
            }
            rightFencer.hasYellowCard() -> {
                mRightPenaltyIndicator!!.setColorFilter(Color.YELLOW)
                mRightPenaltyIndicator!!.visibility = View.VISIBLE
            }
            else -> mRightPenaltyIndicator!!.visibility = View.INVISIBLE
        }
    }

    private fun updateTimer(millisUntilFinished: Long) {
        if (mTimeRemaining != (0).toLong()) mTimer!!.setTextColor(Color.WHITE)
        val minutes = millisUntilFinished / 60000
        val seconds = millisUntilFinished / 1000 - minutes * 60
        val milliseconds = millisUntilFinished % 1000 / 10
        val timeStr = String.format(
            "%1d:%02d.%02d", minutes,
            seconds, milliseconds
        )
        mTimer!!.text = timeStr
    }

    private fun updatePriorityIndicators() {
        mLeftPriorityIndicator!!.visibility = if (leftFencer.hasPriority())
            View.VISIBLE
        else
            View.INVISIBLE
        mRightPriorityIndicator!!.visibility = if (rightFencer.hasPriority())
            View.VISIBLE
        else
            View.INVISIBLE
    }

    fun giveCard(fencer: Int, cardType: Int) {
        val card = Intent(this, CardActivity::class.java)
        var alreadyYellow = false
        when (fencer) {
            0 -> {
                when (cardType) {
                    0 -> {
                        if (leftFencer.hasYellowCard() || leftFencer.hasRedCard())
                            alreadyYellow = true
                        leftFencer.giveYellowCard()
                        if (!alreadyYellow) {
                            mRecentActions!!.push(3)
                            showSnackbar(
                                resources.getString(R.string.toast_gave), resources.getString(R.string.toast_yellow),
                                resources.getString(R.string.toast_card), resources.getString(R.string.toast_left)
                            )
                        }
                    }
                    1 -> {
                        if (rightFencer.score < mMode) rightFencer.addScore()
                        leftFencer.giveRedCard()
                        mRecentActions!!.push(4)
                        showSnackbar(
                            resources.getString(R.string.toast_gave), resources.getString(R.string.toast_red),
                            resources.getString(R.string.toast_card), resources.getString(R.string.toast_left)
                        )
                        if (rightFencer.score >= mMode) {
                            rightFencer.makeWinner(leftFencer.score)
                            mIsOver = true
                        }
                    }

                }
                if (leftFencer.hasRedCard()) card.putExtra("red", true)
                startActivity(card)
            }
            1 -> {
                when (cardType) {
                    0 -> {
                        if (rightFencer.hasYellowCard() || rightFencer.hasRedCard())
                            alreadyYellow = true
                        rightFencer.giveYellowCard()
                        if (!alreadyYellow) {
                            mRecentActions!!.push(5)
                            showSnackbar(
                                resources.getString(R.string.toast_gave), resources.getString(R.string.toast_yellow),
                                resources.getString(R.string.toast_card), resources.getString(R.string.toast_right)
                            )
                        }
                    }
                    1 -> {
                        if (leftFencer.score < mMode) leftFencer.addScore()
                        rightFencer.giveRedCard()
                        mRecentActions!!.push(6)
                        showSnackbar(
                            resources.getString(R.string.toast_gave), resources.getString(R.string.toast_red),
                            resources.getString(R.string.toast_card), resources.getString(R.string.toast_right)
                        )
                        if (leftFencer.score >= mMode) {
                            leftFencer.makeWinner(rightFencer.score)
                            mIsOver = true
                        }
                    }
                }
                if (rightFencer.hasRedCard()) card.putExtra("red", true)
                startActivity(card)
            }
        }
        updateAll()
        pauseTimer()
    }

    @SuppressLint("WrongConstant")
    private fun showSnackbar(verb: String, color: String?, noun: String?, recipient: String?) {
        val text: String = if ((recipient == null || recipient == "") && (color == null || color == ""))
            "$verb $noun"
        else if (noun == null || noun == "") "$verb $recipient"
        else if (color == null || color == "") "$verb $noun $recipient"
        else "$verb $color $noun $recipient"

        mSnackBar!!.setText(text)
        mSnackBar!!.show()
    }
}
