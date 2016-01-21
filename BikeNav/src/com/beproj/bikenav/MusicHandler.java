package com.beproj.bikenav;

import android.content.Context;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MusicHandler
{
	Context context;
    AudioManager am;
    private int iVolume;
    private final static int INT_VOLUME_MAX = 100;
    private final static int INT_VOLUME_MIN = 0;
    private final static float FLOAT_VOLUME_MAX = 1;
    private final static float FLOAT_VOLUME_MIN = 0;
    


    public MusicHandler(Context context)
    {
        this.context = context;

               am= (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);

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