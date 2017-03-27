package com.sclf.qkj.voicerecoder;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.czt.mp3recorder.MP3Recorder;
import com.czt.mp3recorder.view.RecordButton;

import java.io.File;
import java.io.IOException;

public class TestActivity extends AppCompatActivity {

    MP3Recorder mp3Recorder;
    String path = Environment.getExternalStorageDirectory().getPath()+"/test.mp3";
    Handler handler = new Handler(){
      @Override
      public void handleMessage(Message msg) {
          if (msg.what==101){
              Log.d("WL",(Integer)msg.obj+"------lalla");
          }
          super.handleMessage(msg);
      }
  };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        findViewById(R.id.btn_record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mp3Recorder = new MP3Recorder(new File(path),handler,60);
                try {
                    mp3Recorder.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        findViewById(R.id.btn_pause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mp3Recorder.stop();
            }
        });

//        findViewById(R.id.btn_view_record).setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View view) {
//                return false;
//            }
//        });
        ((RecordButton)findViewById(R.id.btn_view_record)).setAudioFinishRecordListener
                (new RecordButton.AudioFinishRecordListener() {
            @Override
            public void onFinish(int voiceRime, String voiceFilePath) {

            }
        });
    }
}
