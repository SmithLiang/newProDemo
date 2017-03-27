package com.sclf.qkj.voicerecoder.nickming.view;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import android.media.MediaRecorder;

/**
 * @ClassName: AudioManager
 * @Description: 录音的管理类（准备工作）
 * @date: 2016-5-23 下午2:10:35
 */
public class AudioManager {

    private MediaRecorder mRecorder;
    private String mDirString;
    private String mCurrentFilePath;

    private boolean isPrepared;// 是否准备好了

    /**
     * 单例化的方法
     *  1 先声明一个static 类型的变量a
     *  2 在声明默认的构造函数
     *  3 再用public synchronized static
     *   类名 getInstance() { if(a==null) { a=new 类();} return a; } 或者用以下的方法
     */

    /**
     * 单例化这个类
     */
    private static AudioManager mInstance;

    private AudioManager(String dir) {
        mDirString = dir;
    }

    public static AudioManager getInstance(String dir) {
        if (mInstance == null) {
            synchronized (AudioManager.class) {
                if (mInstance == null) {
                    mInstance = new AudioManager(dir);

                }
            }
        }
        return mInstance;

    }

    /**
     * 回调函数，准备完毕，准备好后，button才会开始显示录音框
     * @author nickming
     */
    public interface AudioStageListener {
        void wellPrepared();
    }

    public AudioStageListener mListener;

    public void setOnAudioStageListener(AudioStageListener listener) {
        mListener = listener;
    }

    // 准备方法
    public void prepareAudio() {
        try {
            // 一开始应该是false的
            isPrepared = false;

            File dir = new File(mDirString);
            //判断对象file是否存在
            if (!dir.exists()) {
                //创建此抽象路径指定的目录，包括所有必须但不存在的父目录。（及可以创建多级目录，无论是否存在父目录）
                dir.mkdirs();
            }

            String fileNameString = generalFileName();
            File file = new File(dir, fileNameString);

            mCurrentFilePath = file.getAbsolutePath();

            mRecorder = new MediaRecorder();
            // 设置输出文件
            mRecorder.setOutputFile(file.getAbsolutePath());
            // 设置meidaRecorder的音频源是麦克风
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            // 设置文件音频的输出格式为amr
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
            // 设置音频的编码格式为amr
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            // 严格遵守google官方api给出的mediaRecorder的状态流程图
            mRecorder.prepare();

            mRecorder.start();
            // 准备结束
            isPrepared = true;
            // 已经准备好了，可以录制了
            if (mListener != null) {
                mListener.wellPrepared();
            }

        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * 随机生成文件的名称
     *
     * @return
     */
    private String generalFileName() {

        //UUID.randomUUID().toString()是javaJDK提供的一个自动生成主键的方法。
        return UUID.randomUUID().toString() + ".amr";
    }

    // 获得声音的大小
    public int getVoiceLevel(int maxLevel) {
        // mRecorder.getMaxAmplitude()这个是音频的振幅范围，值域是1-32767
        if (isPrepared) {
            try {
                // 取证+1，否则去不到7
                return maxLevel * mRecorder.getMaxAmplitude() / 32768 + 1;
            } catch (Exception e) {

            }
        }

        return 1;
    }

    // 释放资源
    public void release() {
        // 严格按照api流程进行
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;

    }

    // 取消,因为prepare时产生了一个文件，所以cancel方法应该要删除这个文件，
    // 这是与release的方法的区别
    public void cancel() {
        release();
        if (mCurrentFilePath != null) {
            File file = new File(mCurrentFilePath);
            file.delete();//删除文件
            mCurrentFilePath = null;
        }

    }

    public String getCurrentFilePath() {
        return mCurrentFilePath;
    }

}
