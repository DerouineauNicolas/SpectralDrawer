package com.example.spectraldrawer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SoundVisualizer(isRecording: Boolean) {
    var amplitude by remember { mutableStateOf(0f) }

    // Pour la version simple, on simule l'amplitude
    LaunchedEffect(isRecording) {
        while (isRecording) {
            amplitude = (50..300).random().toFloat()
            delay(200)
        }
    }

    val animatedSize = animateFloatAsState(targetValue = if (isRecording) amplitude else 0f)

    Box(
        modifier = Modifier
            .size(200.dp)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    )  {
        Canvas(modifier = Modifier.size(animatedSize.value.dp)) {
            drawCircle(color = Color(0xFF66CCFF))
        }
        if (!isRecording) {
            Text(
                text = "Appuie pour enregistrer",
                color = Color.White
            )
        }
    }
}
