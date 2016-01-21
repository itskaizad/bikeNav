package com.beproj.bikenav;

import java.util.Locale;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.ContactsContract;
import android.speech.tts.TextToSpeech;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class CallStateListener extends PhoneStateListener {
	private int mRingVolume;
	Context context;
	AudioManager mAudioManager;
	TextToSpeech myTTS;

	public CallStateListener(Context cxt) {

		context = cxt;
		myTTS = new TextToSpeech(cxt, new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				if (status != TextToSpeech.ERROR)
					myTTS.setLanguage(Locale.UK);
			}
		});
		mAudioManager = (AudioManager) cxt
				.getSystemService(Context.AUDIO_SERVICE);

	}

	@Override
	public void onCallStateChanged(int state, String incomingNumber) {
		super.onCallStateChanged(state, incomingNumber);
		// Toast.makeText(context,"came in the method",Toast.LENGTH_LONG).show();
		if (state == TelephonyManager.CALL_STATE_RINGING) {
			mRingVolume = mAudioManager
					.getStreamVolume(AudioManager.STREAM_RING);
			mAudioManager.setStreamMute(AudioManager.STREAM_RING, true);
			String phoneNumber = incomingNumber;
			String ContactName = getContactName(context, phoneNumber);
			// Toast.makeText(context, ContactName, Toast.LENGTH_LONG).show();
			if(ContactName==null)
				ContactName = phoneNumber;
			speakWords(ContactName);

		}
		if (state == TelephonyManager.CALL_STATE_IDLE) {
			mAudioManager.setStreamMute(AudioManager.STREAM_RING, false);
			mAudioManager.setStreamVolume(AudioManager.STREAM_RING,
					mRingVolume, AudioManager.FLAG_ALLOW_RINGER_MODES);

		}
	}

	public void speakWords(String speech) {
		// Toast.makeText(context,speech,Toast.LENGTH_LONG).show();
		myTTS.speak("Incoming call from" + speech + "", TextToSpeech.QUEUE_FLUSH, null);
		myTTS.playSilence(3000, TextToSpeech.QUEUE_ADD, null);
		myTTS.speak("Incoming call from" + speech + "", TextToSpeech.QUEUE_ADD, null);
		myTTS.playSilence(3000, TextToSpeech.QUEUE_ADD, null);
		myTTS.speak("Incoming call from" + speech + "", TextToSpeech.QUEUE_ADD, null);
	}

	public static String getContactName(Context context, String phoneNumber) {
		ContentResolver cr = context.getContentResolver();
		Uri uri = Uri.withAppendedPath(
				ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(phoneNumber));
		Cursor cursor = cr.query(uri,
				new String[] { ContactsContract.PhoneLookup.DISPLAY_NAME },
				null, null, null);
		if (cursor == null) {
			return null;
		}
		String contactName = null;
		if (cursor.moveToFirst()) {
			contactName = cursor.getString(cursor
					.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
		}

		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}

		return contactName;
	}
}