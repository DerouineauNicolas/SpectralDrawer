# SpectralDrawer

A simple real-time audio spectrum visualizer for Android built with **Jetpack Compose** and **AudioRecord**.  
The app captures microphone audio, performs an FFT, and displays a 64-band frequency spectrum as animated bars.

## Features
- Live microphone audio capture
- Real-time FFT processing
- 64-band spectrum visualization
- Jetpack Compose UI
- Lightweight and Android-native (no audio dispatcher libraries)

## How It Works
- Audio is read using `AudioRecord`
- Samples are converted to floats and processed with an FFT
- Magnitudes are mapped to decibels
- Bars are drawn on a Compose `Canvas` and update in real time

## Requirements
- Android 6.0+
- Microphone permission (`RECORD_AUDIO`)

## Dependency
```gradle
implementation "com.github.JorenSix:TarsosDSP:2.4.8-android"
