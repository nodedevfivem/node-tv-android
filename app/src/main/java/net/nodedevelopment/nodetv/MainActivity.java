package net.nodedevelopment.nodetv;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
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
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@OptIn(markerClass = UnstableApi.class)
public final class MainActivity extends Activity {
    private static final String VERSION="0.1.0";
    private static final String[] THEMES={"Vintage Amber","NODE Electric Blue","Nebula Violet","Forest Night","Crimson Theater","Graphite","Deep Teal","Copper Sunset"};
    private static final int[] ACCENTS={0xfff0ab4e,0xff42caff,0xffc29bff,0xff69c49a,0xffee7677,0xffb8c0d0,0xff4dd7cc,0xffdc884b};
    private static final int[] BACKGROUNDS={0xff101016,0xff0a1320,0xff14101f,0xff0d1915,0xff1c1014,0xff15161a,0xff0b191b,0xff211611};
    private static final int[] PANELS={0xff202027,0xff172436,0xff251c32,0xff1a2820,0xff2c1b20,0xff26282e,0xff172e30,0xff32231c};
    private static final int[] ROWS={0xff24252e,0xff1d3044,0xff2b243e,0xff22392f,0xff37242b,0xff303239,0xff1d383a,0xff403028};
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final StaticAudio hiss=new StaticAudio();
    private UserStore store;
    private ExoPlayer player;
    private PlayerView playerView;
    private RetroNoiseView noise;
    private TextView signalLabel,nowPlaying,statusText,countText,qualityText;
    private FrameLayout root,videoSlot,retroFrame,fullscreenLayer;
    private LinearLayout mainColumn,updateBanner;
    private TextView updateTitle;
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
    private final Runnable timeout=()->{
        if(playing!=null && player.getPlaybackState()!=Player.STATE_READY)noReception("NO RECEPTION — stream timed out");
    };
    private final Runnable repeatUpdateChecks=new Runnable(){
        @Override public void run(){checkUpdates(false);ui.postDelayed(this,6L*60*60*1000);}
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store=new UserStore(this);selectedRegion=store.lastRegion();mode=selectedRegion;accent=ACCENTS[store.theme()];
        getWindow().setStatusBarColor(0xff0c0d11);getWindow().setNavigationBarColor(0xff0c0d11);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        player=new ExoPlayer.Builder(this).build();
        playerView=new PlayerView(this);
        playerView.setPlayer(player);playerView.setUseController(false);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        player.addListener(new Player.Listener(){
            @Override public void onIsPlayingChanged(boolean isPlaying){
                if(isPlaying&&playing!=null){stopNoise();ui.removeCallbacks(timeout);
                    store.watched(playing);setStatus("● LIVE — "+playing.title);quality();}
            }
            @Override public void onPlaybackStateChanged(int state){
                if(state==Player.STATE_BUFFERING&&playing!=null)setStatus("Tuning: "+playing.title);
                else if(state==Player.STATE_ENDED&&playing!=null)noReception("SIGNAL LOST — stream ended");
            }
            @Override public void onPlayerError(PlaybackException error){noReception("NO RECEPTION — channel unavailable");}
            @Override public void onVideoSizeChanged(VideoSize v){qualityWidth=v.width;qualityHeight=v.height;quality();}
        });
        buildUi();loadRegion(selectedRegion==1);
        ui.postDelayed(()->checkUpdates(false),3500);
        ui.postDelayed(repeatUpdateChecks,6L*60*60*1000);
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
        Button updates=button("↻ Updates");header.addView(updates,wrapButton());updates.setOnClickListener(v->checkUpdates(true));
        updateBanner=vertical();updateBanner.setPadding(dp(12),dp(8),dp(12),dp(8));updateBanner.setBackgroundColor(0xff33291d);
        mainColumn.addView(updateBanner,new LinearLayout.LayoutParams(-1,-2));updateBanner.setVisibility(View.GONE);
        updateTitle=label("NEW UPDATE",14,accent,true);updateBanner.addView(updateTitle);
        LinearLayout actionRow=horizontal();updateBanner.addView(actionRow);
        Button now=goldButton("UPDATE NOW");actionRow.addView(now,wrapButton());now.setOnClickListener(v->downloadUpdate());
        Button later=button("LATER");actionRow.addView(later,wrapButton());later.setOnClickListener(v->{store.snooze();updateBanner.setVisibility(View.GONE);});
        Button details=button("DETAILS");actionRow.addView(details,wrapButton());details.setOnClickListener(v->{
            if(availableRelease!=null)startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(availableRelease.releasePage)));
        });

        LinearLayout screenHeader=horizontal();screenHeader.setPadding(dp(12),dp(7),dp(12),dp(5));mainColumn.addView(screenHeader,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout names=vertical();screenHeader.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        names.addView(label("LIVE TELEVISION",10,accent,true));
        nowPlaying=label("Select a channel to watch",15,0xffeeeae4,true);nowPlaying.setMaxLines(1);nowPlaying.setEllipsize(android.text.TextUtils.TruncateAt.END);names.addView(nowPlaying);
        Button full=button("⛶ Fullscreen");screenHeader.addView(full,wrapButton());full.setOnClickListener(v->toggleFullscreen());

        retroFrame=new FrameLayout(this){@Override protected void onMeasure(int w,int h){
            int width=View.MeasureSpec.getSize(w);super.onMeasure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(width*2/3,View.MeasureSpec.EXACTLY));
        } @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);positionSlot(w,h);} };
        retroFrame.setBackgroundColor(0xff1b1c24);
        mainColumn.addView(retroFrame,new LinearLayout.LayoutParams(-1,-2));
        videoSlot=new FrameLayout(this);videoSlot.setBackgroundColor(Color.BLACK);
        retroFrame.addView(videoSlot,new FrameLayout.LayoutParams(dp(200),dp(160)));
        videoSlot.addView(playerView,new FrameLayout.LayoutParams(-1,-1));
        noise=new RetroNoiseView(this);videoSlot.addView(noise,new FrameLayout.LayoutParams(-1,-1));
        signalLabel=label("NO RECEPTION",12,0xfff8e3c2,true);signalLabel.setGravity(Gravity.CENTER);
        signalLabel.setVisibility(View.GONE);videoSlot.addView(signalLabel,new FrameLayout.LayoutParams(-1,-1));
        ImageView wood=new ImageView(this);wood.setImageResource(R.drawable.retro_frame);
        wood.setScaleType(ImageView.ScaleType.FIT_XY);retroFrame.addView(wood,new FrameLayout.LayoutParams(-1,-1));
        wood.setClickable(false);
        LinearLayout controls=horizontal();controls.setPadding(dp(10),dp(4),dp(10),dp(5));controls.setBackgroundColor(PANELS[store.theme()]);
        mainColumn.addView(controls,new LinearLayout.LayoutParams(-1,dp(48)));
        Button play=goldButton("▶ / ❚❚");controls.addView(play,wrapButton());play.setOnClickListener(v->{if(playing!=null)player.setPlayWhenReady(!player.getPlayWhenReady());});
        Button stop=button("■");controls.addView(stop,wrapButton());stop.setOnClickListener(v->stopPlaying());
        Button favorite=button("♡ Favorite");controls.addView(favorite,wrapButton());favorite.setOnClickListener(v->{if(playing!=null){store.toggleFavorite(playing);refreshCollection();toast(store.isFavorite(playing)?"Added to Favorite TV Channels":"Removed from favorites");}});
        Button mute=button("♬");controls.addView(mute,wrapButton());mute.setOnClickListener(v->{muted=!muted;player.setVolume(muted?0:1f);mute.setText(muted?"Muted":"♬");});
        SeekBar volume=new SeekBar(this);volume.setMax(100);volume.setProgress(100);
        controls.addView(volume,new LinearLayout.LayoutParams(0,dp(34),1));volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean from){if(from){player.setVolume(p/100f);muted=(p==0);}}
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
        TextView version=label("v"+VERSION,10,0xffa29b93,false);footer.addView(version);
        fullscreenLayer=new FrameLayout(this);fullscreenLayer.setBackgroundColor(Color.BLACK);fullscreenLayer.setVisibility(View.GONE);
        root.addView(fullscreenLayer,new FrameLayout.LayoutParams(-1,-1));
        // Start with a real channel catalog, then show whatever is available.
        populateCategories();
    }
    private final Runnable filterDelay=this::applyFilter;

    private void positionSlot(int w,int h){
        if(fullscreen||videoSlot==null||videoSlot.getParent()!=retroFrame)return;
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams((int)(w*.610f),(int)(h*.621f));
        p.leftMargin=(int)(w*.127f);p.topMargin=(int)(h*.173f);videoSlot.setLayoutParams(p);
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
        playing=channel;
        qualityWidth=0;qualityHeight=0;quality();
        nowPlaying.setText(channel.title);setStatus("Tuning "+channel.title+"…");stopNoise();
        ui.removeCallbacks(timeout);
        try {
            player.stop();player.clearMediaItems();
            MediaItem item=new MediaItem.Builder().setUri(channel.url).build();
            player.setMediaItem(item);player.prepare();player.setPlayWhenReady(true);
            ui.postDelayed(timeout,25000);
        }catch(Exception e){noReception("NO RECEPTION — cannot open stream");}
        hideKeyboard();
    }
    private void stopPlaying(){ui.removeCallbacks(timeout);player.stop();playing=null;stopNoise();quality();setStatus("STANDBY");}
    private void noReception(String message){ui.removeCallbacks(timeout);setStatus(message);if(noise!=null){noise.showNoise(true);signalLabel.setVisibility(View.VISIBLE);}hiss.start();qualityText.setText("NO SIGNAL");}
    private void stopNoise(){if(noise!=null){noise.showNoise(false);signalLabel.setVisibility(View.GONE);}hiss.stop();}
    private void quality(){if(qualityText==null)return;qualityText.setText(qualityWidth>0?qualityWidth+"×"+qualityHeight:"LIVE TV");}
    private void setStatus(String s){if(statusText!=null)statusText.setText(s);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private void hideKeyboard(){((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);search.clearFocus();}

    private void toggleFullscreen(){
        if(!fullscreen){
            fullscreen=true;
            retroFrame.removeView(videoSlot);
            fullscreenLayer.addView(videoSlot,new FrameLayout.LayoutParams(-1,-1));
            fullscreenLayer.setVisibility(View.VISIBLE);mainColumn.setVisibility(View.GONE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            getWindow().getDecorView().setSystemUiVisibility(5894|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            fullscreenLayer.setOnClickListener(v->toggleFullscreen());root.requestApplyInsets();
        }else{
            fullscreen=false;
            fullscreenLayer.removeView(videoSlot);
            retroFrame.addView(videoSlot,0,new FrameLayout.LayoutParams(dp(200),dp(160)));
            fullscreenLayer.setVisibility(View.GONE);mainColumn.setVisibility(View.VISIBLE);
            getWindow().getDecorView().setSystemUiVisibility(0);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            retroFrame.post(()->positionSlot(retroFrame.getWidth(),retroFrame.getHeight()));root.requestApplyInsets();
        }
    }
    @Override public void onBackPressed(){if(fullscreen)toggleFullscreen();else super.onBackPressed();}

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

    private void checkUpdates(boolean manual){
        long now=System.currentTimeMillis();
        if(!manual&&now-lastUpdateCheck<30L*60*1000)return;
        lastUpdateCheck=now;
        io.execute(()->{
            try{
                UpdateClient.Release result=UpdateClient.check(VERSION);
                ui.post(()->{
                    availableRelease=result;
                    if(result!=null&&System.currentTimeMillis()>store.snoozeUntil()){
                        updateTitle.setText("NEW UPDATE — "+result.version+" AVAILABLE");
                        updateBanner.setVisibility(View.VISIBLE);
                    } else if(manual)toast(result==null?"NODE TV is up to date":"Update postponed. Use Updates to check again.");
                });
            }catch(Exception ex){if(manual)ui.post(()->toast("Update check unavailable. Check GitHub or your connection."));}
        });
    }
    private void downloadUpdate(){
        if(availableRelease==null||downloading)return;
        downloading=true;updateTitle.setText("DOWNLOADING VERIFIED UPDATE…");
        UpdateClient.Release release=availableRelease;
        io.execute(()->{
            try{
                File file=UpdateClient.downloadVerified(getApplicationContext(),release,(done,total)->{
                    ui.post(()->updateTitle.setText("UPDATE "+release.version+" — "+Math.min(100,(done*100/Math.max(1,total)))+"%"));
                });
                ui.post(()->{downloading=false;downloadedUpdate=file;startInstallerIfAllowed();});
            }catch(Exception ex){ui.post(()->{downloading=false;updateTitle.setText("UPDATE FAILED — TRY AGAIN");toast("Update verification/download failed: "+ex.getMessage());});}
        });
    }
    private void startInstallerIfAllowed(){
        if(downloadedUpdate==null||!downloadedUpdate.exists())return;
        if(!UpdateClient.canInstall(this)){
            awaitingInstallPermission=true;
            toast("Allow NODE TV to install updates, then return to the app.");
            startActivity(UpdateClient.requestInstallPermission(this));return;
        }
        try{UpdateClient.install(this,downloadedUpdate);}catch(ActivityNotFoundException ex){toast("No APK installer found on this device.");}
    }
    @Override protected void onResume(){super.onResume();if(awaitingInstallPermission&&downloadedUpdate!=null){awaitingInstallPermission=false;if(UpdateClient.canInstall(this))startInstallerIfAllowed();}}
    @Override protected void onDestroy(){
        ui.removeCallbacksAndMessages(null);hiss.stop();if(player!=null){playerView.setPlayer(null);player.release();}
        io.shutdownNow();super.onDestroy();
    }
}
