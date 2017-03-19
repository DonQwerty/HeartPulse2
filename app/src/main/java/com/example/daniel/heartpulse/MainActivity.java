package com.example.daniel.heartpulse;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    /* Debugging */
    private static final String TAG = MainActivity.class.getSimpleName();

    /* Recording permission */
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    /* Audio recording */
    private AudioRecord mRecorder;

    /* Recording settings */
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    int BufferElements2Rec = 1024;
    int BytesPerElement = 2;


    /* View objects */
    private ImageButton mHeartImageButton;
    private TextView mStatusTextView;
    private boolean isRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Get references to the views */
        mHeartImageButton = (ImageButton) findViewById(R.id.ib_heart);
        mStatusTextView = (TextView) findViewById(R.id.tv_status);

        mHeartImageButton.setOnClickListener(this);
    }

    public void startRecording() {
        /* Create recorder */
        mRecorder = new AudioRecord(
                MediaRecorder.AudioSource.UNPROCESSED,
                RECORDER_SAMPLERATE,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING,
                BufferElements2Rec * BytesPerElement);
        if (mRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Error initialising recorder");
            return;
        }
        mRecorder.startRecording();
        isRecording = true;

        /* Write data to file */
        new Thread(new Runnable() {

            public void run() {

                writeAudioDataToFile();

            }
        }, "AudioRecorder Thread").start();

        /* Stop in 15 seconds */
        new Timer().schedule(new TimerTask()
        {

            @Override
            public void run()
            {
                runOnUiThread(new Runnable()
                {

                    @Override
                    public void run()
                    {
                        isRecording = false;
                        mRecorder.stop();
                        mRecorder.release();

                        getBeatsFromAudio();
                    }
                });

            }
        }, 5000);

    }

    private void getBeatsFromAudio() {
        mStatusTextView.setText(getString(R.string.btn_status_processing));
    }

    private void writeAudioDataToFile() {
        // Write the output audio in byte
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/8k16bitMono.pcm";

        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format
            mRecorder.read(sData, 0, BufferElements2Rec);
            System.out.println("Short wirting to file" + sData.toString());
            try {
                // writes the data to file from buffer stores the voice buffer
                byte bData[] = short2byte(sData);

                os.write(bData, 0, BufferElements2Rec * BytesPerElement);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];

        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ib_heart:
                mHeartImageButton.setClickable(false);
                mStatusTextView.setText(getString(R.string.btn_status_recording));
                startRecording();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }
}
