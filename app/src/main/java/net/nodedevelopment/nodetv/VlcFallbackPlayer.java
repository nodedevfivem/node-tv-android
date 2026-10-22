package net.nodedevelopment.nodetv;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.TextureView;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IVLCVout;
import java.util.ArrayList;

/**
 * VLC fallback for streams Media3 cannot decode/open. The same TextureView moves between
 * the portrait TV aperture and landscape fullscreen; native VLC MUST be told the new
 * drawable dimensions after each surface/layout change. Otherwise VLC retains the old
 * portrait window geometry (tiny offset video in fullscreen, cropped video on return).
 */
public final class VlcFallbackPlayer {
    public interface Listener { void onPlaying(); void onError(String reason); }
    private final Context context;
    private final Listener listener;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final TextureView surface;
    private LibVLC libVlc;
    private MediaPlayer engine;
    private Channel pending;
    private boolean active, released;
    private float volume=1f;
    private int lastWidth=-1, lastHeight=-1;

    public VlcFallbackPlayer(Context context,Listener listener){
        this.context=context.getApplicationContext();this.listener=listener;
        surface=new TextureView(context);
        surface.setOpaque(true);
        surface.setSurfaceTextureListener(new TextureView.SurfaceTextureListener(){
            @Override public void onSurfaceTextureAvailable(SurfaceTexture texture,int w,int h){
                if(!active||released)return;
                if(pending!=null){Channel ch=pending;pending=null;start(ch);}
                else attachView();
                updateViewport(w,h);
            }
            @Override public void onSurfaceTextureSizeChanged(SurfaceTexture t,int w,int h){
                updateViewport(w,h);
            }
            @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture t){
                detachView();return true;
            }
            @Override public void onSurfaceTextureUpdated(SurfaceTexture t){}
        });
        // A TextureView's surface-size callback is not guaranteed to fire every time its
        // parent changes size. This also catches same-surface portrait/fullscreen moves.
        surface.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or_,ob)->{
            if((r-l)!=(or_-ol)||(b-t)!=(ob-ot))updateViewport(r-l,b-t);
        });
    }
    public TextureView view(){return surface;}
    public void play(Channel channel){
        stop();active=true;pending=channel;
        if(surface.isAvailable()){
            pending=null;start(channel);
        }
    }
    private void initialize(){
        if(libVlc!=null)return;
        ArrayList<String> options=new ArrayList<>();
        options.add("--no-video-title-show");
        options.add("--network-caching=1400");
        options.add("--http-reconnect");
        libVlc=new LibVLC(context,options);
        engine=new MediaPlayer(libVlc);
        engine.setEventListener(event->{
            int type=event.type;
            main.post(()->{
                if(!active||released)return;
                if(type==MediaPlayer.Event.Playing){
                    // Hardware and software VLC outputs may learn the native video layout
                    // only after the first decoded frames. Re-assert automatic fit then.
                    updateViewport(surface.getWidth(),surface.getHeight());
                    listener.onPlaying();
                }
                else if(type==MediaPlayer.Event.EncounteredError){listener.onError("stream unavailable on Android");}
                else if(type==MediaPlayer.Event.EndReached){listener.onError("stream ended");}
            });
        });
    }
    private void fitVideoToWindow(){
        if(engine==null)return;
        // 0 = LibVLC automatic fit-to-drawable; null = stream's own display aspect ratio.
        // Never force 16:9 or crop genuine 4:3/cinematic streams.
        engine.setAspectRatio(null);
        engine.setScale(0f);
    }
    private void attachView(){
        if(engine==null||!active||!surface.isAvailable())return;
        IVLCVout vout=engine.getVLCVout();
        if(!vout.areViewsAttached()){
            vout.setVideoView(surface);
            vout.attachViews();
            lastWidth=-1;lastHeight=-1;
        }
        fitVideoToWindow();
        updateViewport(surface.getWidth(),surface.getHeight());
    }
    /** The containing viewport changed (even if the surface identity stayed the same). */
    public void updateViewport(int w,int h){
        if(!active||released||engine==null||w<=0||h<=0||!surface.isAvailable())return;
        IVLCVout vout=engine.getVLCVout();
        if(!vout.areViewsAttached())return;
        if(lastWidth!=w||lastHeight!=h){
            lastWidth=w;lastHeight=h;
            vout.setWindowSize(w,h);
            fitVideoToWindow();
            Log.d("NodeTV","VLC viewport synced to "+w+"x"+h);
        }
    }
    private void detachView(){
        lastWidth=-1;lastHeight=-1;
        if(engine==null)return;
        IVLCVout vout=engine.getVLCVout();
        if(vout.areViewsAttached())vout.detachViews();
    }
    private void start(Channel ch){
        if(!active||released)return;
        try {
            initialize();
            attachView();
            Media media=new Media(libVlc,Uri.parse(ch.url));
            try{
                media.setHWDecoderEnabled(true,false);
                media.addOption(":network-caching=1400");
                media.addOption(":http-reconnect");
                if(!ch.referer.isEmpty())media.addOption(":http-referrer="+ch.referer);
                media.addOption(":http-user-agent="+(ch.userAgent.isEmpty()?"VLC/3.0.21 LibVLC/3.0.21":ch.userAgent));
                engine.setMedia(media);
            }finally{media.release();}
            engine.setVolume(Math.round(volume*100));
            fitVideoToWindow();
            engine.play();
        }catch(Throwable error){
            Log.w("NodeTV","VLC engine could not start",error);
            active=false;listener.onError("VLC cannot open this stream");
        }
    }
    public void setVolume(float v){
        volume=Math.max(0f,Math.min(1f,v));
        if(engine!=null)engine.setVolume(Math.round(volume*100));
    }
    public boolean isPlaying(){return engine!=null&&engine.isPlaying();}
    public void setPaused(boolean pause){
        if(engine!=null&&active){if(pause)engine.pause();else engine.play();}
    }
    /** Detach before reparenting TextureView, without stopping the active IPTV stream. */
    public void beforeMove(){detachView();}
    /** Reattach after parent move. A later size callback will sync the final orientation. */
    public void afterMove(){
        if(!active||released)return;
        if(surface.isAvailable())attachView();
    }
    public void stop(){
        active=false;pending=null;
        if(engine!=null){engine.stop();detachView();}
    }
    public void release(){
        if(released)return;
        released=true;stop();
        if(engine!=null){engine.setEventListener(null);engine.release();engine=null;}
        if(libVlc!=null){libVlc.release();libVlc=null;}
    }
}
