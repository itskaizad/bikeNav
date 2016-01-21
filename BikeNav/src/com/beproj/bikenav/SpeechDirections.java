package com.beproj.bikenav;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;

public class SpeechDirections  
{
    private int iVolume;
    private final static int INT_VOLUME_MAX = 100;
    private final static int INT_VOLUME_MIN = 0;
    private final static float FLOAT_VOLUME_MAX = 1;
    private final static float FLOAT_VOLUME_MIN = 0;
    

	AudioManager am;
	Context context;
	TextToSpeech myTTS;
	ArrayList<String> squeue= new ArrayList<String>();
	boolean initialized = false;
    float maxVolume = 15;
    float currVolume = 6;
    float start_value;
    float log1 = 0.0f, log2;
    MusicHandler mh;
	
	public SpeechDirections(Context ctxt)
	{
		am=(AudioManager)ctxt.getSystemService(Context.AUDIO_SERVICE);
	
		//Toast.makeText(context,"in speechdir",Toast.LENGTH_SHORT).show();
		//am.setStreamVolume(AudioManager.STREAM_MUSIC, index, flags)
		context = ctxt;
		 myTTS=new TextToSpeech(ctxt,new TextToSpeech.OnInitListener() {
 	        @Override
 	        public void onInit(int status) {
 	       //if(status!=TextToSpeech.ERROR)
 	       //    myTTS.setLanguage(Locale.UK)
 	       Log.d("EyTTS", "Inside");
 	      pause(200,context);
 	       for(int i=0;i<squeue.size();i++) 
 	       {
 	    	  myTTS.speak(squeue.get(i), TextToSpeech.QUEUE_ADD, null);
 	 	      if(i<squeue.size()-1)
 	    	  	myTTS.speak(" and then", TextToSpeech.QUEUE_ADD, null);
 	       }
 	    	  play(1200);
 	       initialized = true;
 	        }
 	    });
	}
	
	@SuppressLint("NewApi") public void speakOut(String txt)
	{
		if(initialized==false)
			squeue.add(txt);
		else
		{
			
			//fade_out();
			
			myTTS.speak(txt, TextToSpeech.QUEUE_ADD, null);
			Log.d("EyTTS", "Not inside");
			//fade_in();
		}
	}
	

	    public void play(int fadeDuration)
	    {
	        //Set current volume, depending on fade or not
	        if (fadeDuration > 0)
	            iVolume = INT_VOLUME_MIN;
	        else
	            iVolume = INT_VOLUME_MAX;

	        updateVolume(0);

	        //Play music


	        //Start increasing volume in increments
	        if(fadeDuration > 0)
	        {
	            final Timer timer = new Timer(true);
	            TimerTask timerTask = new TimerTask()
	            {
	                @Override
	                public void run()
	                {
	                    updateVolume(1);
	                    if (iVolume == INT_VOLUME_MAX)
	                    {
	                        timer.cancel();
	                        timer.purge();
	                    }
	                }
	            };

	            // calculate delay, cannot be zero, set to 1 if zero
	            int delay = fadeDuration/INT_VOLUME_MAX;
	            if (delay == 0) delay = 1;

	            timer.schedule(timerTask, delay, delay);
	        }
	    }

	    public void pause(int fadeDuration,Context cxt)
	    {
	        //Set current volume, depending on fade or not
	        if (fadeDuration > 0)
	            iVolume = INT_VOLUME_MAX;
	        else
	            iVolume = INT_VOLUME_MIN;

	        updateVolume(0);

	        //Start increasing volume in increments
	        if(fadeDuration > 0)
	        {
	            final Timer timer = new Timer(true);
	            TimerTask timerTask = new TimerTask()
	            {
	                @Override
	                public void run()
	                {
	                    updateVolume(-1);
	                    if (iVolume == INT_VOLUME_MIN)
	                    {
	                        //Pause music

	                        timer.cancel();
	                        timer.purge();
	                       
	                       
	                    }
	                }
	            };

	            // calculate delay, cannot be zero, set to 1 if zero
	            int delay = fadeDuration/INT_VOLUME_MAX;
	            if (delay == 0) delay = 1;

	            timer.schedule(timerTask, delay, delay);
	        }

	    }

	    private void updateVolume(int change)
	    {
	        //increment or decrement depending on type of fade
	        iVolume = iVolume + change;

	        //ensure iVolume within boundaries
	        if (iVolume < INT_VOLUME_MIN)
	            iVolume = INT_VOLUME_MIN;
	        else if (iVolume > INT_VOLUME_MAX)
	            iVolume = INT_VOLUME_MAX;

	        //convert to float value
	        float fVolume = 1 - ((float) Math.log(INT_VOLUME_MAX - iVolume) / (float) Math.log(INT_VOLUME_MAX));

	        //ensure fVolume within boundaries
	        if (fVolume < FLOAT_VOLUME_MIN)
	            fVolume = FLOAT_VOLUME_MIN;
	        else if (fVolume > FLOAT_VOLUME_MAX)
	            fVolume = FLOAT_VOLUME_MAX;

	        am.setStreamVolume(
	                AudioManager.STREAM_MUSIC,
	                iVolume,
	                0);

	        Log.d("ivolumevalue",Integer.toString(iVolume));
	    }
	    
	    
}
