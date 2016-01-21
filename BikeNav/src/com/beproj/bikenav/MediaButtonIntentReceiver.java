package com.beproj.bikenav;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class MediaButtonIntentReceiver extends BroadcastReceiver {

	public MediaButtonIntentReceiver() {
		super();
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		String intentAction = intent.getAction();
		if (!Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
			return;
		}
		KeyEvent event = (KeyEvent) intent
				.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
		if (event == null) {
			return;
		}
		long start = 0, end, diff;
		int action = event.getAction();
		if (event.getKeyCode() == 126 || event.getKeyCode() == 127) {
			// do something KEYCODE_MEDIA_PLAY
			if (action == KeyEvent.ACTION_DOWN) {
				start = event.getEventTime();
				long time1 = System.currentTimeMillis() - event.getEventTime();
				// Toast.makeText(context,
				// event.getEventTime()+"BUTTON PRESSED!"+System.currentTimeMillis(),
				// Toast.LENGTH_LONG).show();
				Intent i = new Intent(context, MainActivity2.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(i);

				long time = event.getEventTime();
				if (event.isLongPress() == true) {
					Toast.makeText(context, "Finally long press hua!!!",
							Toast.LENGTH_LONG).show();

				}

			}

			if (action == KeyEvent.ACTION_UP) {
				end = event.getEventTime();
				long time1 = System.currentTimeMillis() - event.getEventTime();
				diff = end - start;
				// Toast.makeText(context,
				// event.getEventTime()+"BUTTON RELEASED!"+System.currentTimeMillis(),
				// Toast.LENGTH_LONG).show();
				long time = event.getEventTime();

				if (event.isLongPress() == true) {
					Toast.makeText(context, "Finally long press hua!!!",
							Toast.LENGTH_LONG).show();

				}

			}
		}
	}

}
