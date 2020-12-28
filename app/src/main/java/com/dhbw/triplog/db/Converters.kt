package com.dhbw.triplog.db

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import java.io.ByteArrayOutputStream

/**
 * Converter class being used to do necessary type conversions before saving something in the DB
 */
class Converters {

    /**
     * Converting Bitmap to ByteArray
     *
     * @param bmp Bitmap type object like Google Maps Screenshot
     *
     * @return ByteArray representation of Bitmap
     */
    @TypeConverter
    fun fromBitmap(bmp: Bitmap) : ByteArray {
        val outputStream = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Converting ByteArray to Bitmap
     *
     * @param bytes ByteArray representation of a Bitmap
     *
     * @return Bitmap back-conversion of ByteArray
     */
    @TypeConverter
    fun toBitmap(bytes: ByteArray) : Bitmap {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}