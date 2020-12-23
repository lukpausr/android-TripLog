package com.dhbw.triplog.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Trip::class],
    version = 2
)
@TypeConverters(Converters::class)
abstract class TripDatabase : RoomDatabase() {

    abstract fun getTripDao(): TripDao

}