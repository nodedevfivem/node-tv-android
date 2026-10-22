package net.nodedevelopment.nodetv;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.TextureView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@OptIn(markerClass = UnstableApi.class)
public final class MainActivity extends Activity {
    private static final String VERSION="0.1.5";
    private static final String[] THEMES={"Vintage Amber","NODE Electric Blue","Nebula Violet","Forest Night","Crimson Theater","Graphite","Deep Teal","Copper Sunset"};
    private static final int[] ACCENTS={0xfff0ab4e,0xff42caff,0xffc29bff,0xff69c49a,0xffee7677,0xffb8c0d0,0xff4dd7cc,0xffdc884b};
    private static final int[] BACKGROUNDS={0xff101016,0xff0a1320,0xff14101f,0xff0d1915,0xff1c1014,0xff15161a,0xff0b191b,0xff211611};
    private static final int[] PANELS={0xff202027,0xff172436,0xff251c32,0xff1a2820,0xff2c1b20,0xff26282e,0xff172e30,0xff32231c};
    private static final int[] ROWS={0xff24252e,0xff1d3044,0xff2b243e,0xff22392f,0xff37242b,0xff303239,0xff1d383a,0xff403028};
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    // Update metadata/download must not wait behind slow IPTV playlist requests.
    private final ExecutorService updateIo=Executors.newSingleThreadExecutor();
    private final StaticAudio hiss=new StaticAudio();
    private UserStore store;
    private ExoPlayer player;
    private DefaultHttpDataSource.Factory networkFactory;
    private VlcFallbackPlayer vlc;
    private boolean usingVlc, activeSignal;
    private float broadcastVolume=1f;
    private static final long EXO_CONNECT_TIMEOUT_MS=11000L;
    private static final long VLC_CONNECT_TIMEOUT_MS=24000L;
    private static final String DEFAULT_STREAM_UA="VLC/3.0.21 LibVLC/3.0.21";
    private PlayerView playerView;
    private RetroNoiseView noise;
    private TextView signalLabel,nowPlaying,statusText,countText,qualityText;
    private FrameLayout root,videoSlot,retroFrame,fullscreenLayer;
    private LinearLayout mainColumn,updateBanner;
    private TextView updateTitle,updateSubtitle;
    private Button updateLaterButton;
    private FrameLayout mandatoryOverlay;
    private TextView mandatoryDescription,fullscreenIndicator;
    private Button fullscreenExitButton;
    private FullscreenGestureView gestureView;
    private AudioManager audioManager;
    private float gestureStartBrightness, originalWindowBrightness=-1f;
    private int gestureStartMediaVolume, gestureMaxMediaVolume=1;
    // videoSlot is permanently parented to root. Never reparent a live TextureView:
    // that destroys its native output surface on some Android devices (audio-only VLC).
    // Move/resize only its FrameLayout bounds while the surface stays attached.
    private int lastVideoLeft=Integer.MIN_VALUE,lastVideoTop=Integer.MIN_VALUE;
    private int lastVideoWidth=Integer.MIN_VALUE,lastVideoHeight=Integer.MIN_VALUE;
    private final Runnable hideFullscreenIndicator=()->{if(fullscreenIndicator!=null)fullscreenIndicator.setVisibility(View.GONE);};
    private final Runnable hideFullscreenExit=()->{
        if(fullscreenExitButton!=null)fullscreenExitButton.setVisibility(View.GONE);
    };
    private boolean mandatoryUpdate,userPaused;
    private EditText search;
    private Spinner categorySpinner,themeSpinner;
    private ListView list;
    private ChannelAdapter adapter;
    private Button[] modeButtons;
    private List<Channel> directory=new ArrayList<>();
    private final List<Channel> filtered=new ArrayList<>();
    private Channel playing;
    private int selectedRegion=1,mode=0,accent;
    private boolean fullscreen, muted;
    private String category="All Categories";
    private long loadGeneration,playGeneration,lastUpdateCheck;
    private int qualityWidth,qualityHeight;
    private UpdateClient.Release availableRelease;
    private File downloadedUpdate;
    private boolean awaitingInstallPermission;
    private boolean downloading;
    // Timers are scoped to the currently selected channel. Old channel timeouts are cancelled.
    private Runnable tuneTimeout;
    private void cancelTuneTimeout(){
        if(tuneTimeout!=null)ui.removeCallbacks(tuneTimeout);
        tuneTimeout=null;
    }
    private void scheduleTuneTimeout(long millis,long generation,boolean vlcEngine){
        cancelTuneTimeout();
        tuneTimeout=()->{
            if(generation!=playGeneration||playing==null||activeSignal||usingVlc!=vlcEngine)return;
            if(vlcEngine)finishNoReception("NO RECEPTION — both playback engines timed out");
            else tryVlcFallback("Stream did not start in time");
        };
        ui.postDelayed(tuneTimeout,millis);
    }
    private final Runnable repeatUpdateChecks=new Runnable(){
        @Override public void run(){checkUpdates(false);ui.postDelayed(this,30L*60*1000);}
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store=new UserStore(this);selectedRegion=store.lastRegion();mode=selectedRegion;accent=ACCENTS[store.theme()];
        audioManager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
        getWindow().setStatusBarColor(0xff0c0d11);getWindow().setNavigationBarColor(0xff0c0d11);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        networkFactory=new DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(12000)
            .setReadTimeoutMs(16000)
            .setUserAgent(DEFAULT_STREAM_UA);
        player=new ExoPlayer.Builder(this)
            .setMediaSourceFactory(new DefaultMediaSourceFactory(networkFactory)).build();
        playerView=new PlayerView(this);
        playerView.setPlayer(player);playerView.setUseController(false);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        player.addListener(new Player.Listener(){
            @Override public void onIsPlayingChanged(boolean isPlaying){
                if(isPlaying&&playing!=null&&!usingVlc)onPlaybackSucceeded("Android",playGeneration);
            }
            @Override public void onPlaybackStateChanged(int state){
                if(usingVlc||playing==null)return;
                if(state==Player.STATE_BUFFERING){
                    if(activeSignal&&!userPaused) {activeSignal=false;scheduleTuneTimeout(12000,playGeneration,false);}
                    setStatus("Buffering: "+playing.title);
                } else if(state==Player.STATE_READY&&player.getPlayWhenReady())onPlaybackSucceeded("Android",playGeneration);
                else if(state==Player.STATE_ENDED&&!userPaused)tryVlcFallback("Channel stream ended");
            }
            @Override public void onPlayerError(PlaybackException error){
                if(playing!=null&&!usingVlc){
                    Log.w("NodeTV", "Media3 playback error: "+error.getErrorCodeName());
                    tryVlcFallback("Android codec/network compatibility");
                }
            }
            @Override public void onVideoSizeChanged(VideoSize v){qualityWidth=v.width;qualityHeight=v.height;quality();}
        });
        buildUi();
        restoreRequiredUpdateGate();
        loadRegion(selectedRegion==1);
        ui.postDelayed(()->checkUpdates(false),1500);
        ui.postDelayed(repeatUpdateChecks,30L*60*1000);
    }

    private int dp(float p){return (int)(getResources().getDisplayMetrics().density*p+.5f);}
    private GradientDrawable bg(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private LinearLayout vertical(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private LinearLayout horizontal(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private TextView label(String s,int size,int color,boolean bold){
        TextView v=new TextView(this);v.setText(s);v.setTextColor(color);v.setTextSize(size);
        v.setGravity(Gravity.CENTER_VERTICAL);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;
    }
    private Button button(String s){
        Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextColor(0xfff4f0e9);
        b.setTextSize(12);b.setPadding(dp(9),0,dp(9),0);b.setMinHeight(0);b.setMinimumHeight(0);
        b.setBackground(bg(0xff30303a,8));return b;
    }
    private Button goldButton(String s){Button b=button(s);b.setBackground(bg(accent,8));b.setTextColor(0xff171414);return b;}
    private LinearLayout.LayoutParams wrapButton(){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(40));lp.setMargins(dp(3),0,dp(3),0);return lp;}
    private void buildUi(){
        root=new FrameLayout(this);root.setBackgroundColor(BACKGROUNDS[store.theme()]);setContentView(root);
        root.setOnApplyWindowInsetsListener((view,insets)->{
            if(fullscreen)view.setPadding(0,0,0,0);
            else view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        mainColumn=vertical();root.addView(mainColumn,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout header=horizontal();header.setPadding(dp(12),dp(6),dp(9),dp(6));header.setBackgroundColor(PANELS[store.theme()]);
        mainColumn.addView(header,new LinearLayout.LayoutParams(-1,dp(66)));
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.node_tv_logo);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        header.addView(logo,new LinearLayout.LayoutParams(dp(55),dp(55)));
        LinearLayout brand=vertical();LinearLayout.LayoutParams brandLp=new LinearLayout.LayoutParams(0,-2,1);brandLp.leftMargin=dp(6);header.addView(brand,brandLp);
        brand.addView(label("NODE TV",20,accent,true));brand.addView(label("NODE DEVELOPMENT  ·  ANDROID",9,0xffbeb8ab,false));
        // The update banner is automatic: users no longer have to find a header button.
        updateBanner=vertical();updateBanner.setPadding(dp(14),dp(9),dp(14),dp(10));
        GradientDrawable updateSurface=bg(0xff392819,10);updateSurface.setStroke(dp(2),accent);
        updateBanner.setBackground(updateSurface);
        LinearLayout.LayoutParams bannerParams=new LinearLayout.LayoutParams(-1,-2);
        bannerParams.setMargins(dp(8),dp(6),dp(8),dp(4));
        mainColumn.addView(updateBanner,bannerParams);updateBanner.setVisibility(View.GONE);
        updateTitle=label("📺  NEW UPDATE AVAILABLE",16,accent,true);updateBanner.addView(updateTitle);
        updateSubtitle=label("A newer version of NODE TV is ready.",12,0xfff5e6d4,false);
        updateBanner.addView(updateSubtitle);
        LinearLayout actionRow=horizontal();actionRow.setPadding(0,dp(6),0,0);updateBanner.addView(actionRow);
        Button now=goldButton("⬇ UPDATE NOW");actionRow.addView(now,wrapButton());now.setOnClickListener(v->downloadUpdate());
        updateLaterButton=button("LATER");actionRow.addView(updateLaterButton,wrapButton());
        updateLaterButton.setOnClickListener(v->{if(!mandatoryUpdate){store.snooze();updateBanner.setVisibility(View.GONE);}});
        Button details=button("DETAILS");actionRow.addView(details,wrapButton());details.setOnClickListener(v->showReleaseDetails());

        LinearLayout screenHeader=horizontal();screenHeader.setPadding(dp(12),dp(7),dp(12),dp(5));mainColumn.addView(screenHeader,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout names=vertical();screenHeader.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        names.addView(label("LIVE TELEVISION",10,accent,true));
        nowPlaying=label("Select a channel to watch",15,0xffeeeae4,true);nowPlaying.setMaxLines(1);nowPlaying.setEllipsize(android.text.TextUtils.TruncateAt.END);names.addView(nowPlaying);
        Button retry=button("↻ Retry");screenHeader.addView(retry,wrapButton());retry.setOnClickListener(v->{if(playing!=null)tune(playing);});
        Button full=button("⛶ Fullscreen");screenHeader.addView(full,wrapButton());full.setOnClickListener(v->toggleFullscreen());

        retroFrame=new FrameLayout(this){@Override protected void onMeasure(int w,int h){
            int width=View.MeasureSpec.getSize(w);super.onMeasure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(width*2/3,View.MeasureSpec.EXACTLY));
        } };
        retroFrame.setBackgroundColor(0xff1b1c24);
        mainColumn.addView(retroFrame,new LinearLayout.LayoutParams(-1,-2));
        videoSlot=new FrameLayout(this);videoSlot.setBackgroundColor(Color.BLACK);
        // Root-level overlay: this is the ONE stable parent for the video surfaces.
        // In portrait we precisely place it inside the retro-TV picture aperture;
        // in landscape we resize it to fullscreen WITHOUT detaching VLC's TextureView.
        root.addView(videoSlot,new FrameLayout.LayoutParams(dp(200),dp(160)));
        videoSlot.addView(playerView,new FrameLayout.LayoutParams(-1,-1));
        // The LibVLC view stays in the exact same TV viewport. It is only visible on fallback.
        vlc=new VlcFallbackPlayer(this,new VlcFallbackPlayer.Listener(){
            @Override public void onPlaying(){
                if(usingVlc&&playing!=null)onPlaybackSucceeded("VLC",playGeneration);
            }
            @Override public void onError(String reason){
                if(usingVlc&&playing!=null)finishNoReception("NO RECEPTION — "+reason);
            }
        });
        videoSlot.addView(vlc.view(),new FrameLayout.LayoutParams(-1,-1));
        vlc.view().setVisibility(View.GONE);
        // The stable video aperture changes dimensions without ever losing its parent.
        // Always synchronize VLC's drawable on size changes.
        videoSlot.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or_,ob)->{
            if(usingVlc && ((r-l)!=(or_-ol)||(b-t)!=(ob-ot)))
                vlc.updateViewport(r-l,b-t);
        });
        noise=new RetroNoiseView(this);videoSlot.addView(noise,new FrameLayout.LayoutParams(-1,-1));
        signalLabel=label("NO RECEPTION",12,0xfff8e3c2,true);signalLabel.setGravity(Gravity.CENTER);
        signalLabel.setVisibility(View.GONE);videoSlot.addView(signalLabel,new FrameLayout.LayoutParams(-1,-1));
        ImageView wood=new ImageView(this);wood.setImageResource(R.drawable.retro_frame);
        wood.setScaleType(ImageView.ScaleType.FIT_XY);retroFrame.addView(wood,new FrameLayout.LayoutParams(-1,-1));
        wood.setClickable(false);
        LinearLayout controls=horizontal();controls.setPadding(dp(10),dp(4),dp(10),dp(5));controls.setBackgroundColor(PANELS[store.theme()]);
        mainColumn.addView(controls,new LinearLayout.LayoutParams(-1,dp(48)));
        Button play=goldButton("▶ / ❚❚");controls.addView(play,wrapButton());play.setOnClickListener(v->togglePlaybackPause());
        Button stop=button("■");controls.addView(stop,wrapButton());stop.setOnClickListener(v->stopPlaying());
        Button favorite=button("♡ Favorite");controls.addView(favorite,wrapButton());favorite.setOnClickListener(v->{if(playing!=null){store.toggleFavorite(playing);refreshCollection();toast(store.isFavorite(playing)?"Added to Favorite TV Channels":"Removed from favorites");}});
        Button mute=button("♬");controls.addView(mute,wrapButton());mute.setOnClickListener(v->{
            muted=!muted;applyBroadcastVolume();mute.setText(muted?"Muted":"♬");
        });
        SeekBar volume=new SeekBar(this);volume.setMax(100);volume.setProgress(100);
        controls.addView(volume,new LinearLayout.LayoutParams(0,dp(34),1));volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean from){
                if(from){broadcastVolume=p/100f;muted=(p==0);applyBroadcastVolume();mute.setText(muted?"Muted":"♬");}
            }
            public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}
        });
        LinearLayout info=horizontal();info.setPadding(dp(12),dp(4),dp(12),dp(5));mainColumn.addView(info);
        statusText=label("Channels ready",11,0xffc2b8aa,false);info.addView(statusText,new LinearLayout.LayoutParams(0,dp(22),1));
        qualityText=label("STANDBY",10,accent,true);info.addView(qualityText);
        // Collections are genuine lists, not mere decoration.
        HorizontalScrollView tabScroll=new HorizontalScrollView(this);tabScroll.setHorizontalScrollBarEnabled(false);
        mainColumn.addView(tabScroll,new LinearLayout.LayoutParams(-1,dp(48)));
        LinearLayout tabs=horizontal();tabs.setPadding(dp(7),dp(3),dp(7),dp(3));tabScroll.addView(tabs);
        String[] labels={"🌎 Worldwide","🇵🇭 Philippines","♥ FAVORITE TV CHANNELS","◷ Recently Watched"};
        modeButtons=new Button[labels.length];
        for(int i=0;i<labels.length;i++){final int selected=i;Button b=(mode==i?goldButton(labels[i]):button(labels[i]));
            modeButtons[i]=b;tabs.addView(b,wrapButton());b.setOnClickListener(v->setMode(selected));}
        LinearLayout filters=horizontal();filters.setPadding(dp(9),dp(4),dp(9),dp(4));mainColumn.addView(filters,new LinearLayout.LayoutParams(-1,dp(50)));
        search=new EditText(this);search.setSingleLine(true);search.setTextColor(Color.WHITE);search.setHintTextColor(0xff999ba4);
        search.setHint("Search TV channels…");search.setTextSize(13);search.setPadding(dp(12),0,dp(12),0);search.setBackground(bg(0xff272832,7));
        filters.addView(search,new LinearLayout.LayoutParams(0,dp(41),1));
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int start,int before,int count){ui.removeCallbacks(filterDelay);ui.postDelayed(filterDelay,180);}
            public void afterTextChanged(Editable s){} });
        categorySpinner=new Spinner(this);categorySpinner.setBackground(bg(0xff2a2b34,7));
        LinearLayout.LayoutParams catLp=new LinearLayout.LayoutParams(dp(138),dp(41));catLp.leftMargin=dp(7);filters.addView(categorySpinner,catLp);
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parent,View view,int pos,long id){
                String chosen=String.valueOf(parent.getItemAtPosition(pos));if(!chosen.equals(category)){category=chosen;applyFilter();}}
            public void onNothingSelected(AdapterView<?> parent){}
        });
        LinearLayout browseHead=horizontal();browseHead.setPadding(dp(12),dp(5),dp(12),dp(4));mainColumn.addView(browseHead);
        TextView browseTitle=label("CHANNEL BROWSER",13,0xffefece6,true);browseHead.addView(browseTitle,new LinearLayout.LayoutParams(0,dp(24),1));
        countText=label("Loading…",10,0xffa9a9b2,false);browseHead.addView(countText);
        list=new ListView(this);list.setBackgroundColor(0xff14151b);list.setDividerHeight(dp(3));
        list.setDivider(new android.graphics.drawable.ColorDrawable(0xff14151b));
        list.setCacheColorHint(Color.TRANSPARENT);mainColumn.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        adapter=new ChannelAdapter();list.setAdapter(adapter);
        list.setOnItemClickListener((parent,v,index,id)->{
            if(index<filtered.size())tune(filtered.get(index));
        });
        LinearLayout footer=horizontal();footer.setPadding(dp(10),dp(2),dp(10),dp(2));footer.setBackgroundColor(PANELS[store.theme()]);
        mainColumn.addView(footer,new LinearLayout.LayoutParams(-1,dp(42)));
        Button themeButton=button("🎨 Theme");footer.addView(themeButton,wrapButton());
        themeSpinner=new Spinner(this);footer.addView(themeSpinner,new LinearLayout.LayoutParams(0,dp(38),1));
        ArrayAdapter<String> th=new ArrayAdapter<>(this,R.layout.dark_spinner_item,THEMES);th.setDropDownViewResource(R.layout.dark_spinner_dropdown);themeSpinner.setAdapter(th);themeSpinner.setSelection(store.theme());
        themeButton.setOnClickListener(v->themeSpinner.performClick());
        themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parent,View view,int pos,long id){
                if(store.theme()!=pos){store.setTheme(pos);accent=ACCENTS[pos];toast("Theme saved — reopening interface");recreate();}
            }
            public void onNothingSelected(AdapterView<?> parent){}
        });
        TextView version=label("v"+VERSION+" ↻",10,0xffa29b93,false);footer.addView(version);
        version.setOnClickListener(v->checkUpdates(true)); // Optional manual refresh retained, moved out of header.
        fullscreenLayer=new FrameLayout(this);
        // Transparent HUD sits ABOVE root-level videoSlot; root provides black backdrop.
        fullscreenLayer.setBackgroundColor(Color.TRANSPARENT);
        fullscreenLayer.setVisibility(View.GONE);
        root.addView(fullscreenLayer,new FrameLayout.LayoutParams(-1,-1));
        gestureView=new FullscreenGestureView(this,new FullscreenGestureView.Listener(){
            @Override public void onSwipeStart(boolean brightnessSide){
                if(brightnessSide)gestureStartBrightness=currentScreenBrightness();
                else if(audioManager!=null){
                    gestureMaxMediaVolume=Math.max(1,audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                    gestureStartMediaVolume=audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                }
            }
            @Override public void onSwipe(boolean brightnessSide,float delta){
                if(brightnessSide)adjustFullscreenBrightness(delta);
                else adjustFullscreenVolume(delta);
            }
            @Override public void onTap(boolean center){
                // Center retains the established pause/play gesture. Tapping anywhere
                // reveals EXIT FULLSCREEN; the button never steals volume/brightness swipes.
                if(center)togglePlaybackPause();
                showFullscreenExitButton();
            }
        });
        fullscreenLayer.addView(gestureView,new FrameLayout.LayoutParams(-1,-1));
        fullscreenIndicator=label("",19,Color.WHITE,true);
        fullscreenIndicator.setGravity(Gravity.CENTER);
        fullscreenIndicator.setPadding(dp(24),dp(14),dp(24),dp(14));
        fullscreenIndicator.setBackground(bg(0xcc1e1d20,15));
        fullscreenIndicator.setClickable(false); // Double tap remains available while HUD is visible.
        fullscreenIndicator.setVisibility(View.GONE);
        FrameLayout.LayoutParams indicatorParams=new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER);
        fullscreenLayer.addView(fullscreenIndicator,indicatorParams);
        // Explicit exit is accessible without relying on the phone's navigation gestures.
        fullscreenExitButton=button("✕  EXIT FULLSCREEN");
        fullscreenExitButton.setTextSize(14);
        fullscreenExitButton.setTextColor(Color.WHITE);
        GradientDrawable exitSurface=bg(0xee25242d,12);
        exitSurface.setStroke(dp(1),0xffffb851);
        fullscreenExitButton.setBackground(exitSurface);
        fullscreenExitButton.setElevation(dp(7));
        fullscreenExitButton.setVisibility(View.GONE);
        fullscreenExitButton.setContentDescription("Exit fullscreen and return to NODE TV portrait mode");
        FrameLayout.LayoutParams exitParams=new FrameLayout.LayoutParams(-2,dp(48),Gravity.TOP|Gravity.END);
        exitParams.setMargins(dp(18),dp(18),dp(18),0);
        fullscreenLayer.addView(fullscreenExitButton,exitParams);
        fullscreenExitButton.setOnClickListener(v->toggleFullscreen());
        createMandatoryOverlay();
        // Handle rotations, update-banner insertion, and window-inset changes using
        // bounds only. Never remove the live TextureView from its parent.
        root.getViewTreeObserver().addOnGlobalLayoutListener(this::layoutVideoSlot);
        // Start with a real channel catalog, then show whatever is available.
        populateCategories();
    }
    private final Runnable filterDelay=this::applyFilter;

    /** Position video surface WITHOUT changing its parent or destroying native output.
     *  Called after root/TV layout and whenever the header/update banner changes size.
     *  The aperture numbers are calibrated to the transparent window in retro_frame.png.
     */
    private void layoutVideoSlot(){
        if(root==null||videoSlot==null)return;
        if(fullscreen){
            int w=root.getWidth()-root.getPaddingLeft()-root.getPaddingRight();
            int h=root.getHeight()-root.getPaddingTop()-root.getPaddingBottom();
            updateVideoSlotBounds(0,0,w,h);
            return;
        }
        if(retroFrame==null||retroFrame.getWidth()==0||retroFrame.getHeight()==0
            ||retroFrame.getVisibility()!=View.VISIBLE)return;
        int[] frame=new int[2], parent=new int[2];
        retroFrame.getLocationOnScreen(frame);
        root.getLocationOnScreen(parent);
        int w=retroFrame.getWidth(),h=retroFrame.getHeight();
        int left=frame[0]-parent[0]-root.getPaddingLeft()+Math.round(w*.127f);
        int top=frame[1]-parent[1]-root.getPaddingTop()+Math.round(h*.173f);
        updateVideoSlotBounds(left,top,Math.round(w*.610f),Math.round(h*.621f));
    }
    private void updateVideoSlotBounds(int left,int top,int width,int height){
        if(width<=0||height<=0)return;
        if(left==lastVideoLeft&&top==lastVideoTop
            &&width==lastVideoWidth&&height==lastVideoHeight)return;
        lastVideoLeft=left;lastVideoTop=top;
        lastVideoWidth=width;lastVideoHeight=height;
        FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(width,height,Gravity.TOP|Gravity.LEFT);
        params.leftMargin=left;params.topMargin=top;
        videoSlot.setLayoutParams(params);
        // Native VLC still needs to learn the new drawable bounds, but never detach it.
        if(usingVlc)videoSlot.post(()->vlc.updateViewport(videoSlot.getWidth(),videoSlot.getHeight()));
    }
    private void setMode(int tab){
        mode=tab;
        if(tab==0||tab==1){selectedRegion=tab;store.setLastRegion(tab);loadRegion(tab==1);}
        else {ui.removeCallbacks(filterDelay);category="All Categories";populateCategories();applyFilter();}
        for(int i=0;i<modeButtons.length;i++){
            modeButtons[i].setBackground(bg(mode==i?accent:0xff30303a,8));
            modeButtons[i].setTextColor(mode==i?0xff171414:0xfff4f0e9);
        }
    }
    private void loadRegion(boolean philippines){
        long generation=++loadGeneration;
        directory=new ArrayList<>();applyFilter();
        setStatus("Loading "+(philippines?"Philippines":"Worldwide")+" channels…");
        io.execute(()->{
            try {
                List<Channel> result=PlaylistRepository.fetch(philippines);
                ui.post(()->{if(generation!=loadGeneration)return;directory=result;category="All Categories";
                    populateCategories();applyFilter();setStatus(result.size()+" channels loaded");});
            } catch(Exception e){ui.post(()->{if(generation!=loadGeneration)return;
                setStatus("Channel directory unavailable. Check your internet and try again.");toast("Could not load channels: "+e.getMessage());});}
        });
    }
    private List<Channel> collection(){return mode==2?store.favorites():mode==3?store.recent():directory;}
    private void populateCategories(){
        if(categorySpinner==null)return;
        Set<String> cats=new LinkedHashSet<>();cats.add("All Categories");
        for(Channel c:collection())cats.add(c.category);
        ArrayList<String> categories=new ArrayList<>(cats);if(categories.size()>1) Collections.sort(categories.subList(1,categories.size()),String.CASE_INSENSITIVE_ORDER);
        ArrayAdapter<String> a=new ArrayAdapter<>(this,R.layout.dark_spinner_item,categories);
        a.setDropDownViewResource(R.layout.dark_spinner_dropdown);categorySpinner.setAdapter(a);
        int desired=categories.indexOf(category);categorySpinner.setSelection(desired<0?0:desired);
    }
    private void applyFilter(){
        if(adapter==null||search==null)return;
        String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);
        filtered.clear();
        for(Channel c:collection()){
            if(!"All Categories".equals(category)&&!c.category.equals(category))continue;
            if(!query.isEmpty() && !(c.title.toLowerCase(Locale.ROOT).contains(query)||c.category.toLowerCase(Locale.ROOT).contains(query)))continue;
            filtered.add(c);
        }
        adapter.notifyDataSetChanged();countText.setText(filtered.size()+" shown");
    }
    private void refreshCollection(){if(mode==2||mode==3){populateCategories();applyFilter();}else adapter.notifyDataSetChanged();}
    private void tune(Channel channel){
        // Stopping old playback before preparing the new channel avoids stale audio/video.
        long generation=++playGeneration;
        cancelTuneTimeout();
        usingVlc=false;activeSignal=false;userPaused=false;
        playing=channel;
        qualityWidth=0;qualityHeight=0;
        nowPlaying.setText(channel.title);
        setStatus("Tuning "+channel.title+"…");
        stopNoise();quality();
        try {
            vlc.stop();vlc.view().setVisibility(View.GONE);
            playerView.setVisibility(View.VISIBLE);
            player.stop();player.clearMediaItems();
            Map<String,String> properties=new HashMap<>();
            if(!channel.referer.isEmpty())properties.put("Referer",channel.referer);
            networkFactory.setDefaultRequestProperties(properties);
            networkFactory.setUserAgent(channel.userAgent.isEmpty()?DEFAULT_STREAM_UA:channel.userAgent);
            String scheme=Uri.parse(channel.url).getScheme();
            if(scheme!=null&&!scheme.equalsIgnoreCase("http")&&!scheme.equalsIgnoreCase("https")){
                tryVlcFallback("Network protocol requires VLC engine");
                hideKeyboard();
                return;
            }
            MediaItem.Builder item=new MediaItem.Builder().setUri(Uri.parse(channel.url));
            String link=channel.url.toLowerCase(Locale.ROOT);
            // IPTV providers commonly put tokens after the extension; use path not suffix-only matching.
            if(link.matches("(?s).*\\.m3u8(?:[?#].*)?$"))item.setMimeType(MimeTypes.APPLICATION_M3U8);
            else if(link.matches("(?s).*\\.mpd(?:[?#].*)?$"))item.setMimeType(MimeTypes.APPLICATION_MPD);
            player.setMediaItem(item.build());player.prepare();player.setPlayWhenReady(true);
            scheduleTuneTimeout(EXO_CONNECT_TIMEOUT_MS,generation,false);
        }catch(Exception ex){
            Log.w("NodeTV","Media3 could not prepare; trying VLC",ex);
            tryVlcFallback("Unable to prepare Android playback engine");
        }
        hideKeyboard();
    }
    private void tryVlcFallback(String reason){
        if(playing==null||usingVlc)return;
        long generation=playGeneration;
        usingVlc=true;activeSignal=false;
        cancelTuneTimeout();
        player.stop();player.clearMediaItems();
        playerView.setVisibility(View.GONE);
        vlc.view().setVisibility(View.VISIBLE);
        setStatus("Tuning with VLC compatibility mode…");
        qualityText.setText("VLC CONNECTING");
        try{
            vlc.setVolume(muted?0:broadcastVolume);
            vlc.play(playing);
            scheduleTuneTimeout(VLC_CONNECT_TIMEOUT_MS,generation,true);
        }catch(Throwable ex){
            Log.e("NodeTV","VLC compatibility engine failed",ex);
            finishNoReception("NO RECEPTION — this stream cannot play on this device");
        }
    }
    private void onPlaybackSucceeded(String engine,long generation){
        if(playing==null||generation!=playGeneration||userPaused)return;
        activeSignal=true;cancelTuneTimeout();stopNoise();
        store.watched(playing);
        setStatus("● LIVE ("+engine+") — "+playing.title);
        quality();
        if(usingVlc&&qualityWidth==0)qualityText.setText("LIVE • VLC");
    }
    private void stopPlaying(){
        ++playGeneration;cancelTuneTimeout();usingVlc=false;activeSignal=false;userPaused=false;
        player.stop();player.clearMediaItems();vlc.stop();vlc.view().setVisibility(View.GONE);
        playerView.setVisibility(View.VISIBLE);
        playing=null;stopNoise();qualityWidth=0;qualityHeight=0;
        qualityText.setText("STANDBY");setStatus("STANDBY");
    }
    private void finishNoReception(String message){
        if(playing==null)return;
        cancelTuneTimeout();activeSignal=false;
        if(usingVlc)vlc.stop();else player.stop();
        setStatus(message);if(noise!=null){noise.showNoise(true);signalLabel.setVisibility(View.VISIBLE);}
        hiss.start();qualityText.setText("NO SIGNAL");
    }
    private void applyBroadcastVolume(){
        float value=muted?0f:broadcastVolume;
        if(player!=null)player.setVolume(value);
        if(vlc!=null)vlc.setVolume(value);
    }
    private void stopNoise(){if(noise!=null){noise.showNoise(false);signalLabel.setVisibility(View.GONE);}hiss.stop();}
    private void quality(){
        if(qualityText==null)return;
        qualityText.setText(qualityWidth>0?qualityWidth+"×"+qualityHeight:(usingVlc?"VLC":"LIVE TV"));
    }
    private void setStatus(String s){if(statusText!=null)statusText.setText(s);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private void hideKeyboard(){((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);search.clearFocus();}

    private void togglePlaybackPause(){
        if(playing==null)return;
        if(!activeSignal){
            if(fullscreen)showFullscreenIndicator("⌛ Connecting to channel…");
            return;
        }
        userPaused=!userPaused;
        if(userPaused){
            cancelTuneTimeout();
            if(usingVlc)vlc.setPaused(true);
            else player.setPlayWhenReady(false);
            setStatus("❚❚ PAUSED — "+playing.title);
            if(fullscreen)showFullscreenIndicator("❚❚  PAUSED");
        }else{
            if(usingVlc)vlc.setPaused(false);
            else player.setPlayWhenReady(true);
            setStatus("▶ RESUMING — "+playing.title);
            if(fullscreen)showFullscreenIndicator("▶  PLAYING");
            // Live channels may resume at the live edge instead of a paused frame.
        }
    }
    private float currentScreenBrightness(){
        float existing=getWindow().getAttributes().screenBrightness;
        if(existing>=0f)return existing;
        try{return Math.max(.02f,Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS)/255f);}
        catch(Exception ex){return .5f;}
    }
    private void adjustFullscreenBrightness(float delta){
        float brightness=Math.max(.02f,Math.min(1f,gestureStartBrightness+delta));
        WindowManager.LayoutParams params=getWindow().getAttributes();
        params.screenBrightness=brightness;
        getWindow().setAttributes(params);
        showFullscreenIndicator("☀  Brightness  "+Math.round(brightness*100)+"%");
    }
    private void adjustFullscreenVolume(float delta){
        if(audioManager==null)return;
        int desired=Math.max(0,Math.min(gestureMaxMediaVolume,
            Math.round(gestureStartMediaVolume+delta*gestureMaxMediaVolume)));
        try{
            if(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)!=desired)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,desired,0);
            showFullscreenIndicator("♫  Media volume  "+Math.round(100f*desired/gestureMaxMediaVolume)+"%");
        }catch(SecurityException ex){showFullscreenIndicator("Volume controlled by device settings");}
    }
    private void showFullscreenIndicator(String content){
        if(!fullscreen||fullscreenIndicator==null)return;
        fullscreenIndicator.setText(content);
        fullscreenIndicator.setVisibility(View.VISIBLE);
        ui.removeCallbacks(hideFullscreenIndicator);
        ui.postDelayed(hideFullscreenIndicator,1000);
    }
    private void showFullscreenExitButton(){
        if(!fullscreen||fullscreenExitButton==null)return;
        fullscreenExitButton.setVisibility(View.VISIBLE);
        ui.removeCallbacks(hideFullscreenExit);
        ui.postDelayed(hideFullscreenExit,4000);
    }
    private void toggleFullscreen(){
        if(!fullscreen){
            if(mandatoryUpdate)return;
            originalWindowBrightness=getWindow().getAttributes().screenBrightness;
            fullscreen=true;
            // Keep mainColumn attached and hide it only visually: videoSlot is a root
            // sibling and will NEVER be removed/reparented while streaming.
            mainColumn.setVisibility(View.GONE);
            root.setBackgroundColor(Color.BLACK);
            fullscreenLayer.setVisibility(View.VISIBLE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            root.requestApplyInsets();
            layoutVideoSlot();
        }else{
            fullscreen=false;
            ui.removeCallbacks(hideFullscreenExit);
            if(fullscreenExitButton!=null)fullscreenExitButton.setVisibility(View.GONE);
            ui.removeCallbacks(hideFullscreenIndicator);
            if(fullscreenIndicator!=null)fullscreenIndicator.setVisibility(View.GONE);
            WindowManager.LayoutParams params=getWindow().getAttributes();
            params.screenBrightness=originalWindowBrightness;
            getWindow().setAttributes(params);
            fullscreenLayer.setVisibility(View.GONE);
            root.setBackgroundColor(BACKGROUNDS[store.theme()]);
            mainColumn.setVisibility(View.VISIBLE);
            getWindow().getDecorView().setSystemUiVisibility(0);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            root.requestApplyInsets();
            // Global layout listener will apply the final portrait aperture after
            // Android's orientation/insets/layout settle, without touching the surface.
            ui.post(this::layoutVideoSlot);
        }
    }
    @Override public void onBackPressed(){if(fullscreen)toggleFullscreen();else if(mandatoryUpdate){
        // Android Back cannot bypass a confirmed mandatory update while app is open.
        toast("NODE TV requires an update to continue.");
    }else super.onBackPressed();}

    private final class ChannelAdapter extends BaseAdapter {
        @Override public int getCount(){return filtered.size();}
        @Override public Object getItem(int pos){return filtered.get(pos);}
        @Override public long getItemId(int pos){return pos;}
        @Override public View getView(int pos,View old,ViewGroup parent){
            LinearLayout row;TextView texts;Button star;
            if(old instanceof LinearLayout){row=(LinearLayout)old;texts=(TextView)row.getChildAt(0);star=(Button)row.getChildAt(1);}
            else {
                row=horizontal();row.setPadding(dp(11),dp(4),dp(8),dp(4));row.setBackground(bg(ROWS[store.theme()],8));
                texts=label("",13,0xffeeeae3,true);texts.setMaxLines(2);texts.setEllipsize(android.text.TextUtils.TruncateAt.END);
                row.addView(texts,new LinearLayout.LayoutParams(0,dp(58),1));
                star=button("♡");row.addView(star,new LinearLayout.LayoutParams(dp(44),dp(44)));
            }
            Channel ch=filtered.get(pos);
            texts.setText(ch.title+"\n"+ch.category+" · "+ch.region);
            // Ensure tapping the title works even though the row includes a clickable favorite button.
            texts.setOnClickListener(v->tune(ch));
            row.setOnClickListener(v->tune(ch));
            star.setText(store.isFavorite(ch)?"♥":"♡");star.setTextColor(store.isFavorite(ch)?accent:0xffd5d0ca);
            star.setOnClickListener(v->{store.toggleFavorite(ch);refreshCollection();});
            return row;
        }
    }

    /** Apply the last verified publisher requirement before the network check finishes.
     * Never invent a required update on the first launch without a verified GitHub Release.
     */
    private void restoreRequiredUpdateGate(){
        String required=store.requiredVersion();
        if(required.isEmpty())return;
        if(UpdateClient.compareVersions(required, VERSION)<=0){
            store.clearRequiredVersion();
            return;
        }
        mandatoryUpdate=true;
        updateTitle.setText("⚠  UPDATE REQUIRED — v"+required.replaceFirst("^[vV]", ""));
        updateSubtitle.setText("Connect to GitHub to download the required NODE TV update.");
        updateLaterButton.setVisibility(View.GONE);
        updateBanner.setVisibility(View.VISIBLE);
        mandatoryDescription.setText("NODE TV v"+required.replaceFirst("^[vV]", "")
            +" is required. Checking the official GitHub release for a verified installer…");
        mandatoryOverlay.setVisibility(View.VISIBLE);
    }
    private void checkUpdates(boolean manual){
        long now=System.currentTimeMillis();
        if(!manual&&now-lastUpdateCheck<10L*60*1000)return;
        lastUpdateCheck=now;
        updateIo.execute(()->{
            try{
                UpdateClient.Release result=UpdateClient.check(VERSION);
                ui.post(()->{
                    availableRelease=result;
                    if(result!=null&&result.mandatory){
                        String cached=store.requiredVersion();
                        if(cached.isEmpty()||UpdateClient.compareVersions(result.version,cached)>0){
                            store.setRequiredVersion(result.version);
                        }
                    }
                    String required=store.requiredVersion();
                    mandatoryUpdate=!required.isEmpty()
                        &&UpdateClient.compareVersions(required,VERSION)>0;
                    if(mandatoryUpdate&&fullscreen)toggleFullscreen();
                    if(mandatoryUpdate&&playing!=null)stopPlaying();
                    if(result!=null){
                        updateTitle.setText(mandatoryUpdate?"⚠  UPDATE REQUIRED — "+result.version
                            :"📺  NEW UPDATE AVAILABLE — "+result.version);
                        updateSubtitle.setText(mandatoryUpdate
                            ?"Install the latest NODE TV to continue watching."
                            :"A newer NODE TV version is available from GitHub.");
                        updateLaterButton.setVisibility(mandatoryUpdate?View.GONE:View.VISIBLE);
                        updateBanner.setVisibility(mandatoryUpdate||System.currentTimeMillis()>store.snoozeUntil()
                            ?View.VISIBLE:View.GONE);
                        mandatoryDescription.setText("NODE TV "+result.version+" is required to continue. "
                            +"Tap UPDATE NOW, then approve the official Android APK installation.");
                    }else if(mandatoryUpdate){
                        updateTitle.setText("⚠  UPDATE REQUIRED — "+required);
                        updateSubtitle.setText("Official APK temporarily unavailable. Try again later.");
                        updateBanner.setVisibility(View.VISIBLE);
                        mandatoryDescription.setText("A previously verified required update ("+required
                            +") is still needed, but a downloadable GitHub Release is currently unavailable. "
                            +"Tap UPDATE NOW to retry or DETAILS to visit GitHub.");
                    }else{
                        updateBanner.setVisibility(View.GONE);
                    }
                    mandatoryOverlay.setVisibility(mandatoryUpdate?View.VISIBLE:View.GONE);
                    if(manual)toast(result==null?"No newer installable release on GitHub":
                        (mandatoryUpdate?"Update required: "+result.version:"Latest version: "+result.version));
                });
            }catch(Exception ex){
                // Retry on return-to-app instead of enforcing a stale 10-minute throttle.
                lastUpdateCheck=0L;
                ui.post(()->{
                    if(mandatoryUpdate){
                        mandatoryOverlay.setVisibility(View.VISIBLE);
                        mandatoryDescription.setText("Cannot contact GitHub. Connect to the internet and "
                            +"tap UPDATE NOW to retry; DETAILS opens official releases.");
                    }
                    if(manual)toast("Update check unavailable. Check GitHub or your connection.");
                });
            }
        });
    }
    private void showReleaseDetails(){
        String page=availableRelease!=null ? availableRelease.releasePage :
            "https://github.com/nodedevfivem/node-tv-android/releases/latest";
        try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(page)));}
        catch(ActivityNotFoundException ex){toast("No browser found to display release details.");}
    }
    private void createMandatoryOverlay(){
        mandatoryOverlay=new FrameLayout(this);
        mandatoryOverlay.setBackgroundColor(0xf5111118);
        mandatoryOverlay.setVisibility(View.GONE);
        root.addView(mandatoryOverlay,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout card=vertical();card.setPadding(dp(20),dp(22),dp(20),dp(20));
        GradientDrawable surface=bg(0xff35261e,16);surface.setStroke(dp(2),0xffffb851);
        card.setBackground(surface);
        FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);
        params.setMargins(dp(15),dp(38),dp(15),0);
        mandatoryOverlay.addView(card,params);
        TextView title=label("⚠  NODE TV — UPDATE REQUIRED",20,0xffffc573,true);
        card.addView(title);
        mandatoryDescription=label("A newer NODE TV release must be installed before watching.",14,Color.WHITE,false);
        mandatoryDescription.setPadding(0,dp(10),0,dp(16));card.addView(mandatoryDescription);
        LinearLayout actions=horizontal();card.addView(actions);
        Button update=goldButton("⬇ UPDATE NOW");update.setBackground(bg(0xffffb851,8));actions.addView(update,wrapButton());
        update.setOnClickListener(v->downloadUpdate());
        Button details=button("DETAILS");actions.addView(details,wrapButton());
        details.setOnClickListener(v->showReleaseDetails());
        TextView info=label("Installation requires your confirmation in Android. No silent installs.",11,0xffdbc8b3,false);
        info.setPadding(0,dp(14),0,0);card.addView(info);
    }
    private void downloadUpdate(){
        if(downloading)return;
        if(availableRelease==null){
            toast("Checking for the official GitHub APK…");
            checkUpdates(true);
            return;
        }
        downloading=true;updateTitle.setText("DOWNLOADING VERIFIED UPDATE…");
        if(mandatoryUpdate)mandatoryDescription.setText("Downloading verified update from GitHub…");
        UpdateClient.Release release=availableRelease;
        updateIo.execute(()->{
            try{
                File file=UpdateClient.downloadVerified(getApplicationContext(),release,(done,total)->{
                    ui.post(()->{
                        int pct=(int)Math.min(100,(done*100/Math.max(1,total)));
                        updateTitle.setText("UPDATE "+release.version+" — "+pct+"%");
                        if(mandatoryUpdate)mandatoryDescription.setText("Downloading verified APK… "+pct+"%  —  Keep NODE TV open.");
                    });
                });
                ui.post(()->{
                    downloading=false;downloadedUpdate=file;
                    if(mandatoryUpdate)mandatoryDescription.setText("Download verified. Approve Android's installation request to complete the update.");
                    startInstallerIfAllowed();
                });
            }catch(Exception ex){ui.post(()->{
                downloading=false;updateTitle.setText("UPDATE FAILED — TRY AGAIN");
                if(mandatoryUpdate)mandatoryDescription.setText("Download failed. Check internet, then tap UPDATE NOW to retry. "
                    +ex.getMessage());
                toast("Update verification/download failed: "+ex.getMessage());
            });}
        });
    }
    private void startInstallerIfAllowed(){
        if(downloadedUpdate==null||!downloadedUpdate.exists())return;
        if(!UpdateClient.canInstall(this)){
            awaitingInstallPermission=true;
            toast("Allow NODE TV to install updates, then return to the app.");
            startActivity(UpdateClient.requestInstallPermission(this));return;
        }
        try{UpdateClient.install(this,downloadedUpdate);}
        catch(ActivityNotFoundException|SecurityException ex){toast("Unable to open Android APK installer: "+ex.getMessage());}
    }
    @Override protected void onResume(){
        super.onResume();
        if(store!=null)checkUpdates(false);
        if(awaitingInstallPermission&&downloadedUpdate!=null){
            awaitingInstallPermission=false;
            if(UpdateClient.canInstall(this))startInstallerIfAllowed();
        }
    }
    @Override protected void onDestroy(){
        ui.removeCallbacksAndMessages(null);hiss.stop();
        if(fullscreen){
            WindowManager.LayoutParams w=getWindow().getAttributes();
            w.screenBrightness=originalWindowBrightness;getWindow().setAttributes(w);
        }
        if(vlc!=null)vlc.release();
        if(player!=null){playerView.setPlayer(null);player.release();}
        io.shutdownNow();updateIo.shutdownNow();super.onDestroy();
    }
}
