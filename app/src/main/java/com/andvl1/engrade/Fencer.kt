package com.andvl1.engrade

class Fencer {
    // POOL METHODS
    var name = ""
    var team = ""
    var number = -1
    // BOUT METHODS
    var score = 0
        private set
    var indicator = 0
        private set
    private var mNumWins = 0
    private var mNumLosses = 0
    private var mHasYellowCard = false
    private var mHasRedCard = false
    private var mHasPriority = false
    var isWinner = false
        private set

    fun updateIndicator(touchesReceived: Int) {
        indicator = indicator + this.score - touchesReceived
    }

    fun addScore() {
        score++
    }

    fun subtractScore() {
        score--
    }

    fun resetScore() {
        score = 0
    }

    fun hasYellowCard(): Boolean {
        return mHasYellowCard
    }

    fun giveYellowCard() {
        mHasYellowCard = true
    }

    fun takeYellowCard() {
        mHasYellowCard = false
    }

    fun hasRedCard(): Boolean {
        return mHasRedCard
    }

    fun giveRedCard() {
        mHasRedCard = true
    }

    fun takeRedCard() {
        mHasRedCard = false
    }

    fun hasPriority(): Boolean {
        return mHasPriority
    }

    fun givePriority() {
        mHasPriority = true
    }

    fun resetPriority() {
        mHasPriority = false
    }

    fun makeWinner(touchesReceived: Int) {
        isWinner = true
        mNumWins++
        updateIndicator(touchesReceived)
    }

    fun takeWinner(touchesReceived: Int) {
        isWinner = false
        mNumWins--
        updateIndicator(touchesReceived - this.score)
    }

    fun makeLoser(touchesReceived: Int) {
        isWinner = false
        mNumLosses++
        updateIndicator(touchesReceived)
    }
}
