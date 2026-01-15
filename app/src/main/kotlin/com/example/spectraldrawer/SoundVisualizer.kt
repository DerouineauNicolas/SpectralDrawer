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
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.*
import be.tarsos.dsp.util.fft.FFT
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.sqrt

fun computeMagnitudes(
    fftBuffer: FloatArray,
    bufferSize: Int,
    numBands: Int,
    sampleRate: Int
): FloatArray {
    val newAmps = FloatArray(numBands)

    val bins = bufferSize / 2

    val minFreq = 20.0
    val maxFreq = sampleRate / 2.0
    val logMin = log10(minFreq)
    val logMax = log10(maxFreq)
    val logRange = logMax - logMin

    for (band in 0 until numBands) {
        val logFreqLow = logMin + band * logRange / numBands
        val logFreqHigh = logMin + (band + 1) * logRange / numBands
        val freqLow = Math.pow(10.0, logFreqLow).toFloat()
        val freqHigh = Math.pow(10.0, logFreqHigh).toFloat()

        val binLow = (freqLow * bufferSize / sampleRate).toInt().coerceIn(0, bins - 1)
        val binHigh = (freqHigh * bufferSize / sampleRate).toInt().coerceIn(0, bins - 1)

        var sum = 0f
        var count = 0
        for (bin in binLow until min(binHigh, bins)) {
            val real = fftBuffer[2 * bin]
            val imag = fftBuffer[2 * bin + 1]
            sum += sqrt(real * real + imag * imag)
            count++
        }

        val magnitude = if (count > 0) sum / count else 0f
        // Reduce sensitivity by dividing magnitude by a gain factor and using a higher noise floor
        val gainFactor = 100f  // Reduce magnitude by factor of 100
        val adjustedMagnitude = (magnitude / gainFactor).coerceAtLeast(1e-4f)
        newAmps[band] = 20f * log10(adjustedMagnitude)
    }

    return newAmps
}

@Composable
fun SoundVisualizer(isRecording: Boolean) {

    val MIN_DB = -60f

    var amplitudes by remember {
        mutableStateOf(FloatArray(256) { MIN_DB })
    }

    val coroutineScope = rememberCoroutineScope()

    // Keep reference to stop AudioRecord
    var audioRecord by remember { mutableStateOf<AudioRecord?>(null) }
    var recordJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(isRecording) {
        if (isRecording) {

            recordJob = coroutineScope.launch(Dispatchers.Default) {

                val sampleRate = 44100
                val bufferSize = 2048

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


// Prepare real+imaginary pairs for FFT
                        var fftIndex = 0
                        for (i in 0 until bufferSize) {
                            fftBuffer[fftIndex++] = floatBuffer[i]     // real part
                            fftBuffer[fftIndex++] = 0f                // imaginary part
                        }

// Run FFT
                        fft.forwardTransform(fftBuffer)

                        val newAmps =
                            computeMagnitudes(fftBuffer, bufferSize, amplitudes.size, sampleRate)

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
            .size(350.dp, 300.dp)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val widthStep = size.width / amplitudes.size

            amplitudes.forEachIndexed { i, amp ->
                val minDb = -60f
                val maxDb = 0f

                val clamped = amp.coerceIn(minDb, maxDb)
                val normalized = (clamped - minDb) / (maxDb - minDb)
                val height = normalized * size.height

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

        // Frequency scale labels (bottom)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .align(Alignment.BottomCenter)
        ) {
            val minFreq = 20f
            val maxFreq = 20000f
            val logMin = log10(minFreq)
            val logMax = log10(maxFreq)

            fun freqToX(freq: Float): Float {
                val logF = log10(freq)
                val norm = (logF - logMin) / (logMax - logMin)
                return norm * size.width
            }

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 32f
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                val y = size.height - 4f
                drawText("20Hz", freqToX(20f), y, paint)
                drawText("200Hz", freqToX(200f), y, paint)
                drawText("2kHz", freqToX(2000f), y, paint)
                drawText("20kHz", freqToX(20000f), y, paint)
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .width(40.dp)
                .align(Alignment.CenterStart)
        ) {
            val minDb = -60f
            val maxDb = 0f

            fun dbToY(db: Float): Float {
                val clamped = db.coerceIn(minDb, maxDb)
                val normalized = (clamped - minDb) / (maxDb - minDb)
                return size.height * (1f - normalized)
            }

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 32f
                }

                drawText("0dB", 4f, dbToY(0f) + 12f, paint)
                drawText("-30dB", 4f, dbToY(-30f) + 12f, paint)
                drawText("-60dB", 4f, dbToY(-60f) + 12f, paint)
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
