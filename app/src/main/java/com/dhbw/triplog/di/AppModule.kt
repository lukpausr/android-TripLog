package com.dhbw.triplog.di

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.room.Room
import com.dhbw.triplog.db.TripDatabase
import com.dhbw.triplog.other.Constants.SHARED_PREFERENCES_NAME
import com.dhbw.triplog.other.Constants.TRIP_DATABASE_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

/**
 * Data Injection
 */
@Module
@InstallIn(ApplicationComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideTripDatabase(
        @ApplicationContext app: Context
    ) = Room.databaseBuilder(
        app,
        TripDatabase::class.java,
        TRIP_DATABASE_NAME
    ).build()

    @Singleton
    @Provides
    fun provideTripDao(db: TripDatabase) = db.getTripDao()

    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext app: Context) =
            app.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
}