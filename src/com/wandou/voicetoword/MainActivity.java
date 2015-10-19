package com.wandou.voicetoword;

 
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUnderstander;
import com.iflytek.cloud.SpeechUnderstanderListener;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.TextUnderstander;
import com.iflytek.cloud.TextUnderstanderListener;
import com.iflytek.cloud.UnderstanderResult;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.WakeuperListener;
import com.iflytek.cloud.WakeuperResult;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.cloud.util.ResourceUtil;
import com.iflytek.cloud.util.ResourceUtil.RESOURCE_TYPE;
import com.wandou.action.PhoneCtrl;
import com.wandou.action.ReadContacts;
import com.wandou.action.WordToVoice;

import android.R.integer;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

public class MainActivity extends  Activity implements OnClickListener {

	protected static final String TAG = "VoiceCtrl";
	public Button mStart,mSelectPerson;
	public EditText mRecognizerText,mUnderstanderText;
	private Toast mToast;
	
	private WordToVoice mTts;
	// 语音听写对象
	private SpeechRecognizer mIat;
	// 语音听写UI
	private RecognizerDialog mIatDialog;
	
	// 语义理解对象（语音到语义）。
	private SpeechUnderstander mSpeechUnderstander;
	// 语义理解对象（文本到语义）。
	private TextUnderstander mTextUnderstander;

	// 用HashMap存储听写结果
	private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
	// 引擎类型
	private String mEngineType = SpeechConstant.TYPE_CLOUD;
	private SharedPreferences mSharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initLayout();
		
		SpeechUtility.createUtility(this, SpeechConstant.APPID +"=55b71019");   	
		// 初始化识别无UI识别对象
		// 使用SpeechRecognizer对象，可根据回调消息自定义界面；
		mIat = SpeechRecognizer.createRecognizer(MainActivity.this,mInitListener);		
		// 初始化听写Dialog，如果只使用有UI听写功能，无需创建SpeechRecognizer
		// 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
		mIatDialog = new RecognizerDialog(MainActivity.this, mInitListener);
	
		// 初始化合成对象
		mTts = new WordToVoice(this, mInitListener);
		mTextUnderstander = TextUnderstander.createTextUnderstander(MainActivity.this,textUnderstanderListener);		
	//	mSpeechUnderstander = SpeechUnderstander.createUnderstander( MainActivity.this, speechUnderstanderListener);

		//其实这里的mSharedPreferences并没有存储数据
		mSharedPreferences = getSharedPreferences(this.getPackageName(),MODE_PRIVATE);
		mToast = Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT);
			
		// 设置参数
	    setParam();
	}

	private void initLayout() {
		mStart= (Button) findViewById(R.id.btn_start);
		mSelectPerson = (Button)findViewById(R.id.btn_person_select);
		mRecognizerText = (EditText) findViewById(R.id.editText);
		mUnderstanderText = (EditText) findViewById(R.id.editText1);
		mStart.setOnClickListener(this);
		mSelectPerson.setOnClickListener(this);
		
		// 云端发音人名称列表
		cloudVoicersEntries = getResources().getStringArray(
				R.array.voicer_cloud_entries);
		cloudVoicersValue = getResources().getStringArray(
				R.array.voicer_cloud_values);
	}

	// 开始听写
	// 如何判断一次听写结束：OnResult isLast=true 或者 onError
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_start:
			mRecognizerText.setText(null);// 清空显示内容
			mUnderstanderText.setText(null);
			mIatResults.clear();
			boolean isShowDialog = mSharedPreferences.getBoolean(
					getString(R.string.pref_key_iat_show), true);

			if (isShowDialog) {
				// 显示听写对话框
				mIatDialog.setListener(mRecognizerDialogListener);
				mIatDialog.show();
				showTip(getString(R.string.text_begin));
			} else {
				// 不显示听写对话框
				int ret = mIat.startListening(mRecognizerListener);
				if (ret != ErrorCode.SUCCESS) {
					showTip("听写失败,错误码：" + ret);
				} else {
					showTip(getString(R.string.text_begin));
				}
			}
			break;
		case R.id.btn_person_select:
			showPersonSelectDialog();
			break;

		}
	}
	
	/**
	 * 初始化监听器。
	 */
	private InitListener mInitListener = new InitListener() {

		@Override
		public void onInit(int code) {
			Log.d(TAG, "SpeechRecognizer init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
				showTip("初始化失败，错误码：" + code);
			}
		}
	};
	
	/**
	 * 听写UI监听器
	 */
	private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
		public void onResult(RecognizerResult results, boolean isLast) {
		//	Log.e("TAG","here"+results.getResultString());
			printResult(results);
		}
		/**
		 * 识别回调错误.
		 */
		public void onError(SpeechError error) {
			showTip(error.getPlainDescription(true));
		}

	};

	/**
	 * 听写监听器。
	 */
	private RecognizerListener mRecognizerListener = new RecognizerListener() {

		@Override
		public void onBeginOfSpeech() {
			// 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
			showTip("开始说话");
		}

		@Override
		public void onError(SpeechError error) {
			// Tips：
			// 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
			// 如果使用本地功能（语记）需要提示用户开启语记的录音权限。
			showTip(error.getPlainDescription(true));
		}

		@Override
		public void onEndOfSpeech() {
			// 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
			showTip("结束说话");
		}

		@Override
		public void onResult(RecognizerResult results, boolean isLast) {
			//Log.e("TAG", "here"+results.getResultString());
			printResult(results);
			if (isLast) {
				// TODO 最后的结果
			}
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			// 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
			// 若使用本地能力，会话id为null
			//	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
			//		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
			//		Log.d(TAG, "session id =" + sid);
			//	}
		}

		@Override
		public void onVolumeChanged(int arg0) {
			// TODO Auto-generated method stub
			showTip("当前正在说话，音量大小：" + arg0);
		}
	};
	
	
	 /**
     * 初始化监听器（语音到语义）。
     */
//    private InitListener speechUnderstanderListener = new InitListener() {
//		@Override
//		public void onInit(int code) {
//			Log.d(TAG, "speechUnderstanderListener init() code = " + code);
//			if (code != ErrorCode.SUCCESS) {
//        		showTip("初始化失败,错误码："+code);
//        	}		
//		}
//    };
    
    /**
     * 初始化监听器（文本到语义）。
     */
    private InitListener textUnderstanderListener = new InitListener() {

		@Override
		public void onInit(int code) {
			Log.d(TAG, "textUnderstanderListener init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
        		showTip("初始化失败,错误码："+code);
        	}	
		}
    };

	
    
	 /**
     * 语义理解回调。
     */
//	private SpeechUnderstanderListener mSpeechUnderstanderListener = new SpeechUnderstanderListener() {
//
//		@Override
//		public void onResult(final UnderstanderResult result) {
//			if (null != result) {
//				Log.d("TAG", result.getResultString());
//				
//				// 显示
//				String text = result.getResultString();
//				if (!TextUtils.isEmpty(text)) {
//					mUnderstanderText	.setText(text);	
//				}
//			} else {
//				showTip("识别结果不正确。");
//			}	
//		}
//
//        @Override
//        public void onEndOfSpeech() {
//        	// 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
//        	showTip("结束说话");
//        }
//        
//        @Override
//        public void onBeginOfSpeech() {
//        	// 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
//        	showTip("开始说话");
//        }
//
//		@Override
//		public void onError(SpeechError error) {
//			showTip(error.getPlainDescription(true));
//		}
//
//		@Override
//		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
//			// 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
//			//	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
//			//		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
//			//		Log.d(TAG, "session id =" + sid);
//			//	}
//		}
//		
//		@Override
//		public void onVolumeChanged(int arg0) {
//			// TODO Auto-generated method stub
//			showTip("onVolumeChanged：" + arg0);
//		}
//    };
	
private TextUnderstanderListener mTextUnderstanderListener = new TextUnderstanderListener() {
		
		@Override
		public void onResult(final UnderstanderResult result) {

 		    String resulttext = JsonParser.parseIatResult(result.getResultString() );
		    Log.e("TAG", "fuck2-" +result.getResultString() );
			String text = null;
			// 读取json结果中的text字段
			try {
				JSONObject resultJson = new JSONObject(result.getResultString());
				text = resultJson.optString("answer");
				JSONObject resultJson2 = new JSONObject(text);
				text = resultJson2.optString("text");
			} catch (JSONException e) {
				e.printStackTrace();
			}		
			sayWhat(text);
		
//			
//			if (null != result) {
//				// 显示
//				String text = result.getResultString();
//				if (!TextUtils.isEmpty(text)) {
//					 
//					mUnderstanderText.setText(text);
//				}
//			} else {
//				Log.d("TAG", "understander result:null");
//				showTip("识别结果不正确。");
//			}
		}
		@Override
		public void onError(SpeechError error) {
			// 文本语义不能使用回调错误码14002，请确认您下载sdk时是否勾选语义场景和私有语义的发布
			showTip("onError Code："	+ error.getErrorCode());
			
		}
	};
	
	
	private void sayWhat(String text) {
//
//		if (text.indexOf("黄建煌") >= 0) {
//			text = "黄建煌是个大傻逼";
//		}
//		if (text.indexOf("极强") >= 0 || text.indexOf("季强") >= 0) {
//			text = "纪强是个超级大英雄";
//		}
		if (text == "") {
			text = "我不知道你在说什么，请说人话";
		}
		mUnderstanderText.setText(text);
		mTts.startSpeaking(text);
	}

	
	//Toast输出
	private void showTip(final String str) {
		mToast.setText(str);
		mToast.show();
	}
	
	private void printResult(RecognizerResult results) {
		String text = JsonParser.parseIatResult(results.getResultString());
		Log.e("TAG", "fuck2-" + results.getResultString());
		String sn = null;
		// 读取json结果中的sn字段
		try {
			JSONObject resultJson = new JSONObject(results.getResultString());
			sn = resultJson.optString("sn");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		mIatResults.put(sn, text);

		StringBuffer resultBuffer = new StringBuffer();
		for (String key : mIatResults.keySet()) {
			resultBuffer.append(mIatResults.get(key));
		}
		String resultText = resultBuffer.toString();
		mRecognizerText.setText(resultText);
		mRecognizerText.setSelection(mRecognizerText.length());

		if (resultText.indexOf("请打开")  >= 0){
			PhoneCtrl phoneCtrl = new PhoneCtrl(MainActivity.this);
			phoneCtrl.setText(resultText);
			sayWhat("正在打开，请稍候");
		}
		else if (resultText.indexOf("打电话")  >= 0){
			ReadContacts rc = new ReadContacts(MainActivity.this);
			if(!rc.callToContact(resultText)){
				sayWhat("不好意思，我没有找到");
			}
		}else {
			int ret = mTextUnderstander.understandText(resultText,
					mTextUnderstanderListener);
			if (ret != 0) {
				showTip("语义理解失败,错误码:" + ret);
			}
		}
 
	}
	
	/*
	 * 发音人选择
	 */
	private static int selectedNumCloud = 0;
	// 默认云端发音人
	public static String voicerCloud = "xiaoyan";

	// 云端发音人列表
	private String[] cloudVoicersEntries;
	private String[] cloudVoicersValue;
	
	private void showPersonSelectDialog(){
		new AlertDialog.Builder(this).setTitle("在线合成发音人选项")
		.setSingleChoiceItems(cloudVoicersEntries, // 单选框有几项,各是什么名字
				selectedNumCloud, // 默认的选项
				new DialogInterface.OnClickListener() { // 点击单选框后的处理
			public void onClick(DialogInterface dialog,
					int which) { // 点击了哪一项
				voicerCloud = cloudVoicersValue[which];
//				if ("catherine".equals(voicerCloud) || "henry".equals(voicerCloud) || "vimary".equals(voicerCloud)) {
//					 ((EditText) findViewById(R.id.tts_text)).setText(R.string.text_tts_source_en);
//				}else {
//					((EditText) findViewById(R.id.tts_text)).setText(R.string.text_tts_source);
//				}
				mTts.setVoicer(voicerCloud);
				selectedNumCloud = which;
				dialog.dismiss();
			}
		}).show();
	}
	
	/**
	 * 参数设置
	 * 
	 * @param param
	 * @return
	 */
	public void setParam() {
		// 清空参数
		mIat.setParameter(SpeechConstant.PARAMS, null);

		// 设置听写引擎
		mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
		// 设置返回结果格式
		mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");
		mTextUnderstander.setParameter(SpeechConstant.DOMAIN, "iat");
		
		String lag = mSharedPreferences.getString("iat_language_preference",	"mandarin");
			
		if (lag.equals("en_us")) {
			// 设置语言
			mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
		} else {
			// 设置语言
			mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
			// 设置语言区域
			mIat.setParameter(SpeechConstant.ACCENT, lag);
		}

		// 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
		mIat.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("iat_vadbos_preference", "4000"));
		mTextUnderstander.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("understander_vadbos_preference", "4000"));
		// 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
		mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "1000"));
		mTextUnderstander.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("understander_vadeos_preference", "1000"));
		// 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
		mIat.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("iat_punc_preference", "0"));
		mTextUnderstander.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("understander_punc_preference", "0"));
		// 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
		// 注：AUDIO_FORMAT参数语记需要更新版本才能生效
		mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
		mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/iat.wav");
		mTextUnderstander.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
		mTextUnderstander.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/sud.wav");
		
		// 设置听写结果是否结果动态修正，为“1”则在听写过程中动态递增地返回结果，否则只在听写结束之后返回最终结果
		// 注：该参数暂时只对在线听写有效
	mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, mSharedPreferences.getString("iat_dwa_preference", "0"));
	}
}
