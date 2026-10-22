package net.nodedevelopment.nodetv;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;
import java.util.Random;

/** Optional analog hiss while a channel has no reception; independent of stream volume. */
public final class StaticAudio {
    private volatile boolean playing;
    private Thread thread;
    public synchronized void start(){
        if(playing)return;
        playing=true;
        thread=new Thread(()->{
            AudioTrack track=null;
            try {
                int min=AudioTrack.getMinBufferSize(16000,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
                int size=Math.max(4096,min);
                track=new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAudioFormat(new AudioFormat.Builder().setSampleRate(16000).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(size).setTransferMode(AudioTrack.MODE_STREAM).build();
                track.setVolume(0.10f);track.play();
                Random r=new Random();short[] buf=new short[1024];
                while(playing){for(int i=0;i<buf.length;i++)buf[i]=(short)(r.nextInt(6000)-3000);track.write(buf,0,buf.length);}
            }catch(Exception ex){Log.w("NodeTV","Static audio unavailable",ex);}
            finally {if(track!=null){try{track.pause();track.flush();track.release();}catch(Exception ignored){}}}
        },"NODE-TV-CRT-Hiss");
        thread.setDaemon(true);thread.start();
    }
    public synchronized void stop(){playing=false;}
}
