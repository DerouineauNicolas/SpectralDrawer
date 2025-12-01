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
import kotlinx.coroutines.*
import be.tarsos.dsp.util.fft.FFT
import kotlin.math.log10
import kotlin.math.min

@Composable
fun SoundVisualizer(isRecording: Boolean) {

    var amplitudes by remember { mutableStateOf(FloatArray(64)) }

    val coroutineScope = rememberCoroutineScope()

    // Keep reference to stop AudioRecord
    var audioRecord by remember { mutableStateOf<AudioRecord?>(null) }
    var recordJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(isRecording) {
        if (isRecording) {

            recordJob = coroutineScope.launch(Dispatchers.Default) {

                val sampleRate = 44100
                val bufferSize = 1024

                val minBufSize = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val audioBufSize = maxOf(minBufSize, bufferSize * 2)

                val recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    audioBufSize
                )

                audioRecord = recorder

                val shortBuffer = ShortArray(bufferSize)
                val floatBuffer = FloatArray(bufferSize)
                val fftBuffer = FloatArray(bufferSize * 2)

                val fft = FFT(bufferSize)

                recorder.startRecording()

                while (isActive) {
                    val read = recorder.read(shortBuffer, 0, bufferSize)

                    if (read > 0) {
                        // Convert PCM16 -> Float
                        for (i in 0 until read) {
                            floatBuffer[i] = shortBuffer[i] / 32768f
                        }

                        // Prepare FFT buffer
                        System.arraycopy(
                            floatBuffer,
                            0,
                            fftBuffer,
                            0,
                            min(floatBuffer.size, fftBuffer.size)
                        )

// Prepare real+imaginary pairs for FFT
                        var fftIndex = 0
                        for (i in 0 until bufferSize) {
                            fftBuffer[fftIndex++] = floatBuffer[i]     // real part
                            fftBuffer[fftIndex++] = 0f                // imaginary part
                        }

// Run FFT
                        fft.forwardTransform(fftBuffer)

// Compute magnitude properly
                        val newAmps = FloatArray(amplitudes.size)
                        val bins = bufferSize / 2                     // number of frequency bins

                        val step = bins / newAmps.size

                        for (i in newAmps.indices) {
                            val real = fftBuffer[2 * (i * step)]
                            val imag = fftBuffer[2 * (i * step) + 1]

                            val magnitude = kotlin.math.sqrt(real * real + imag * imag)

                            newAmps[i] = (20 * log10(magnitude + 1e-6))
                                .coerceAtLeast(0.0).toFloat()
                        }

                        amplitudes = newAmps
                    }
                }


            }
        } else {
            recordJob?.cancel()
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        }
    }

    // UI
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
                    topLeft = androidx.compose.ui.geometry.Offset(
                        i * widthStep,
                        size.height - height
                    ),
                    size = androidx.compose.ui.geometry.Size(widthStep - 2, height)
                )
            }
        }

        if (!isRecording) {
            Text(
                "",
                color = Color.White
            )
        }
    }
}
