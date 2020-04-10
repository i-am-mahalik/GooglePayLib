package com.hs.gpaylib

interface GPayListener {
    fun isAppElgibleForPayment(isEligible:Boolean)
    fun onSuccess()
    fun onError(errorMessage:String)
    fun inCancelled(cancelledMessage:String)

}