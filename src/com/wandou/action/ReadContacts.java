package com.wandou.action;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * Title:
 *
 * @author 豌豆先生 jitsiang@163.com
 * @date 2015年9月1日
 * @version
 */
public class ReadContacts {
	private Context context;
	private Map<String, String> contacts = new HashMap<String, String>();

	public ReadContacts(Context context) {
		this.context = context;
		
		readAllContacts();
	}

	public boolean callToContact(String command) {
		boolean find  = false;
		if (command.contains("给")) {
			String name = command.trim().substring(command.indexOf("给") + 1);
			Log.i("TAG", "name" + name);
			for (String key : contacts.keySet()){
				Pattern pattern = Pattern.compile(key);
				Matcher matcher = pattern.matcher(name);
//if (key.equals(name)){
				if (matcher.find()) {
					find = true;
					String phoneNumber = contacts.get(key);
					Log.i("TAG", find+name + "/" + phoneNumber);
					Intent dialIntent = new Intent(Intent.ACTION_CALL,
							Uri.parse("tel:" + phoneNumber));
					context.startActivity(dialIntent);
					break;
				}  
			}
		}
		return find;
	}

	public void readAllContacts() {
		Cursor cursor = context.getContentResolver().query(
				ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
		int contactIdIndex = 0;
		int nameIndex = 0;
		if (cursor.getCount() > 0) {
			contactIdIndex = cursor
					.getColumnIndex(ContactsContract.Contacts._ID);
			nameIndex = cursor
					.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
		}
		String name,phoneNumber;
		while (cursor.moveToNext()) {
			String contactId = cursor.getString(contactIdIndex);
			name = cursor.getString(nameIndex);
			Log.i("TAG", contactId);
			Log.i("TAG", name);

			Cursor phones = context.getContentResolver().query(
					ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
					null,
					ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "="
							+ contactId, null, null);
			int phoneIndex = 0;
			if (phones.getCount() > 0) {
				phoneIndex = phones
						.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
			}
           //一个联系人有多个电话号码
			while (phones.moveToNext()) {
				phoneNumber = phones.getString(phoneIndex);
				Log.i("TAG", phoneNumber);	
				contacts.put(name, phoneNumber);		
			}
		
	 //	phoneNumber = phones.getString(phoneIndex);
		
		}

	}

}
