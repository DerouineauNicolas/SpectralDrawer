# SpectralDrawer

A simple real-time audio spectrum visualizer for Android built with **Jetpack Compose** and **AudioRecord**.  
The app captures microphone audio, performs an FFT, and displays a 256-band logarithmic frequency spectrum as animated bars.

## Features
- Live microphone audio capture
- Real-time FFT processing with 2048-point FFT
- 256-band logarithmic spectrum visualization
- Jetpack Compose UI
- Uses TarsosDSP for efficient FFT computation

## How It Works
- Audio is read using `AudioRecord` at 44.1kHz
- Samples are processed with a 2048-point FFT
- Magnitudes are computed with logarithmic frequency scaling (20Hz - 22kHz)
- Bars are drawn on a Compose `Canvas` and update in real time

## Requirements
- Android 6.0+
- Microphone permission (`RECORD_AUDIO`)

## Dependency
```gradle
implementation "com.github.JorenSix:TarsosDSP:2.4.8-android"
