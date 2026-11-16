package com.mg.pillreminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mg.pillreminder.ui.medicine.MedicineListScreen
import com.mg.pillreminder.ui.theme.PillReminderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PillReminderTheme {
                MedicineListScreen()
            }
        }
    }
}
