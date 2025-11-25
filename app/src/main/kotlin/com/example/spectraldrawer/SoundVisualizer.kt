package com.example.spectraldrawer

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor

import be.tarsos.dsp.io.jvm.AudioDispatcherFactory
//import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.util.fft.FFT
import kotlin.math.log10
import kotlin.math.min

@Composable
fun SoundVisualizer(isRecording: Boolean) {
    var amplitudes by remember { mutableStateOf(FloatArray(64)) }
    val animatedAmps = amplitudes.map { animateFloatAsState(it) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isRecording) {
        if (isRecording) {
            coroutineScope.launch(Dispatchers.Default) {
                val sampleRate = 44100
                val bufferSize = 1024
                val dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferSize, 0)

                val fft = FFT(bufferSize)
                val fftBuffer = FloatArray(bufferSize * 2)

                dispatcher.addAudioProcessor(object : AudioProcessor {
                    override fun process(audioEvent: AudioEvent?): Boolean {
                        audioEvent ?: return true
                        val floatBuffer = audioEvent.floatBuffer

                        // FFT
                        System.arraycopy(floatBuffer, 0, fftBuffer, 0, min(floatBuffer.size, fftBuffer.size))
                        fft.forwardTransform(fftBuffer)
                        fft.modulus(fftBuffer, fftBuffer)

                        // Calcul des amplitudes (log)
                        val newAmps = FloatArray(amplitudes.size)
                        val step = fftBuffer.size / amplitudes.size
                        for (i in amplitudes.indices) {
                            val value = fftBuffer[i * step]
                            newAmps[i] = (20 * log10(value + 1e-6)).coerceAtLeast(0.0).toFloat()
                        }
                        amplitudes = newAmps
                        return true
                    }

                    override fun processingFinished() {}
                })

                dispatcher.run()
            }
        }
    }

    Box(
        modifier = Modifier
            .size(300.dp)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val widthStep = size.width / amplitudes.size
            amplitudes.forEachIndexed { i, amp ->
                val height = (amp / 60f * size.height).coerceIn(0f, size.height)
                drawRect(
                    color = Color(0xFF66CCFF),
                    topLeft = androidx.compose.ui.geometry.Offset(i * widthStep, size.height - height),
                    size = androidx.compose.ui.geometry.Size(widthStep - 2, height)
                )
            }
        }

        if (!isRecording) {
            Text(
                text = "Appuie pour enregistrer",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
