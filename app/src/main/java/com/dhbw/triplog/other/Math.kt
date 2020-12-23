package com.dhbw.triplog.other

object Math {
    fun max(a: Int, b: Int, c: Int) : Int{
        val temp = kotlin.math.max(a, b)
        return kotlin.math.max(temp, c)
    }
}