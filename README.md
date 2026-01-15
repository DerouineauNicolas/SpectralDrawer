# SpectralDrawer

A real-time audio spectrum visualizer for Android built with **Jetpack Compose** and **AudioRecord**.  
The app captures microphone audio, performs an FFT, and displays a live logarithmic frequency spectrum as animated bars.

## Features
- Live microphone audio capture
- **Adjustable FFT size** (256-4096 points) with slider control
- **Dynamic number of frequency bands** that match the FFT size
- Real-time FFT processing
- Logarithmic frequency spectrum visualization (20Hz - 22kHz)
- Jetpack Compose UI
- Uses TarsosDSP for efficient FFT computation
- Configurable sensitivity with gain factor

## How It Works
- Audio is read using `AudioRecord` at 44.1kHz
- Users can adjust the FFT size via a slider (256-4096)
- Number of frequency bands automatically matches the selected FFT size
- Samples are processed with the selected FFT size
- Magnitudes are computed with logarithmic frequency scaling
- Sensitivity is reduced by a gain factor (100x) to avoid clipping at max volume
- Bars are drawn on a Compose `Canvas` and update in real time

## UI Controls
- **FFT Size Slider**: Adjust the FFT size from 256 to 4096 points (determines the frequency resolution and number of bands displayed)

## Requirements
- Android 6.0+
- Microphone permission (`RECORD_AUDIO`)

## Privacy Policy

This application uses the device microphone to capture audio in real time
for the sole purpose of audio analysis and visualization.

No audio data is recorded, stored, or transmitted outside the device.
All processing is performed locally on the user's device.

The application does not collect, store, or share any personal data
with third parties.

If you have any questions regarding this privacy policy,
you can contact the developer at: your@email.com

## Dependency
```gradle
implementation "com.github.JorenSix:TarsosDSP:2.4.8-android"
