package com.example.tripapp2

import android.app.Application
import com.example.tripapp2.data.repository.TripRepository

/**
 * Application class — inicjalizuje TripRepository z Context.
 *
 * WAŻNE: Dodaj android:name=".TripApp" do AndroidManifest.xml:
 *
 * <application
 *     android:name=".TripApp"
 *     ...>
 */
class TripApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicjalizacja TripRepository z Application Context
        // Dzięki temu ViewModele mogą używać TripRepository.getInstance() bez context
        TripRepository.getInstance(this)
    }
}
