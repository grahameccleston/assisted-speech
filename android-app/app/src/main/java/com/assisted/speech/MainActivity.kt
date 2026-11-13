package com.assisted.speech

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.assisted.speech.ui.MainScreen
import com.assisted.speech.ui.SpeechViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: SpeechViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen(viewModel = viewModel)
        }
    }
}
