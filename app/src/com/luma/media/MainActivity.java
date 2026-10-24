/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.*;
import android.net.Uri;
import android.provider.*;
import android.view.*;
import android.widget.*;
import android.text.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Dependency-free native Android media centre. Foreground media service; no analytics or ads. */
public class MainActivity extends Activity {
    final Handler main = new Handler(Looper.getMainLooper());
    final ExecutorService io = Executors.newFixedThreadPool(2);
    final ArrayList<Item> localMusic = new ArrayList<>(), localVideo = new ArrayList<>(), online = new ArrayList<>(), saved = new ArrayList<>(), shown = new ArrayList<>();
    SharedPreferences prefs;
    ArtworkLoader artwork;
    NativeEq equalizer;
    String eqSettingsState="";
    GridView grid;
    SwipeDeck deck;
    TextView emptyMedia;
    int libraryHeight=400,gridColumns=4;
    boolean settingsOpen;
    boolean settingsFadePending;
    ScrollView settingsScroll;
    LinearLayout settingsPage;
    String settingsAnchor;
    int settingsScrollY,settingsAnchorOffset;
    boolean settingsRestoring;
    boolean setupOpen,returningFromAccessSettings,resetInProgress;
    int setupStep;
    boolean expanded,tvMode,appVisible;
    int priorOrientation=-1;
    DiscView disc;
    AudioReactiveGlow reactiveGlow;
    boolean glowPermissionDialog,glowUnavailable;
    float glowLow,glowMid,glowHigh;
    final Set<DiscView> discViews=Collections.newSetFromMap(new WeakHashMap<DiscView,Boolean>());
    String discUrl="";
    FrameLayout tvStage;
    LinearLayout tvControls;
    TextView tvTitle,tvStatus;
    Button tvPlay;
    Typeface font,setupScript;
    LinearLayout root, body, content, nav, playerBox, videoBox;
    ListView list;
    TextView headline, subtitle, notice, nowTitle, nowDetail, time;
    EditText search;
    Button playButton;
    ImageButton sortButton,favoriteButton;
    Button setupPrimary;
    int deviceAccent;
    boolean accentWaveScheduled;
    final Set<Switch> accentSwitches=Collections.newSetFromMap(new WeakHashMap<Switch,Boolean>());
    final Runnable accentWave=new Runnable(){public void run(){accentWaveScheduled=false;if(destroyed||!appVisible||!"rgb".equals(accentStyle()))return;applyAccentWave();accentWaveScheduled=true;main.postDelayed(this,200);}};
    SeekBar seek;
    View surface,attachedVideoView;
    MediaPlayer attachedVideoPlayer;
    Surface attachedVideoSurface;
    ItemAdapter adapter;
    MediaPlayer player;
    AudioManager audio;

    Item current;
    PlaybackQueue<Item> queue = new PlaybackQueue<>(item->item.url);
    boolean prepared, playing, isVideo, dragging, dark, landscape, showingSaved, destroyed;
    int tab=0, generation=0, bg, pageBottom, normalBg, panel, ink, muted, accent, accentInk, line;
    String query="", country="ph", status="Your library, beautifully simple.", sourceStatus="";
    Runnable pendingSearch;
    final String[] titles={"Music", "Radio", "Videos", "TV"};
    final String[] icons={"♫", "◉", "▶", "▣"};

    static class Item implements MediaOrder.Entry {
        String title, detail, url, kind;
        String art="",artist="",type="";
        long dateAdded,dateModified;
        public String label(){return title;}public String mediaType(){return type;}public long added(){return dateAdded;}public long modified(){return dateModified;}
        Item type(String value){type=value==null?"":value;return this;}
        Item(String t,String d,String u,String k){title=t==null?"Untitled":t;detail=d==null?"":d;url=u==null?"":u;kind=k==null?"music":k;type=kind;}
        Item art(String value){art=value==null?"":value;return this;}
        JSONObject json() throws JSONException {return new JSONObject().put("title",title).put("detail",detail).put("url",url).put("kind",kind).put("art",art).put("artist",artist).put("type",type).put("dateAdded",dateAdded).put("dateModified",dateModified);}
    }
    PlaybackService playback;
    boolean serviceBound,serviceEq,pendingNowPlaying;
    int libraryPosition,libraryOffset;
    final Set<SpinningThumbnailView> movingArt=Collections.newSetFromMap(new WeakHashMap<SpinningThumbnailView,Boolean>());
    final Runnable playbackChanged=()->syncPlayback(false);
    final ServiceConnection playbackConnection=new ServiceConnection(){
        @Override public void onServiceConnected(ComponentName name,IBinder binder){
            if(!serviceBound||!appVisible)return;
            playback=((PlaybackService.LocalBinder)binder).service();
            if(prefs.getBoolean("pendingPlaybackReset",false)){playback.listener=null;playback.stopPlayback();playback.queue.setRepeatMode(PlaybackQueue.OFF);playback.queue.setShuffle(false,null);prefs.edit().remove("pendingPlaybackReset").apply();}
            if(!serviceEq&&equalizer!=null)equalizer.close();
            equalizer=playback.equalizer;serviceEq=true;queue=playback.queue;playback.listener=playbackChanged;syncPlayback(true);
        }
        @Override public void onServiceDisconnected(ComponentName name){playback=null;player=null;prepared=false;playing=false;updatePlayer();}
    };
    void syncPlayback(boolean rebuild){
        if(playback==null||destroyed)return;
        boolean previousVideo=isVideo;
        player=playback.player;current=playback.current;prepared=playback.prepared;playing=playback.playing;isVideo=playback.isVideo;status=playback.status;
        if(pendingNowPlaying){pendingNowPlaying=false;if(current!=null&&!tvMode){expanded=true;settingsOpen=false;rebuild=true;}}
        if(tvMode&&(current==null||!current.kind.equals("tv"))){exitTV();return;}
        if(expanded&&current==null){if(previousVideo){expanded=false;rebuild=true;}else main.postDelayed(()->{if(!destroyed&&current==null&&expanded){expanded=false;render();}},450);}
        if(expanded&&previousVideo!=isVideo)rebuild=true;
        if(settingsOpen&&!eqSettingsState.equals(equalizerUiState()))rebuild=true;
        if(rebuild){render();if(list!=null)list.setSelectionFromTop(libraryPosition,libraryOffset);if(grid!=null)grid.setSelection(libraryPosition);}
        if(playing&&isVideo&&appVisible)getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(player!=null&&isVideo&&appVisible)attachVideoOutput();
        if(player!=null&&isVideo&&prepared)try{fitVideo(player.getVideoWidth(),player.getVideoHeight());}catch(Exception ignored){}
        if(!isVideo)clearVideoOutputCache();updatePlayer();
    }
    boolean playbackReady(){if(playback!=null)return true;toast("Connecting to the player. Please tap again.");return false;}
    void playbackPermission(){if(Build.VERSION.SDK_INT>=33&&!allowed(Manifest.permission.POST_NOTIFICATIONS)&&!prefs.getBoolean("askedPlaybackNotifications",false)){prefs.edit().putBoolean("askedPlaybackNotifications",true).apply();requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},30);}}
    void updateArtworkMotion(){for(SpinningThumbnailView image:movingArt){boolean selected=current!=null&&current.url.equals(image.mediaUrl);image.playback(selected,playing&&appVisible);}}
    void configureArtwork(SpinningThumbnailView image){
        movingArt.add(image);
        image.setOnClickListener(v->{for(int n=0;n<shown.size();n++)if(shown.get(n).url.equals(image.mediaUrl)){if(current==null||!current.url.equals(image.mediaUrl)||(player==null&&!prepared))activateItem(n);return;}});
        image.setOnLongClickListener(v->{for(Item item:shown)if(item.url.equals(image.mediaUrl)){saveToggle(item);return true;}return false;});
        GestureDetector taps=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent event){return true;}
            @Override public boolean onSingleTapConfirmed(MotionEvent event){image.performClick();return true;}
            @Override public boolean onDoubleTap(MotionEvent event){if(current!=null&&current.url.equals(image.mediaUrl))openExpanded();return true;}
            @Override public void onLongPress(MotionEvent event){image.performLongClick();}
        });
        final boolean[] tracking={false};
        image.setOnTouchListener((v,event)->{int action=event.getActionMasked();if(action==MotionEvent.ACTION_DOWN)tracking[0]=current!=null&&current.url.equals(image.mediaUrl);if(!tracking[0])return false;boolean consumed=taps.onTouchEvent(event);if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)tracking[0]=false;return consumed;});
        image.setFocusable(false);image.setFocusableInTouchMode(false);
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        pendingNowPlaying=getIntent().getBooleanExtra("openNowPlaying",false);
        prefs=getSharedPreferences("luma",0);
        boolean upgraded=false;try{android.content.pm.PackageInfo info=getPackageManager().getPackageInfo(getPackageName(),0);upgraded=info.lastUpdateTime-info.firstInstallTime>1000;}catch(Exception ignored){}
        boolean existing=upgraded||!prefs.getAll().isEmpty();
        setupOpen=MediaAccessPolicy.needsSetup(prefs.getBoolean("setupComplete",false),prefs.getBoolean("setupStarted",false),existing);
        if(setupOpen){setupStep=prefs.getInt("setupFlowVersion",0)==3?Math.max(0,Math.min(4,prefs.getInt("setupStep",0))):0;prefs.edit().putBoolean("setupStarted",true).putInt("setupFlowVersion",3).apply();}
        else if(!prefs.contains("setupComplete"))prefs.edit().putBoolean("setupComplete",true).apply();
        retireCustomFolders();
        queue.setRepeatMode(prefs.getInt("repeatMode",PlaybackQueue.OFF));queue.setShuffle(prefs.getBoolean("shuffle",false),null);
        equalizer=new NativeEq(prefs);
        artwork=new ArtworkLoader(this,prefs);
        reactiveGlow=new AudioReactiveGlow(new AudioReactiveGlow.Listener(){public void energy(float low,float mid,float high){glowUnavailable=false;glowLow=AudioGlowMath.clamp(low);glowMid=AudioGlowMath.clamp(mid);glowHigh=AudioGlowMath.clamp(high);for(DiscView view:new ArrayList<>(discViews))if(current!=null&&current.url.equals(view.mediaUrl)&&view.isShown())view.energy(glowLow,glowMid,glowHigh);}public void unavailable(){glowUnavailable=true;clearDiscEnergy();}});
        if(state!=null){tab=state.getInt("tab",0); query=state.getString("query","");country=state.getString("country","ph");settingsOpen=state.getBoolean("settings",false);settingsScrollY=state.getInt("settingsScrollY",0);settingsAnchor=state.getString("settingsAnchor",null);settingsAnchorOffset=state.getInt("settingsAnchorOffset",0);returningFromAccessSettings=state.getBoolean("returningFromAccessSettings",false);}
        font=Typeface.createFromAsset(getAssets(),"Manrope.ttf");
        audio=(AudioManager)getSystemService(AUDIO_SERVICE);
        loadSaved();
        render();
        if(!setupOpen){if(tab==0||tab==2)loadLocal();else loadOnline();}
        main.post(ticker);
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);pendingNowPlaying=intent.getBooleanExtra("openNowPlaying",false);if(playback!=null)syncPlayback(true);}
    @Override public void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);rememberSettingsScroll();b.putInt("settingsScrollY",settingsScrollY);b.putString("settingsAnchor",settingsAnchor);b.putInt("settingsAnchorOffset",settingsAnchorOffset);b.putInt("tab",tab);b.putString("query",query);b.putString("country",country);b.putBoolean("settings",settingsOpen);b.putBoolean("returningFromAccessSettings",returningFromAccessSettings);}
    int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    void colors(){
        dark=(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        landscape=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        SurfacePalette palette=SurfacePalette.of(dark,prefs.getBoolean("oled",false));
        accent=Color.parseColor(dark?"#C6E9B2":"#D8EBCD");accentInk=Color.parseColor("#233D26");
        if(Build.VERSION.SDK_INT>=31){try{int id=getResources().getIdentifier(dark?"system_accent1_200":"system_accent1_100","color","android");if(id!=0)accent=getColor(id);}catch(Exception ignored){}}
        deviceAccent=accent;int cardAccent=AccentStyle.base(accentStyle(),deviceAccent);accent="rgb".equals(accentStyle())?AccentStyle.wave(SystemClock.uptimeMillis()):cardAccent;
        palette=palette.tinted(cardAccent,dark);bg=palette.page;pageBottom=PageGradient.bottom(cardAccent,dark,prefs.getBoolean("oled",false));normalBg=palette.inner;panel=palette.card;ink=palette.text;muted=palette.muted;line=palette.line;
        double lum=.2126*channel(Color.red(accent))+.7152*channel(Color.green(accent))+.0722*channel(Color.blue(accent));accentInk=lum>.179?Color.BLACK:Color.WHITE;
        getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(pageBottom);
        getWindow().getDecorView().setSystemUiVisibility(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }
    double channel(int value){double v=value/255.0;return v<=.04045?v/12.92:Math.pow((v+.055)/1.055,2.4);}
    GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    TextView text(String s,int sp,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(color);v.setFontFeatureSettings("kern");v.setTypeface(font);return v;}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    void padding(View v,int x,int y){v.setPadding(dp(x),dp(y),dp(x),dp(y));}
    Button button(String label,Runnable r){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(12);b.setTypeface(font);b.setTextColor(ink);b.setBackground(round(panel,16));b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setStateListAnimator(null);b.setMinWidth(0);b.setMinimumWidth(0);padding(b,12,1);b.setOnClickListener(v->r.run());return b;}
    ImageButton toolIcon(int kind,String label,Runnable action){ImageButton b=new ImageButton(this);b.setImageDrawable(new FluentIcon(kind,ink));b.setScaleType(ImageView.ScaleType.CENTER_INSIDE);b.setBackground(round(panel,16));padding(b,12,12);b.setContentDescription(label);b.setTooltipText(label);b.setOnClickListener(v->action.run());return b;}
    int selectedIconColor(){double foreground=.2126*channel(Color.red(accent))+.7152*channel(Color.green(accent))+.0722*channel(Color.blue(accent)),background=.2126*channel(Color.red(panel))+.7152*channel(Color.green(panel))+.0722*channel(Color.blue(panel));return (Math.max(foreground,background)+.05)/(Math.min(foreground,background)+.05)>=3?accent:ink;}
    void addWeight(LinearLayout l,View v){l.addView(v,new LinearLayout.LayoutParams(0,-2,1));}
    GradientDrawable mediaSelector(){
        if(dark)return round(normalBg,14);
        int stable=AccentStyle.base(accentStyle(),deviceAccent);
        GradientDrawable selected=round(LightSurfaceStyle.selectorFill(panel,stable,ink,muted),14);
        selected.setStroke(dp(1),LightSurfaceStyle.selectorStroke(stable,ink));return selected;
    }
    GradientDrawable searchBackground(){
        if(dark)return round(panel,18);
        int stable=AccentStyle.base(accentStyle(),deviceAccent);
        GradientDrawable field=round(LightSurfaceStyle.searchFill(stable),18);
        field.setStroke(dp(1),LightSurfaceStyle.searchStroke(stable,ink));return field;
    }
    void applyPageBackground(){
        if(dark&&prefs.getBoolean("oled",false))root.setBackgroundColor(Color.BLACK);
        else root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{bg,pageBottom}));
    }
    void addRemoteHeaderTools(LinearLayout header){
        if(tab!=1&&tab!=3)return;
        ImageButton url=toolIcon(14,tab==1?"Add radio URL":"Add TV URL",()->addUrl());url.setBackgroundColor(Color.TRANSPARENT);url.setStateListAnimator(null);padding(url,14,14);header.addView(url,new LinearLayout.LayoutParams(dp(48),dp(48)));
        if(tab==3){ImageButton region=toolIcon(15,"Region: "+country.toUpperCase(Locale.ROOT),()->chooseCountry());region.setBackgroundColor(Color.TRANSPARENT);region.setStateListAnimator(null);padding(region,14,14);header.addView(region,new LinearLayout.LayoutParams(dp(48),dp(48)));}
    }
    void render(){
        rememberSettingsScroll();settingsScroll=null;settingsPage=null;
        cancelPlayHolds();stopDiscAnimation();accentSwitches.clear();setupPrimary=null;colors();scheduleAccentWave();setTheme(dark?com.luma.media.R.style.DarkAppTheme:com.luma.media.R.style.AppTheme);root=column();applyPageBackground();setContentView(root);movingArt.clear();
        // Android 15 enforces edge-to-edge: apply the actual system bar and keyboard insets.
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);}
            else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });root.requestApplyInsets();
        if(resetInProgress){LinearLayout wait=column();padding(wait,22,24);wait.addView(text("Resetting Tuhkar…",24,ink));wait.addView(text("Only app settings and cache are being cleared. Your media files are safe.",14,muted));root.addView(wait);return;}
        if(setupOpen){renderSetup();return;}
        if(tvMode){renderTV();return;}
        if(expanded&&current!=null){if(isVideo)renderVideoExpanded();else renderExpanded();return;}
        if(settingsOpen){renderSettings();if(!landscape)addBottomNav();return;}
        body=row();body.setGravity(Gravity.TOP);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        if(landscape)addNavRail(body);
        content=column();padding(content,16,landscape?8:12);body.addView(content,new LinearLayout.LayoutParams(0,-1,1));
        LinearLayout brand=row();HeaderWordmark logo=new HeaderWordmark(this);logo.setPadding(0,0,dp(8),0);brand.addView(logo,new LinearLayout.LayoutParams(0,dp(48),1));ImageButton refresh=toolIcon(12,tab==0?"Refresh music library":tab==1?"Refresh radio stations":tab==2?"Refresh video library":"Refresh TV channels",()->refreshSection());refresh.setBackgroundColor(Color.TRANSPARENT);refresh.setStateListAnimator(null);padding(refresh,12,12);LinearLayout.LayoutParams refreshParams=new LinearLayout.LayoutParams(dp(48),dp(48));refreshParams.leftMargin=dp(8);brand.addView(refresh,refreshParams);content.addView(brand);
        headline=text(titles[tab],landscape?27:36,ink);bold(headline);padding(headline,0,4);content.addView(headline);
        subtitle=text(tab==0?"On your device. In your element.":tab==1?"A world of sound, one station away.":tab==2?"Your collection, on the big picture.":"Tune in to something new.",12,muted);content.addView(subtitle);if(landscape)subtitle.setVisibility(View.GONE);
        LinearLayout actions=row();padding(actions,0,8);
        search=new EditText(this);search.setSingleLine(true);search.setTextSize(14);search.setTypeface(font);search.setTextColor(ink);search.setHintTextColor(muted);search.setHint(tab==0||tab==2?"Search your library":"Search stations or channels");search.setBackground(searchBackground());padding(search,14,4);search.setText(query);actions.addView(search,new LinearLayout.LayoutParams(0,dp(46),1));
        content.addView(actions);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){query=s.toString();if(pendingSearch!=null)main.removeCallbacks(pendingSearch);if(tab==1&&!showingSaved){pendingSearch=()->loadOnline();main.postDelayed(pendingSearch,600);}else refreshList();}public void afterTextChanged(Editable e){}});
        notice=text("",11,muted);padding(notice,8,5);if(tab==1||tab==3){notice.setMaxLines(2);notice.setEllipsize(TextUtils.TruncateAt.END);}
        LinearLayout library=column();library.setBackground(round(panel,24));library.setClipToOutline(true);padding(library,8,8);LinearLayout header=row();header.addView(notice,new LinearLayout.LayoutParams(0,-2,1));addRemoteHeaderTools(header);
        ImageButton favorite=new ImageButton(this);favoriteButton=favorite;favorite.setImageDrawable(new FluentIcon(16,showingSaved?selectedIconColor():muted));favorite.setScaleType(ImageView.ScaleType.CENTER_INSIDE);favorite.setBackgroundColor(Color.TRANSPARENT);favorite.setStateListAnimator(null);padding(favorite,12,12);favorite.setSelected(showingSaved);favorite.setContentDescription(showingSaved?"Saved filter on. Show all media":"Show saved media");favorite.setTooltipText(showingSaved?"Show all media":"Show saved media");favorite.setOnClickListener(v->{showingSaved=!showingSaved;render();refreshList();});LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(dp(48),dp(48));header.addView(favorite,fp);
        ImageButton sort=toolIcon(18,"Sort media",()->showSort());sortButton=sort;sort.setBackgroundColor(Color.TRANSPARENT);padding(sort,14,14);sort.setTooltipText("Sort: "+sortNames()[Math.max(0,Math.min(3,prefs.getInt("sortOrder",3)))]);header.addView(sort,new LinearLayout.LayoutParams(dp(48),dp(48)));library.addView(header,new LinearLayout.LayoutParams(-1,-2));content.addView(library,new LinearLayout.LayoutParams(-1,0,1));
        buildLibrary(library);
        playerBox=column();playerBox.setPadding(dp(landscape?12:0),dp(2),dp(landscape?12:0),0);playerBox.setOnClickListener(v->openExpanded());playerBox.setContentDescription((current==null?"Player":current.title)+". "+status+". Tap to open Now Playing.");playerBox.setBackgroundColor(Color.TRANSPARENT);if(landscape){LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(dp(Math.max(180,Math.min(320,getResources().getConfiguration().screenWidthDp*.34f))),-1);body.addView(playerBox,pp);}else{LinearLayout.LayoutParams miniParams=new LinearLayout.LayoutParams(-1,-2);miniParams.setMargins(dp(16),0,dp(16),0);root.addView(playerBox,miniParams);}
        videoBox=column();LinearLayout.LayoutParams videoParams=new LinearLayout.LayoutParams(-1,-2);videoParams.bottomMargin=dp(12);playerBox.addView(videoBox,videoParams);videoBox.setGravity(Gravity.CENTER);videoBox.setBackground(round(Color.BLACK,12));videoBox.setClipToOutline(true);addRoundedPicture(videoBox,dp(landscape?150:Math.min(190,getResources().getConfiguration().screenHeightDp*.24f)));surface.setContentDescription("TV preview. Double-tap for landscape fullscreen.");videoBox.setVisibility(isVideo&&current!=null?View.VISIBLE:View.GONE);
        installInlineVideoGesture();
        videoBox.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(!tvMode&&player!=null&&prepared&&isVideo)try{fitVideo(player.getVideoWidth(),player.getVideoHeight());}catch(Exception ignored){}});
        addPlaybackControls(playerBox,false);
        if(!landscape)addBottomNav();
        refreshList();
    }
    void refreshSection(){if(tab==0||tab==2)loadLocal();else loadOnline();}
    void addBottomNav(){HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);scroll.setFillViewport(true);nav=row();padding(nav,16,3);nav.setMinimumWidth(dp(272));scroll.addView(nav,new FrameLayout.LayoutParams(-1,-2));LinearLayout.LayoutParams navParams=new LinearLayout.LayoutParams(-1,dp(54));navParams.topMargin=dp(settingsOpen?0:8);root.addView(scroll,navParams);buildNav();if(settingsOpen)scroll.post(()->scroll.fullScroll(View.FOCUS_RIGHT));}
    void addNavRail(LinearLayout host){ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);nav=column();padding(nav,10,12);scroll.addView(nav,new ScrollView.LayoutParams(-1,-2));host.addView(scroll,new LinearLayout.LayoutParams(dp(80),-1));buildNav();if(settingsOpen)scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));}
    void openSettings(){if(settingsOpen)return;generation++;if(pendingSearch!=null)main.removeCallbacks(pendingSearch);settingsOpen=true;settingsFadePending=true;expanded=false;render();}
    void buildNav(){for(int i=0;i<5;i++){final int t=i;boolean selected=i==4?settingsOpen:!settingsOpen&&i==tab;String label=i==4?"Settings":i==1?"FM":titles[i];ImageButton b=new ImageButton(this);b.setImageDrawable(new FluentIcon(i,selected?accentInk:muted));b.setScaleType(ImageView.ScaleType.CENTER_INSIDE);b.setBackground(selected&&!landscape?new android.graphics.drawable.InsetDrawable(round(accent,20),0,dp(4),0,dp(4)):round(selected?accent:Color.TRANSPARENT,20));padding(b,landscape?16:12,landscape?14:12);b.setContentDescription(label);b.setTooltipText(label);b.setSelected(selected);b.setOnClickListener(v->{if(t==4)openSettings();else switchTab(t);});if(landscape){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(58));lp.bottomMargin=dp(10);nav.addView(b,lp);}else nav.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));}}
    void switchTab(int t){if(tab==t){if(settingsOpen)closeSettings();return;}settingsOpen=false;generation++;if(pendingSearch!=null)main.removeCallbacks(pendingSearch);tab=t;query="";showingSaved=false;sourceStatus="";online.clear();render();if(t==0||t==2)loadLocal();else loadOnline();}
    int layoutMode(){return Math.max(0,Math.min(3,prefs.getInt("layoutMode",3)));}
    void activateItem(int position){if(position<0||position>=shown.size()||!playbackReady())return;queue.reset(shown,shown.get(position));start(shown.get(position));}
    void buildLibrary(LinearLayout library){
        int mode=layoutMode();list=null;grid=null;deck=null;adapter=new ItemAdapter();
        TextView empty=text(tab==0||tab==2?"Nothing here yet\n\nAllow media access in Settings. Check Music/Movies-only filters if files are missing.":"Nothing here yet\n\nUse Refresh or add a direct stream URL.",13,muted);empty.setGravity(Gravity.CENTER);padding(empty,16,20);
        if(mode==0){deck=new SwipeDeck(this);library.addView(deck,new LinearLayout.LayoutParams(-1,0,1));emptyMedia=empty;library.addView(empty,new LinearLayout.LayoutParams(-1,0,1));}
        else{AbsListView collection;if(mode==3){list=new ListView(this);list.setDividerHeight(dp(1));list.setDivider(new android.graphics.drawable.ColorDrawable(line));list.setCacheColorHint(Color.TRANSPARENT);collection=list;}else{grid=new GridView(this);gridColumns=mode==1?(landscape?6:4):3;grid.setNumColumns(gridColumns);grid.setHorizontalSpacing(dp(4));grid.setVerticalSpacing(dp(4));grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);collection=grid;}collection.setFastScrollEnabled(true);collection.setFastScrollAlwaysVisible(true);collection.setVerticalScrollBarEnabled(false);collection.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);collection.setSelector(mediaSelector());collection.setClipToPadding(false);collection.setAdapter(adapter);library.addView(collection,new LinearLayout.LayoutParams(-1,0,1));library.addView(empty,new LinearLayout.LayoutParams(-1,0,1));collection.setEmptyView(empty);collection.setOnItemClickListener((p,v,pos,id)->activateItem(pos));collection.setOnItemLongClickListener((p,v,pos,id)->{saveToggle(shown.get(pos));return true;});}
        View viewport=mode==0?deck:mode==3?list:grid;viewport.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{int height=b-t;boolean changed=height>0&&height!=libraryHeight;if(height>0)libraryHeight=height;if(grid!=null){int maxColumns=layoutMode()==1?(landscape?6:4):3;int columns=Math.max(1,Math.min(maxColumns,(r-l)/(dp(48)+dp(4))));if(columns!=gridColumns){gridColumns=columns;grid.setNumColumns(columns);changed=true;}}if(changed&&adapter!=null)adapter.notifyDataSetChanged();});
    }
    void refreshList(){
        if(adapter==null)return;shown.clear();List<Item> source=showingSaved?saved:(tab==0?localMusic:tab==2?localVideo:online);String q=query.toLowerCase(Locale.ROOT).trim();String kind=tab==0?"music":tab==1?"radio":tab==2?"video":"tv";for(Item i:source){if(showingSaved&&!i.kind.equals(kind))continue;if((i.title+" "+i.detail).toLowerCase(Locale.ROOT).contains(q))shown.add(i);}Collections.sort(shown,MediaOrder.comparator(prefs.getInt("sortOrder",3)));updateLibraryNotice();adapter.notifyDataSetChanged();
        if(deck!=null){boolean has=!shown.isEmpty();deck.setVisibility(has?View.VISIBLE:View.GONE);if(emptyMedia!=null)emptyMedia.setVisibility(has?View.GONE:View.VISIBLE);deck.bind(shown.size(),new SwipeDeck.Factory(){public View create(int index){return swipeCard(shown.get(index),index);}public void activate(int index){activateItem(index);}public void save(int index){if(index>=0&&index<shown.size())saveToggle(shown.get(index));}public void swipe(int index){swipeSelect(index);}});if(current!=null)for(int i=0;i<shown.size();i++)if(shown.get(i).url.equals(current.url)){deck.align(i);break;}}
    }
    View swipeCard(Item item,int index){
        LinearLayout card=column();padding(card,12,8);FrameLayout art=new FrameLayout(this);card.addView(art,new LinearLayout.LayoutParams(-1,0,1));ImageView image=(item.kind.equals("music")||item.kind.equals("radio"))?new DiscView(this):new ThumbnailView(this);if(image instanceof DiscView){((DiscView)image).setHoleColor(panel);configureDisc((DiscView)image,item);}else image.setBackground(round(normalBg,999));art.addView(image,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));art.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{int size=Math.max(1,Math.min(r-l,b-t));android.view.ViewGroup.LayoutParams measured=image.getLayoutParams();if(measured.width!=size||measured.height!=size)image.setLayoutParams(new FrameLayout.LayoutParams(size,size,Gravity.CENTER));});artwork.bind(image,item,accent);
        TextView title=text(item.title,16,ink);semibold(title);title.setGravity(Gravity.CENTER);title.setMaxLines(1);title.setEllipsize(TextUtils.TruncateAt.END);card.addView(title);TextView detail=text(item.detail,11,muted);detail.setGravity(Gravity.CENTER);detail.setMaxLines(1);detail.setEllipsize(TextUtils.TruncateAt.END);card.addView(detail);TextView hint=text((index+1)+" / "+shown.size()+" · swipe: next / previous · tap to play",10,muted);hint.setGravity(Gravity.CENTER);padding(hint,0,5);card.addView(hint);return card;
    }
    static class MediaRow {ImageView image;TextView title,detail,saved;}
    class ItemAdapter extends BaseAdapter {
        final Set<String> savedKeys=new HashSet<>();
        @Override public void notifyDataSetChanged(){savedKeys.clear();for(Item item:saved)savedKeys.add(item.kind+"\n"+item.url);super.notifyDataSetChanged();}
        public int getCount(){return shown.size();}public Object getItem(int p){return shown.get(p);}public long getItemId(int p){return p;}
        public View getView(int position,View convert,android.view.ViewGroup parent){
            int mode=layoutMode();boolean tiles=mode==1||mode==2;int rows=mode==1?Math.max(1,24/gridColumns):mode==2?Math.max(1,9/gridColumns):10;float scale=getResources().getDisplayMetrics().scaledDensity;int minimum=mode==2?Math.max(dp(84),(int)(32*scale)+dp(48)):mode==1?Math.max(dp(48),(int)(14*scale)+dp(32)):Math.max(dp(48),(int)(30*scale)+dp(12));int cellHeight=Math.max(minimum,Math.max(1,(libraryHeight-(rows-1)*dp(tiles?4:1))/rows));LinearLayout r;MediaRow holder;
            if(convert==null){r=tiles?column():row();padding(r,tiles?4:8,tiles?4:6);holder=new MediaRow();holder.image=new SpinningThumbnailView(MainActivity.this);configureArtwork((SpinningThumbnailView)holder.image);holder.image.setScaleType(ImageView.ScaleType.CENTER_CROP);holder.image.setBackground(round(normalBg,999));holder.image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);holder.title=text("",mode==1?10:tiles?13:14,ink);semibold(holder.title);holder.title.setMaxLines(1);holder.title.setEllipsize(TextUtils.TruncateAt.END);holder.detail=text("",10,muted);holder.detail.setMaxLines(1);holder.detail.setEllipsize(TextUtils.TruncateAt.END);holder.saved=text("",13,muted);
                if(tiles){FrameLayout well=new FrameLayout(MainActivity.this);well.addView(holder.image,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));final ImageView circle=holder.image;well.addOnLayoutChangeListener((v,l,t,right,bottom,ol,ot,or,ob)->{int side=Math.max(1,Math.min(right-l,bottom-t));android.view.ViewGroup.LayoutParams size=circle.getLayoutParams();if(size.width!=side||size.height!=side)circle.setLayoutParams(new FrameLayout.LayoutParams(side,side,Gravity.CENTER));});r.addView(well,new LinearLayout.LayoutParams(-1,0,1));holder.title.setGravity(Gravity.CENTER);r.addView(holder.title);if(mode==2){holder.detail.setGravity(Gravity.CENTER);r.addView(holder.detail);}}
                else{r.addView(holder.image,new LinearLayout.LayoutParams(dp(44),dp(44)));LinearLayout words=column();padding(words,10,0);words.addView(holder.title);words.addView(holder.detail);r.addView(words,new LinearLayout.LayoutParams(0,-2,1));r.addView(holder.saved);}r.setTag(holder);
            }else{r=(LinearLayout)convert;holder=(MediaRow)r.getTag();}
            android.view.ViewGroup.LayoutParams rowSize=r.getLayoutParams();if(rowSize==null||rowSize.width!=-1||rowSize.height!=cellHeight)r.setLayoutParams(new AbsListView.LayoutParams(-1,cellHeight));Item item=shown.get(position);if(!tiles){int artSize=Math.min(dp(62),cellHeight-dp(12));android.view.ViewGroup.LayoutParams imageSize=holder.image.getLayoutParams();if(imageSize.width!=artSize||imageSize.height!=artSize)holder.image.setLayoutParams(new LinearLayout.LayoutParams(artSize,artSize));}boolean savedItem=savedKeys.contains(item.kind+"\n"+item.url);String title=(tiles&&savedItem?"♥ ":"")+item.title;if(!android.text.TextUtils.equals(holder.title.getText(),title))holder.title.setText(title);if(!android.text.TextUtils.equals(holder.detail.getText(),item.detail))holder.detail.setText(item.detail);String heart=savedItem?"♥":"";if(!android.text.TextUtils.equals(holder.saved.getText(),heart))holder.saved.setText(heart);((SpinningThumbnailView)holder.image).bindUrl(item.url);artwork.bind(holder.image,item,muted);((SpinningThumbnailView)holder.image).playback(current!=null&&current.url.equals(item.url),playing&&appVisible);r.setContentDescription(item.title+", "+item.detail+". Tap to play. Hold to save or remove.");return r;
        }
    }
    String extension(String filename,String fallback){if(filename!=null){int dot=filename.lastIndexOf('.');if(dot>=0&&dot<filename.length()-1)return filename.substring(dot+1).toLowerCase(Locale.ROOT);}return fallback==null?"":fallback;}
    String[] sortNames(){return new String[]{"Type","Date added","Date modified","Alphabetical"};}
    void showSort(){dialog("Sort media").setSingleChoiceItems(sortNames(),Math.max(0,Math.min(3,prefs.getInt("sortOrder",3))),(d,w)->{prefs.edit().putInt("sortOrder",w).apply();d.dismiss();if(sortButton!=null){sortButton.setTooltipText("Sort: "+sortNames()[w]);sortButton.setContentDescription("Sort media: "+sortNames()[w]);}refreshList();}).setNegativeButton("Cancel",null).show();}
    void librarySettings(LinearLayout page){
        LinearLayout styles=settingsCard(page,"Media layout");String[] layouts={"Disc swipe","Grid small · up to 24","Grid big · up to 9","List view · up to 10"};
        styles.addView(text("Tap a preview to choose. Applies to Music, FM, Videos and TV.",11,muted));
        for(int rowIndex=0;rowIndex<2;rowIndex++){
            LinearLayout choices=row();LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,-2);rowParams.topMargin=dp(10);styles.addView(choices,rowParams);
            for(int col=0;col<2;col++){
                final int mode=rowIndex*2+col;boolean selected=mode==layoutMode();LinearLayout option=column();padding(option,10,10);
                GradientDrawable background=round(selected?SurfacePalette.mix(panel,accent,.14f):normalBg,16);background.setStroke(dp(selected?2:1),selected?accent:SurfacePalette.mix(panel,muted,.25f));option.setBackground(background);
                option.setClickable(true);option.setFocusable(true);option.setSelected(selected);option.setContentDescription(layouts[mode]+(selected?", selected":", tap to select"));option.setTooltipText(layouts[mode]);
                option.setOnClickListener(v->{if(layoutMode()!=mode){prefs.edit().putInt("layoutMode",mode).apply();render();}});
                LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1);if(col==0)params.rightMargin=dp(8);choices.addView(option,params);
                LayoutPreviewView preview=new LayoutPreviewView(this,mode,accent,muted);preview.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);option.addView(preview,new LinearLayout.LayoutParams(-1,dp(92)));
                TextView label=text((selected?"✓ ":"")+layouts[mode],12,ink);label.setMinHeight(dp(40));label.setGravity(Gravity.CENTER_VERTICAL);label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);if(selected)bold(label);option.addView(label);
            }
        }
        styles.addView(text("Previews are illustrative. Larger text and smaller screens fit fewer items.",11,muted));
    }
    String equalizerUiState(){return prepared+":"+equalizer.enabled()+":"+java.util.Arrays.hashCode(equalizer.frequencies)+":"+equalizer.min+":"+equalizer.max+":"+(equalizer.effect!=null)+":"+equalizer.error;}
    void equalizerSettings(LinearLayout page){
        LinearLayout card=settingsCard(page,"Equalizer");Switch enabled=new Switch(this);enabled.setText("Enable equalizer");enabled.setTextColor(ink);enabled.setTypeface(font);enabled.setChecked(equalizer.enabled());tintSettingsSwitch(enabled);enabled.setOnCheckedChangeListener((b,on)->{prefs.edit().putBoolean("eqEnabled",on).apply();if(player!=null&&prepared)equalizer.attach(player.getAudioSessionId());else if(!on)equalizer.close();render();});card.addView(enabled);
        eqSettingsState=equalizerUiState();
        Button presetSelect=button(NativeEq.PRESETS[equalizer.preset()]+" ▾",()->dialog("Sound presets").setSingleChoiceItems(NativeEq.PRESETS,equalizer.preset(),(d,w)->{equalizer.setPreset(w);d.dismiss();render();}).setNegativeButton("Cancel",null).show());card.addView(presetSelect);
        if(!prepared||player==null)card.addView(text("Choose your preset now. Start a song or station to detect this phone's EQ bands and apply your settings.",12,muted));
        else if(!equalizer.error.isEmpty()){
            card.addView(text(equalizer.error,12,muted));card.addView(button("Reconnect equalizer",()->{if(player!=null&&prepared){equalizer.attach(player.getAudioSessionId());render();}}));
        }else card.addView(text(equalizer.enabled()?"Equalizer connected to Tuhkar playback.":"Equalizer is off. Your preset will apply when enabled.",12,muted));
        if(equalizer.frequencies==null)return;
        int bands=equalizer.frequencies.length;card.addView(text(bands+" EQ bands on this phone",12,muted));
        for(int band=0;band<bands;band++){final int b=band;float hz=equalizer.frequencies[band];String frequency=hz>=1000?String.format(Locale.ROOT,"%.1f kHz",hz/1000):Math.round(hz)+" Hz";TextView value=text(frequency+" · "+String.format(Locale.ROOT,"%+.1f dB",equalizer.level(b)),12,ink);card.addView(value);SeekBar slider=new SeekBar(this);slider.setMax(equalizer.max-equalizer.min);slider.setProgress(Math.round(equalizer.level(b))-equalizer.min);slider.setContentDescription(frequency+" equalizer gain");slider.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));slider.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));card.addView(slider,new LinearLayout.LayoutParams(-1,dp(44)));slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar v){}public void onStopTrackingTouch(SeekBar v){}public void onProgressChanged(SeekBar v,int progress,boolean user){if(user){float gain=progress+equalizer.min;equalizer.setCustom(b,gain);presetSelect.setText("Custom ▾");value.setText(frequency+" · "+String.format(Locale.ROOT,"%+.1f dB",gain));}}});}
        card.addView(text("Try a preset, then adjust the sliders. Keep boosts low to avoid distortion.",11,muted));
    }

    void notice(String s){sourceStatus=s;updateLibraryNotice();}
    void updateLibraryNotice(){if(notice==null)return;int total=0;String kind=tab==0?"music":tab==1?"radio":tab==2?"video":"tv";List<Item> source=showingSaved?saved:tab==0?localMusic:tab==2?localVideo:online;for(Item item:source)if(!showingSaved||kind.equals(item.kind))total++;String summary=LibrarySummary.format(shown.size(),total,tab,sourceStatus);notice.setText(summary);notice.setTooltipText(summary);}

    boolean allowed(String permission){return checkSelfPermission(permission)==PackageManager.PERMISSION_GRANTED;}
    boolean musicAccess(){return Build.VERSION.SDK_INT>=33?allowed(Manifest.permission.READ_MEDIA_AUDIO):allowed(Manifest.permission.READ_EXTERNAL_STORAGE);}
    boolean videoAccess(){return Build.VERSION.SDK_INT>=33?allowed(Manifest.permission.READ_MEDIA_VIDEO):allowed(Manifest.permission.READ_EXTERNAL_STORAGE);}
    boolean selectedVideoAccess(){return Build.VERSION.SDK_INT>=34&&allowed(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);}
    String mediaAccessStatus(){return "Music: "+MediaAccessPolicy.status(musicAccess(),false)+"\nVideos: "+MediaAccessPolicy.status(videoAccess(),selectedVideoAccess());}
    void openAccessSettings(){returningFromAccessSettings=true;startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}
    void requestMedia(){
        ArrayList<String> missing=new ArrayList<>();
        if(Build.VERSION.SDK_INT>=33){if(!musicAccess())missing.add(Manifest.permission.READ_MEDIA_AUDIO);if(!videoAccess())missing.add(Manifest.permission.READ_MEDIA_VIDEO);}
        else if(!musicAccess())missing.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        if(missing.isEmpty()){openAccessSettings();return;}
        ArrayList<String> requestable=new ArrayList<>();
        for(String permission:missing)if(!prefs.getBoolean("asked:"+permission,false)||shouldShowRequestPermissionRationale(permission))requestable.add(permission);
        // Selected-video access is a valid grant, and Android 14+ lets users expand that selection.
        if(Build.VERSION.SDK_INT>=34&&selectedVideoAccess()&&!videoAccess()&&!requestable.contains(Manifest.permission.READ_MEDIA_VIDEO))requestable.add(Manifest.permission.READ_MEDIA_VIDEO);
        if(requestable.isEmpty()){dialog("Media access").setMessage("Android is no longer showing the permission prompt. Open App settings → Permissions to change music and video access. Your files are never changed by granting or denying read access.").setPositiveButton("Open app settings",(d,w)->openAccessSettings()).setNegativeButton("Not now",null).show();return;}
        if(Build.VERSION.SDK_INT>=34&&requestable.contains(Manifest.permission.READ_MEDIA_VIDEO))requestable.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
        for(String permission:requestable)prefs.edit().putBoolean("asked:"+permission,true).apply();
        requestPermissions(requestable.toArray(new String[0]),10);
    }
    void refreshAccess(){localMusic.clear();localVideo.clear();generation++;if(setupOpen||settingsOpen)render();else if(tab==0||tab==2)loadLocal();}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==50){if(setupOpen&&setupStep==2)setupPermissionResult();return;}if(r==10)refreshAccess();else if(r==30&&setupOpen)render();else if(r==40){glowUnavailable=false;if(settingsOpen||setupOpen)render();else syncDisc();}}
    @Override protected void onResume(){super.onResume();if(returningFromAccessSettings){returningFromAccessSettings=false;if(setupOpen&&setupStep==3)setupPermissionResult();else refreshAccess();}}
    void loadLocal(){
        final int token=++generation;final int target=tab;
        boolean permission=target==0?musicAccess():(videoAccess()||selectedVideoAccess());
        final boolean standardOnly=standardFolderOnly(target);
        if(!permission){notice("Open Settings to allow music and video access. No files are uploaded.");refreshList();return;}
        notice("Scanning media…");io.execute(()->{
            ArrayList<Item> result=new ArrayList<>();String warning="";
            if(permission)try{
                Uri collection=target==0?MediaStore.Audio.Media.EXTERNAL_CONTENT_URI:MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                String[] cols=target==0?new String[]{"_id","_display_name","title","duration","date_added","date_modified","mime_type","artist","album","album_id"}:new String[]{"_id","_display_name","title","duration","date_added","date_modified","mime_type"};
                int pathColumn=cols.length;cols=Arrays.copyOf(cols,cols.length+1);cols[pathColumn]=Build.VERSION.SDK_INT>=29?MediaStore.MediaColumns.RELATIVE_PATH:MediaStore.MediaColumns.DATA;
                try(Cursor c=getContentResolver().query(collection,cols,null,null,"title COLLATE NOCASE ASC")){
                    if(c!=null)while(c.moveToNext()&&result.size()<5000){if(standardOnly&&!(Build.VERSION.SDK_INT>=29?StandardFolderPolicy.relative(c.getString(pathColumn),target):StandardFolderPolicy.absolute(c.getString(pathColumn),target)))continue;String title=c.getString(2);if(title==null||title.isEmpty())title=c.getString(1);long duration=c.getLong(3);Item item=new Item(title,"On device · "+format(duration),ContentUris.withAppendedId(collection,c.getLong(0)).toString(),target==0?"music":"video");if(target==0){item.artist=c.getString(7)==null?"":c.getString(7);item.detail=item.artist+" · "+c.getString(8)+" · "+format(duration);item.art="content://media/external/audio/albumart/"+c.getLong(9);}item.dateAdded=c.getLong(4)*1000L;item.dateModified=c.getLong(5)*1000L;item.type=extension(c.getString(1),c.getString(6));result.add(item);}
                }
            }catch(Exception e){warning="Some media could not be read. Review media access in Settings.";}
            final String w=warning;main.post(()->{if(destroyed||token!=generation)return;ArrayList<Item> dest=target==0?localMusic:localVideo;dest.clear();dest.addAll(result);notice(result.size()+" items · hold to save"+(standardOnly?(target==0?" · Music folder only":" · Movies folder only"):"")+(result.size()>=5000?" · 5,000-item scan limit":"")+(w.isEmpty()?"":" · "+w));refreshList();});
        });
    }
    String get(String url,int maxBytes)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(15000);c.setRequestProperty("User-Agent","Tuhkar/1.35 (Android)");
        try{int code=c.getResponseCode();if(code!=200)throw new IOException("Server returned "+code);try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n,total=0;while((n=in.read(buf))!=-1){total+=n;if(total>maxBytes)throw new IOException("Directory exceeds safety limit");out.write(buf,0,n);}return out.toString("UTF-8");}}finally{c.disconnect();}
    }
    void loadOnline(){
        final int token=++generation,target=tab;final String q=query, region=country;
        if(target!=1&&target!=3)return;notice("Connecting to public directory…");
        io.execute(()->{ArrayList<Item> result=new ArrayList<>();String error=null;
            try{
                if(target==1){String path=q.trim().isEmpty()?"/json/stations/topclick/100":"/json/stations/search?limit=150&hidebroken=true&order=clickcount&reverse=true&name="+URLEncoder.encode(q.trim(),"UTF-8");String json=null;Exception last=null;
                    for(String host:new String[]{"https://de1.api.radio-browser.info","https://nl1.api.radio-browser.info","https://at1.api.radio-browser.info"}){try{json=get(host+path,2500000);break;}catch(Exception e){last=e;}}
                    if(json==null)throw last;JSONArray a=new JSONArray(json);for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);String u=o.optString("url_resolved",o.optString("url"));if(validUrl(u))result.add(new Item(o.optString("name","Radio"),o.optString("country")+" · "+o.optString("codec")+" · "+o.optInt("bitrate")+" kbps",u,"radio").art(o.optString("favicon","")).type(o.optString("codec","radio")));}
                }else {
                    File cache=new File(getCacheDir(),"tv-"+region+".m3u");String m3u;
                    if(cache.exists()&&System.currentTimeMillis()-cache.lastModified()<6*60*60*1000L){try(FileInputStream f=new FileInputStream(cache);ByteArrayOutputStream b=new ByteArrayOutputStream()){byte[] chunk=new byte[8192];int n;while((n=f.read(chunk))!=-1)b.write(chunk,0,n);m3u=b.toString("UTF-8");}}
                    else{m3u=get("https://iptv-org.github.io/iptv/countries/"+region+".m3u",6000000);try(FileOutputStream f=new FileOutputStream(cache)){f.write(m3u.getBytes("UTF-8"));}}
                    String name="Channel",logoUrl="",detail="Public IPTV · "+region.toUpperCase(Locale.ROOT);try(BufferedReader reader=new BufferedReader(new StringReader(m3u))){String s;while((s=reader.readLine())!=null&&result.size()<3000){s=s.trim();if(s.startsWith("#EXTINF:")){int comma=s.lastIndexOf(',');name=comma>=0?s.substring(comma+1).trim():"Channel";java.util.regex.Matcher logoMatch=java.util.regex.Pattern.compile("tvg-logo=\"([^\"]*)\"").matcher(s);logoUrl=logoMatch.find()?logoMatch.group(1):"";}else if(validUrl(s))result.add(new Item(name,detail,s,"tv").art(logoUrl));}}
                }
            }catch(Exception e){error="Directory unavailable. Check internet or try Refresh. Saved URLs remain available.";}
            final String err=error;main.post(()->{if(destroyed||token!=generation)return;online.clear();online.addAll(result);notice(err!=null?err:(target==1?result.size()+" stations · search worldwide · Radio Browser":result.size()+" channels · "+region.toUpperCase(Locale.ROOT)+" · iptv-org")+" · availability varies");refreshList();});
        });
    }
    boolean validUrl(String u){return u!=null&&(u.startsWith("https://")||u.startsWith("http://"));}
    void chooseCountry(){
        String[] names={"Philippines","United States","United Kingdom","Canada","Australia","Germany","France","Spain","Italy","India","Japan","South Korea","Indonesia","Malaysia","Singapore","Thailand","Vietnam","Brazil","Mexico","South Africa","Other country code…"};
        String[] codes={"ph","us","uk","ca","au","de","fr","es","it","in","jp","kr","id","my","sg","th","vn","br","mx","za"};
        dialog("Public TV directory · region").setItems(names,(d,w)->{if(w<codes.length){country=codes[w];render();loadOnline();}else{EditText input=new EditText(this);input.setHint("Two-letter code, e.g. nz");dialog("Country code").setView(input).setPositiveButton("Load",(dd,ww)->{String code=input.getText().toString().trim().toLowerCase(Locale.ROOT);if(code.matches("[a-z]{2}")){country=code;render();loadOnline();}else toast("Enter two letters.");}).setNegativeButton("Cancel",null).show();}}).show();
    }
    void addUrl(){
        LinearLayout form=column();padding(form,22,8);EditText name=new EditText(this);name.setHint("Station or channel name");name.setTextColor(ink);name.setHintTextColor(muted);EditText url=new EditText(this);url.setHint("https://… direct audio or HLS URL");url.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);url.setTextColor(ink);url.setHintTextColor(muted);form.setBackgroundColor(normalBg);form.addView(name);form.addView(url);TextView help=text("Use a direct media URL, not a website, YouTube page, or playlist directory. Radio URLs are saved under Radio; video URLs under TV.",12,muted);form.addView(help);
        final String kind=(tab==0||tab==1)?"radio":"tv";
        AlertDialog dialog=dialog("Add "+(kind.equals("radio")?"radio":"TV")+" stream").setView(form).setPositiveButton("Save",null).setNegativeButton("Cancel",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String u=url.getText().toString().trim(),n=name.getText().toString().trim();if(!validUrl(u)||Uri.parse(u).getHost()==null){url.setError("Enter a valid http or https media URL");return;}Item item=new Item(n.isEmpty()?Uri.parse(u).getHost():n,"Custom stream",u,kind);if(!isSaved(item)){if(saved.size()>=1000){toast("Saved-item limit reached (1,000)");return;}saved.add(item);}persistSaved();dialog.dismiss();showingSaved=true;if(kind.equals("radio")&&tab!=1){switchTab(1);showingSaved=true;}else if(kind.equals("tv")&&tab!=3){switchTab(3);showingSaved=true;}render();refreshList();}));dialog.show();
    }
    void loadSaved(){try{JSONArray a=new JSONArray(prefs.getString("saved","[]"));for(int n=0;n<a.length();n++){JSONObject o=a.getJSONObject(n);Item item=new Item(o.getString("title"),o.getString("detail"),o.getString("url"),o.getString("kind")).art(o.optString("art",""));item.artist=o.optString("artist","");item.type=o.optString("type",item.kind);item.dateAdded=o.optLong("dateAdded",0);item.dateModified=o.optLong("dateModified",0);saved.add(item);}}catch(Exception ignored){}}
    boolean isSaved(Item i){for(Item s:saved)if(s.url.equals(i.url)&&s.kind.equals(i.kind))return true;return false;}
    void persistSaved(){try{JSONArray a=new JSONArray();for(Item i:saved)a.put(i.json());prefs.edit().putString("saved",a.toString()).apply();}catch(Exception ignored){}}
    void saveToggle(Item i){if(isSaved(i)){for(int n=saved.size()-1;n>=0;n--)if(saved.get(n).url.equals(i.url)&&saved.get(n).kind.equals(i.kind))saved.remove(n);toast("Removed from saved");}else{if(saved.size()>=1000){toast("Saved-item limit reached (1,000)");return;}saved.add(i);toast("Saved for quick access");}persistSaved();refreshList();}
    AlertDialog.Builder dialog(String title){TextView heading=text(title,20,ink);bold(heading);padding(heading,24,18);return new AlertDialog.Builder(this).setCustomTitle(heading);}
    void bold(TextView view){view.setTypeface(font);view.setFontVariationSettings("'wght' 700");}
    void semibold(TextView view){view.setTypeface(font);view.setFontVariationSettings("'wght' 600");}
    void configureDisc(DiscView view,Item item){view.setAccent(accentStyle(),deviceAccent);view.mediaUrl=item.url;view.setReactiveGlow(prefs.getBoolean("reactiveDiscGlow",true));discViews.add(view);view.playback(current!=null&&current.url.equals(item.url),playing,appVisible);}
    void clearDiscEnergy(){glowLow=glowMid=glowHigh=0;for(DiscView view:new ArrayList<>(discViews))view.energy(0,0,0);}
    void stopDiscAnimation(){for(DiscView view:new ArrayList<>(discViews))view.playback(false,false,false);discViews.clear();disc=null;if(reactiveGlow!=null)reactiveGlow.stop();clearDiscEnergy();}
    void requestGlowPermission(){
        if(allowed(Manifest.permission.RECORD_AUDIO))return;
        if(prefs.getBoolean("askedGlowPermission",false)&&!shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)){openAccessSettings();return;}
        if(glowPermissionDialog)return;glowPermissionDialog=true;
        AlertDialog prompt=dialog("Audio-reactive disc glow").setMessage("Android calls this permission ‘Record audio’. Tuhkar uses Android's Visualizer only on its own playback session to animate the disc glow. It does not use microphone-recording APIs, save audio or upload audio. Device/stream support varies.\n\nDecline to keep a soft non-reactive glow. You can change this in Settings.").setNegativeButton("Not now",(d,w)->prefs.edit().putBoolean("askedGlowPermission",true).apply()).setPositiveButton("Allow",(d,w)->{prefs.edit().putBoolean("askedGlowPermission",true).apply();requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},40);}).create();prompt.setOnDismissListener(d->{glowPermissionDialog=false;prefs.edit().putBoolean("askedGlowPermission",true).apply();});prompt.show();
    }
    void syncDisc(){
        boolean visibleDisc=false;for(DiscView view:new ArrayList<>(discViews)){boolean selected=current!=null&&current.url.equals(view.mediaUrl);view.setReactiveGlow(prefs.getBoolean("reactiveDiscGlow",true));view.playback(selected,playing,appVisible);if(selected&&view.isAttachedToWindow()&&view.isShown())visibleDisc=true;}
        boolean wanted=visibleDisc&&playing&&prepared&&appVisible&&!isVideo&&!settingsOpen&&!setupOpen&&!tvMode&&!resetInProgress&&prefs.getBoolean("reactiveDiscGlow",true)&&android.animation.ValueAnimator.areAnimatorsEnabled();
        if(!wanted){if(reactiveGlow!=null)reactiveGlow.stop();clearDiscEnergy();return;}
        if(!allowed(Manifest.permission.RECORD_AUDIO)){reactiveGlow.stop();clearDiscEnergy();if(!prefs.getBoolean("askedGlowPermission",false)&&!glowPermissionDialog)main.post(()->{if(!destroyed&&appVisible&&!settingsOpen&&!setupOpen&&playing&&!prefs.getBoolean("askedGlowPermission",false)&&!glowPermissionDialog)requestGlowPermission();});return;}
        if(player!=null)try{reactiveGlow.start(player.getAudioSessionId());}catch(Exception e){glowUnavailable=true;reactiveGlow.stop();clearDiscEnergy();}
    }
    ImageButton backButton(Runnable action){ImageButton b=new ImageButton(this);b.setImageDrawable(new FluentIcon(5,ink));b.setBackground(round(panel,24));padding(b,12,12);b.setMinimumWidth(dp(48));b.setMinimumHeight(dp(48));b.setContentDescription("Back");b.setTooltipText("Back");b.setOnClickListener(v->action.run());return b;}
    Button transport(String label,String description,Runnable action){Button b=button(label,action);b.setTextSize(20);b.setContentDescription(description);padding(b,4,0);if(description.startsWith("Previous")||description.startsWith("Next")){b.setText("");b.setCompoundDrawables(null,null,null,null);b.setPadding(0,0,0,0);b.setBackground(new SkipButtonDrawable(description.startsWith("Previous"),panel,ink,getResources().getDisplayMetrics().density));}return b;}
    void openExpanded(){if(current==null){toast("Choose a track or station first.");return;}expanded=true;render();}
    ImageButton repeatButton,shuffleButton;
    ImageButton queueToggle(Runnable action){ImageButton b=new ImageButton(this);b.setScaleType(ImageView.ScaleType.CENTER_INSIDE);b.setBackground(round(Color.TRANSPARENT,24));padding(b,14,14);b.setOnClickListener(v->action.run());return b;}
    void cycleRepeat(){if(!playbackReady())return;queue.setRepeatMode((queue.repeatMode()+1)%3);prefs.edit().putInt("repeatMode",queue.repeatMode()).apply();playback.publish();updateQueueButtons();toast(queue.repeatMode()==PlaybackQueue.OFF?"Repeat off":queue.repeatMode()==PlaybackQueue.ALL?"Repeat all":"Repeat one");}
    void toggleShuffle(){if(!playbackReady())return;queue.setShuffle(!queue.isShuffled(),current);prefs.edit().putBoolean("shuffle",queue.isShuffled()).apply();playback.publish();updateQueueButtons();toast(queue.isShuffled()?"Shuffle on":"Shuffle off · original queue order restored");}
    void updateQueueButtons(){
        boolean live=current!=null&&(current.kind.equals("radio")||current.kind.equals("tv"));
        if(repeatButton!=null){repeatButton.setImageTintList(null);int mode=queue.repeatMode();boolean active=mode!=PlaybackQueue.OFF&&!live;repeatButton.setImageDrawable(new FluentIcon(mode==PlaybackQueue.ONE?10:8,active?accentInk:muted));repeatButton.setBackground(active?new android.graphics.drawable.InsetDrawable(round(accent,999),dp(8)):round(Color.TRANSPARENT,24));padding(repeatButton,14,14);repeatButton.setSelected(active);repeatButton.setEnabled(!live);repeatButton.setAlpha(live?.4f:1f);String label=mode==0?"Repeat off":mode==1?"Repeat all":"Repeat one";repeatButton.setContentDescription(live?label+"; unavailable for live streams":label+". Tap to change.");repeatButton.setTooltipText(live?"Repeat is for music and local video":label);}
        if(shuffleButton!=null){shuffleButton.setImageTintList(null);boolean on=queue.isShuffled();shuffleButton.setImageDrawable(new FluentIcon(9,on?accentInk:muted));shuffleButton.setBackground(on?new android.graphics.drawable.InsetDrawable(round(accent,999),dp(8)):round(Color.TRANSPARENT,24));padding(shuffleButton,14,14);shuffleButton.setSelected(on);shuffleButton.setContentDescription(on?"Shuffle on. Tap to restore original queue order.":"Shuffle off. Tap to shuffle queue.");shuffleButton.setTooltipText(on?"Shuffle on":"Shuffle off");}
    }
    void onPlayHoldChanged(){if(tvMode){main.removeCallbacks(hideTV);if(!(tvPlay instanceof HoldPlayButton)||!((HoldPlayButton)tvPlay).isHolding())revealTV();}}
    void cancelPlayHolds(){if(playButton instanceof HoldPlayButton)((HoldPlayButton)playButton).cancelHold();if(tvPlay instanceof HoldPlayButton)((HoldPlayButton)tvPlay).cancelHold();}
    Button holdingPlay(boolean television,boolean ringMode){
        Runnable tap=()->{toggle();if(tvMode)revealTV();},halt=()->{stop();toast("Playback stopped");};
        HoldPlayButton b=ringMode?new RingPlayButton(this,accentInk,accent,tap,halt,()->onPlayHoldChanged(),value->{if(playback!=null)playback.seekTo(value);}):new HoldPlayButton(this,television?Color.WHITE:accentInk,tap,halt,()->onPlayHoldChanged());
        b.setText(playing?"Ⅱ":"▶");b.setAllCaps(false);b.setTextSize(20);b.setTypeface(font);b.setTextColor(television?Color.WHITE:accentInk);b.setBackground(ringMode?new android.graphics.drawable.InsetDrawable(round(accent,24),dp(8)):round(television?0xFF252925:accent,24));b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setStateListAnimator(null);padding(b,4,0);return b;
    }

    void addPlaybackControls(LinearLayout parent,boolean full){
        nowTitle=null;nowDetail=null;
        if(full){LinearLayout names=column();nowTitle=text(current==null?"Ready when you are":current.title,23,ink);semibold(nowTitle);nowTitle.setMaxLines(2);nowTitle.setEllipsize(TextUtils.TruncateAt.END);nowDetail=text(status,12,muted);nowDetail.setMaxLines(1);nowDetail.setEllipsize(TextUtils.TruncateAt.END);names.addView(nowTitle);names.addView(nowDetail);padding(names,0,4);parent.addView(names,new LinearLayout.LayoutParams(-1,-2));}
        LinearLayout controls=row();controls.setGravity(Gravity.CENTER);final int controlsWidth=full?260:276;controls.setMinimumWidth(dp(controlsWidth));repeatButton=queueToggle(()->cycleRepeat());Button prev=transport("‹","Previous",()->next(-1));playButton=holdingPlay(false,!full);Button next=transport("›","Next",()->next(1));shuffleButton=queueToggle(()->toggleShuffle());
        View[] buttons={repeatButton,prev,playButton,next,shuffleButton};for(View b:buttons){int size=b==playButton&&!full?64:48;LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(size),dp(size));lp.setMargins(dp(2),0,dp(2),0);controls.addView(b,lp);}
        HorizontalScrollView strip=new HorizontalScrollView(this);strip.setFillViewport(true);strip.setHorizontalScrollBarEnabled(true);strip.setClipToPadding(false);strip.addView(controls,new android.widget.FrameLayout.LayoutParams(-2,-1));parent.addView(strip,new LinearLayout.LayoutParams(-1,dp(full?56:64)));
        TextView scrollHint=text("Swipe controls for Repeat / Shuffle",10,muted);scrollHint.setGravity(Gravity.CENTER);scrollHint.setVisibility(View.GONE);parent.addView(scrollHint);strip.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(r-l<dp(controlsWidth)){scrollHint.setVisibility(View.VISIBLE);if(r-l!=or-ol)strip.post(()->strip.scrollTo(Math.max(0,(controls.getWidth()-strip.getWidth())/2),0));}else scrollHint.setVisibility(View.GONE);});updateQueueButtons();
        seek=null;time=null;
        if(full){
        LinearLayout progress=row();seek=new SeekBar(this);seek.setMax(1000);seek.setPadding(dp(8),0,dp(8),0);seek.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));seek.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));time=text("0:00",10,muted);time.setGravity(Gravity.END);progress.addView(seek,new LinearLayout.LayoutParams(0,dp(32),1));progress.addView(time,new LinearLayout.LayoutParams(-2,dp(24)));parent.addView(progress);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){dragging=true;}public void onStopTrackingTouch(SeekBar b){if(player!=null&&prepared)try{int duration=player.getDuration();if(duration>0)playback.seekTo((long)duration*b.getProgress()/1000);}catch(Exception ignored){}dragging=false;}public void onProgressChanged(SeekBar b,int p,boolean user){}});
        }
        if(full){TextView holdHint=text("Hold Play/Pause for 3 seconds to stop",10,muted);holdHint.setGravity(Gravity.CENTER);padding(holdHint,0,4);parent.addView(holdHint);}
    }
    void renderExpanded(){
        surface=null;videoBox=null;LinearLayout page=column();padding(page,20,12);root.addView(page,new LinearLayout.LayoutParams(-1,-1));LinearLayout top=row();top.addView(backButton(()->{expanded=false;render();}));TextView caption=text("Now playing",15,muted);bold(caption);caption.setGravity(Gravity.CENTER);top.addView(caption,new LinearLayout.LayoutParams(0,-2,1));top.addView(new Space(this),new LinearLayout.LayoutParams(dp(48),dp(48)));page.addView(top);
        LinearLayout layout=landscape?row():column();page.addView(layout,new LinearLayout.LayoutParams(-1,0,1));FrameLayout artFrame=new FrameLayout(this);padding(artFrame,16,16);
        if(landscape)layout.addView(artFrame,new LinearLayout.LayoutParams(0,-1,1));else layout.addView(artFrame,new LinearLayout.LayoutParams(-1,0,1));
        disc=new DiscView(this);disc.setHoleColor(bg);artFrame.addView(disc,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));artFrame.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{int available=Math.max(1,Math.min(r-l-dp(32),b-t-dp(32)));if(disc!=null&&disc.getParent()==artFrame)disc.setLayoutParams(new FrameLayout.LayoutParams(available,available,Gravity.CENTER));});
        discUrl=current==null?"":current.url;if(current!=null){artwork.bind(disc,current,accent);configureDisc(disc,current);}syncDisc();
        LinearLayout controls=column();padding(controls,8,12);if(landscape){controls.setGravity(Gravity.CENTER_VERTICAL);layout.addView(controls,new LinearLayout.LayoutParams(0,-1,1));}else layout.addView(controls,new LinearLayout.LayoutParams(-1,-2));playerBox=controls;addPlaybackControls(controls,true);updatePlayer();
    }
    void addRoundedPicture(LinearLayout host,int height){RoundedVideoView picture=new RoundedVideoView(this);surface=picture;host.addView(picture,new LinearLayout.LayoutParams(-1,height));picture.onOutputChanged(()->{if(surface==picture){attachVideoOutput();if(appVisible&&player!=null&&prepared)try{fitVideo(player.getVideoWidth(),player.getVideoHeight());}catch(Exception ignored){}}else if(picture.output()==null)detachVideoOutput(picture);});}
    void clearVideoOutputCache(){attachedVideoView=null;attachedVideoPlayer=null;attachedVideoSurface=null;}
    void detachVideoOutput(View view){if(attachedVideoView!=view)return;if(attachedVideoPlayer!=null&&attachedVideoPlayer==player)try{player.setSurface(null);}catch(Exception ignored){}clearVideoOutputCache();}
    void attachVideoOutput(){if(player==null||!isVideo||!appVisible||surface==null)return;try{Surface output=surface instanceof RoundedVideoView?((RoundedVideoView)surface).output():surface instanceof SurfaceView?((SurfaceView)surface).getHolder().getSurface():null;if(output!=null&&!output.isValid())output=null;if(attachedVideoPlayer==player&&attachedVideoView==surface&&attachedVideoSurface==output)return;if(surface instanceof SurfaceView){if(output!=null)player.setDisplay(((SurfaceView)surface).getHolder());else player.setSurface(null);}else player.setSurface(output);attachedVideoPlayer=player;attachedVideoView=surface;attachedVideoSurface=output;}catch(Exception ignored){clearVideoOutputCache();}}
    void renderVideoExpanded(){
        LinearLayout page=column();padding(page,16,8);root.addView(page,new LinearLayout.LayoutParams(-1,-1));LinearLayout top=row();top.addView(backButton(()->{expanded=false;render();}));TextView caption=text("Now playing",20,ink);bold(caption);padding(caption,12,4);top.addView(caption);page.addView(top);
        videoBox=column();videoBox.setGravity(Gravity.CENTER);videoBox.setBackground(round(Color.BLACK,12));videoBox.setClipToOutline(true);LinearLayout.LayoutParams pictureParams=new LinearLayout.LayoutParams(-1,0,1);pictureParams.bottomMargin=dp(12);page.addView(videoBox,pictureParams);addRoundedPicture(videoBox,-1);installInlineVideoGesture();
        videoBox.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(player!=null&&prepared)try{fitVideo(player.getVideoWidth(),player.getVideoHeight());}catch(Exception ignored){}});
        playerBox=column();page.addView(playerBox,new LinearLayout.LayoutParams(-1,-2));addPlaybackControls(playerBox,true);updatePlayer();
    }
    void installInlineVideoGesture(){
        final View preview=surface;
        preview.setOnClickListener(v->{if(current!=null&&current.kind.equals("tv"))enterTV();});
        final GestureDetector taps=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent event){return true;}
            @Override public boolean onSingleTapUp(MotionEvent event){return true;}
            @Override public boolean onDoubleTap(MotionEvent event){if(current!=null&&current.kind.equals("tv")){preview.performClick();return true;}return false;}
        });
        View.OnTouchListener touch=(view,event)->{MotionEvent local=MotionEvent.obtain(event);if(view==preview)local.offsetLocation(preview.getLeft(),preview.getTop());taps.onTouchEvent(local);local.recycle();return true;};
        preview.setOnTouchListener(touch);videoBox.setOnTouchListener(touch);videoBox.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        videoBox.setOnClickListener(v->preview.performClick());
        preview.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);if(current!=null&&current.kind.equals("tv"))info.addAction(new android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK,"Open TV fullscreen"));else{info.removeAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);info.setClickable(false);}}});
    }
    void enterTV(){if(tvMode||current==null||!current.kind.equals("tv"))return;expanded=false;settingsOpen=false;tvMode=true;priorOrientation=getRequestedOrientation();setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);render();}
    void exitTV(){main.removeCallbacks(hideTV);tvMode=false;getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);setRequestedOrientation(priorOrientation);render();updatePlayer();}
    final Runnable hideTV=()->{if(tvMode&&playing&&tvControls!=null){if(tvPlay instanceof HoldPlayButton&&((HoldPlayButton)tvPlay).isHolding())return;tvControls.setVisibility(View.GONE);immersiveTV();}};
    void immersiveTV(){if(!tvMode)return;getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);}
    void revealTV(){if(!tvMode||tvControls==null)return;tvControls.setVisibility(View.VISIBLE);main.removeCallbacks(hideTV);if(playing)main.postDelayed(hideTV,4000);}
    void renderTV(){
        root.setPadding(0,0,0,0);root.setBackgroundColor(Color.BLACK);root.setOnApplyWindowInsetsListener((v,insets)->{int left=insets.getSystemWindowInsetLeft(),right=insets.getSystemWindowInsetRight(),bottom=insets.getSystemWindowInsetBottom();if(Build.VERSION.SDK_INT>=30){android.graphics.Insets safe=insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());left=safe.left;right=safe.right;bottom=safe.bottom;}if(tvControls!=null)tvControls.setPadding(dp(16)+left,dp(12),dp(16)+right,dp(12)+bottom);return insets;});
        tvStage=new FrameLayout(this);root.addView(tvStage,new LinearLayout.LayoutParams(-1,-1));surface=new SurfaceView(this);tvStage.addView(surface,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));surface.setContentDescription("TV picture. Tap to show or hide playback controls.");
        surface.setOnClickListener(v->{if(tvControls.getVisibility()==View.VISIBLE&&playing){tvControls.setVisibility(View.GONE);main.removeCallbacks(hideTV);}else revealTV();});tvStage.setOnClickListener(v->revealTV());
        tvControls=column();tvControls.setBackgroundColor(0xE6000000);padding(tvControls,16,12);FrameLayout.LayoutParams bar=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);tvStage.addView(tvControls,bar);
        tvTitle=text(current==null?"TV":current.title,17,Color.WHITE);semibold(tvTitle);tvTitle.setMaxLines(1);tvTitle.setEllipsize(TextUtils.TruncateAt.END);tvControls.addView(tvTitle);tvStatus=text(status,11,0xFFCCCCCC);tvControls.addView(tvStatus);
        LinearLayout buttons=row();padding(buttons,0,6);ImageButton back=backButton(()->exitTV());back.setImageDrawable(new FluentIcon(5,Color.WHITE));Button previous=transport("|‹","Previous channel",()->{next(-1);revealTV();});tvPlay=holdingPlay(true,false);Button next=transport("›|","Next channel",()->{next(1);revealTV();});View[] all={back,previous,tvPlay,next};for(View b:all){if(b instanceof Button)((Button)b).setTextColor(Color.WHITE);boolean skip=b==previous||b==next;b.setBackground(skip?new SkipButtonDrawable(b==previous,0xFF252925,Color.WHITE,getResources().getDisplayMetrics().density):round(0xFF252925,24));if(skip){((Button)b).setCompoundDrawables(null,null,null,null);b.setPadding(0,0,0,0);}LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(48),dp(48));lp.rightMargin=dp(8);buttons.addView(b,lp);}TextView hint=text("Tap picture to reveal controls",11,0xFFCCCCCC);hint.setGravity(Gravity.END);buttons.addView(hint,new LinearLayout.LayoutParams(0,-2,1));tvControls.addView(buttons);
        final SurfaceView fullscreenPicture=(SurfaceView)surface;fullscreenPicture.getHolder().addCallback(new SurfaceHolder.Callback(){public void surfaceCreated(SurfaceHolder h){if(surface==fullscreenPicture)attachVideoOutput();}public void surfaceChanged(SurfaceHolder h,int f,int w,int height){if(surface==fullscreenPicture){attachVideoOutput();if(player!=null&&prepared)try{fitVideo(player.getVideoWidth(),player.getVideoHeight());}catch(Exception ignored){}}}public void surfaceDestroyed(SurfaceHolder h){detachVideoOutput(fullscreenPicture);}});
        tvStage.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(player!=null&&prepared)try{fitVideo(player.getVideoWidth(),player.getVideoHeight());}catch(Exception ignored){}});root.requestApplyInsets();immersiveTV();revealTV();updatePlayer();
    }

    void closeSettings(){settingsOpen=false;render();if(tab==0||tab==2)loadLocal();}
    LinearLayout settingsCard(LinearLayout parent,String title){LinearLayout card=column();padding(card,18,16);card.setBackground(round(panel,22));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(14);parent.addView(card,lp);card.setTag("settings:"+title);TextView heading=text(title,18,ink);bold(heading);card.addView(heading);return card;}
    void tintSettingsSwitch(Switch control){accentSwitches.add(control);
        int[][] states={new int[]{-android.R.attr.state_enabled},new int[]{android.R.attr.state_checked},new int[]{}};
        control.setThumbTintList(new android.content.res.ColorStateList(states,new int[]{SurfacePalette.mix(panel,muted,.35f),accent,muted}));
        control.setTrackTintList(new android.content.res.ColorStateList(states,new int[]{SurfacePalette.mix(panel,muted,.12f),SurfacePalette.mix(panel,accent,.48f),SurfacePalette.mix(panel,muted,.28f)}));
        control.setMinimumHeight(dp(48));
    }
    void rememberSettingsScroll(){
        if(settingsRestoring||settingsScroll==null||settingsPage==null||!settingsScroll.isAttachedToWindow())return;
        settingsScrollY=settingsScroll.getScrollY();settingsAnchor=null;settingsAnchorOffset=0;
        for(int i=0;i<settingsPage.getChildCount();i++){View child=settingsPage.getChildAt(i);Object tag=child.getTag();if(tag instanceof String&&((String)tag).startsWith("settings:")&&child.getTop()<=settingsScrollY){settingsAnchor=(String)tag;settingsAnchorOffset=Math.max(0,settingsScrollY-child.getTop());}}
    }
    void restoreSettingsScroll(ScrollView scroll,LinearLayout page){
        settingsScroll=scroll;settingsPage=page;settingsRestoring=true;
        scroll.setOnScrollChangeListener((v,x,y,oldX,oldY)->{if(settingsScroll==scroll&&!settingsRestoring)rememberSettingsScroll();});
        scroll.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener(){public void onGlobalLayout(){
            if(scroll.getHeight()<=0||page.getHeight()<=0)return;
            if(scroll.getViewTreeObserver().isAlive())scroll.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            if(settingsScroll!=scroll)return;
            View anchor=settingsAnchor==null?null:page.findViewWithTag(settingsAnchor);
            int maximum=Math.max(0,page.getHeight()-scroll.getHeight()+scroll.getPaddingTop()+scroll.getPaddingBottom());
            int target=SettingsScrollAnchor.restore(settingsScrollY,anchor==null?-1:anchor.getTop(),anchor==null?0:anchor.getHeight(),settingsAnchorOffset,maximum);
            scroll.scrollTo(0,target);settingsRestoring=false;rememberSettingsScroll();
        }});
    }
    void renderSettings(){
        LinearLayout settingsHost=root;if(landscape){body=row();root.addView(body,new LinearLayout.LayoutParams(-1,0,1));addNavRail(body);settingsHost=column();body.addView(settingsHost,new LinearLayout.LayoutParams(0,-1,1));}ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalFadingEdgeEnabled(true);scroll.setFadingEdgeLength(dp(12));scroll.setPadding(0,0,0,0);scroll.setClipToPadding(true);scroll.setClipChildren(true);LinearLayout.LayoutParams settingsScrollParams=new LinearLayout.LayoutParams(-1,0,1);settingsScrollParams.bottomMargin=dp(8);settingsHost.addView(scroll,settingsScrollParams);LinearLayout page=column();padding(page,landscape?32:22,16);scroll.addView(page);
        if(settingsFadePending){settingsFadePending=false;if(android.animation.ValueAnimator.areAnimatorsEnabled()){page.setAlpha(.97f);page.animate().alpha(1f).setDuration(140).start();}}
        LinearLayout top=row();top.addView(backButton(()->closeSettings()));TextView title=text("Settings",28,ink);bold(title);padding(title,14,4);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));page.addView(top);
        TextView summary=text("Make this space yours.",13,muted);padding(summary,0,12);page.addView(summary);
        librarySettings(page);discGlowSettings(page);equalizerSettings(page);mediaAccessSettings(page);
        LinearLayout oled=settingsCard(page,"OLED mode");Switch black=new Switch(this);black.setText("Black background in dark mode");black.setTextColor(ink);black.setTypeface(font);black.setChecked(prefs.getBoolean("oled",false));tintSettingsSwitch(black);black.setOnCheckedChangeListener((b,on)->{prefs.edit().putBoolean("oled",on).apply();render();});oled.addView(black);TextView oledNote=text("Black background, colored cards. Follows your phone's theme.",12,muted);padding(oledNote,0,8);oled.addView(oledNote);
        accentSettings(page);
        LinearLayout art=settingsCard(page,"Artwork");Switch downloads=new Switch(this);downloads.setText("Download missing artwork");downloads.setTypeface(font);downloads.setTextColor(ink);downloads.setChecked(prefs.getBoolean("artDownloads",true));tintSettingsSwitch(downloads);downloads.setOnCheckedChangeListener((b,on)->prefs.edit().putBoolean("artDownloads",on).apply());art.addView(downloads);
        TextView privacy=text("Finds missing covers online. Titles and artists are shared with artwork services; your files stay on your phone. May use mobile data.",12,muted);padding(privacy,0,10);art.addView(privacy);
        art.addView(button("Clear artwork cache",()->dialog("Clear artwork cache?").setMessage("Remove saved covers? They can download again.").setPositiveButton("Clear",(d,w)->{artwork.clear();toast("Artwork cache clearing");}).setNegativeButton("Cancel",null).show()));
        LinearLayout resetCard=settingsCard(page,"Reset app");LinearLayout resetRow=row();resetRow.addView(text("Clear Tuhkar settings, saved items and cache. Your actual music/videos are never deleted.",12,muted),new LinearLayout.LayoutParams(0,-2,1));ImageButton reset=toolIcon(17,"Reset Tuhkar",()->confirmReset());reset.setImageDrawable(new FluentIcon(17,Color.parseColor(dark?"#FF6B6B":"#B3261E")));reset.setBackgroundColor(Color.TRANSPARENT);padding(reset,12,12);resetRow.addView(reset,new LinearLayout.LayoutParams(dp(48),dp(48)));resetCard.addView(resetRow);
        LinearLayout info=settingsCard(page,"About");info.addView(text("Tuhkar v1.0.0 · by KnightWin",12,muted));info.addView(button("About & privacy",()->about()));
        restoreSettingsScroll(scroll,page);
    }
    void beginSetup(){generation++;if(pendingSearch!=null)main.removeCallbacks(pendingSearch);expanded=false;settingsOpen=false;setupOpen=true;setupGo(0);}
    void setupGo(int step){setupStep=Math.max(0,Math.min(4,step));prefs.edit().putBoolean("setupStarted",true).putBoolean("setupComplete",false).putInt("setupFlowVersion",3).putInt("setupStep",setupStep).apply();render();}
    void setupLibrary(){setupGo(1);}
    ArrayList<String> setupMissingPermissions(){
        ArrayList<String> missing=new ArrayList<>();
        if(Build.VERSION.SDK_INT>=33){if(!musicAccess())missing.add(Manifest.permission.READ_MEDIA_AUDIO);if(!videoAccess()&&!selectedVideoAccess())missing.add(Manifest.permission.READ_MEDIA_VIDEO);if(!allowed(Manifest.permission.POST_NOTIFICATIONS))missing.add(Manifest.permission.POST_NOTIFICATIONS);}
        else if(!musicAccess())missing.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        if(!allowed(Manifest.permission.RECORD_AUDIO))missing.add(Manifest.permission.RECORD_AUDIO);
        return missing;
    }
    void setupPermissionResult(){localMusic.clear();localVideo.clear();generation++;setupGo(setupMissingPermissions().isEmpty()?4:3);}
    void setupRequestPermissions(){
        if(setupStep==2)return;
        ArrayList<String> missing=setupMissingPermissions();
        if(missing.isEmpty()){setupGo(4);return;}
        ArrayList<String> requestable=new ArrayList<>();
        for(String permission:missing)if(!prefs.getBoolean("asked:"+permission,false)||shouldShowRequestPermissionRationale(permission))requestable.add(permission);
        if(requestable.isEmpty()){setupGo(3);openAccessSettings();return;}
        if(Build.VERSION.SDK_INT>=34&&requestable.contains(Manifest.permission.READ_MEDIA_VIDEO))requestable.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
        for(String permission:requestable)prefs.edit().putBoolean("asked:"+permission,true).apply();
        prefs.edit().putBoolean("askedPlaybackNotifications",true).putBoolean("askedGlowPermission",true).apply();
        setupGo(2);requestPermissions(requestable.toArray(new String[0]),50);
    }
    void setupReviewContinue(){prefs.edit().putBoolean("askedPlaybackNotifications",true).putBoolean("askedGlowPermission",true).apply();setupGo(4);}
    void setupPrimaryAction(){if(setupStep==0)setupLibrary();else if(setupStep==1||setupStep==3)setupRequestPermissions();else if(setupStep==2)setupPermissionResult();else finishSetup(false);}
    void setupBack(){if(setupStep==4)finishSetup(false);else if(setupStep!=2)setupGo(0);}
    void finishSetup(boolean streaming){prefs.edit().putBoolean("askedPlaybackNotifications",true).putBoolean("askedGlowPermission",true).putBoolean("setupComplete",true).remove("setupStep").apply();setupOpen=false;settingsOpen=false;expanded=false;query="";sourceStatus="";tab=streaming?1:0;generation++;render();if(tab==0)loadLocal();else loadOnline();}
    void setupAction(LinearLayout host,Button action,int gap){
        action.setMinHeight(dp(52));action.setMinimumHeight(dp(52));action.setSingleLine(false);action.setMaxLines(3);padding(action,16,10);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(gap);host.addView(action,params);
    }
    boolean standardFolderOnly(int target){return prefs.getBoolean(target==0?"musicStandardFolderOnly":"videoStandardFolderOnly",target==0);}
    void standardFolderOptions(LinearLayout host){
        for(int target:new int[]{0,2}){
            final int kind=target;Switch standard=new Switch(this);standard.setText(target==0?"Music folder only":"Movies folder only");standard.setTextColor(ink);standard.setTypeface(font);standard.setTextSize(14);standard.setChecked(standardFolderOnly(target));tintSettingsSwitch(standard);
            standard.setOnCheckedChangeListener((b,on)->{prefs.edit().putBoolean(kind==0?"musicStandardFolderOnly":"videoStandardFolderOnly",on).apply();localMusic.clear();localVideo.clear();generation++;});
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(target==0?6:2);host.addView(standard,params);
        }
        TextView note=text("On: use only the Music or Movies folder and its subfolders. Off: include other folders too.",12,muted);padding(note,0,10);host.addView(note);
    }
    void setupBrand(LinearLayout page){
        FrameLayout brand=new FrameLayout(this);HeaderWordmark logo=new HeaderWordmark(this);brand.addView(logo,new FrameLayout.LayoutParams(dp(Math.min(240,Math.max(120,getResources().getConfiguration().screenWidthDp-(landscape?80:44)))),dp(80),Gravity.CENTER));page.addView(brand,new LinearLayout.LayoutParams(-1,dp(100)));
    }
    void setupScriptTitle(LinearLayout page,String label){
        if(setupScript==null)setupScript=Typeface.createFromAsset(getAssets(),"SetupScript.ttf");
        TextView title=text(label,label.equals("Welcome")?44:36,ink);title.setTypeface(setupScript);title.setGravity(Gravity.CENTER);padding(title,0,12);page.addView(title);
    }
    void renderSetup(){
        // Setup has no player/nav views; service callbacks must not mutate detached controls.
        list=null;grid=null;deck=null;notice=null;playerBox=null;videoBox=null;surface=null;seek=null;time=null;nowTitle=null;nowDetail=null;playButton=null;repeatButton=null;shuffleButton=null;
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
        LinearLayout page=column();page.setGravity(Gravity.CENTER_VERTICAL);padding(page,landscape?40:22,24);scroll.addView(page);
        if(setupStep==0){
            setupScriptTitle(page,"Welcome");setupBrand(page);
            TextView intro=text("Your music. Your videos. Your vibe.",14,muted);intro.setGravity(Gravity.CENTER);padding(intro,0,16);page.addView(intro);
            Button setUp=button("Set it Up",()->setupLibrary());setUp.setBackground(round(accent,18));setUp.setTextColor(accentInk);setupPrimary=setUp;setupAction(page,setUp,16);
            setupAction(page,button("Skip Setup",()->finishSetup(true)),12);return;
        }
        if(setupStep==4){
            setupScriptTitle(page,"Congratulations!");TextView ready=text("You're now ready to use",16,ink);ready.setGravity(Gravity.CENTER);page.addView(ready);setupBrand(page);
            TextView enjoy=text("Make yourself at home. Let the music play.",13,muted);enjoy.setGravity(Gravity.CENTER);padding(enjoy,0,16);page.addView(enjoy);
            Button start=button("Get started",()->finishSetup(false));start.setBackground(round(accent,18));start.setTextColor(accentInk);setupPrimary=start;setupAction(page,start,16);return;
        }
        setupBrand(page);
        LinearLayout card=settingsCard(page,setupStep==1?"Before we begin":setupStep==2?"Android permissions":"Some access is still off");
        if(setupStep==1){
            card.addView(text("Next opens Android's permission prompts for:",14,ink));
            TextView terms=text("Media access — find music and videos on your phone and SD card. Your files are never changed.\n\nNotifications — show playback controls.\n\nRecording — Android's name for the disc glow's playback analysis. No microphone recording or saved/uploaded audio.\n\nYou choose what to allow. Declined features can be enabled later in Settings.\n\nOptional cover searches share titles/artists with artwork services. Turn downloads off in Settings.",13,muted);padding(terms,0,12);card.addView(terms);
            card.addView(text("By tapping Next, you confirm you've read this notice. Android handles your permission choices.",12,muted));
            Button next=button("Next",()->setupPrimaryAction());next.setBackground(round(accent,18));next.setTextColor(accentInk);setupPrimary=next;setupAction(page,next,8);
            setupAction(page,button("Back",()->setupBack()),12);
        }else if(setupStep==2){
            card.addView(text("Choose what to allow in the Android prompts. We'll check your choices when they finish.",14,ink));
            card.addView(text("If the prompts have already closed, tap Check permissions to continue.",12,muted));
            setupAction(page,button("Check permissions",()->setupPermissionResult()),8);
        }else{
            StringBuilder summary=new StringBuilder();
            for(String permission:setupMissingPermissions()){
                String label=permission.equals(Manifest.permission.RECORD_AUDIO)?"Beat-reactive disc glow":permission.equals(Manifest.permission.POST_NOTIFICATIONS)?"Playback notifications":permission.equals(Manifest.permission.READ_MEDIA_AUDIO)?"Music library":permission.equals(Manifest.permission.READ_MEDIA_VIDEO)?"Video library":"Music and video library";
                if(summary.length()>0)summary.append("\n");summary.append("• ").append(label);
            }
            card.addView(text(summary.toString(),14,ink));TextView note=text("Retry the missing permissions, or continue with these features off. If Android has blocked a prompt, Retry opens App settings.",12,muted);padding(note,0,12);card.addView(note);
            setupAction(page,button("Retry",()->setupRequestPermissions()),8);setupAction(page,button("Continue anyway",()->setupReviewContinue()),12);
        }
    }
    void discGlowSettings(LinearLayout page){
        LinearLayout card=settingsCard(page,"Disc glow");Switch glow=new Switch(this);glow.setText("Audio-reactive accent glow");glow.setTextColor(ink);glow.setTypeface(font);glow.setChecked(prefs.getBoolean("reactiveDiscGlow",true));tintSettingsSwitch(glow);glow.setOnCheckedChangeListener((b,on)->{prefs.edit().putBoolean("reactiveDiscGlow",on).apply();if(on&&!allowed(Manifest.permission.RECORD_AUDIO))requestGlowPermission();if(!on){reactiveGlow.stop();clearDiscEnergy();}syncDisc();});card.addView(glow);
        card.addView(text("On: the rim, glow and shadow breathe visibly with playback energy. Off: a dim, steady ring and soft glow; no pulsing. Artwork still spins while playing.",12,muted));
        card.addView(text(glowUnavailable?"Audio analysis is unavailable on this device/session. The glow stays non-reactive.":allowed(Manifest.permission.RECORD_AUDIO)?"Playback analysis permission allowed. Glow reacts only while a playing disc is visible.":"Actual music reaction needs Android's Record audio permission. No microphone-recording APIs, saved audio or uploads.",12,muted));
        if(!allowed(Manifest.permission.RECORD_AUDIO))card.addView(button("Allow audio analysis",()->requestGlowPermission()));
    }
    void mediaAccessSettings(LinearLayout page){
        LinearLayout card=settingsCard(page,"Media access");card.addView(text(mediaAccessStatus(),13,ink));
        LinearLayout accessActions=row();ImageButton access=toolIcon(19,"Manage media access",()->requestMedia());access.setBackgroundColor(Color.TRANSPARENT);padding(access,12,12);accessActions.addView(access,new LinearLayout.LayoutParams(dp(48),dp(48)));ImageButton setup=toolIcon(20,"Run setup again",()->beginSetup());setup.setBackgroundColor(Color.TRANSPARENT);padding(setup,12,12);LinearLayout.LayoutParams setupIconParams=new LinearLayout.LayoutParams(dp(48),dp(48));setupIconParams.leftMargin=dp(12);accessActions.addView(setup,setupIconParams);card.addView(accessActions);standardFolderOptions(card);
    }
    void releaseOldFolderGrants(){
        for(android.content.UriPermission grant:getContentResolver().getPersistedUriPermissions())try{
            int flags=(grant.isReadPermission()?Intent.FLAG_GRANT_READ_URI_PERMISSION:0)|(grant.isWritePermission()?Intent.FLAG_GRANT_WRITE_URI_PERMISSION:0);
            if(flags!=0)getContentResolver().releasePersistableUriPermission(grant.getUri(),flags);
        }catch(Exception ignored){}
    }
    void retireCustomFolders(){if(prefs.getBoolean("indexedLibraryOnly",false))return;releaseOldFolderGrants();prefs.edit().remove("trees").remove("musicTrees").remove("videoTrees").remove("musicFolderOnly").remove("videoFolderOnly").putBoolean("indexedLibraryOnly",true).apply();}
    void confirmReset(){
        AlertDialog warning=dialog("Reset Tuhkar?").setMessage("This clears all Tuhkar settings, saved stations/items, playback queue, EQ preferences, setup progress and cached artwork. Playback will stop and setup will restart.\n\nYour actual music and video files on internal storage or SD cards are NEVER deleted or changed.\n\nAndroid's media/notification/audio-analysis permission grants may remain. Manage those in Android App settings.").setNegativeButton("Cancel",null).setPositiveButton("Reset app",(d,w)->resetApp()).create();
        warning.setOnShowListener(d->warning.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor(dark?"#FF6B6B":"#B3261E")));warning.show();
    }
    void resetApp(){
        if(resetInProgress)return;resetInProgress=true;generation++;if(pendingSearch!=null)main.removeCallbacks(pendingSearch);cancelPlayHolds();stopDiscAnimation();
        if(playback!=null){playback.listener=null;playback.stopPlayback();}if(equalizer!=null)equalizer.close();queue.clear();queue.setRepeatMode(PlaybackQueue.OFF);queue.setShuffle(false,null);
        getIntent().removeExtra("openNowPlaying");getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);current=null;player=null;prepared=false;playing=false;isVideo=false;pendingNowPlaying=false;expanded=false;tvMode=false;settingsOpen=false;returningFromAccessSettings=false;
        saved.clear();localMusic.clear();localVideo.clear();online.clear();shown.clear();movingArt.clear();query="";sourceStatus="";status="Your library, beautifully simple.";country="ph";showingSaved=false;libraryPosition=0;libraryOffset=0;eqSettingsState="";glowUnavailable=false;glowPermissionDialog=false;
        settingsScroll=null;settingsPage=null;settingsRestoring=false;settingsAnchor=null;settingsAnchorOffset=0;settingsScrollY=0;releaseOldFolderGrants();prefs.edit().clear().putBoolean("setupStarted",true).putBoolean("setupComplete",false).putInt("setupStep",0).putBoolean("indexedLibraryOnly",true).putBoolean("pendingPlaybackReset",playback==null).commit();
        setupOpen=true;setupStep=0;tab=0;render();
        final ArtworkLoader oldArtwork=artwork;oldArtwork.close();
        new Thread(()->{boolean clean=oldArtwork.resetDiskCache();main.post(()->{if(destroyed)return;artwork=new ArtworkLoader(this,prefs);artwork.setActive(appVisible);resetInProgress=false;if(playback!=null)playback.listener=playbackChanged;render();if(!clean)toast("Settings reset. Some cache files could not be cleared; your media files were not touched.");});},"Tuhkar-reset").start();
    }
    String accentStyle(){return AccentStyle.normalize(prefs.getString("accentStyle","device"));}
    void chooseAccent(String style){prefs.edit().putString("accentStyle",AccentStyle.normalize(style)).remove("accent").apply();render();}
    void accentSettings(LinearLayout page){
        LinearLayout card=settingsCard(page,"Accent color");card.addView(text("Device follows Android. RGB is a slow color wave on controls and discs only; card backgrounds stay steady. Disc lighting follows every selected palette.",12,muted));
        for(int i=0;i<AccentStyle.IDS.length;i++){final String id=AccentStyle.IDS[i];String label=AccentStyle.NAMES[i];boolean selected=id.equals(accentStyle());Button choice=button((selected?"✓ ":"")+label,()->chooseAccent(id));choice.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);choice.setSingleLine(false);choice.setMinHeight(dp(50));choice.setMinimumHeight(dp(50));choice.setSelected(selected);choice.setBackground(round(selected?SurfacePalette.mix(panel,AccentStyle.base(id,deviceAccent),.24f):normalBg,16));int[] swatchColors=AccentStyle.ring(id,deviceAccent);GradientDrawable swatch=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,swatchColors);swatch.setCornerRadius(dp(12));swatch.setBounds(0,0,dp(28),dp(28));choice.setCompoundDrawables(swatch,null,null,null);choice.setCompoundDrawablePadding(dp(12));LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(8);card.addView(choice,params);}
    }
    void colorBackground(View view,int color){if(view==null)return;android.graphics.drawable.Drawable drawable=view.getBackground();while(drawable instanceof android.graphics.drawable.InsetDrawable)drawable=((android.graphics.drawable.InsetDrawable)drawable).getDrawable();if(drawable instanceof GradientDrawable)((GradientDrawable)drawable).setColor(color);}
    void scheduleAccentWave(){if(!accentWaveScheduled&&appVisible&&"rgb".equals(accentStyle())){accentWaveScheduled=true;main.postDelayed(accentWave,200);}}
    void applyAccentWave(){accent=AccentStyle.wave(SystemClock.uptimeMillis());accentInk=Color.BLACK;
        if(nav!=null)for(int i=0;i<nav.getChildCount();i++){View item=nav.getChildAt(i);if(item instanceof ImageButton&&item.isSelected()){colorBackground(item,accent);((ImageButton)item).setImageTintList(android.content.res.ColorStateList.valueOf(accentInk));}}
        if(playButton!=null){colorBackground(playButton,accent);playButton.setTextColor(accentInk);if(playButton instanceof HoldPlayButton)((HoldPlayButton)playButton).accentInk(accentInk);if(playButton instanceof RingPlayButton)((RingPlayButton)playButton).accentColor(accent);}
        for(ImageButton toggle:new ImageButton[]{repeatButton,shuffleButton})if(toggle!=null&&toggle.isSelected()){colorBackground(toggle,accent);toggle.setImageTintList(android.content.res.ColorStateList.valueOf(accentInk));}
        if(favoriteButton!=null&&favoriteButton.isSelected())favoriteButton.setImageTintList(android.content.res.ColorStateList.valueOf(selectedIconColor()));
        if(setupPrimary!=null){colorBackground(setupPrimary,accent);setupPrimary.setTextColor(accentInk);}
        if(seek!=null){seek.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));seek.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));}
        for(Switch toggle:new ArrayList<>(accentSwitches))tintSettingsSwitch(toggle);
    }
    void about(){dialog("Tuhkar v1.0.0").setMessage("Created by KnightWin · GNU GPL v3\n\nMusic & videos • Radio & TV\nEqualizer, disc effects, themes and saved favorites.\n\nPRIVACY\n• No ads or analytics. Your media files stay on your device.\n• Optional artwork searches share titles/artists with cover services. Turn downloads off in Settings.\n• Beat glow needs Android’s Record audio permission but analyzes only app playback—no microphone recording or saved/uploaded audio.\n• Streams/artwork contact third parties. HTTP links aren’t encrypted.\n• Reset clears settings and cache, never your music/videos.\n\nStreams may be unavailable or region-limited. Use only authorized links.\n\nCredits: Radio Browser · iptv-org · iTunes · Wikipedia\nManrope & Lobster Two fonts · SIL OFL").setPositiveButton("Done",null).show();}
    void swipeSelect(int index){if(index<0||index>=shown.size()||!playbackReady())return;boolean keepPlaying=playing;Item item=shown.get(index);queue.reset(shown,item);if(keepPlaying){playback.play(item);playbackPermission();}else playback.selectPaused(item);}
    void start(Item item){if(!playbackReady())return;playback.play(item);playbackPermission();}
    void fitVideo(int width,int height){if(width<=0||height<=0||surface==null)return;surface.post(()->{if(destroyed||surface==null)return;if(tvMode){if(tvStage==null)return;int w=tvStage.getWidth(),h=tvStage.getHeight();if(w<=0||h<=0)return;float fit=Math.min((float)w/width,(float)h/height);int newW=Math.max(1,(int)(width*fit)),newH=Math.max(1,(int)(height*fit));android.view.ViewGroup.LayoutParams currentSize=surface.getLayoutParams();if(currentSize.width!=newW||currentSize.height!=newH)surface.setLayoutParams(new FrameLayout.LayoutParams(newW,newH,Gravity.CENTER));return;}if(videoBox==null)return;int maxW=videoBox.getWidth(),maxH=expanded?videoBox.getHeight():landscape?dp(150):Math.min(Math.max(1,Math.round((float)maxW*height/width)),dp(getResources().getConfiguration().screenHeightDp*.30f));if(maxW<=0||maxH<=0)return;float scale=Math.min((float)maxW/width,(float)maxH/height);videoBox.setMinimumHeight(maxH);int fittedW=Math.max(1,(int)(width*scale)),fittedH=Math.max(1,(int)(height*scale));android.view.ViewGroup.LayoutParams size=surface.getLayoutParams();if(size.width!=fittedW||size.height!=fittedH)surface.setLayoutParams(new LinearLayout.LayoutParams(fittedW,fittedH));});}
    void pause(){if(playback!=null)playback.pause();}
    void toggle(){if(!playbackReady())return;if(playback.current==null){toast("Choose something to play first.");return;}playback.toggle();}
    void stop(){cancelPlayHolds();if(playback!=null)playback.stopPlayback();}
    void next(int direction){if(playbackReady())playback.next(direction);}
    void updatePlayer(){
        updateQueueButtons();updateArtworkMotion();updateRingProgress();
        if(deck!=null&&current!=null&&!settingsOpen&&!setupOpen&&!expanded)for(int i=0;i<shown.size();i++)if(shown.get(i).url.equals(current.url)){deck.align(i);break;}
        if(tvMode){if(tvTitle!=null)tvTitle.setText(current==null?"TV":current.title);if(tvStatus!=null)tvStatus.setText(status);if(tvPlay!=null)tvPlay.setText(playing?"Ⅱ":"▶");if(!playing)revealTV();else if(tvControls!=null&&tvControls.getVisibility()==View.VISIBLE){main.removeCallbacks(hideTV);main.postDelayed(hideTV,4000);}return;}
        if(playerBox!=null&&!expanded)playerBox.setContentDescription((current==null?"Player":current.title)+". "+status+". Tap to open Now Playing.");if(nowTitle!=null)nowTitle.setText(current==null?"Ready when you are":current.title);if(nowDetail!=null)nowDetail.setText(status);if(playButton!=null)playButton.setText(playing?"Ⅱ":"▶");if(videoBox!=null)videoBox.setVisibility(current!=null&&isVideo?View.VISIBLE:View.GONE);if(surface!=null&&!tvMode)surface.setContentDescription(current!=null&&current.kind.equals("tv")?"TV preview. Double-tap for landscape fullscreen.":"Video preview");if(seek!=null)seek.setEnabled(prepared);
        if(expanded&&disc!=null&&current!=null&&!current.url.equals(discUrl)){discUrl=current.url;artwork.bind(disc,current,accent);configureDisc(disc,current);}syncDisc();
    }
    String format(long ms){long seconds=Math.max(0,ms/1000);return seconds>=3600?String.format(Locale.ROOT,"%d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60):String.format(Locale.ROOT,"%d:%02d",seconds/60,seconds%60);}
    void updateRingProgress(){if(playButton instanceof RingPlayButton){RingPlayButton ring=(RingPlayButton)playButton;ring.media(current==null?"":current.url);ring.progress(playback==null?0:playback.position(),playback==null?0:playback.duration(),playing);}}
    final Runnable ticker=new Runnable(){public void run(){if(destroyed||!appVisible)return;syncDisc();updateRingProgress();if(!tvMode&&player!=null&&prepared&&seek!=null&&time!=null)try{int duration=player.getDuration(),pos=player.getCurrentPosition();if(!dragging&&duration>0)seek.setProgress((int)((long)pos*1000/duration));seek.setEnabled(duration>0&&!((current!=null)&&(current.kind.equals("radio")||current.kind.equals("tv"))));time.setText(duration>0?format(pos)+" / "+format(duration):"LIVE");}catch(Exception ignored){}else if(time!=null){time.setText("0:00");if(seek!=null)seek.setProgress(0);}main.postDelayed(this,500);}};
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override public void onBackPressed(){if(resetInProgress)return;if(setupOpen){setupBack();return;}if(tvMode){exitTV();return;}if(expanded){expanded=false;render();return;}if(settingsOpen){closeSettings();return;}super.onBackPressed();}
    @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);render();updatePlayer();if(player!=null&&prepared&&isVideo)try{fitVideo(player.getVideoWidth(),player.getVideoHeight());}catch(Exception ignored){}}
    @Override public void onStop(){
        super.onStop();rememberSettingsScroll();cancelPlayHolds();appVisible=false;main.removeCallbacks(accentWave);accentWaveScheduled=false;main.removeCallbacks(hideTV);syncDisc();updateArtworkMotion();if(artwork!=null)artwork.setActive(false);main.removeCallbacks(ticker);
        if(list!=null){libraryPosition=list.getFirstVisiblePosition();View first=list.getChildAt(0);libraryOffset=first==null?0:first.getTop();}else if(grid!=null)libraryPosition=grid.getFirstVisiblePosition();
        if(playback!=null){playback.background();if(playback.listener==playbackChanged)playback.listener=null;}
        if(serviceBound){unbindService(playbackConnection);serviceBound=false;}clearVideoOutputCache();playback=null;player=null;
    }
    @Override public void onStart(){super.onStart();appVisible=true;scheduleAccentWave();syncDisc();if(artwork!=null)artwork.setActive(true);main.removeCallbacks(ticker);main.post(ticker);if(!serviceBound)serviceBound=bindService(new Intent(this,PlaybackService.class),playbackConnection,Context.BIND_AUTO_CREATE);}
    @Override public void onDestroy(){cancelPlayHolds();stopDiscAnimation();if(reactiveGlow!=null)reactiveGlow.stop();destroyed=true;generation++;main.removeCallbacksAndMessages(null);if(playback!=null&&playback.listener==playbackChanged)playback.listener=null;if(serviceBound){unbindService(playbackConnection);serviceBound=false;}if(!serviceEq&&equalizer!=null)equalizer.close();io.shutdownNow();if(artwork!=null)artwork.close();super.onDestroy();}
}
