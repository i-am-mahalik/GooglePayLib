package com.hs.gpaylib

import java.io.Serializable
import java.util.*

data class GooglePayParam(var price:String,var countryCode:String,var currency: String) :Serializable
