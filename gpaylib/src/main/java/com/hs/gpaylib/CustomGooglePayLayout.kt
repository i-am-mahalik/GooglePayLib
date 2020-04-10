package com.hs.gpaylib

import android.widget.RelativeLayout
import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import android.view.LayoutInflater



class CustomGooglePayLayout : RelativeLayout {


    var mInflater: LayoutInflater

    constructor(context: Context): super(context) {
        mInflater = LayoutInflater.from(context)
        init()

    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context) {
        mInflater = LayoutInflater.from(context)
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context) {
        mInflater = LayoutInflater.from(context)
        init()
    }

    fun init() {
        val v = mInflater.inflate(R.layout.custom_google_pay_layout, this, true)
    }
}