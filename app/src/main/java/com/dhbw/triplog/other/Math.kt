package com.dhbw.triplog.other

/**
 * Static class like implementation of different Math methods
 */
object Math {

    /**
     * Used to determine the maximum of three numbers
     *
     * @param a Value a
     * @param b Value b
     * @param c Value c
     *
     * @return Maximum value of a, b and c
     */
    fun max(a: Int, b: Int, c: Int) : Int{
        val temp = kotlin.math.max(a, b)
        return kotlin.math.max(temp, c)
    }
}