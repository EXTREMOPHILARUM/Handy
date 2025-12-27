package com.pais.handy;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Manages audio recording for the Handy application on Android.
 * This class provides a JNI-friendly interface to Android's AudioRecord API.
 */
public class AudioRecordManager {
    private static final String TAG = "AudioRecordManager";
    private static final int SAMPLE_RATE = 16000; // Whisper requirement
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecord;
    private int bufferSize;
    private boolean isRecording = false;

    public AudioRecordManager() {
        // Calculate minimum buffer size
        bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        );

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2; // Fallback: 1 second of audio
            Log.w(TAG, "Using fallback buffer size: " + bufferSize);
        }

        Log.i(TAG, "AudioRecordManager initialized with buffer size: " + bufferSize);
    }

    /**
     * Start audio recording
     * @return true if recording started successfully, false otherwise
     */
    public boolean startRecording() {
        try {
            if (audioRecord != null && audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.release();
            }

            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            );

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize");
                return false;
            }

            audioRecord.startRecording();
            isRecording = true;
            Log.i(TAG, "Recording started");
            return true;

        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied for audio recording", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Failed to start recording", e);
            return false;
        }
    }

    /**
     * Read audio samples into a buffer
     * @param numSamples Number of samples to read
     * @return Array of audio samples as short values, or null on error
     */
    public short[] readAudio(int numSamples) {
        if (!isRecording || audioRecord == null) {
            Log.w(TAG, "Cannot read audio: not recording");
            return null;
        }

        try {
            short[] buffer = new short[numSamples];
            int samplesRead = audioRecord.read(buffer, 0, numSamples);

            if (samplesRead < 0) {
                Log.e(TAG, "Error reading audio: " + samplesRead);
                return null;
            }

            if (samplesRead < numSamples) {
                // Return only the samples that were read
                short[] trimmed = new short[samplesRead];
                System.arraycopy(buffer, 0, trimmed, 0, samplesRead);
                return trimmed;
            }

            return buffer;

        } catch (Exception e) {
            Log.e(TAG, "Exception while reading audio", e);
            return null;
        }
    }

    /**
     * Stop audio recording and release resources
     */
    public void stopRecording() {
        isRecording = false;

        if (audioRecord != null) {
            try {
                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    audioRecord.stop();
                }
                audioRecord.release();
                audioRecord = null;
                Log.i(TAG, "Recording stopped and resources released");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping recording", e);
            }
        }
    }

    /**
     * Check if currently recording
     * @return true if recording, false otherwise
     */
    public boolean isRecording() {
        return isRecording;
    }

    /**
     * Get the sample rate used for recording
     * @return Sample rate in Hz
     */
    public int getSampleRate() {
        return SAMPLE_RATE;
    }

    /**
     * Get the buffer size used for recording
     * @return Buffer size in bytes
     */
    public int getBufferSize() {
        return bufferSize;
    }
}
