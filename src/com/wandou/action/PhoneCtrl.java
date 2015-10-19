package com.wandou.action;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 *Title:
 *@author 豌豆先生 jitsiang@163.com
 *@date 2015年8月28日
 *@version 
 */
public class PhoneCtrl {
private String mRecognizerCommand = null;
private String contactName,contactNum;
private Context context;
public PhoneCtrl(Context context){
	this.context = context;
}

private void action(){
	if (mRecognizerCommand.indexOf("打开浏览器") >=  0){
		Intent intent = new Intent();        
		intent.setAction("android.intent.action.VIEW");    
		Uri content_url = Uri.parse("http://www.baidu.com");   
		intent.setData(content_url);  
		context.startActivity(intent);
	}
	if (mRecognizerCommand.indexOf("拨打") >=  0){
		
	}
	
}

public void setText(String result){
	mRecognizerCommand = result;
	action();
}
}
