package com.czt.mp3recorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import com.czt.mp3recorder.util.LameUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MP3Recorder {
	// =======================AudioRecord Default
	// Settings=======================
	private static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
	/**
	 * 以下三项为默认配置参数。Google Android文档明确表明只有以下3个参数是可以在所有设备上保证支持的。
	 */
	private static final int DEFAULT_SAMPLING_RATE = 44100;// 模拟器仅支持从麦克风输入8kHz采样率,,输入采样频率
															// Hz
	private static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	/**
	 * 下面是对此的封装 private static final int DEFAULT_AUDIO_FORMAT =
	 * AudioFormat.ENCODING_PCM_16BIT;
	 */
	private static final PCMFormat DEFAULT_AUDIO_FORMAT = PCMFormat.PCM_16BIT;

	// ======================Lame Default Settings=====================
	// quality ： MP3音频质量。0~9。 其中0是最好，非常慢，9是最差。
	// 2 ：near-best quality, not too slow
	// 5 ：good quality, fast
	// 7 ：ok quality, really fast
	private static final int DEFAULT_LAME_MP3_QUALITY = 5;
	/**
	 * 与DEFAULT_CHANNEL_CONFIG相关，因为是mono单声，所以是1 输入声道数
	 */
	private static final int DEFAULT_LAME_IN_CHANNEL = 1;
	/**
	 * Encoded bit rate. MP3 file will be encoded with bit rate 32kbps
	 */
	private static final int DEFAULT_LAME_MP3_BIT_RATE = 32;

	/**
	 * 自定义 每160帧作为一个周期，通知一下需要进行编码
	 */
	private static final int FRAME_COUNT = 160;
	private AudioRecord mAudioRecord = null;
	private int mBufferSize;
	private short[] mPCMBuffer;
	private DataEncodeThread mEncodeThread;
	private boolean mIsRecording = false;
	private File mRecordFile;

	private Handler handler;// 时间handler
	private int maxTime;// 录音最大时间

	public static boolean is_refuse_permision = false;
	/**
	 * Default constructor. Setup recorder with default sampling rate 1 channel,
	 * 16 bits pcm
	 * 
	 * @param recordFile
	 *            target file
	 */
	public static String pathUrl = Environment.getExternalStorageDirectory()
			+ "/fft/record.mp3";

	public MP3Recorder(File file, Handler handler, int maxTime) {
		mRecordFile = file;
		this.handler = handler;
		this.maxTime = maxTime;
	}

	public MP3Recorder() {
	}
	/**
	 * Start recording. Create an encoding thread. Start record from this
	 * thread.
	 * 
	 * @throws IOException
	 *             initAudioRecorder throws
	 */
	public void start() throws IOException {
		if (mIsRecording)
			return;
		initAudioRecorder();
		try {
			mAudioRecord.startRecording();
			is_refuse_permision = false;
			mIsRecording = true;
		} catch (IllegalStateException exception) {
			is_refuse_permision = true;
			exception.printStackTrace();
		}
		if (!is_refuse_permision) {
			new Thread() {
				@Override
				public void run() {
					// 设置线程权限
					android.os.Process
							.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
					while (mIsRecording) {
						int readSize = mAudioRecord.read(mPCMBuffer, 0,
								mBufferSize);
						if (readSize > 0) {
							mEncodeThread.addTask(mPCMBuffer, readSize);
							calculateRealVolume(mPCMBuffer, readSize);
						}
					}
					// release and finalize audioRecord
					mAudioRecord.stop();
					mAudioRecord.release();
					mAudioRecord = null;
					// stop the encoding thread and try to wait
					// until the thread finishes its job
					Message msg = Message.obtain(mEncodeThread.getHandler(),
							DataEncodeThread.PROCESS_STOP);
					msg.sendToTarget();										
				}

				/**
				 * 此计算方法来自samsung开发范例
				 * @param buffer
				 *            buffer
				 * @param readSize
				 *            readSize
				 */
				private void calculateRealVolume(short[] buffer, int readSize) {
					double sum = 0;
					for (int i = 0; i < readSize; i++) {
						// 这里没有做运算的优化，为了更加清晰的展示代码
						sum += buffer[i] * buffer[i];
					}
					if (readSize > 0) {
						double amplitude = sum / readSize;
						mVolume = (int) Math.sqrt(amplitude);
					}
				}
			}.start();
		}
		
		if (!is_refuse_permision) {
			// 录音计时器
			// 此处thread负责发送消息, 来控制时间显示
			Thread time_thread;
			time_thread = new Thread(new Runnable() {
				@Override
				public void run() {
					int time = 0;
					for (int i = 0; i < maxTime; i++) {
						try {
							if (!mIsRecording) {
								break;
							}
							Thread.sleep(1000);
							Message msg = new Message();
							msg.what = 101;
							time += 1;
							msg.obj = time;
							handler.sendMessage(msg);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			});
			
			time_thread.start();
		}

	}

	private int mVolume;

	public int getVolume() {
		return mVolume;
	}

	private static final int MAX_VOLUME = 2000;

	public int getMaxVolume() {
		return MAX_VOLUME;
	}

	public void stop() {
		mIsRecording = false;
	}

	public boolean isRecording() {
		return mIsRecording;
	}

	/**
	 * Initialize audio recorder
	 */
	public void initAudioRecorder() throws IOException {
		mBufferSize = AudioRecord.getMinBufferSize(DEFAULT_SAMPLING_RATE,
				DEFAULT_CHANNEL_CONFIG, DEFAULT_AUDIO_FORMAT.getAudioFormat());

		int bytesPerFrame = DEFAULT_AUDIO_FORMAT.getBytesPerFrame();
		/*
		 * Get number of samples. Calculate the buffer size (round up to the
		 * factor of given frame size) 使能被整除，方便下面的周期性通知
		 */
		int frameSize = mBufferSize / bytesPerFrame;
		if (frameSize % FRAME_COUNT != 0) {
			frameSize += (FRAME_COUNT - frameSize % FRAME_COUNT);
			mBufferSize = frameSize * bytesPerFrame;
		}

		/* Setup audio recorder */
		mAudioRecord = new AudioRecord(DEFAULT_AUDIO_SOURCE,
				DEFAULT_SAMPLING_RATE, DEFAULT_CHANNEL_CONFIG,
				DEFAULT_AUDIO_FORMAT.getAudioFormat(), mBufferSize);

		mPCMBuffer = new short[mBufferSize];
		/*
		 * Initialize lame buffer mp3 sampling rate is the same as the recorded
		 * pcm sampling rate The bit rate is 32kbps
		 */
		LameUtil.init(DEFAULT_SAMPLING_RATE, DEFAULT_LAME_IN_CHANNEL,
				DEFAULT_SAMPLING_RATE, DEFAULT_LAME_MP3_BIT_RATE,
				DEFAULT_LAME_MP3_QUALITY);
		// Create and run thread used to encode data
		// The thread will
		mEncodeThread = new DataEncodeThread(mRecordFile, mBufferSize);
		mEncodeThread.start();
		mAudioRecord.setRecordPositionUpdateListener(mEncodeThread,
				mEncodeThread.getHandler());
		mAudioRecord.setPositionNotificationPeriod(FRAME_COUNT);
	}

	/**
	 * 文件转化为字节数组
	 */
	public static byte[] getBytesFromFile(File file) {
		byte[] ret = null;
		try {
			if (file == null) {
				// log.error("helper:the file is null!");
				return null;
			}
			FileInputStream in = new FileInputStream(file);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] b = new byte[1024];
			int n;
			while ((n = in.read(b)) != -1) {
				out.write(b, 0, n);
			}
			ret = out.toByteArray();
			in.close();
			out.flush();
			out.close();
		} catch (IOException e) {
			// log.error("helper:get bytes from file process error!");
			e.printStackTrace();
		}
		return ret;
	}
	
	
	public void cleanFile(boolean isDelete){
		if(isDelete){
			if(mRecordFile.exists()){
				mRecordFile.delete();
			}
		}
	}
}