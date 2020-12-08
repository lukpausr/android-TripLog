package com.dhbw.triplog.other

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter

object DataExportUtility {
    fun writeGPSDataToFile() {
        val row1 = listOf("a", "b", "c")
        val row2 = listOf("d", "e", "f")
        csvWriter().open("test.csv") {
            writeRow(row1)
            writeRow(row2)
            writeRow("g", "h", "i")
            writeRows(listOf(row1, row2))
        }
    }
}