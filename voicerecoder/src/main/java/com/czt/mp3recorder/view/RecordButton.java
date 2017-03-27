package com.czt.mp3recorder.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import com.czt.mp3recorder.MP3Recorder;
import com.sclf.qkj.voicerecoder.R;
import com.sclf.qkj.voicerecoder.nickming.view.DialogManager;

import java.io.File;
import java.io.IOException;


@SuppressLint("AppCompatCustomView")
public class RecordButton extends Button  {

    public static String pathUrl = Environment.getExternalStorageDirectory()
            + "/record.mp3"; //默认的音频文件
    //三个初始化状态
    private static final int STATE_NORMAL =1; //初始状态
    private static final int STATE_RECORDING=2; //录制
    private static final int STATE_CANCEL =3;//取消

    private static final int MAX_SCROOL_DISTANCE = 50;//上滑取消的最大距离

    private static final int MIN_RECORD_TIME = 1300;//录制时间最短1.3s
    private static final int TOO_SHORT_TIME = 1300; //录制时间过短,显示Dialog时间段,
    private int mState = STATE_NORMAL; //状态变量

    private boolean isRecording = false;//是否已开始录音
    private DialogManager dialogManager;
    private MP3Recorder mp3Recorder;
    private int mTime = 0;//录制时长
    private boolean mReady; //触发长按时,初始化工作是否完成

    boolean isFirstClick = false;

    public RecordButton(Context context) {
       this(context,null);
    }

    public RecordButton(final Context context, AttributeSet attrs) {
        super(context, attrs);

        dialogManager = new DialogManager(context);
        mp3Recorder = new MP3Recorder(new File(pathUrl));
        isFirstClick = true;
        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mReady = true;
                try {
                    mp3Recorder.initAudioRecorder();
                    mHandler.sendEmptyMessage(MSG_AUDIO_START);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            }
        });
    }


    //关于音量 及Dialog显示
    public static final int MSG_AUDIO_START = 0X110;
    public static final int MSG_VOICE_CHANGE = 0X111;
    public static final int MSG_DIALOG_CANCEL = 0X112;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_AUDIO_START://录音开始
                    dialogManager.showRecordingDialog();
                    isRecording = true;
                    //ToDo 计算录音时间
                    new Thread(VoiceTimerRunnabel).start();
                    break;
                case MSG_VOICE_CHANGE://音量改变,Dialog显示变化
                    //todo 变化dialog音量变化

                    break;
                case MSG_DIALOG_CANCEL://取消显示框
                    break;
            }
            super.handleMessage(msg);
        }
    };

    //录制完成,或者出错 复位
    private void reset(){
        isRecording = false;
        mReady =false;
        mTime =0;
        changeState(STATE_NORMAL);
    }


    private Runnable  VoiceTimerRunnabel = new Runnable() {
        @Override
        public void run() {
            while (isRecording){
                try {
                    Thread.sleep(1000);
                    mTime +=1;
                    mHandler.sendEmptyMessage(MSG_AUDIO_START);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };



    //改变录音Dialog显示的不同状态
    private void changeState(int state){
        if (mState != state){
            mState =state;
            switch (mState){
                case STATE_NORMAL:
                    setBackgroundResource(R.drawable.button_recordnormal);
                    setText(R.string.normal);
                    break;
                case STATE_RECORDING:
                    setBackgroundResource(R.drawable.button_recording);
                    setText(R.string.recording);
                    if (isRecording){
                        dialogManager.recording();
                    }
                    break;
                case STATE_CANCEL:
                    setBackgroundResource(R.drawable.button_recording);
                    setText(R.string.want_to_cancle);
                    dialogManager.wantToCancel();
                    break;
            }
        }
    }





    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //获取初始的状态和坐标
        int action = event.getAction();
        int x = (int)event.getX();
        int y = (int) event.getY();

        switch (action){
            case MotionEvent.ACTION_DOWN://刚开始触摸
                changeState(STATE_RECORDING);
                break;
            case MotionEvent.ACTION_MOVE://手指在移动
                if (isRecording){
                    //根据移动的x,y坐标判断是否想要取消
                    if (isCancelRecord(x,y)){
                        changeState(STATE_CANCEL);
                    }else {
                        changeState(STATE_RECORDING);
                    }
                }
                break;
            case MotionEvent.ACTION_UP://抬起手指,结束
                //录制结束,重置所有状态
                if (!mReady){
                   reset();
                   return super.onTouchEvent(event);
                }
                //若: 按住时间过短就选择抬手,显示录制时间过短Dialog, 并且不做录制记录
                if (!isRecording || mTime <MIN_RECORD_TIME){
                    dialogManager.timeShort();
                    mHandler.sendEmptyMessageDelayed(MSG_DIALOG_CANCEL,TOO_SHORT_TIME);
                    // 不做录音文件处理,结束,  释放资源,删除无用文件
                    mp3Recorder.stop();
                    mp3Recorder.cleanFile(true);
                }
                //表示正在正常录音结束
                else if (mState == STATE_RECORDING){
                    dialogManager.dimissDialog();
                    //todo 释放录音对象, 并且保存时间,文件回调
                    if (mListner != null){
                        mListner.onFinish(mTime,pathUrl);
                    }
                }else if (mState == STATE_CANCEL){
                    dialogManager.dimissDialog();
                    //录音取消,销毁对象
                }
                //重置所有标示
                reset();
                break;
        }
        return super.onTouchEvent(event);
    }

    //初始状态获取View的宽高
    @Override
    public boolean onPreDraw() {
        return false;
    }

    //初始化工作是否准备充足
//    @Override
//    public void prepared() {
//        mHandler.sendEmptyMessage(MSG_AUDIO_START);
//    }


    //滑动取消录音
    private boolean isCancelRecord(int x, int y){
        //超过按钮的宽度
        if (x <0 || x> getWidth()){
            return true;
        }
        //超过按钮的高度
        if (y < -MAX_SCROOL_DISTANCE || y> getHeight() + MAX_SCROOL_DISTANCE){
            return true;
        }
        return false;
    }

    /**
     * 录制完成回调 传递音频时间和 音频路径
     */
    public interface AudioFinishRecordListener{
        void onFinish(int voiceRime, String voiceFilePath);
    }
    private AudioFinishRecordListener mListner;
    public void setAudioFinishRecordListener(AudioFinishRecordListener audioFinishRecordListener){
        mListner = audioFinishRecordListener;
    }


//    private static class MyHandler extends Handler{
//        WeakReference<Context> weakReference;
//
//        public MyHandler(Context con){
//            weakReference = new WeakReference<Context>(con);
//        }
//
//        @Override
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//            if (weakReference != null){
//
//            }
//        }
//    }
}
