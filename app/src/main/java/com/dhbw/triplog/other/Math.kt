package com.dhbw.triplog.other

/**
 * Static class like implementation of different Math methods
 */
object Math {

    /**
     * Used to determine the minimum of three numbers
     *
     * @param a Value a
     * @param b Value b
     * @param c Value c
     *
     * @return Minimum value of a, b and c
     */
    fun min(a: Int, b: Int, c: Int) : Int{
        val temp = kotlin.math.min(a, b)
        return kotlin.math.min(temp, c)
    }
}