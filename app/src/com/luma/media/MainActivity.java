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
    final ThreadPoolExecutor io = new ThreadPoolExecutor(2,2,20,TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>());
    {io.allowCoreThreadTimeOut(true);}
    final ArrayList<Item> localMusic = new ArrayList<>(), localVideo = new ArrayList<>(), online = new ArrayList<>(), saved = new ArrayList<>(), shown = new ArrayList<>();
    SharedPreferences prefs;
    ArtworkLoader artwork;
    NativeEq equalizer;
    String eqSettingsState="";
    GridView grid;
    SwipeDeck deck;
    TextView emptyMedia;
    int libraryHeight=400,gridColumns=4;
    MiniPlayerCard miniCard;
    FrameLayout inlineVideoStage;
    AutoHideControls inlineControls;
    ImageView miniArtwork;
    String miniArtworkUrl="";
    boolean fullscreenWasExpanded;
    boolean settingsOpen;
    boolean homeOpen=true,homeLocalLoading,homeOnlineLoading,homePendingRefresh;
    int homeEpoch,homeScrollY;
    long homeLoadedAt;
    String homeDataKey="",homeCallsign="";
    HomeFeed.Local homeLocal;
    HomeFeed.Online homeOnline;
    ScrollView homeScroll;
    LinearLayout homeContent;
    String settingsCategory="";
    View sectionPage,animatedSection;
    boolean sectionTransitioning;
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
    Button playButton;
    ImageButton sortButton,favoriteButton;
    Button setupPrimary,activeSourceTab,homeShuffleButton;
    int deviceAccent;
    boolean accentWaveScheduled;
    final Set<Switch> accentSwitches=Collections.newSetFromMap(new WeakHashMap<Switch,Boolean>());
    final Runnable accentWave=new Runnable(){public void run(){accentWaveScheduled=false;if(destroyed||!appVisible||!"rgb".equals(accentStyle()))return;applyAccentWave();accentWaveScheduled=true;main.postDelayed(this,200);}};
    SeekBar seek;
    View surface,attachedVideoView;
    MediaPlayer attachedVideoPlayer;
    Surface attachedVideoSurface;
    final RoundedVideoView.RetainedOutput videoOutput=new RoundedVideoView.RetainedOutput();
    ItemAdapter adapter;
    MediaPlayer player;

    Item current;
    PlaybackQueue<Item> queue = new PlaybackQueue<>(item->item.url);
    boolean prepared, playing, isVideo, dragging, dark, landscape, showingSaved, destroyed;
    int tab=0, generation=0, bg, pageBottom, normalBg, panel, ink, muted, accent, accentInk, line;
    String query="", country="ph", fmCountry="all", status="Your library, beautifully simple.", sourceStatus="";
    LinearLayout mediaSearchBox,mediaSearchResults,mediaSearchPanel,searchLibrary;
    EditText mediaSearchField;
    TextView mediaSearchStatus;
    FrameLayout portraitSearchOverlay;
    ImageButton mediaSearchToggle;
    android.animation.ValueAnimator portraitSearchExpansion;
    int portraitSearchHeight;
    boolean mediaSearchVisible,searchLocalLoading,searchOnlineLoading,suppressSearchEdits;
    String mediaSearchQuery="",searchNote="";
    int searchScope=-1,searchEpoch;
    long searchLastTouch;
    Runnable mediaSearchPending;
    Future<?> mediaSearchLocalTask,mediaSearchOnlineTask;
    final ArrayList<Item> searchResults=new ArrayList<>();
    final SearchSnapshot<Item> searchSongs=new SearchSnapshot<>(),searchVideos=new SearchSnapshot<>();
    final Runnable hideMediaSearch=()->dismissMediaSearch();

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
    final Runnable playbackChanged=()->syncPlayback(false);
    final ServiceConnection playbackConnection=new ServiceConnection(){
        @Override public void onServiceConnected(ComponentName name,IBinder binder){
            if(!serviceBound||!appVisible)return;
            playback=((PlaybackService.LocalBinder)binder).service();if(appVisible)playback.foregroundUi();else playback.background();
            if(prefs.getBoolean("pendingPlaybackReset",false)){playback.listener=null;playback.stopPlayback();playback.queue.setRepeatMode(PlaybackQueue.OFF);playback.queue.setShuffle(false,null);prefs.edit().remove("pendingPlaybackReset").apply();}
            if(!serviceEq&&equalizer!=null)equalizer.close();
            equalizer=playback.equalizer;serviceEq=true;queue=playback.queue;playback.listener=playbackChanged;syncPlayback(true);
        }
        @Override public void onServiceDisconnected(ComponentName name){playback=null;player=null;prepared=false;playing=false;updatePlayer();}
    };
    void syncPlayback(boolean rebuild){
        if(playback==null||destroyed)return;
        boolean previousVideo=isVideo;equalizer=playback.equalizer;
        player=playback.player;current=playback.current;prepared=playback.prepared;playing=playback.playing;isVideo=playback.isVideo;status=playback.status;
        if(pendingNowPlaying){pendingNowPlaying=false;if(current!=null&&!tvMode){expanded=true;settingsOpen=false;rebuild=true;}}
        if(tvMode&&!fullscreenVideoAvailable()){exitTV();return;}
        if(expanded&&current==null){if(previousVideo){expanded=false;rebuild=true;}else main.postDelayed(()->{if(!destroyed&&current==null&&expanded){expanded=false;render();}},450);}
        if((expanded||homeOpen)&&previousVideo!=isVideo)rebuild=true;
        if(settingsOpen&&"audio".equals(settingsCategory)&&!eqSettingsState.equals(equalizerUiState()))rebuild=true;
        if(rebuild){render();if(list!=null)list.setSelectionFromTop(libraryPosition,libraryOffset);if(grid!=null)grid.setSelection(libraryPosition);}
        if(playing&&isVideo&&appVisible)getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(player!=null&&isVideo&&appVisible)attachVideoOutput();
        if(player!=null&&isVideo&&prepared)try{fitVideo(player.getVideoWidth(),player.getVideoHeight());}catch(Exception ignored){}
        if(!isVideo)clearVideoOutputCache();updatePlayer();
    }
    boolean playbackReady(){if(playback!=null)return true;toast("Connecting to the player. Please tap again.");return false;}
    void playbackPermission(){if(Build.VERSION.SDK_INT>=33&&!allowed(Manifest.permission.POST_NOTIFICATIONS)&&!prefs.getBoolean("askedPlaybackNotifications",false)){prefs.edit().putBoolean("askedPlaybackNotifications",true).apply();requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},30);}}
    void updateArtworkMotion(){if(miniArtwork instanceof SpinningThumbnailView)((SpinningThumbnailView)miniArtwork).playback(current!=null,playing&&appVisible&&android.animation.ValueAnimator.areAnimatorsEnabled());}
    void configureArtwork(SpinningThumbnailView image){
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
        prefs=getSharedPreferences("luma",0);fmCountry=RadioDirectory.normalize(prefs.getString("fmCountry","all"));
        boolean upgraded=false;try{android.content.pm.PackageInfo info=getPackageManager().getPackageInfo(getPackageName(),0);upgraded=info.lastUpdateTime-info.firstInstallTime>1000;}catch(Exception ignored){}
        boolean existing=upgraded||!prefs.getAll().isEmpty();
        setupOpen=MediaAccessPolicy.needsSetup(prefs.getBoolean("setupComplete",false),prefs.getBoolean("setupStarted",false),existing);
        if(setupOpen){setupStep=prefs.getInt("setupFlowVersion",0)==3?Math.max(0,Math.min(4,prefs.getInt("setupStep",0))):0;prefs.edit().putBoolean("setupStarted",true).putInt("setupFlowVersion",3).apply();}
        else if(!prefs.contains("setupComplete"))prefs.edit().putBoolean("setupComplete",true).apply();
        retireCustomFolders();
        queue.setRepeatMode(prefs.getInt("repeatMode",PlaybackQueue.OFF));queue.setShuffle(prefs.getBoolean("shuffle",false),null);
        equalizer=new NativeEq(prefs);
        artwork=new ArtworkLoader(this,prefs);
        reactiveGlow=new AudioReactiveGlow(new AudioReactiveGlow.Listener(){public void energy(float low,float mid,float high){glowUnavailable=false;glowLow=AudioGlowMath.clamp(low);glowMid=AudioGlowMath.clamp(mid);glowHigh=AudioGlowMath.clamp(high);if(miniCard!=null)miniCard.energy((glowLow+glowMid+glowHigh)/3f);for(DiscView view:new ArrayList<>(discViews))if(current!=null&&current.url.equals(view.mediaUrl)&&view.isShown())view.energy(glowLow,glowMid,glowHigh);}public void unavailable(){glowUnavailable=true;clearDiscEnergy();}});
        if(state!=null){tab=state.getInt("tab",0); query=state.getString("query","");country=state.getString("country","ph");fmCountry=RadioDirectory.normalize(state.getString("fmCountry",fmCountry));settingsOpen=state.getBoolean("settings",false);homeOpen=state.getBoolean("home",true);homeCallsign=state.getString("homeCallsign","");homeScrollY=state.getInt("homeScrollY",0);settingsCategory=SettingsNavigation.normalize(state.getString("settingsCategory",""));settingsScrollY=state.getInt("settingsScrollY",0);settingsAnchor=state.getString("settingsAnchor",null);settingsAnchorOffset=state.getInt("settingsAnchorOffset",0);returningFromAccessSettings=state.getBoolean("returningFromAccessSettings",false);}
        font=Typeface.createFromAsset(getAssets(),"Manrope.ttf");
        loadSaved();
        render();
        if(!setupOpen&&!homeOpen){if(tab==0||tab==2)loadLocal();else loadOnline();}
        main.post(ticker);
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);pendingNowPlaying=intent.getBooleanExtra("openNowPlaying",false);if(playback!=null)syncPlayback(true);}
    @Override public void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);rememberSettingsScroll();b.putInt("settingsScrollY",settingsScrollY);b.putString("settingsAnchor",settingsAnchor);b.putInt("settingsAnchorOffset",settingsAnchorOffset);b.putInt("tab",tab);b.putString("query",query);b.putString("country",country);b.putString("fmCountry",fmCountry);b.putBoolean("settings",settingsOpen);b.putBoolean("home",homeOpen);b.putString("homeCallsign",homeCallsign);b.putInt("homeScrollY",homeScrollY);b.putString("settingsCategory",settingsCategory);b.putBoolean("returningFromAccessSettings",returningFromAccessSettings);}
    int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    void colors(){
        dark=AppearanceMode.dark(prefs.getString("appearanceMode","system"),(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
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
    GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();if(color==accent&&"cutie".equals(accentStyle())){d.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);d.setColors(new int[]{0xFFF4C2D7,Color.WHITE,0xFFB0D8ED});}else d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    TextView text(String s,int sp,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(sp);v.setTextColor(color);v.setFontFeatureSettings("kern");v.setTypeface(font);if(settingsOpen&&!setupOpen){v.setFontVariationSettings("'wght' 500");if(color==muted){v.setTextSize(Math.max(13,sp));v.setTextColor(SurfacePalette.mix(muted,ink,.25f));}}return v;}
    TextView spacedSettingsNote(String label,int sp,int color){TextView note=text(label,sp,color);note.setPadding(0,dp(6),0,0);return note;}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    void padding(View v,int x,int y){v.setPadding(dp(x),dp(y),dp(x),dp(y));}
    Button button(String label,Runnable r){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(12);b.setTypeface(font);b.setTextColor(ink);b.setBackground(round(panel,16));b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setStateListAnimator(null);b.setMinWidth(0);b.setMinimumWidth(0);padding(b,12,1);b.setOnClickListener(v->r.run());return b;}
    ImageButton toolIcon(int kind,String label,Runnable action){ImageButton b=new ImageButton(this);b.setImageDrawable(new FluentIcon(kind,ink));b.setScaleType(ImageView.ScaleType.CENTER_INSIDE);b.setBackground(round(panel,16));padding(b,12,12);b.setContentDescription(label);b.setTooltipText(label);b.setOnClickListener(v->action.run());return b;}
    int selectedIconColor(){double foreground=.2126*channel(Color.red(accent))+.7152*channel(Color.green(accent))+.0722*channel(Color.blue(accent)),background=.2126*channel(Color.red(panel))+.7152*channel(Color.green(panel))+.0722*channel(Color.blue(panel));return (Math.max(foreground,background)+.05)/(Math.min(foreground,background)+.05)>=3?accent:ink;}
    GradientDrawable mediaSelector(){
        if("cutie".equals(accentStyle())||"tuhkar".equals(accentStyle())){int orange=AccentStyle.hoverAccent(accentStyle(),accent);GradientDrawable selected=round(LightSurfaceStyle.selectorFill(panel,orange,ink,muted),14);selected.setStroke(dp(1),orange);return selected;}
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
        ImageButton region=toolIcon(15,"Region: "+(tab==1?RadioDirectory.label(fmCountry):country.toUpperCase(Locale.ROOT)),()->chooseCountry());region.setBackgroundColor(Color.TRANSPARENT);region.setStateListAnimator(null);padding(region,14,14);header.addView(region,new LinearLayout.LayoutParams(dp(48),dp(48)));
    }
    void render(){
        cancelSectionTransition();if(portraitSearchExpansion!=null){portraitSearchExpansion.cancel();portraitSearchExpansion=null;}if(portraitSearchOverlay!=null){portraitSearchOverlay.animate().cancel();if(portraitSearchOverlay.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)portraitSearchOverlay.getParent()).removeView(portraitSearchOverlay);portraitSearchOverlay=null;}portraitSearchHeight=0;main.removeCallbacks(hideMediaSearch);mediaSearchBox=null;mediaSearchResults=null;mediaSearchPanel=null;searchLibrary=null;mediaSearchField=null;mediaSearchStatus=null;mediaSearchToggle=null;if(miniArtwork instanceof SpinningThumbnailView)((SpinningThumbnailView)miniArtwork).playback(false,false);sectionPage=null;dragging=false;miniCard=null;main.removeCallbacks(hideInlineControls);inlineVideoStage=null;inlineControls=null;miniArtwork=null;miniArtworkUrl="";nowTitle=null;nowDetail=null;seek=null;time=null;playButton=null;repeatButton=null;shuffleButton=null;
        videoOutput.setRetaining(appVisible&&isVideo&&current!=null&&!settingsOpen&&!setupOpen&&!tvMode&&!resetInProgress);
        rememberSettingsScroll();settingsScroll=null;settingsPage=null;
        cancelPlayHolds();stopDiscAnimation();accentSwitches.clear();setupPrimary=null;activeSourceTab=null;homeShuffleButton=null;colors();scheduleAccentWave();setTheme(dark?com.luma.media.R.style.DarkAppTheme:com.luma.media.R.style.AppTheme);root=column();applyPageBackground();setContentView(root);
        // Android 15 enforces edge-to-edge: apply the actual system bar and keyboard insets.
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);}
            else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            updatePortraitSearchOverlay();
            return insets;
        });root.requestApplyInsets();
        if(resetInProgress){LinearLayout wait=column();padding(wait,22,24);wait.addView(text("Resetting Tuhkar…",24,ink));wait.addView(text("Only app settings and cache are being cleared. Your media files are safe.",14,muted));root.addView(wait);return;}
        if(setupOpen){renderSetup();return;}
        if(tvMode){renderTV();return;}
        if(expanded&&current!=null){if(isVideo)renderVideoExpanded();else renderExpanded();return;}
        if(settingsOpen){renderSettings();if(!landscape)addBottomNav();return;}
        if(homeOpen){renderHome();return;}
        body=row();body.setGravity(Gravity.TOP);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        if(landscape)addNavRail(body);
        content=column();sectionPage=content;padding(content,16,landscape?8:12);body.addView(content,new LinearLayout.LayoutParams(0,-1,1));
        LinearLayout brand=row();
        if(!landscape){HeaderWordmark logo=new HeaderWordmark(this);logo.setPadding(0,0,dp(8),0);brand.addView(logo,new LinearLayout.LayoutParams(0,dp(48),1));}
        else{headline=text(tab<2?"Music":"Video",27,ink);bold(headline);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-2,dp(48));hp.rightMargin=dp(12);headline.setGravity(Gravity.CENTER_VERTICAL);brand.addView(headline,hp);addSourceTabs(brand,true);}
        ImageButton refresh=toolIcon(12,tab==0?"Refresh music library":tab==1?"Refresh radio stations":tab==2?"Refresh video library":"Refresh TV channels",()->refreshSection());refresh.setBackgroundColor(Color.TRANSPARENT);refresh.setStateListAnimator(null);padding(refresh,12,12);LinearLayout.LayoutParams refreshParams=new LinearLayout.LayoutParams(dp(48),dp(48));refreshParams.leftMargin=dp(8);brand.addView(refresh,refreshParams);addMediaSearchIcon(brand);content.addView(brand);addMediaSearch(content);
        if(!landscape){headline=text(tab<2?"Music":"Video",36,ink);bold(headline);padding(headline,0,4);content.addView(headline);}
        subtitle=text(tab==0?"On your device. In your element.":tab==1?"A world of sound, one station away.":tab==2?"Your collection, on the big picture.":"Tune in to something new.",12,muted);content.addView(subtitle);if(landscape)subtitle.setVisibility(View.GONE);if(!landscape)addSourceTabs(content);
        attachLibrarySearchResults(content);
        notice=text("",11,muted);padding(notice,8,5);if(tab==1||tab==3){notice.setMaxLines(2);notice.setEllipsize(TextUtils.TruncateAt.END);}
        LinearLayout library=column();searchLibrary=library;library.setBackground(round(panel,24));library.setClipToOutline(true);padding(library,8,8);LinearLayout header=row();header.addView(notice,new LinearLayout.LayoutParams(0,-2,1));addRemoteHeaderTools(header);
        ImageButton favorite=new ImageButton(this);favoriteButton=favorite;favorite.setImageDrawable(new FluentIcon(16,showingSaved?selectedIconColor():muted));favorite.setScaleType(ImageView.ScaleType.CENTER_INSIDE);favorite.setBackgroundColor(Color.TRANSPARENT);favorite.setStateListAnimator(null);padding(favorite,12,12);favorite.setSelected(showingSaved);favorite.setContentDescription(showingSaved?"Saved filter on. Show all media":"Show saved media");favorite.setTooltipText(showingSaved?"Show all media":"Show saved media");favorite.setOnClickListener(v->{showingSaved=!showingSaved;render();refreshList();});LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(dp(48),dp(48));header.addView(favorite,fp);
        ImageButton sort=toolIcon(18,"Sort media",()->showSort());sortButton=sort;sort.setBackgroundColor(Color.TRANSPARENT);padding(sort,14,14);sort.setTooltipText("Sort: "+sortNames()[Math.max(0,Math.min(3,prefs.getInt("sortOrder",3)))]);header.addView(sort,new LinearLayout.LayoutParams(dp(48),dp(48)));library.addView(header,new LinearLayout.LayoutParams(-1,-2));LinearLayout.LayoutParams libraryParams=new LinearLayout.LayoutParams(-1,0,1);libraryParams.topMargin=dp(8);content.addView(library,libraryParams);
        buildLibrary(library);
        playerBox=column();playerBox.setPadding(dp(landscape?12:0),dp(2),dp(landscape?12:0),0);playerBox.setOnClickListener(v->openExpanded());playerBox.setContentDescription((current==null?"Player":current.title)+". "+status+". Tap to open Now Playing.");playerBox.setBackgroundColor(Color.TRANSPARENT);if(landscape&&tab>=2){playerBox.setGravity(Gravity.CENTER_VERTICAL);playerBox.setPadding(dp(12),dp(12),dp(12),dp(12));LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(dp(Math.max(180,Math.min(320,getResources().getConfiguration().screenWidthDp*.34f))),-1);body.addView(playerBox,pp);}else if(landscape){playerBox.setPadding(0,dp(8),0,0);content.addView(playerBox,new LinearLayout.LayoutParams(-1,-2));}else{LinearLayout.LayoutParams miniParams=new LinearLayout.LayoutParams(-1,-2);miniParams.setMargins(dp(16),0,dp(16),0);root.addView(playerBox,miniParams);}
        videoBox=column();LinearLayout.LayoutParams videoParams=new LinearLayout.LayoutParams(-1,-2);videoParams.bottomMargin=dp(12);playerBox.addView(videoBox,videoParams);videoBox.setGravity(Gravity.CENTER);videoBox.setBackground(round(Color.BLACK,12));videoBox.setClipToOutline(true);addRoundedPicture(videoBox,dp(landscape?150:Math.min(190,getResources().getConfiguration().screenHeightDp*.24f)));surface.setContentDescription("TV preview. Double-tap for landscape fullscreen.");videoBox.setVisibility(inlineVideoVisible()?View.VISIBLE:View.GONE);
        installInlineVideoGesture();
        videoBox.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(!tvMode&&player!=null&&prepared&&isVideo)try{fitVideo(player.getVideoWidth(),player.getVideoHeight());}catch(Exception ignored){}});
        if(tab<2)addAudioMiniPlayer(playerBox);else addInlineControls();
        updateLandscapeVideoPane();
        if(!landscape)addBottomNav();
        refreshList();updateLibrarySearchLayout();
    }
    void openHome(){
        generation++;if(mediaSearchPending!=null)main.removeCallbacks(mediaSearchPending);homeOpen=true;settingsOpen=false;expanded=false;homeCallsign=HomeDiscovery.callsign(new Random());homeScrollY=0;render();
    }
    void ensureHomeData(boolean force){
        if(destroyed||!homeOpen||settingsOpen||expanded||tvMode||!appVisible||setupOpen)return;
        String key=fmCountry+":"+country+":"+musicAccess()+":"+videoAccess()+":"+selectedVideoAccess()+":"+standardFolderOnly(0)+":"+standardFolderOnly(2);
        if(homeLocalLoading||homeOnlineLoading){if(force||!key.equals(homeDataKey))homePendingRefresh=true;return;}
        if(!force&&key.equals(homeDataKey)&&SystemClock.elapsedRealtime()-homeLoadedAt<600000&&homeLocal!=null&&homeOnline!=null)return;
        homePendingRefresh=false;homeDataKey=key;homeLocalLoading=homeOnlineLoading=true;final int token=++homeEpoch;
        final boolean music=musicAccess(),video=videoAccess()||selectedVideoAccess(),musicOnly=standardFolderOnly(0),moviesOnly=standardFolderOnly(2);
        final String fmRegion=fmCountry,tvRegion=country;final ArrayList<Item> savedSnapshot=new ArrayList<>(saved);
        fillHomeSections();
        io.execute(()->{HomeFeed.Local result=HomeFeed.local(getApplicationContext(),music,video,musicOnly,moviesOnly,new Random());main.post(()->{if(destroyed||token!=homeEpoch)return;homeLocal=result;homeLocalLoading=false;finishHomeUpdate();});});
        io.execute(()->{HomeFeed.Online result=HomeFeed.online(this,fmRegion,tvRegion,savedSnapshot,new Random());main.post(()->{if(destroyed||token!=homeEpoch)return;homeOnline=result;homeOnlineLoading=false;finishHomeUpdate();});});
    }
    void finishHomeUpdate(){if(!homeLocalLoading&&!homeOnlineLoading){homeLoadedAt=SystemClock.elapsedRealtime();if(homePendingRefresh){homePendingRefresh=false;homeLoadedAt=0;ensureHomeData(true);}}if(appVisible)fillHomeSections();}
    void renderHome(){
        list=null;grid=null;deck=null;adapter=null;surface=null;videoBox=null;
        body=row();root.addView(body,new LinearLayout.LayoutParams(-1,0,1));if(landscape)addNavRail(body);
        LinearLayout page=column();padding(page,landscape?16:0,landscape?8:12);body.addView(page,new LinearLayout.LayoutParams(0,-1,1));sectionPage=page;
        homeScroll=new ScrollView(this);homeScroll.setFillViewport(true);homeScroll.setVerticalScrollBarEnabled(false);page.addView(homeScroll,new LinearLayout.LayoutParams(-1,0,1));homeContent=column();homeScroll.addView(homeContent);homeScroll.setOnScrollChangeListener((v,x,y,ox,oy)->homeScrollY=y);fillHomeSections();
        playerBox=column();playerBox.setPadding(dp(landscape?0:16),dp(8),dp(landscape?0:16),0);page.addView(playerBox,new LinearLayout.LayoutParams(-1,-2));
        if(current!=null&&isVideo){videoBox=column();videoBox.setGravity(Gravity.CENTER);videoBox.setBackground(round(Color.BLACK,12));videoBox.setClipToOutline(true);playerBox.addView(videoBox);addRoundedPicture(videoBox,dp(landscape?150:160));installInlineVideoGesture();videoBox.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(player!=null&&prepared)try{fitVideo(player.getVideoWidth(),player.getVideoHeight());}catch(Exception ignored){}});addInlineControls();}else addAudioMiniPlayer(playerBox);
        if(!landscape)addBottomNav();main.post(()->ensureHomeData(false));
    }
    void homeIntro(LinearLayout parent){
        LinearLayout intro=column();padding(intro,landscape?0:16,0);parent.addView(intro,new LinearLayout.LayoutParams(-1,-2));parent=intro;
        LinearLayout branding=row(),welcome=column();if(!landscape){HeaderWordmark wordmark=new HeaderWordmark(this);branding.addView(wordmark,new LinearLayout.LayoutParams(0,dp(48),1));}else branding.addView(welcome,new LinearLayout.LayoutParams(0,-2,1));
        TextView greeting=text(HomeDiscovery.greeting(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)),13,muted);greeting.setPadding(0,dp(landscape?0:10),0,dp(2));welcome.addView(greeting);
        if(!HomeDiscovery.validCallsign(homeCallsign))homeCallsign=HomeDiscovery.callsign(new Random());TextView callsign=text(homeCallsign,landscape?24:28,ink);semibold(callsign);welcome.addView(callsign);
        ImageButton refresh=new ImageButton(this);refresh.setImageDrawable(new FluentIcon(12,ink));refresh.setBackgroundColor(Color.TRANSPARENT);padding(refresh,12,12);refresh.setContentDescription("Refresh Home discoveries");refresh.setTooltipText("Refresh Home");refresh.setOnClickListener(v->{homeCallsign=HomeDiscovery.callsign(new Random());homeLoadedAt=0;searchSongs.clear();searchVideos.clear();ensureHomeData(true);fillHomeSections();if(!mediaSearchQuery.trim().isEmpty())runMediaSearch();});branding.addView(refresh,new LinearLayout.LayoutParams(dp(48),dp(48)));addMediaSearchIcon(branding);parent.addView(branding);addMediaSearch(parent);
        if(!landscape)parent.addView(welcome);
        LinearLayout actions=row();actions.setPadding(0,dp(landscape?10:18),0,dp(4));parent.addView(actions);
        homeShuffleButton=button("Shuffle picks",()->playHomeSongPicks(true));homeShuffleButton.setTextSize(14);bold(homeShuffleButton);homeShuffleButton.setTextColor(accentInk);homeShuffleButton.setBackground(round(accent,26));FluentIcon shuffle=new FluentIcon(9,accentInk);shuffle.setBounds(0,0,dp(18),dp(18));homeShuffleButton.setCompoundDrawables(shuffle,null,null,null);homeShuffleButton.setCompoundDrawablePadding(dp(8));
        LinearLayout.LayoutParams first=new LinearLayout.LayoutParams(0,dp(48),1);first.rightMargin=dp(10);actions.addView(homeShuffleButton,first);
        Button play=button("▶  Play picks",()->playHomeSongPicks(false));play.setTextSize(14);bold(play);GradientDrawable outline=round(Color.TRANSPARENT,26);outline.setStroke(dp(1),line);play.setBackground(outline);actions.addView(play,new LinearLayout.LayoutParams(0,dp(48),1));
    }
    void playHomeSongPicks(boolean shuffle){if(homeLocal==null||homeLocal.songs.isEmpty()){toast("No song picks yet. Allow media access or wait for loading.");return;}ArrayList<Item> choices=new ArrayList<>(homeLocal.songs);if(shuffle)Collections.shuffle(choices);playHomeItem(choices.get(0),choices);}
    LinearLayout homeSection(LinearLayout parent,String title,String summary){return homeSection(parent,title,summary,null,"");}
    LinearLayout homeSection(LinearLayout parent,String title,String summary,Runnable browse,String actionLabel){
        LinearLayout section=column();LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(landscape?16:24);parent.addView(section,params);LinearLayout header=row();int inset=!landscape&&browse!=null?dp(16):0;header.setPadding(inset,0,inset,0);TextView label=text(title,21,ink);bold(label);header.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        if(browse!=null){Button action=button(actionLabel,browse);action.setTextColor(selectedIconColor());action.setBackgroundColor(Color.TRANSPARENT);semibold(action);header.addView(action,new LinearLayout.LayoutParams(-2,dp(48)));}section.addView(header);
        TextView note=text(summary,12,muted);note.setMaxLines(3);note.setPadding(inset,dp(4),inset,dp(12));section.addView(note);return section;
    }
    void homeShelf(LinearLayout parent,List<Item> items,String empty){
        if(items.isEmpty()){TextView note=text(empty,13,muted);padding(note,landscape?0:16,10);parent.addView(note);return;}
        HorizontalScrollView shelf=new HorizontalScrollView(this);shelf.setHorizontalScrollBarEnabled(false);shelf.setHorizontalFadingEdgeEnabled(landscape);shelf.setFadingEdgeLength(dp(24));shelf.setNestedScrollingEnabled(true);shelf.setClipToPadding(false);LinearLayout cards=row();cards.setGravity(Gravity.TOP);cards.setPadding(dp(landscape?0:16),0,dp(landscape?0:16),0);shelf.addView(cards,new FrameLayout.LayoutParams(-2,-2));parent.addView(shelf,new LinearLayout.LayoutParams(-1,-2));
        int width=dp(HomeShelfLayout.tileWidth(getResources().getConfiguration().screenWidthDp,landscape));
        for(Item item:items){LinearLayout tile=column();LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(width,-2);tp.rightMargin=item==items.get(items.size()-1)?0:dp(12);cards.addView(tile,tp);
            ImageView cover=new ImageView(this);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);cover.setBackground(round(normalBg,16));cover.setClipToOutline(true);cover.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);artwork.bind(cover,item,accent);tile.addView(cover,new LinearLayout.LayoutParams(width,width));
            TextView title=text(item.title,14,ink);semibold(title);title.setSingleLine(true);title.setEllipsize(TextUtils.TruncateAt.END);title.setPadding(0,dp(8),0,0);tile.addView(title);TextView detail=text(item.artist.isEmpty()?item.detail:item.artist,12,muted);detail.setSingleLine(true);detail.setEllipsize(TextUtils.TruncateAt.END);tile.addView(detail);
            tile.setFocusable(true);tile.setContentDescription(item.title+". "+item.detail+". Tap to play; hold to save.");tile.setOnClickListener(v->playHomeItem(item,items));tile.setOnLongClickListener(v->{saveToggle(item);return true;});
        }
    }
    void homeItems(LinearLayout parent,List<Item> items,String empty){
        if(items.isEmpty()){TextView note=text(empty,13,muted);padding(note,0,10);parent.addView(note);return;}
        for(Item item:items){LinearLayout entry=row();padding(entry,0,6);entry.setMinimumHeight(dp(60));ThumbnailView cover=new ThumbnailView(this);cover.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);artwork.bind(cover,item,accent);entry.addView(cover,new LinearLayout.LayoutParams(dp(44),dp(44)));
            LinearLayout words=column();padding(words,10,0);TextView title=text(item.title,14,ink);semibold(title);title.setSingleLine(true);title.setEllipsize(TextUtils.TruncateAt.END);words.addView(title);TextView detail=text(item.detail,11,muted);detail.setMaxLines(2);detail.setEllipsize(TextUtils.TruncateAt.END);words.addView(detail);entry.addView(words,new LinearLayout.LayoutParams(0,-2,1));TextView playHint=text("▶",17,selectedIconColor());playHint.setGravity(Gravity.CENTER);playHint.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);entry.addView(playHint,new LinearLayout.LayoutParams(dp(40),dp(48)));
            entry.setFocusable(true);entry.setBackground(round(Color.TRANSPARENT,14));entry.setContentDescription(item.title+". "+item.kind+". Tap to play; hold to save.");entry.setOnClickListener(v->playHomeItem(item,items));entry.setOnLongClickListener(v->{saveToggle(item);return true;});parent.addView(entry,new LinearLayout.LayoutParams(-1,-2));
        }
    }
    void playHomeItem(Item item,List<Item> choices){if(!playbackReady())return;ArrayList<Item> sameKind=new ArrayList<>();for(Item choice:choices)if(choice.kind.equals(item.kind))sameKind.add(choice);queue.reset(sameKind,item);start(item);}
    void fillHomeSections(){
        if(!homeOpen||settingsOpen||expanded||tvMode||homeContent==null||homeScroll==null)return;int scrollY=homeScrollY;homeContent.removeAllViews();homeIntro(homeContent);
        LinearLayout columns=landscape?row():column();columns.setGravity(Gravity.TOP);homeContent.addView(columns,new LinearLayout.LayoutParams(-1,-2));LinearLayout discoveries=column(),recommendations=column();padding(recommendations,landscape?0:16,0);
        if(landscape){LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,-2,1);left.rightMargin=dp(12);columns.addView(recommendations,left);columns.addView(discoveries,new LinearLayout.LayoutParams(0,-2,1));}else{columns.addView(discoveries);columns.addView(recommendations);}
        LinearLayout recent=homeSection(discoveries,"Recently Added",homeLocalLoading?"Reading your granted local media…":homeLocal==null?"Local media not loaded yet.":homeLocal.message,()->dialog("Open library").setItems(new String[]{"Music","Videos"},(d,w)->switchTab(w==0?0:2)).show(),"See all");
        homeShelf(recent,homeLocal==null?Collections.emptyList():homeLocal.recent,homeLocalLoading?"Loading local media…":"No readable dated media yet. Check media access and folder filters in Settings.");
        LinearLayout updates=homeSection(discoveries,"Recent FM updates",homeOnlineLoading?"Checking directory changes for "+RadioDirectory.label(fmCountry)+"…":homeOnline==null?"Station directory not loaded yet.":homeOnline.fmMessage,()->switchTab(1),"Browse FM");
        homeShelf(updates,homeOnline==null?Collections.emptyList():homeOnline.updates,homeOnlineLoading?"Loading directory changes…":"No fresh directory entries available. Refresh when connected.");
        TextView title=text("Media you might like",22,ink);bold(title);title.setPadding(0,dp(landscape?16:18),0,0);recommendations.addView(title);TextView randomNote=text("Random picks, not listening-profile recommendations. Refresh for another mix.",12,muted);recommendations.addView(randomNote);
        LinearLayout songs=homeSection(recommendations,"Songs","From your accessible local music");homeItems(songs,homeLocal==null?Collections.emptyList():homeLocal.songs,"No songs available yet.");if(homeLocal!=null&&homeLocal.songs.size()<6)songs.addView(text(homeLocal.songs.size()+" of 6 available · grant access or add more songs.",12,muted));
        LinearLayout fm=homeSection(recommendations,"FM",homeOnline==null?"Public station discoveries":homeOnline.fmMessage);homeItems(fm,homeOnline==null?Collections.emptyList():homeOnline.fm,"No station picks available offline.");if(homeOnline!=null&&homeOnline.fm.size()<2)fm.addView(text(homeOnline.fm.size()+" of 2 available.",12,muted));
        LinearLayout video=homeSection(recommendations,"Video","From your accessible local videos");homeItems(video,homeLocal==null?Collections.emptyList():homeLocal.videos,"No video pick available. Check media access/folder filters.");
        LinearLayout tv=homeSection(recommendations,"TV",homeOnline==null?"Public channel directory":homeOnline.tvMessage);homeItems(tv,homeOnline==null?Collections.emptyList():homeOnline.tv,"No channel pick available. Check connection or saved channels.");
        final ScrollView scroll=homeScroll;scroll.post(()->{if(homeScroll==scroll&&homeOpen)scroll.scrollTo(0,scrollY);});
    }
    void ensurePortraitSearchOverlay(){
        if(portraitSearchOverlay!=null)return;
        android.view.ViewGroup host=findViewById(android.R.id.content);
        portraitSearchOverlay=new FrameLayout(this);portraitSearchOverlay.setBackgroundColor(0x66000000);portraitSearchOverlay.setVisibility(View.GONE);portraitSearchOverlay.setClickable(true);portraitSearchOverlay.setOnClickListener(v->dismissMediaSearch());portraitSearchOverlay.setOnTouchListener((v,e)->{if(e.getActionMasked()==MotionEvent.ACTION_UP&&mediaSearchToggle!=null){int[] location=new int[2];mediaSearchToggle.getLocationOnScreen(location);if(e.getRawX()>=location[0]&&e.getRawX()<location[0]+mediaSearchToggle.getWidth()&&e.getRawY()>=location[1]&&e.getRawY()<location[1]+mediaSearchToggle.getHeight()){toggleMediaSearch();return true;}}return false;});
        host.addView(portraitSearchOverlay,new android.view.ViewGroup.LayoutParams(-1,-1));
        portraitSearchOverlay.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(r-l!=or-ol||b-t!=ob-ot)updatePortraitSearchOverlay();});
    }
    void updatePortraitSearchOverlay(){
        if(portraitSearchOverlay==null||mediaSearchPanel==null)return;
        boolean active=mediaSearchVisible||!mediaSearchQuery.trim().isEmpty();
        root.setImportantForAccessibility(active?View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS:View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        if(!active){
            if(portraitSearchOverlay.getVisibility()==View.VISIBLE){final FrameLayout overlay=portraitSearchOverlay;overlay.animate().cancel();if(android.animation.ValueAnimator.areAnimatorsEnabled())overlay.animate().alpha(0).setDuration(160).withEndAction(()->{if(overlay==portraitSearchOverlay&&!mediaSearchVisible&&mediaSearchQuery.trim().isEmpty()){overlay.setVisibility(View.GONE);overlay.setAlpha(1);}}).start();else overlay.setVisibility(View.GONE);}
            return;
        }
        portraitSearchOverlay.animate().cancel();if(portraitSearchOverlay.getVisibility()!=View.VISIBLE){portraitSearchOverlay.setVisibility(View.VISIBLE);portraitSearchOverlay.setAlpha(android.animation.ValueAnimator.areAnimatorsEnabled()?0:1);portraitSearchOverlay.animate().alpha(1).setDuration(160).start();}else portraitSearchOverlay.setAlpha(1);
        int left=root.getPaddingLeft()+dp(landscape?96:16),right=root.getPaddingRight()+dp(landscape?112:16);
        int freeWidth=Math.max(1,portraitSearchOverlay.getWidth()-left-right),panelWidth=PortraitSearchLayout.width(freeWidth,landscape?dp(720):freeWidth);int side=(freeWidth-panelWidth)/2;left+=side;right+=side;
        int top=root.getPaddingTop()+dp(landscape?8:68),available=Math.max(1,portraitSearchOverlay.getHeight()-top-root.getPaddingBottom()-dp(16));
        int statusHeight=dp(40);if(mediaSearchStatus!=null){int width=Math.max(dp(80),panelWidth-dp(24));mediaSearchStatus.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));statusHeight=mediaSearchStatus.getMeasuredHeight();}
        int fieldHeight=mediaSearchBox!=null&&mediaSearchBox.getVisibility()==View.VISIBLE?dp(70):dp(8);
        int height=PortraitSearchLayout.contentHeight(available,dp(520),fieldHeight,statusHeight,searchResultRowHeight(),searchResults.size(),!mediaSearchQuery.trim().isEmpty());
        FrameLayout.LayoutParams params=(FrameLayout.LayoutParams)mediaSearchPanel.getLayoutParams();params.gravity=Gravity.TOP;params.leftMargin=left;params.rightMargin=right;params.topMargin=top;
        if(portraitSearchHeight!=height){if(portraitSearchExpansion!=null)portraitSearchExpansion.cancel();int from=mediaSearchPanel.getHeight();portraitSearchHeight=height;
            if(from>0&&android.animation.ValueAnimator.areAnimatorsEnabled()){
                final LinearLayout panel=mediaSearchPanel;portraitSearchExpansion=android.animation.ValueAnimator.ofInt(from,height);portraitSearchExpansion.setDuration(200);portraitSearchExpansion.setInterpolator(new android.view.animation.DecelerateInterpolator());portraitSearchExpansion.addUpdateListener(a->{if(panel!=mediaSearchPanel)return;android.view.ViewGroup.LayoutParams p=panel.getLayoutParams();p.height=(Integer)a.getAnimatedValue();panel.setLayoutParams(p);});portraitSearchExpansion.start();
            }else params.height=height;
        }
        mediaSearchPanel.setLayoutParams(params);
        if(mediaSearchResults!=null)mediaSearchResults.setLayoutParams(!mediaSearchQuery.trim().isEmpty()?new LinearLayout.LayoutParams(-1,0,1):new LinearLayout.LayoutParams(-1,-2));
    }
    void cancelMediaSearchTasks(){if(mediaSearchLocalTask!=null)mediaSearchLocalTask.cancel(true);if(mediaSearchOnlineTask!=null)mediaSearchOnlineTask.cancel(true);io.purge();}
    int mediaSearchScope(){return homeOpen?0:tab<2?1:2;}
    void toggleMediaSearch(){mediaSearchVisible=!mediaSearchVisible;if(mediaSearchBox!=null){mediaSearchBox.animate().cancel();mediaSearchBox.setAlpha(1);mediaSearchBox.setTranslationY(0);mediaSearchBox.setVisibility(mediaSearchVisible?View.VISIBLE:View.GONE);}if(mediaSearchVisible&&mediaSearchField!=null){updatePortraitSearchOverlay();mediaSearchField.requestFocus();android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);keyboard.showSoftInput(mediaSearchField,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);touchMediaSearch();}else hideMediaSearchField();updateLibrarySearchLayout();}
    void hideMediaSearchField(){
        mediaSearchVisible=false;main.removeCallbacks(hideMediaSearch);
        if(mediaSearchBox!=null){final LinearLayout box=mediaSearchBox;box.animate().cancel();
            if(box.getVisibility()==View.VISIBLE&&android.animation.ValueAnimator.areAnimatorsEnabled())box.animate().alpha(0).translationY(-dp(4)).setDuration(160).withEndAction(()->{if(box==mediaSearchBox&&!mediaSearchVisible){box.setVisibility(View.GONE);box.setAlpha(1);box.setTranslationY(0);updateLibrarySearchLayout();}}).start();
            else{box.setVisibility(View.GONE);box.setAlpha(1);box.setTranslationY(0);}
        }
        if(mediaSearchField!=null){mediaSearchField.clearFocus();android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);keyboard.hideSoftInputFromWindow(mediaSearchField.getWindowToken(),0);}
        updateLibrarySearchLayout();scheduleMediaSearchIdle();
    }
    void dismissMediaSearch(){
        hideMediaSearchField();main.removeCallbacks(hideMediaSearch);if(mediaSearchPending!=null)main.removeCallbacks(mediaSearchPending);cancelMediaSearchTasks();searchEpoch++;
        mediaSearchQuery="";searchResults.clear();searchLocalLoading=false;searchOnlineLoading=false;searchNote="";
        if(mediaSearchField!=null){suppressSearchEdits=true;mediaSearchField.setText("");suppressSearchEdits=false;}drawMediaSearchResults();
    }
    void submitMediaSearch(){if(mediaSearchPending!=null)main.removeCallbacks(mediaSearchPending);runMediaSearch();touchMediaSearch();hideMediaSearchField();}
    void touchMediaSearch(){searchLastTouch=SystemClock.uptimeMillis();scheduleMediaSearchIdle();}
    void scheduleMediaSearchIdle(){main.removeCallbacks(hideMediaSearch);if(mediaSearchVisible||!mediaSearchQuery.trim().isEmpty())main.postDelayed(hideMediaSearch,Math.max(0,SearchPolicy.IDLE_MS-(SystemClock.uptimeMillis()-searchLastTouch)));}
    void addMediaSearchIcon(LinearLayout header){ImageButton icon=toolIcon(24,"Toggle media search",()->toggleMediaSearch());mediaSearchToggle=icon;icon.setBackgroundColor(Color.TRANSPARENT);header.addView(icon,new LinearLayout.LayoutParams(dp(48),dp(48)));}
    void addMediaSearch(LinearLayout parent){
        boolean restoreSearchFocus=mediaSearchField!=null&&mediaSearchField.hasFocus();
        int scope=mediaSearchScope();if(searchScope!=scope){cancelMediaSearchTasks();if(mediaSearchPending!=null)main.removeCallbacks(mediaSearchPending);searchScope=scope;mediaSearchQuery="";mediaSearchVisible=false;searchResults.clear();searchEpoch++;searchNote="";searchLocalLoading=false;searchOnlineLoading=false;}
        LinearLayout panel=column();mediaSearchPanel=panel;{ensurePortraitSearchOverlay();if(portraitSearchExpansion!=null){portraitSearchExpansion.cancel();portraitSearchExpansion=null;}portraitSearchOverlay.removeAllViews();portraitSearchHeight=0;panel.setBackground(round(this.panel,24));panel.setClipToOutline(true);padding(panel,12,4);panel.setClickable(true);portraitSearchOverlay.addView(panel,new FrameLayout.LayoutParams(-1,dp(70),Gravity.TOP));}mediaSearchBox=row();padding(mediaSearchBox,0,8);panel.addView(mediaSearchBox);mediaSearchBox.setVisibility(mediaSearchVisible?View.VISIBLE:View.GONE);
        mediaSearchField=new EditText(this);mediaSearchField.setSingleLine(true);mediaSearchField.setTextSize(14);mediaSearchField.setTypeface(font);mediaSearchField.setTextColor(ink);mediaSearchField.setHintTextColor(muted);mediaSearchField.setHint(scope==0?"Search music, radio, videos and TV":scope==1?"Search music and Internet Radio":"Search videos and Online TV");mediaSearchField.setBackground(searchBackground());padding(mediaSearchField,14,4);mediaSearchField.setText(mediaSearchQuery);mediaSearchBox.addView(mediaSearchField,new LinearLayout.LayoutParams(-1,dp(46)));
        mediaSearchField.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH|android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);mediaSearchField.setOnEditorActionListener((v,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH||action==android.view.inputmethod.EditorInfo.IME_ACTION_DONE||(event!=null&&event.getKeyCode()==KeyEvent.KEYCODE_ENTER&&event.getAction()==KeyEvent.ACTION_UP)){submitMediaSearch();return true;}return false;});
        mediaSearchField.setOnTouchListener((v,e)->{touchMediaSearch();return false;});mediaSearchField.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int start,int before,int count){if(suppressSearchEdits)return;mediaSearchQuery=s.toString();touchMediaSearch();searchEpoch++;cancelMediaSearchTasks();if(mediaSearchPending!=null)main.removeCallbacks(mediaSearchPending);mediaSearchPending=()->runMediaSearch();main.postDelayed(mediaSearchPending,450);}});
        mediaSearchResults=column();panel.addView(mediaSearchResults,new LinearLayout.LayoutParams(-1,-2));drawMediaSearchResults();scheduleMediaSearchIdle();if(restoreSearchFocus&&mediaSearchVisible){mediaSearchField.requestFocus();mediaSearchField.setSelection(mediaSearchField.length());}
    }
    ArrayList<Item> searchLocalItems(int target,boolean allowed,boolean standardOnly){
        if(!allowed)return new ArrayList<>();SearchSnapshot<Item> cache=target==0?searchSongs:searchVideos;
        String key=target+":"+standardOnly+":"+allowed;int token=cache.generation();ArrayList<Item> items=cache.get(key,SystemClock.elapsedRealtime());
        if(items!=null)return items;items=HomeFeed.scan(getApplicationContext(),target,allowed,standardOnly);
        if(!Thread.currentThread().isInterrupted())cache.save(key,items,SystemClock.elapsedRealtime(),token);return items;
    }
    void runMediaSearch(){
        cancelMediaSearchTasks();final int epoch=++searchEpoch,scope=searchScope;final String q=mediaSearchQuery.trim(),fm=fmCountry,tv=country;searchResults.clear();searchNote="";if(q.isEmpty()){searchLocalLoading=false;searchOnlineLoading=false;drawMediaSearchResults();return;}
        final boolean music=musicAccess(),video=videoAccess()||selectedVideoAccess(),musicOnly=standardFolderOnly(0),moviesOnly=standardFolderOnly(2);final ArrayList<Item> savedCopy=new ArrayList<>(saved);searchLocalLoading=true;searchOnlineLoading=true;drawMediaSearchResults();
        mediaSearchLocalTask=io.submit(()->{ArrayList<Item> local=new ArrayList<>();if(SearchPolicy.allows(scope,"music"))local.addAll(searchLocalItems(0,music,musicOnly));if(SearchPolicy.allows(scope,"video"))local.addAll(searchLocalItems(2,video,moviesOnly));for(Item item:savedCopy)if(SearchPolicy.allows(scope,item.kind))local.add(item);main.post(()->{if(destroyed||epoch!=searchEpoch)return;mergeMediaSearch(local,q,false);searchLocalLoading=false;drawMediaSearchResults();});});
        mediaSearchOnlineTask=io.submit(()->{SearchFeed.Result result=SearchFeed.online(this,scope,q,fm,tv);main.post(()->{if(destroyed||epoch!=searchEpoch)return;mergeMediaSearch(result.items,q,true);searchOnlineLoading=false;searchNote=result.note;drawMediaSearchResults();});});
    }
    void mergeMediaSearch(List<Item> items,String q,boolean directory){LinkedHashMap<String,Item> unique=new LinkedHashMap<>();for(Item item:searchResults)unique.put(item.kind+"\n"+item.url,item);for(Item item:items)if(SearchPolicy.allows(searchScope,item.kind)&&(directory&&item.kind.equals("radio")||SearchPolicy.matches(q,item.title,item.detail)))unique.putIfAbsent(item.kind+"\n"+item.url,item);searchResults.clear();searchResults.addAll(unique.values());Collections.sort(searchResults,(a,b)->a.title.compareToIgnoreCase(b.title));}
    void attachLibrarySearchResults(LinearLayout parent){updateLibrarySearchLayout();}
    void updateLibrarySearchLayout(){
        if(searchLibrary!=null)searchLibrary.setVisibility(View.VISIBLE);
        updatePortraitSearchOverlay();
    }
    int searchResultRowHeight(){return Math.max(dp(48),(int)Math.ceil(getResources().getDisplayMetrics().scaledDensity*30)+dp(8));}
    void drawMediaSearchResults(){
        updateLibrarySearchLayout();
        if(mediaSearchResults==null)return;mediaSearchResults.removeAllViews();if(mediaSearchQuery.trim().isEmpty()){mediaSearchResults.setVisibility(View.GONE);return;}mediaSearchResults.setVisibility(View.VISIBLE);
        TextView status=text((searchLocalLoading||searchOnlineLoading?"Searching… · ":"")+searchResults.size()+" matches · local access/folder filters and selected regions apply. "+searchNote,11,muted);mediaSearchStatus=status;mediaSearchResults.addView(status);
        ListView matches=new ListView(this);matches.setDividerHeight(0);matches.setAdapter(new BaseAdapter(){public int getCount(){return searchResults.size();}public Object getItem(int i){return searchResults.get(i);}public long getItemId(int i){return i;}public View getView(int i,View convert,android.view.ViewGroup host){Item item=searchResults.get(i);LinearLayout entry;MediaRow holder;
            if(convert==null){entry=row();padding(entry,8,4);holder=new MediaRow();holder.image=new ThumbnailView(MainActivity.this);holder.image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);entry.addView(holder.image,new LinearLayout.LayoutParams(dp(40),dp(40)));LinearLayout words=column();padding(words,10,0);holder.title=text("",14,ink);semibold(holder.title);holder.title.setSingleLine(true);holder.title.setEllipsize(TextUtils.TruncateAt.END);holder.detail=text("",11,muted);holder.detail.setSingleLine(true);holder.detail.setEllipsize(TextUtils.TruncateAt.END);words.addView(holder.title);words.addView(holder.detail);entry.addView(words,new LinearLayout.LayoutParams(0,-2,1));entry.setTag(holder);}else{entry=(LinearLayout)convert;holder=(MediaRow)entry.getTag();}
            entry.setMinimumHeight(searchResultRowHeight());holder.title.setText(item.title);holder.detail.setText((item.kind.equals("music")?"Music":item.kind.equals("radio")?"Internet Radio":item.kind.equals("video")?"Video":"Online TV")+" · "+item.detail);artwork.bind(holder.image,item,accent);return entry;}});
        matches.setOnTouchListener((v,e)->{touchMediaSearch();if(e.getActionMasked()==MotionEvent.ACTION_DOWN)v.getParent().requestDisallowInterceptTouchEvent(true);return false;});matches.setOnItemClickListener((p,v,i,id)->{touchMediaSearch();if(i<searchResults.size()){hideMediaSearchField();playHomeItem(searchResults.get(i),new ArrayList<>(searchResults));}});mediaSearchResults.addView(matches,new LinearLayout.LayoutParams(-1,0,1));updatePortraitSearchOverlay();
    }
    void refreshSection(){searchSongs.clear();searchVideos.clear();if(!mediaSearchQuery.trim().isEmpty())runMediaSearch();if(tab==0||tab==2)loadLocal();else loadOnline();}
    void addBottomNav(){HorizontalScrollView scroll=new NavSwipeBar(this,direction->swipeSection(direction));scroll.setHorizontalScrollBarEnabled(false);scroll.setFillViewport(true);nav=row();padding(nav,16,3);nav.setMinimumWidth(dp(272));scroll.addView(nav,new FrameLayout.LayoutParams(-1,-2));LinearLayout.LayoutParams navParams=new LinearLayout.LayoutParams(-1,dp(54));navParams.topMargin=dp(settingsOpen?0:8);root.addView(scroll,navParams);buildNav();if(settingsOpen)scroll.post(()->scroll.fullScroll(View.FOCUS_RIGHT));}
    void cancelSectionTransition(){
        if(animatedSection!=null){animatedSection.animate().cancel();animatedSection.setAlpha(1f);animatedSection.setTranslationX(0);animatedSection=null;}sectionTransitioning=false;
    }
    void swipeSection(int direction){
        if(!appVisible||setupOpen||tvMode||expanded||landscape||sectionTransitioning)return;
        int currentSection=settingsOpen?3:homeOpen?0:1+tab/2,target=NavSwipeMath.target(currentSection,direction);if(target==currentSection)return;
        selectDestination(target);
        if(sectionPage==null||!android.animation.ValueAnimator.areAnimatorsEnabled())return;
        View entering=sectionPage;entering.animate().cancel();animatedSection=entering;sectionTransitioning=true;
        entering.setAlpha(.94f);entering.setTranslationX(dp(12)*Integer.signum(direction));
        entering.animate().alpha(1f).translationX(0).setDuration(180).setInterpolator(new android.view.animation.DecelerateInterpolator()).withEndAction(()->{if(animatedSection==entering){animatedSection=null;sectionTransitioning=false;}}).start();
    }
    void addNavRail(LinearLayout host){ScrollView scroll=new ScrollView(this);scroll.setVerticalScrollBarEnabled(false);nav=column();padding(nav,10,12);ImageView railLogo=new ImageView(this);railLogo.setImageResource(R.mipmap.ic_launcher_round);railLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);railLogo.setContentDescription("Tuhkar");railLogo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);LinearLayout.LayoutParams logoParams=new LinearLayout.LayoutParams(dp(40),dp(40));logoParams.gravity=Gravity.CENTER_HORIZONTAL;logoParams.bottomMargin=dp(12);nav.addView(railLogo,logoParams);scroll.addView(nav,new ScrollView.LayoutParams(-1,-2));host.addView(scroll,new LinearLayout.LayoutParams(dp(80),-1));buildNav();if(settingsOpen)scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));}
    void openSettings(){if(settingsOpen)return;generation++;if(mediaSearchPending!=null)main.removeCallbacks(mediaSearchPending);settingsOpen=true;settingsCategory="";settingsScrollY=0;settingsAnchor=null;settingsAnchorOffset=0;settingsFadePending=true;expanded=false;render();}
    void selectDestination(int destination){
        if(destination==0){openHome();return;}
        if(destination==3){openSettings();return;}
        int fallback=destination==1?0:2;
        int remembered=prefs.getInt(destination==1?"audioSourceTab":"videoSourceTab",fallback);
        switchTab(remembered>=fallback&&remembered<=fallback+1?remembered:fallback);
    }
    void addSourceTabs(LinearLayout host){addSourceTabs(host,false);}
    void addSourceTabs(LinearLayout host,boolean compact){
        LinearLayout tabs=row();tabs.setPadding(0,dp(8),0,0);if(compact)tabs.setPadding(0,0,0,0);host.addView(tabs,compact?new LinearLayout.LayoutParams(0,dp(48),1):new LinearLayout.LayoutParams(-1,-2));
        int first=tab<2?0:2;
        for(int index=first;index<first+2;index++){
            final int source=index;boolean selected=tab==source;
            Button choice=button(source==0?"Collections":source==1?"Internet Radio":source==2?"Collections":"Online TV",()->switchTab(source));
            choice.setTextColor(selected?accentInk:muted);choice.setBackground(round(selected?accent:normalBg,24));choice.setSelected(selected);if(selected)activeSourceTab=choice;
            FluentIcon sourceIcon=new FluentIcon(source==0?22:source==1?1:source==2?23:3,selected?accentInk:muted);sourceIcon.setBounds(0,0,dp(20),dp(20));choice.setCompoundDrawables(sourceIcon,null,null,null);choice.setCompoundDrawablePadding(dp(8));
            choice.setContentDescription((source==0?"Collections":source==1?"Internet Radio":source==2?"Collections":"Online TV")+(selected?", selected":", select source"));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),1);if(index==first)lp.rightMargin=dp(8);tabs.addView(choice,lp);
        }
    }
    void buildNav(){for(int i=0;i<4;i++){final int destination=i;boolean selected=i==3?settingsOpen:!settingsOpen&&(i==0?homeOpen:!homeOpen&&i==1+tab/2);String label=i==0?"Home":i==1?"Music":i==2?"Video":"Settings";int icon=i==0?21:i==1?0:i==2?2:4;ImageButton b=new ImageButton(this);b.setImageDrawable(new FluentIcon(icon,selected?accentInk:muted));b.setScaleType(ImageView.ScaleType.CENTER_INSIDE);b.setBackground(selected&&!landscape?new android.graphics.drawable.InsetDrawable(round(accent,20),0,dp(4),0,dp(4)):round(selected?accent:Color.TRANSPARENT,20));padding(b,landscape?16:12,landscape?14:12);b.setContentDescription(label);b.setTooltipText(label);b.setSelected(selected);b.setOnClickListener(v->selectDestination(destination));if(landscape){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(58));lp.bottomMargin=dp(10);nav.addView(b,lp);}else nav.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));}}
    void switchTab(int t){if(t<0||t>3)return;boolean wasHome=homeOpen;homeOpen=false;prefs.edit().putInt(t<2?"audioSourceTab":"videoSourceTab",t).apply();if(tab==t){if(settingsOpen)closeSettings();else if(wasHome){render();if(t==0||t==2)loadLocal();else loadOnline();}return;}settingsOpen=false;generation++;if(mediaSearchPending!=null)main.removeCallbacks(mediaSearchPending);tab=t;query="";showingSaved=false;sourceStatus="";online.clear();render();if(t==0||t==2)loadLocal();else loadOnline();}
    int layoutMode(){return Math.max(0,Math.min(3,prefs.getInt("layoutMode",3)));}
    void activateItem(int position){if(position<0||position>=shown.size()||!playbackReady())return;queue.reset(shown,shown.get(position));start(shown.get(position));}
    void buildLibrary(LinearLayout library){
        int mode=layoutMode();list=null;grid=null;deck=null;adapter=new ItemAdapter();
        TextView empty=text(tab==0||tab==2?"Nothing here yet\n\nAllow media access in Settings. Check Music/Movies-only filters if files are missing.":"Nothing here yet\n\nUse Refresh or add a direct stream URL.",13,muted);empty.setGravity(Gravity.CENTER);padding(empty,16,20);
        if(mode==0){deck=new SwipeDeck(this);library.addView(deck,new LinearLayout.LayoutParams(-1,0,1));emptyMedia=empty;library.addView(empty,new LinearLayout.LayoutParams(-1,0,1));}
        else{AbsListView collection;if(mode==3){list=new ListView(this);list.setDividerHeight(dp(1));list.setDivider(new android.graphics.drawable.ColorDrawable(line));list.setCacheColorHint(Color.TRANSPARENT);collection=list;}else{grid=new GridView(this);gridColumns=mode==1?2:3;grid.setNumColumns(gridColumns);grid.setHorizontalSpacing(dp(4));grid.setVerticalSpacing(dp(4));grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);collection=grid;}collection.setFastScrollEnabled(true);collection.setFastScrollAlwaysVisible(true);collection.setVerticalScrollBarEnabled(false);collection.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);collection.setSelector(mediaSelector());collection.setClipToPadding(false);collection.setAdapter(adapter);library.addView(collection,new LinearLayout.LayoutParams(-1,0,1));library.addView(empty,new LinearLayout.LayoutParams(-1,0,1));collection.setEmptyView(empty);collection.setOnItemClickListener((p,v,pos,id)->activateItem(pos));collection.setOnItemLongClickListener((p,v,pos,id)->{saveToggle(shown.get(pos));return true;});}
        View viewport=mode==0?deck:mode==3?list:grid;viewport.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{int height=b-t;boolean changed=height>0&&height!=libraryHeight;if(height>0)libraryHeight=height;if(grid!=null){int maxColumns=layoutMode()==1?2:3;int columns=Math.max(1,Math.min(maxColumns,(r-l)/(dp(48)+dp(4))));if(columns!=gridColumns){gridColumns=columns;grid.setNumColumns(columns);changed=true;}}if(changed&&adapter!=null)adapter.notifyDataSetChanged();});
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
            int mode=layoutMode();boolean tiles=mode==2;int rows=mode==1?8:mode==2?Math.max(1,9/gridColumns):10;float scale=getResources().getDisplayMetrics().scaledDensity;int minimum=mode==2?Math.max(dp(84),(int)(32*scale)+dp(48)):mode==1?Math.max(dp(52),(int)(30*scale)+dp(8)):Math.max(dp(48),(int)(30*scale)+dp(12));int cellHeight=Math.max(minimum,Math.max(1,(libraryHeight-(rows-1)*dp(tiles?4:1))/rows));LinearLayout r;MediaRow holder;
            if(convert==null){r=tiles?column():row();padding(r,tiles?4:mode==1?6:8,tiles?4:mode==1?4:6);holder=new MediaRow();holder.image=new SpinningThumbnailView(MainActivity.this);configureArtwork((SpinningThumbnailView)holder.image);holder.image.setScaleType(ImageView.ScaleType.CENTER_CROP);holder.image.setBackground(round(normalBg,999));holder.image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);holder.title=text("",mode==1?12:tiles?13:14,ink);semibold(holder.title);holder.title.setMaxLines(1);holder.title.setEllipsize(TextUtils.TruncateAt.END);holder.detail=text("",10,muted);holder.detail.setMaxLines(1);holder.detail.setEllipsize(TextUtils.TruncateAt.END);holder.saved=text("",13,muted);
                if(tiles){FrameLayout well=new FrameLayout(MainActivity.this);well.addView(holder.image,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));final ImageView circle=holder.image;well.addOnLayoutChangeListener((v,l,t,right,bottom,ol,ot,or,ob)->{int side=Math.max(1,Math.min(right-l,bottom-t));android.view.ViewGroup.LayoutParams size=circle.getLayoutParams();if(size.width!=side||size.height!=side)circle.setLayoutParams(new FrameLayout.LayoutParams(side,side,Gravity.CENTER));});r.addView(well,new LinearLayout.LayoutParams(-1,0,1));holder.title.setGravity(Gravity.CENTER);r.addView(holder.title);if(mode==2){holder.detail.setGravity(Gravity.CENTER);r.addView(holder.detail);}}
                else{r.addView(holder.image,new LinearLayout.LayoutParams(dp(40),dp(40)));LinearLayout words=column();padding(words,mode==1?8:10,0);words.addView(holder.title);words.addView(holder.detail);r.addView(words,new LinearLayout.LayoutParams(0,-2,1));r.addView(holder.saved);}r.setTag(holder);
            }else{r=(LinearLayout)convert;holder=(MediaRow)r.getTag();}
            android.view.ViewGroup.LayoutParams rowSize=r.getLayoutParams();if(rowSize==null||rowSize.width!=-1||rowSize.height!=cellHeight)r.setLayoutParams(new AbsListView.LayoutParams(-1,cellHeight));Item item=shown.get(position);if(!tiles){int artSize=Math.min(dp(mode==1?32:62),cellHeight-dp(12));android.view.ViewGroup.LayoutParams imageSize=holder.image.getLayoutParams();if(imageSize.width!=artSize||imageSize.height!=artSize)holder.image.setLayoutParams(new LinearLayout.LayoutParams(artSize,artSize));}boolean savedItem=savedKeys.contains(item.kind+"\n"+item.url);String title=(tiles&&savedItem?"♥ ":"")+item.title;if(!android.text.TextUtils.equals(holder.title.getText(),title))holder.title.setText(title);if(!android.text.TextUtils.equals(holder.detail.getText(),item.detail))holder.detail.setText(item.detail);String heart=savedItem?"♥":"";if(!android.text.TextUtils.equals(holder.saved.getText(),heart))holder.saved.setText(heart);((SpinningThumbnailView)holder.image).bindUrl(item.url);artwork.bind(holder.image,item,muted);r.setContentDescription(item.title+", "+item.detail+". Tap to play. Hold to save or remove.");return r;
        }
    }
    String extension(String filename,String fallback){if(filename!=null){int dot=filename.lastIndexOf('.');if(dot>=0&&dot<filename.length()-1)return filename.substring(dot+1).toLowerCase(Locale.ROOT);}return fallback==null?"":fallback;}
    String[] sortNames(){return new String[]{"Type","Date added","Date modified","Alphabetical"};}
    void showSort(){dialog("Sort media").setSingleChoiceItems(sortNames(),Math.max(0,Math.min(3,prefs.getInt("sortOrder",3))),(d,w)->{prefs.edit().putInt("sortOrder",w).apply();d.dismiss();if(sortButton!=null){sortButton.setTooltipText("Sort: "+sortNames()[w]);sortButton.setContentDescription("Sort media: "+sortNames()[w]);}refreshList();}).setNegativeButton("Cancel",null).show();}
    void librarySettings(LinearLayout page){
        LinearLayout styles=settingsCard(page,"Media layout");String[] layouts={"Disc swipe","2 column list","Grid","List view · up to 10"};
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
        styles.addView(spacedSettingsNote("Previews are illustrative. Larger text and smaller screens fit fewer items.",11,muted));
    }
    String equalizerUiState(){return prepared+":"+equalizer.enabled()+":"+java.util.Arrays.hashCode(equalizer.frequencies)+":"+equalizer.min+":"+equalizer.max+":"+(equalizer.effect!=null)+":"+equalizer.error;}
    void audioEnhancementsSettings(LinearLayout page){
        LinearLayout card=settingsCard(page,"Audio Enhancements");equalizerSettings(card);
        View divider=new View(this);divider.setBackgroundColor(line);LinearLayout.LayoutParams dividerParams=new LinearLayout.LayoutParams(-1,dp(1));dividerParams.topMargin=dp(16);dividerParams.bottomMargin=dp(12);card.addView(divider,dividerParams);
        LinearLayout knobs=row();card.addView(knobs);TextView status=spacedSettingsNote("Music only · Crossfade up to 2 seconds. Reverb depends on device support.",11,muted);
        for(int i=0;i<2;i++){
            final boolean fade=i==1;LinearLayout option=column();option.setGravity(Gravity.CENTER);knobs.addView(option,new LinearLayout.LayoutParams(0,-2,1));
            TextView label=text(fade?"Crossfade":"Reverb",14,ink);semibold(label);label.setGravity(Gravity.CENTER);option.addView(label);
            MinimalKnob knob=new MinimalKnob(this,fade?"Crossfade, music only":"Reverb amount, music only",fade?2000:100,fade?100:1,prefs.getInt(fade?"crossfadeMs":"reverbAmount",0),fade,accent,ink,muted,font);
            option.addView(knob,new LinearLayout.LayoutParams(-1,dp(116)));
            knob.listener=value->{prefs.edit().putInt(fade?"crossfadeMs":"reverbAmount",value).apply();if(playback!=null){playback.applyAudioEnhancements();status.setText(playback.reverb.error.isEmpty()?"Music only · Crossfade up to 2 seconds. Reverb depends on device support.":playback.reverb.error);}};
        }
        card.addView(status);
    }
    void equalizerSettings(LinearLayout page){
        LinearLayout card=column();page.addView(card);Switch enabled=new Switch(this);enabled.setText("Equalizer");enabled.setTextColor(ink);semibold(enabled);enabled.setChecked(equalizer.enabled());tintSettingsSwitch(enabled);enabled.setOnCheckedChangeListener((b,on)->{prefs.edit().putBoolean("eqEnabled",on).apply();if(player!=null&&prepared)equalizer.attach(player.getAudioSessionId());else if(!on)equalizer.close();render();});card.addView(enabled);
        eqSettingsState=equalizerUiState();
        Button presetSelect=button("Preset · "+NativeEq.PRESETS[equalizer.preset()]+" ▾",()->dialog("Sound presets").setSingleChoiceItems(NativeEq.PRESETS,equalizer.preset(),(d,w)->{equalizer.setPreset(w);d.dismiss();render();}).setNegativeButton("Cancel",null).show());semibold(presetSelect);presetSelect.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);presetSelect.setBackground(round(normalBg,14));padding(presetSelect,14,2);card.addView(presetSelect);
        if(!prepared||player==null)card.addView(spacedSettingsNote("Choose your preset now. Start a song or station to detect this phone's EQ bands and apply your settings.",12,muted));
        else if(!equalizer.error.isEmpty()){
            card.addView(spacedSettingsNote(equalizer.error,12,muted));card.addView(button("Reconnect equalizer",()->{if(player!=null&&prepared){equalizer.attach(player.getAudioSessionId());render();}}));
        }else card.addView(spacedSettingsNote(equalizer.enabled()?"Equalizer connected to Tuhkar playback.":"Equalizer is off. Your preset will apply when enabled.",12,muted));
        if(equalizer.preset()!=NativeEq.PRESETS.length-1)return;
        float[] editBands=equalizer.editorFrequencies();
        int bands=editBands.length;card.addView(text(equalizer.frequencies==null?"8 reference bands · mapped to your device when playback starts.":bands+" native bands",11,muted));
        for(int band=0;band<bands;band++){
            final int b=band;float hz=editBands[band];String frequency=hz>=1000?String.format(Locale.ROOT,"%.1f kHz",hz/1000):Math.round(hz)+" Hz";
            LinearLayout bandRow=row();card.addView(bandRow);TextView label=text(frequency,11,muted);bandRow.addView(label,new LinearLayout.LayoutParams(dp(54),-2));
            SeekBar slider=new SeekBar(this);slider.setMax(equalizer.max-equalizer.min);slider.setProgress(Math.round(equalizer.editorLevel(b))-equalizer.min);slider.setContentDescription(frequency+" equalizer gain");slider.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));slider.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));bandRow.addView(slider,new LinearLayout.LayoutParams(0,dp(48),1));
            TextView value=text(String.format(Locale.ROOT,"%+.0f dB",equalizer.editorLevel(b)),11,ink);value.setGravity(Gravity.RIGHT);bandRow.addView(value,new LinearLayout.LayoutParams(dp(42),-2));
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar v){}public void onStopTrackingTouch(SeekBar v){}public void onProgressChanged(SeekBar v,int progress,boolean user){if(user){float gain=progress+equalizer.min;equalizer.setCustom(b,gain);presetSelect.setText("Preset · Custom ▾");value.setText(String.format(Locale.ROOT,"%+.0f dB",gain));}}});
        }
        card.addView(text("Pick a preset or fine-tune. Large boosts may distort.",11,muted));
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
    void refreshAccess(){localMusic.clear();localVideo.clear();generation++;homeLoadedAt=0;if(setupOpen||settingsOpen)render();else if(homeOpen){render();ensureHomeData(true);}else if(tab==0||tab==2)loadLocal();}
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
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(15000);c.setRequestProperty("User-Agent","Tuhkar/2.1.0 (Android)");
        try{int code=c.getResponseCode();if(code!=200)throw new IOException("Server returned "+code);try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n,total=0;while((n=in.read(buf))!=-1){total+=n;if(total>maxBytes)throw new IOException("Directory exceeds safety limit");out.write(buf,0,n);}return out.toString("UTF-8");}}finally{c.disconnect();}
    }
    void loadOnline(){
        final int token=++generation,target=tab;final String q=query, region=target==1?fmCountry:country;
        if(target!=1&&target!=3)return;notice("Connecting to public directory…");
        io.execute(()->{ArrayList<Item> result=new ArrayList<>();String error=null;
            try{
                if(target==1){String path=RadioDirectory.path(q,region);String json=null;Exception last=null;
                    for(String host:new String[]{"https://de1.api.radio-browser.info","https://nl1.api.radio-browser.info","https://at1.api.radio-browser.info"}){try{json=get(host+path,2500000);break;}catch(Exception e){last=e;}}
                    if(json==null)throw last;JSONArray a=new JSONArray(json);for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);String u=o.optString("url_resolved",o.optString("url"));if(validUrl(u))result.add(new Item(o.optString("name","Radio"),o.optString("country")+" · "+o.optString("codec")+" · "+o.optInt("bitrate")+" kbps",u,"radio").art(o.optString("favicon","")).type(o.optString("codec","radio")));}
                }else {
                    File cache=new File(getCacheDir(),"tv-"+region+".m3u");String m3u;
                    if(cache.exists()&&System.currentTimeMillis()-cache.lastModified()<6*60*60*1000L){try(FileInputStream f=new FileInputStream(cache);ByteArrayOutputStream b=new ByteArrayOutputStream()){byte[] chunk=new byte[8192];int n;while((n=f.read(chunk))!=-1)b.write(chunk,0,n);m3u=b.toString("UTF-8");}}
                    else{m3u=get("https://iptv-org.github.io/iptv/countries/"+region+".m3u",6000000);try(FileOutputStream f=new FileOutputStream(cache)){f.write(m3u.getBytes("UTF-8"));}}
                    String name="Channel",logoUrl="",detail="Public IPTV · "+region.toUpperCase(Locale.ROOT);try(BufferedReader reader=new BufferedReader(new StringReader(m3u))){String s;while((s=reader.readLine())!=null&&result.size()<3000){s=s.trim();if(s.startsWith("#EXTINF:")){int comma=s.lastIndexOf(',');name=comma>=0?s.substring(comma+1).trim():"Channel";java.util.regex.Matcher logoMatch=java.util.regex.Pattern.compile("tvg-logo=\"([^\"]*)\"").matcher(s);logoUrl=logoMatch.find()?logoMatch.group(1):"";}else if(validUrl(s))result.add(new Item(name,detail,s,"tv").art(logoUrl));}}
                }
            }catch(Exception e){error="Directory unavailable. Check internet or try Refresh. Saved URLs remain available.";}
            final String err=error;main.post(()->{if(destroyed||token!=generation)return;online.clear();online.addAll(result);notice(err!=null?err:(target==1?result.size()+" stations · "+RadioDirectory.label(region)+" · Radio Browser":result.size()+" channels · "+region.toUpperCase(Locale.ROOT)+" · iptv-org")+" · availability varies");refreshList();});
        });
    }
    boolean validUrl(String u){return u!=null&&(u.startsWith("https://")||u.startsWith("http://"));}
    void chooseCountry(){
        String[] names={"Philippines","United States","United Kingdom","Canada","Australia","Germany","France","Spain","Italy","India","Japan","South Korea","Indonesia","Malaysia","Singapore","Thailand","Vietnam","Brazil","Mexico","South Africa","Other country code…"};
        String[] codes={"ph","us","uk","ca","au","de","fr","es","it","in","jp","kr","id","my","sg","th","vn","br","mx","za"};
        final boolean radio=tab==1;ArrayList<String> options=new ArrayList<>(java.util.Arrays.asList(names));if(radio)options.add(0,"Worldwide");
        dialog(radio?"Public radio directory · region":"Public TV directory · region").setItems(options.toArray(new String[0]),(d,w)->{
            int index=radio?w-1:w;
            if(index<0){applyRegion(true,"all");return;}
            if(index<codes.length){applyRegion(radio,codes[index]);return;}
            EditText input=new EditText(this);input.setHint("Two-letter code, e.g. nz");dialog("Country code").setView(input).setPositiveButton("Load",(dd,ww)->{
                String code=input.getText().toString().trim().toLowerCase(Locale.ROOT);if(code.matches("[a-z]{2}"))applyRegion(radio,code);else toast("Enter two letters.");
            }).setNegativeButton("Cancel",null).show();
        }).show();
    }
    void applyRegion(boolean radio,String code){
        if(radio){fmCountry=RadioDirectory.normalize(code);prefs.edit().putString("fmCountry",fmCountry).apply();}else country=code;
        if(mediaSearchPending!=null)main.removeCallbacks(mediaSearchPending);showingSaved=false;online.clear();render();loadOnline();
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
    void clearDiscEnergy(){glowLow=glowMid=glowHigh=0;if(miniCard!=null)miniCard.energy(0);for(DiscView view:new ArrayList<>(discViews))view.energy(0,0,0);}
    void stopDiscAnimation(){for(DiscView view:new ArrayList<>(discViews))view.playback(false,false,false);discViews.clear();disc=null;if(reactiveGlow!=null)reactiveGlow.stop();clearDiscEnergy();}
    void requestGlowPermission(){
        if(allowed(Manifest.permission.RECORD_AUDIO))return;
        if(prefs.getBoolean("askedGlowPermission",false)&&!shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)){openAccessSettings();return;}
        if(glowPermissionDialog)return;glowPermissionDialog=true;
        AlertDialog prompt=dialog("Audio-reactive glow").setMessage("Android calls this permission ‘Record audio’. Tuhkar uses Android's Visualizer only on its own playback session to animate disc and FM mini-player glow. It does not use microphone-recording APIs, save audio or upload audio. Device/stream support varies.\n\nDecline to keep a soft non-reactive glow. You can change this in Settings.").setNegativeButton("Not now",(d,w)->prefs.edit().putBoolean("askedGlowPermission",true).apply()).setPositiveButton("Allow",(d,w)->{prefs.edit().putBoolean("askedGlowPermission",true).apply();requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},40);}).create();prompt.setOnDismissListener(d->{glowPermissionDialog=false;prefs.edit().putBoolean("askedGlowPermission",true).apply();});prompt.show();
    }
    void syncDisc(){
        boolean visibleDisc=false;for(DiscView view:new ArrayList<>(discViews)){boolean selected=current!=null&&current.url.equals(view.mediaUrl);view.setReactiveGlow(prefs.getBoolean("reactiveDiscGlow",true));view.playback(selected,playing,appVisible);if(selected&&view.isAttachedToWindow()&&view.isShown())visibleDisc=true;}
        boolean visibleFm=miniCard!=null&&miniCard.isShown()&&current!=null&&current.kind.equals("radio");
        boolean wanted=(visibleDisc||visibleFm)&&playing&&prepared&&appVisible&&!isVideo&&!settingsOpen&&!setupOpen&&!tvMode&&!resetInProgress&&prefs.getBoolean("reactiveDiscGlow",true)&&android.animation.ValueAnimator.areAnimatorsEnabled();
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

    void addAudioMiniPlayer(LinearLayout parent){
        MiniPlayerCard card=new MiniPlayerCard(this,panel,accent,fraction->{if(playback!=null&&prepared&&!playback.live())playback.seekTo((long)(playback.duration()*fraction));});miniCard=card;
        parent.addView(card,new LinearLayout.LayoutParams(-1,dp(72)));card.setOnClickListener(v->openExpanded());
        LinearLayout line=row();padding(line,10,8);card.addView(line,new FrameLayout.LayoutParams(-1,-1));
        miniArtwork=new SpinningThumbnailView(this);miniArtwork.setScaleType(ImageView.ScaleType.CENTER_CROP);miniArtwork.setBackground(round(normalBg,999));miniArtwork.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);line.addView(miniArtwork,new LinearLayout.LayoutParams(dp(40),dp(40)));
        LinearLayout names=column();padding(names,10,0);nowTitle=text(current==null?"Ready when you are":current.title,14,ink);semibold(nowTitle);nowTitle.setSingleLine(true);nowTitle.setEllipsize(TextUtils.TruncateAt.END);nowDetail=text("",11,muted);nowDetail.setSingleLine(true);nowDetail.setEllipsize(TextUtils.TruncateAt.END);names.addView(nowTitle);names.addView(nowDetail);line.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        playButton=holdingPlay(false,false);line.addView(playButton,new LinearLayout.LayoutParams(dp(48),dp(48)));Button next=transport("›","Next",()->next(1));line.addView(next,new LinearLayout.LayoutParams(dp(48),dp(48)));card.controls(playButton,next);
        updateAudioMiniPlayer();
    }
    void updateAudioMiniPlayer(){
        if(miniCard==null)return;
        // A retained current item includes paused/loading playback; Stop clears it.
        miniCard.setVisibility(current==null?View.GONE:View.VISIBLE);
        if(playerBox!=null)playerBox.setVisibility(current==null?View.GONE:View.VISIBLE);
        boolean live=current!=null&&(current.kind.equals("radio")||current.kind.equals("tv"));
        long duration=playback==null?0:playback.duration(),position=playback==null?0:playback.position();
        miniCard.media(position,duration,playing&&appVisible,live,prepared);miniCard.setContentDescription((current==null?"Player":current.title)+(live?". Live playback. Tap for Now Playing.":". Drag to seek. Tap for Now Playing."));
        if(nowDetail!=null)nowDetail.setText(live?"LIVE · "+status:current==null?"Select music or an FM station":current.detail);
        String url=current==null?"":current.url;if(miniArtwork instanceof SpinningThumbnailView)((SpinningThumbnailView)miniArtwork).bindUrl(url);updateArtworkMotion();if(miniArtwork!=null&&!url.equals(miniArtworkUrl)){miniArtworkUrl=url;miniArtwork.setImageDrawable(null);if(current!=null)artwork.bind(miniArtwork,current,accent);}
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
    void addRoundedPicture(LinearLayout host,int height){RoundedVideoView picture=new RoundedVideoView(this,videoOutput);surface=picture;inlineVideoStage=new FrameLayout(this);host.addView(inlineVideoStage,new LinearLayout.LayoutParams(-1,height));inlineVideoStage.addView(picture,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));picture.onOutputChanged(()->{if(picture.output()==null){detachVideoOutput(picture);return;}if(surface==picture){attachVideoOutput();if(appVisible&&player!=null&&prepared)try{fitVideo(player.getVideoWidth(),player.getVideoHeight());}catch(Exception ignored){}}});}
    void clearVideoOutputCache(){attachedVideoView=null;attachedVideoPlayer=null;attachedVideoSurface=null;}
    void detachVideoOutput(View view){if(attachedVideoView!=view)return;if(attachedVideoPlayer!=null&&attachedVideoPlayer==player)try{player.setSurface(null);}catch(Exception ignored){}clearVideoOutputCache();}
    boolean inlineVideoVisible(){return !settingsOpen&&!setupOpen&&current!=null&&isVideo;}
    void attachVideoOutput(){if(player==null||!isVideo||!appVisible||surface==null||settingsOpen||setupOpen||(!tvMode&&!expanded&&!inlineVideoVisible()))return;try{Surface output=surface instanceof RoundedVideoView?((RoundedVideoView)surface).output():surface instanceof SurfaceView?((SurfaceView)surface).getHolder().getSurface():null;if(output!=null&&!output.isValid())output=null;if(output!=null&&attachedVideoPlayer==player&&attachedVideoSurface==output){attachedVideoView=surface;return;}if(attachedVideoPlayer==player&&attachedVideoView==surface&&attachedVideoSurface==output)return;if(surface instanceof SurfaceView){if(output!=null)player.setDisplay(((SurfaceView)surface).getHolder());else player.setSurface(null);}else player.setSurface(output);attachedVideoPlayer=player;attachedVideoView=surface;attachedVideoSurface=output;}catch(Exception ignored){clearVideoOutputCache();}}
    void renderVideoExpanded(){
        LinearLayout page=column();padding(page,16,8);root.addView(page,new LinearLayout.LayoutParams(-1,-1));LinearLayout top=row();top.addView(backButton(()->{expanded=false;render();}));TextView caption=text("Now playing",20,ink);bold(caption);padding(caption,12,4);top.addView(caption);page.addView(top);
        videoBox=column();videoBox.setGravity(Gravity.CENTER);videoBox.setBackground(round(Color.BLACK,12));videoBox.setClipToOutline(true);LinearLayout.LayoutParams pictureParams=new LinearLayout.LayoutParams(-1,0,1);pictureParams.bottomMargin=dp(12);page.addView(videoBox,pictureParams);addRoundedPicture(videoBox,-1);installInlineVideoGesture();
        videoBox.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(player!=null&&prepared)try{fitVideo(player.getVideoWidth(),player.getVideoHeight());}catch(Exception ignored){}});
        addInlineControls();updatePlayer();
    }
    boolean touchExploration(){android.view.accessibility.AccessibilityManager manager=(android.view.accessibility.AccessibilityManager)getSystemService(Context.ACCESSIBILITY_SERVICE);return manager!=null&&manager.isTouchExplorationEnabled();}
    final Runnable hideInlineControls=()->{if(!appVisible||inlineControls==null)return;if(inlineControls.touching||dragging||(playButton instanceof HoldPlayButton&&((HoldPlayButton)playButton).isHolding())){revealInlineControls();return;}if(!touchExploration())inlineControls.setVisibility(View.GONE);};
    void revealInlineControls(){if(inlineControls==null)return;inlineControls.setVisibility(View.VISIBLE);main.removeCallbacks(hideInlineControls);if(appVisible&&!touchExploration())main.postDelayed(hideInlineControls,5000);}
    void addInlineControls(){
        if(inlineVideoStage==null)return;
        inlineControls=new AutoHideControls(this,()->revealInlineControls());inlineControls.setBackgroundColor(0xCF000000);inlineControls.setPadding(dp(4),dp(4),dp(4),dp(4));
        inlineVideoStage.addView(inlineControls,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        addPlaybackControls(inlineControls,false);revealInlineControls();
    }
    void installInlineVideoGesture(){
        final View preview=surface;
        preview.setOnClickListener(v->{if(fullscreenVideoAvailable())enterTV();});
        final GestureDetector taps=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent event){return true;}
            @Override public boolean onSingleTapConfirmed(MotionEvent event){revealInlineControls();return true;}
            @Override public boolean onDoubleTap(MotionEvent event){if(fullscreenVideoAvailable()){preview.performClick();return true;}return false;}
        });
        View.OnTouchListener touch=(view,event)->{MotionEvent local=MotionEvent.obtain(event);if(view==preview)local.offsetLocation(preview.getLeft(),preview.getTop());taps.onTouchEvent(local);local.recycle();return true;};
        preview.setOnTouchListener(touch);videoBox.setOnTouchListener(touch);videoBox.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        videoBox.setOnClickListener(v->preview.performClick());
        preview.setAccessibilityDelegate(new View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(host,info);if(fullscreenVideoAvailable())info.addAction(new android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK,"Open video fullscreen"));else{info.removeAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);info.setClickable(false);}}});
    }
    boolean fullscreenVideoAvailable(){return current!=null&&(current.kind.equals("video")||current.kind.equals("tv"));}
    void enterTV(){if(tvMode||!fullscreenVideoAvailable())return;fullscreenWasExpanded=expanded;expanded=false;settingsOpen=false;tvMode=true;priorOrientation=getRequestedOrientation();setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);render();}
    void exitTV(){main.removeCallbacks(hideTV);tvMode=false;expanded=fullscreenWasExpanded&&fullscreenVideoAvailable();fullscreenWasExpanded=false;getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);setRequestedOrientation(priorOrientation);render();updatePlayer();}
    final Runnable hideTV=()->{if(tvMode&&appVisible&&tvControls!=null){if(tvControls instanceof AutoHideControls&&((AutoHideControls)tvControls).touching){revealTV();return;}if(tvPlay instanceof HoldPlayButton&&((HoldPlayButton)tvPlay).isHolding()){revealTV();return;}if(!touchExploration()){tvControls.setVisibility(View.GONE);immersiveTV();}}};
    void immersiveTV(){if(!tvMode)return;getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);}
    void revealTV(){if(!tvMode||tvControls==null)return;tvControls.setVisibility(View.VISIBLE);main.removeCallbacks(hideTV);if(appVisible&&!touchExploration())main.postDelayed(hideTV,5000);}
    void renderTV(){
        root.setPadding(0,0,0,0);root.setBackgroundColor(Color.BLACK);root.setOnApplyWindowInsetsListener((v,insets)->{int left=insets.getSystemWindowInsetLeft(),right=insets.getSystemWindowInsetRight(),bottom=insets.getSystemWindowInsetBottom();if(Build.VERSION.SDK_INT>=30){android.graphics.Insets safe=insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());left=safe.left;right=safe.right;bottom=safe.bottom;}if(tvControls!=null)tvControls.setPadding(dp(16)+left,dp(12),dp(16)+right,dp(12)+bottom);return insets;});
        tvStage=new FrameLayout(this);root.addView(tvStage,new LinearLayout.LayoutParams(-1,-1));surface=new SurfaceView(this);tvStage.addView(surface,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));surface.setContentDescription("Video picture. Tap for controls; double-tap to exit fullscreen.");
        surface.setOnClickListener(v->revealTV());
        GestureDetector fullscreenTaps=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent event){return true;}
            @Override public boolean onSingleTapConfirmed(MotionEvent event){if(tvMode&&surface!=null)surface.performClick();return true;}
            @Override public boolean onDoubleTap(MotionEvent event){if(tvMode)exitTV();return true;}
        });
        View.OnTouchListener fullscreenTouch=(view,event)->{fullscreenTaps.onTouchEvent(event);return true;};surface.setOnTouchListener(fullscreenTouch);tvStage.setOnTouchListener(fullscreenTouch);tvStage.setOnClickListener(v->revealTV());
        tvControls=new AutoHideControls(this,()->revealTV());tvControls.setBackgroundColor(0xE6000000);padding(tvControls,16,12);FrameLayout.LayoutParams bar=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);tvStage.addView(tvControls,bar);
        tvTitle=text(current==null?"TV":current.title,17,Color.WHITE);semibold(tvTitle);tvTitle.setMaxLines(1);tvTitle.setEllipsize(TextUtils.TruncateAt.END);tvControls.addView(tvTitle);tvStatus=text(status,11,0xFFCCCCCC);tvControls.addView(tvStatus);
        LinearLayout buttons=row();padding(buttons,0,6);ImageButton back=backButton(()->exitTV());back.setImageDrawable(new FluentIcon(5,Color.WHITE));Button previous=transport("|‹","Previous video/channel",()->{next(-1);revealTV();});tvPlay=holdingPlay(true,false);Button next=transport("›|","Next video/channel",()->{next(1);revealTV();});View[] all={back,previous,tvPlay,next};for(View b:all){if(b instanceof Button)((Button)b).setTextColor(Color.WHITE);boolean skip=b==previous||b==next;b.setBackground(skip?new SkipButtonDrawable(b==previous,0xFF252925,Color.WHITE,getResources().getDisplayMetrics().density):round(0xFF252925,24));if(skip){((Button)b).setCompoundDrawables(null,null,null,null);b.setPadding(0,0,0,0);}LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(48),dp(48));lp.rightMargin=dp(8);buttons.addView(b,lp);}TextView hint=text("Tap picture to reveal controls",11,0xFFCCCCCC);hint.setGravity(Gravity.END);buttons.addView(hint,new LinearLayout.LayoutParams(0,-2,1));tvControls.addView(buttons);
        final SurfaceView fullscreenPicture=(SurfaceView)surface;fullscreenPicture.getHolder().addCallback(new SurfaceHolder.Callback(){public void surfaceCreated(SurfaceHolder h){if(surface==fullscreenPicture)attachVideoOutput();}public void surfaceChanged(SurfaceHolder h,int f,int w,int height){if(surface==fullscreenPicture){attachVideoOutput();if(player!=null&&prepared)try{fitVideo(player.getVideoWidth(),player.getVideoHeight());}catch(Exception ignored){}}}public void surfaceDestroyed(SurfaceHolder h){detachVideoOutput(fullscreenPicture);}});
        tvStage.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(player!=null&&prepared)try{fitVideo(player.getVideoWidth(),player.getVideoHeight());}catch(Exception ignored){}});root.requestApplyInsets();immersiveTV();revealTV();updatePlayer();
    }

    void closeSettings(){settingsOpen=false;render();if(!homeOpen){if(tab==0||tab==2)loadLocal();else loadOnline();}}
    LinearLayout settingsCard(LinearLayout parent,String title){LinearLayout card=column();padding(card,18,16);card.setBackground(round(panel,22));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(14);parent.addView(card,lp);card.setTag("settings:"+title);TextView heading=text(title,18,ink);bold(heading);card.addView(heading);return card;}
    void tintSettingsSwitch(Switch control){accentSwitches.add(control);int switchTone=AccentStyle.switchAccent(accentStyle(),accent);
        int[][] states={new int[]{-android.R.attr.state_enabled},new int[]{android.R.attr.state_checked},new int[]{}};
        control.setThumbTintList(new android.content.res.ColorStateList(states,new int[]{SurfacePalette.mix(panel,muted,.35f),switchTone,muted}));
        control.setTrackTintList(new android.content.res.ColorStateList(states,new int[]{SurfacePalette.mix(panel,muted,.12f),SurfacePalette.mix(panel,switchTone,.48f),SurfacePalette.mix(panel,muted,.28f)}));
        control.setMinimumHeight(dp(48));
    }
    void chooseSettingsCategory(String category){
        rememberSettingsScroll();settingsCategory=SettingsNavigation.normalize(category);settingsScroll=null;settingsPage=null;settingsScrollY=0;settingsAnchor=null;settingsAnchorOffset=0;settingsFadePending=true;render();
    }
    void settingsBack(){if(!landscape&&!settingsCategory.isEmpty())chooseSettingsCategory("");else closeSettings();}
    ScrollView settingsScroller(LinearLayout host){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalFadingEdgeEnabled(true);scroll.setFadingEdgeLength(dp(12));scroll.setPadding(0,0,0,0);scroll.setClipToPadding(true);scroll.setClipChildren(true);
        LinearLayout.LayoutParams settingsScrollParams=new LinearLayout.LayoutParams(-1,0,1);settingsScrollParams.bottomMargin=dp(8);host.addView(scroll,settingsScrollParams);return scroll;
    }
    View settingsCategoryRow(int index,boolean selected,boolean rail){
        LinearLayout item=row();padding(item,rail?12:16,14);item.setMinimumHeight(dp(76));item.setBackground(round(selected?SurfacePalette.mix(panel,accent,.22f):panel,rail?18:22));
        ImageView glyph=new ImageView(this);glyph.setImageDrawable(new FluentIcon(SettingsNavigation.ICONS[index],selectedIconColor()));glyph.setScaleType(ImageView.ScaleType.CENTER_INSIDE);glyph.setBackground(round(SurfacePalette.mix(panel,accent,.18f),14));padding(glyph,10,10);item.addView(glyph,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout words=column();padding(words,12,0);TextView label=text(SettingsNavigation.TITLES[index],rail?14:16,ink);semibold(label);words.addView(label);
        if(!rail){TextView description=text(SettingsNavigation.SUMMARIES[index],13,muted);description.setMaxLines(2);words.addView(description);}item.addView(words,new LinearLayout.LayoutParams(0,-2,1));
        if(!rail){TextView chevron=text("›",24,muted);item.addView(chevron,new LinearLayout.LayoutParams(dp(24),-2));}
        item.setSelected(selected);item.setFocusable(true);item.setContentDescription(SettingsNavigation.TITLES[index]+". "+SettingsNavigation.SUMMARIES[index]+(selected?", selected":""));item.setOnClickListener(v->chooseSettingsCategory(SettingsNavigation.IDS[index]));return item;
    }
    void settingsHome(LinearLayout page){
        TextView hero=text("Settings",40,ink);bold(hero);hero.setPadding(0,dp(22),0,dp(8));page.addView(hero);TextView summary=text("Make Tuhkar feel like yours.",14,muted);summary.setPadding(0,0,0,dp(24));page.addView(summary);
        String[] groups={"Personalize","Listening","Your app"};
        for(int group=0;group<3;group++){TextView caption=text(groups[group],13,selectedIconColor());bold(caption);caption.setPadding(dp(12),dp(4),0,dp(10));page.addView(caption);
            LinearLayout cluster=column();cluster.setBackground(round(panel,26));cluster.setClipToOutline(true);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(20);page.addView(cluster,cp);
            for(int row=0;row<2;row++){cluster.addView(settingsCategoryRow(group*2+row,false,false),new LinearLayout.LayoutParams(-1,-2));if(row==0){View divider=new View(this);divider.setBackgroundColor(line);LinearLayout.LayoutParams dp2=new LinearLayout.LayoutParams(-1,dp(1));dp2.setMargins(dp(70),0,dp(18),0);cluster.addView(divider,dp2);}}
        }
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
        LinearLayout settingsHost=root;
        if(landscape){
            body=row();root.addView(body,new LinearLayout.LayoutParams(-1,0,1));addNavRail(body);
            LinearLayout categories=column();padding(categories,10,12);body.addView(categories,new LinearLayout.LayoutParams(dp(Math.min(260,Math.max(156,getResources().getConfiguration().screenWidthDp*.28f))),-1));
            TextView heading=text("Settings",26,ink);bold(heading);padding(heading,4,12);categories.addView(heading);
            if(settingsCategory.isEmpty()){settingsCategory="appearance";settingsScrollY=0;settingsAnchor=null;settingsAnchorOffset=0;}
            ScrollView rail=new ScrollView(this);rail.setVerticalScrollBarEnabled(false);LinearLayout links=column();rail.addView(links);categories.addView(rail,new LinearLayout.LayoutParams(-1,0,1));
            for(int index=0;index<SettingsNavigation.IDS.length;index++){LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(6);links.addView(settingsCategoryRow(index,SettingsNavigation.IDS[index].equals(settingsCategory),true),rp);}
            settingsHost=column();body.addView(settingsHost,new LinearLayout.LayoutParams(0,-1,1));
        }
        ScrollView scroll=settingsScroller(settingsHost);LinearLayout page=column();padding(page,landscape?20:22,16);scroll.addView(page);sectionPage=page;
        if(settingsFadePending){settingsFadePending=false;if(android.animation.ValueAnimator.areAnimatorsEnabled()){page.setAlpha(.97f);page.animate().alpha(1f).setDuration(140).start();}}
        if(settingsCategory.isEmpty())settingsHome(page);
        else{
            LinearLayout top=row();top.addView(backButton(()->settingsBack()));TextView title=text(SettingsNavigation.TITLES[SettingsNavigation.index(settingsCategory)],28,ink);bold(title);padding(title,14,4);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));page.addView(top);
            TextView summary=text(SettingsNavigation.SUMMARIES[SettingsNavigation.index(settingsCategory)],13,muted);padding(summary,0,16);page.addView(summary);
            switch(settingsCategory){
                case "appearance":appearanceSettings(page);break;
                case "library":librarySettings(page);artworkSettings(page);break;
                case "playback":playbackSettings(page);discGlowSettings(page);break;
                case "audio":audioEnhancementsSettings(page);break;
                case "access":mediaAccessSettings(page);break;
                case "about":aboutSettings(page);break;
            }
        }
        styleSettingsSwitchText(page);restoreSettingsScroll(scroll,page);
    }
    void appearanceSettings(LinearLayout page){
        LinearLayout mode=settingsCard(page,"App mode");mode.addView(text("Choose an app appearance, or follow your device automatically.",13,muted));
        LinearLayout choices=row();padding(choices,0,10);mode.addView(choices);
        String[] ids={"system","light","dark"},labels={"System","Light","Dark"};String selectedMode=AppearanceMode.normalize(prefs.getString("appearanceMode","system"));
        for(int index=0;index<ids.length;index++){final String id=ids[index];boolean selected=id.equals(selectedMode);Button option=button(labels[index],()->{prefs.edit().putString("appearanceMode",id).apply();render();});option.setTextColor(selected?accentInk:ink);option.setBackground(round(selected?accent:normalBg,18));option.setSelected(selected);option.setContentDescription(labels[index]+(selected?", selected":""));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),1);if(index<2)lp.rightMargin=dp(6);choices.addView(option,lp);}
        LinearLayout oled=settingsCard(page,"OLED mode");Switch black=new Switch(this);black.setText("Black background in dark mode");black.setTextColor(ink);black.setTypeface(font);black.setChecked(prefs.getBoolean("oled",false));tintSettingsSwitch(black);black.setOnCheckedChangeListener((b,on)->{prefs.edit().putBoolean("oled",on).apply();render();});oled.addView(black);TextView oledNote=text("Pure black page background when the app uses Dark mode. Cards keep their accent color.",12,muted);padding(oledNote,0,8);oled.addView(oledNote);
        accentSettings(page);
    }
    void artworkSettings(LinearLayout page){
        LinearLayout art=settingsCard(page,"Artwork");Switch downloads=new Switch(this);downloads.setText("Download missing artwork");downloads.setTypeface(font);downloads.setTextColor(ink);downloads.setChecked(prefs.getBoolean("artDownloads",true));tintSettingsSwitch(downloads);downloads.setOnCheckedChangeListener((b,on)->prefs.edit().putBoolean("artDownloads",on).apply());art.addView(downloads);
        TextView privacy=text("Finds missing covers online. Titles and artists are shared with artwork services; your files stay on your phone. May use mobile data.",12,muted);padding(privacy,0,10);art.addView(privacy);
        art.addView(button("Clear artwork cache",()->dialog("Clear artwork cache?").setMessage("Remove saved covers? They can download again.").setPositiveButton("Clear",(d,w)->{artwork.clear();toast("Artwork cache clearing");}).setNegativeButton("Cancel",null).show()));
    }
    void aboutSettings(LinearLayout page){
        LinearLayout resetCard=settingsCard(page,"Reset app");LinearLayout resetRow=row();resetRow.addView(text("Clear Tuhkar settings, saved items and cache. Your actual music/videos are never deleted.",12,muted),new LinearLayout.LayoutParams(0,-2,1));ImageButton reset=toolIcon(17,"Reset Tuhkar",()->confirmReset());reset.setImageDrawable(new FluentIcon(17,Color.parseColor(dark?"#FF6B6B":"#B3261E")));reset.setBackgroundColor(Color.TRANSPARENT);padding(reset,12,12);resetRow.addView(reset,new LinearLayout.LayoutParams(dp(48),dp(48)));resetCard.addView(resetRow);
        LinearLayout info=settingsCard(page,"About");LinearLayout aboutRow=row();aboutRow.addView(text("Tuhkar v2.1.0 · by KnightWin",12,muted),new LinearLayout.LayoutParams(0,-2,1));ImageButton aboutLogo=new ImageButton(this);aboutLogo.setImageResource(R.drawable.tuhkar_wordmark);aboutLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);aboutLogo.setBackgroundColor(Color.TRANSPARENT);padding(aboutLogo,4,4);aboutLogo.setContentDescription("About and privacy");aboutLogo.setTooltipText("About and privacy");aboutLogo.setOnClickListener(v->about());aboutRow.addView(aboutLogo,new LinearLayout.LayoutParams(dp(112),dp(56)));info.addView(aboutRow);
    }
    void playbackSettings(LinearLayout page){
        LinearLayout card=settingsCard(page,"Playback");Switch background=new Switch(this);background.setText("Background video audio");background.setTextColor(ink);background.setTypeface(font);background.setTextSize(14);background.setChecked(prefs.getBoolean("backgroundVideoAudio",false));tintSettingsSwitch(background);
        background.setOnCheckedChangeListener((button,on)->prefs.edit().putBoolean("backgroundVideoAudio",on).apply());card.addView(background);
        card.addView(spacedSettingsNote("Keep local video audio playing when you leave Tuhkar or lock the screen. TV still pauses. Off by default.",12,muted));
    }
    void styleSettingsSwitchText(View view){
        if(view instanceof Switch)((Switch)view).setFontVariationSettings("'wght' 500");
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)styleSettingsSwitchText(group.getChildAt(i));}
    }
    void beginSetup(){generation++;if(mediaSearchPending!=null)main.removeCallbacks(mediaSearchPending);expanded=false;settingsOpen=false;setupOpen=true;setupGo(0);}
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
    void finishSetup(boolean streaming){prefs.edit().putBoolean("askedPlaybackNotifications",true).putBoolean("askedGlowPermission",true).putBoolean("setupComplete",true).remove("setupStep").apply();setupOpen=false;settingsOpen=false;homeOpen=!streaming;expanded=false;query="";sourceStatus="";tab=streaming?1:0;generation++;render();if(streaming)loadOnline();}
    void setupAction(LinearLayout host,Button action,int gap){
        if("Back".contentEquals(action.getText())||"Skip Setup".contentEquals(action.getText()))semibold(action);else bold(action);
        action.setMinHeight(dp(52));action.setMinimumHeight(dp(52));action.setSingleLine(false);action.setMaxLines(3);padding(action,16,10);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(gap);host.addView(action,params);
    }
    boolean standardFolderOnly(int target){return prefs.getBoolean(target==0?"musicStandardFolderOnly":"videoStandardFolderOnly",target==0);}
    void standardFolderOptions(LinearLayout host){
        for(int target:new int[]{0,2}){
            final int kind=target;Switch standard=new Switch(this);standard.setText(target==0?"Music folder only":"Movies folder only");standard.setTextColor(ink);standard.setTypeface(font);standard.setTextSize(14);standard.setMinHeight(dp(48));standard.setChecked(standardFolderOnly(target));tintSettingsSwitch(standard);
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
        LinearLayout card=settingsCard(page,"Media access");
        LinearLayout accessActions=row();accessActions.addView(text(mediaAccessStatus(),13,ink),new LinearLayout.LayoutParams(0,-2,1));ImageButton access=toolIcon(19,"Manage media access",()->requestMedia());access.setBackgroundColor(Color.TRANSPARENT);padding(access,12,12);accessActions.addView(access,new LinearLayout.LayoutParams(dp(48),dp(48)));ImageButton setup=toolIcon(20,"Run setup again",()->beginSetup());setup.setBackgroundColor(Color.TRANSPARENT);padding(setup,12,12);LinearLayout.LayoutParams setupIconParams=new LinearLayout.LayoutParams(dp(48),dp(48));setupIconParams.leftMargin=dp(12);accessActions.addView(setup,setupIconParams);LinearLayout.LayoutParams headerParams=new LinearLayout.LayoutParams(-1,-2);headerParams.bottomMargin=dp(4);card.addView(accessActions,headerParams);standardFolderOptions(card);
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
        if(resetInProgress)return;resetInProgress=true;generation++;if(mediaSearchPending!=null)main.removeCallbacks(mediaSearchPending);cancelPlayHolds();stopDiscAnimation();
        if(playback!=null){playback.listener=null;playback.stopPlayback();}if(equalizer!=null)equalizer.close();queue.clear();queue.setRepeatMode(PlaybackQueue.OFF);queue.setShuffle(false,null);
        getIntent().removeExtra("openNowPlaying");getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);current=null;player=null;prepared=false;playing=false;isVideo=false;pendingNowPlaying=false;expanded=false;tvMode=false;settingsOpen=false;returningFromAccessSettings=false;
        homeEpoch++;homeLocal=null;homeOnline=null;homeLocalLoading=homeOnlineLoading=homePendingRefresh=false;homeLoadedAt=0;homeDataKey="";homeOpen=false;homeCallsign="";homeScrollY=0;saved.clear();localMusic.clear();localVideo.clear();online.clear();shown.clear();query="";sourceStatus="";status="Your library, beautifully simple.";country="ph";fmCountry="all";showingSaved=false;libraryPosition=0;libraryOffset=0;eqSettingsState="";glowUnavailable=false;glowPermissionDialog=false;
        settingsCategory="";settingsScroll=null;settingsPage=null;settingsRestoring=false;settingsAnchor=null;settingsAnchorOffset=0;settingsScrollY=0;releaseOldFolderGrants();prefs.edit().clear().putBoolean("setupStarted",true).putBoolean("setupComplete",false).putInt("setupStep",0).putBoolean("indexedLibraryOnly",true).putBoolean("pendingPlaybackReset",playback==null).commit();
        setupOpen=true;setupStep=0;tab=0;render();
        final ArtworkLoader oldArtwork=artwork;oldArtwork.close();
        new Thread(()->{boolean clean=oldArtwork.resetDiskCache();main.post(()->{if(destroyed)return;artwork=new ArtworkLoader(this,prefs);artwork.setActive(appVisible);resetInProgress=false;if(playback!=null)playback.listener=playbackChanged;render();if(!clean)toast("Settings reset. Some cache files could not be cleared; your media files were not touched.");});},"Tuhkar-reset").start();
    }
    String accentStyle(){return AccentStyle.normalize(prefs.getString("accentStyle","device"));}
    void chooseAccent(String style){prefs.edit().putString("accentStyle",AccentStyle.normalize(style)).remove("accent").apply();render();}
    void accentSettings(LinearLayout page){
        LinearLayout card=settingsCard(page,"Accent color");card.addView(text("Device follows Android. RGB is a slow color wave on controls and discs only; card backgrounds stay steady. Disc lighting follows every selected palette.",12,muted));
        for(int start=0;start<AccentStyle.IDS.length;start+=2){
            LinearLayout choices=row();LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,-2);rowParams.topMargin=dp(8);card.addView(choices,rowParams);
            for(int column=0;column<2;column++){
                int index=start+column;LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1);if(column==0)params.rightMargin=dp(8);
                if(index>=AccentStyle.IDS.length){choices.addView(new Space(this),params);continue;}
                final String id=AccentStyle.IDS[index];String label=AccentStyle.NAMES[index];boolean selected=id.equals(accentStyle());
                Button choice=button((selected?"✓ ":"")+label,()->chooseAccent(id));choice.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);choice.setSingleLine(false);choice.setMinHeight(dp(56));choice.setMinimumHeight(dp(56));choice.setSelected(selected);choice.setContentDescription(label+(selected?", selected":""));padding(choice,8,4);
                choice.setBackground(round(selected?SurfacePalette.mix(panel,AccentStyle.base(id,deviceAccent),.24f):normalBg,16));
                int[] swatchColors=AccentStyle.ring(id,deviceAccent);GradientDrawable swatch=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,swatchColors);swatch.setCornerRadius(dp(10));swatch.setBounds(0,0,dp(22),dp(22));choice.setCompoundDrawables(swatch,null,null,null);choice.setCompoundDrawablePadding(dp(8));choices.addView(choice,params);
            }
        }
    }
    void colorBackground(View view,int color){if(view==null)return;android.graphics.drawable.Drawable drawable=view.getBackground();while(drawable instanceof android.graphics.drawable.InsetDrawable)drawable=((android.graphics.drawable.InsetDrawable)drawable).getDrawable();if(drawable instanceof GradientDrawable)((GradientDrawable)drawable).setColor(color);}
    void scheduleAccentWave(){if(!accentWaveScheduled&&appVisible&&"rgb".equals(accentStyle())){accentWaveScheduled=true;main.postDelayed(accentWave,200);}}
    void applyAccentWave(){accent=AccentStyle.wave(SystemClock.uptimeMillis());accentInk=Color.BLACK;
        if(activeSourceTab!=null){colorBackground(activeSourceTab,accent);activeSourceTab.setTextColor(accentInk);activeSourceTab.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(accentInk));}
        if(nav!=null)for(int i=0;i<nav.getChildCount();i++){View item=nav.getChildAt(i);if(item instanceof ImageButton&&item.isSelected()){colorBackground(item,accent);((ImageButton)item).setImageTintList(android.content.res.ColorStateList.valueOf(accentInk));}}
        if(playButton!=null){colorBackground(playButton,accent);playButton.setTextColor(accentInk);if(playButton instanceof HoldPlayButton)((HoldPlayButton)playButton).accentInk(accentInk);if(playButton instanceof RingPlayButton)((RingPlayButton)playButton).accentColor(accent);}
        for(ImageButton toggle:new ImageButton[]{repeatButton,shuffleButton})if(toggle!=null&&toggle.isSelected()){colorBackground(toggle,accent);toggle.setImageTintList(android.content.res.ColorStateList.valueOf(accentInk));}
        if(favoriteButton!=null&&favoriteButton.isSelected())favoriteButton.setImageTintList(android.content.res.ColorStateList.valueOf(selectedIconColor()));
        if(setupPrimary!=null){colorBackground(setupPrimary,accent);setupPrimary.setTextColor(accentInk);}
        if(homeShuffleButton!=null){colorBackground(homeShuffleButton,accent);homeShuffleButton.setTextColor(accentInk);homeShuffleButton.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(accentInk));}
        if(miniCard!=null)miniCard.accent(accent);
        if(seek!=null){seek.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));seek.setThumbTintList(android.content.res.ColorStateList.valueOf(accent));}
        for(Switch toggle:new ArrayList<>(accentSwitches))tintSettingsSwitch(toggle);
    }
    void about(){dialog("Tuhkar v2.1.0").setMessage("Created by KnightWin · GNU GPL v3\n\nMusic & videos • Radio & TV\nEqualizer, disc effects, themes and saved favorites.\n\nPRIVACY\n• No ads or analytics. Your media files stay on your device.\n• Optional artwork searches share titles/artists with cover services. Turn downloads off in Settings.\n• Beat glow needs Android’s Record audio permission but analyzes only app playback—no microphone recording or saved/uploaded audio.\n• Streams/artwork contact third parties. HTTP links aren’t encrypted.\n• Reset clears settings and cache, never your music/videos.\n\nStreams may be unavailable or region-limited. Use only authorized links.\n\nCredits: Radio Browser · iptv-org · iTunes · Wikipedia\nManrope & Lobster Two fonts · SIL OFL").setPositiveButton("Done",null).show();}
    void swipeSelect(int index){if(index<0||index>=shown.size()||!playbackReady())return;boolean keepPlaying=playing;Item item=shown.get(index);queue.reset(shown,item);if(keepPlaying){playback.play(item);playbackPermission();}else playback.selectPaused(item);}
    void start(Item item){if(!playbackReady())return;playback.play(item);playbackPermission();}
    void fitVideo(int width,int height){if(width<=0||height<=0||surface==null)return;surface.post(()->{if(destroyed||surface==null)return;if(tvMode){if(tvStage==null)return;int w=tvStage.getWidth(),h=tvStage.getHeight();if(w<=0||h<=0)return;float fit=Math.min((float)w/width,(float)h/height);int newW=Math.max(1,(int)(width*fit)),newH=Math.max(1,(int)(height*fit));android.view.ViewGroup.LayoutParams currentSize=surface.getLayoutParams();if(currentSize.width!=newW||currentSize.height!=newH)surface.setLayoutParams(new FrameLayout.LayoutParams(newW,newH,Gravity.CENTER));return;}if(videoBox==null)return;int maxW=videoBox.getWidth(),maxH=expanded?videoBox.getHeight():landscape?dp(150):Math.min(Math.max(1,Math.round((float)maxW*height/width)),dp(getResources().getConfiguration().screenHeightDp*.30f));if(maxW<=0||maxH<=0)return;float scale=Math.min((float)maxW/width,(float)maxH/height);videoBox.setMinimumHeight(maxH);int fittedW=Math.max(1,(int)(width*scale)),fittedH=Math.max(1,(int)(height*scale));android.view.ViewGroup.LayoutParams size=surface.getLayoutParams();if(size.width!=fittedW||size.height!=fittedH)surface.setLayoutParams(new FrameLayout.LayoutParams(fittedW,fittedH,Gravity.CENTER));if(!expanded&&inlineVideoStage!=null){android.view.ViewGroup.LayoutParams stageSize=inlineVideoStage.getLayoutParams();if(stageSize.height!=maxH){stageSize.height=maxH;inlineVideoStage.setLayoutParams(stageSize);}}});}
    void toggle(){if(!playbackReady())return;if(playback.current==null){toast("Choose something to play first.");return;}playback.toggle();}
    void stop(){cancelPlayHolds();if(playback!=null)playback.stopPlayback();}
    void next(int direction){if(playbackReady())playback.next(direction);}
    void updateLandscapeVideoPane(){
        if(landscape&&tab>=2&&!homeOpen&&!settingsOpen&&!setupOpen&&!expanded&&!tvMode&&playerBox!=null)
            playerBox.setVisibility(inlineVideoVisible()?View.VISIBLE:View.GONE);
    }
    void updatePlayer(){
        updateLandscapeVideoPane();
        updateQueueButtons();updateArtworkMotion();updateRingProgress();
        if(deck!=null&&current!=null&&!settingsOpen&&!setupOpen&&!expanded)for(int i=0;i<shown.size();i++)if(shown.get(i).url.equals(current.url)){deck.align(i);break;}
        if(tvMode){if(tvTitle!=null)tvTitle.setText(current==null?"TV":current.title);if(tvStatus!=null)tvStatus.setText(status);if(tvPlay!=null)tvPlay.setText(playing?"Ⅱ":"▶");return;}
        if(playerBox!=null&&!expanded)playerBox.setContentDescription((current==null?"Player":current.title)+". "+status+". Tap to open Now Playing.");if(nowTitle!=null)nowTitle.setText(current==null?"Ready when you are":current.title);if(nowDetail!=null)nowDetail.setText(status);if(playButton!=null)playButton.setText(playing?"Ⅱ":"▶");if(videoBox!=null)videoBox.setVisibility(inlineVideoVisible()?View.VISIBLE:View.GONE);if(surface!=null&&!tvMode)surface.setContentDescription(current!=null&&current.kind.equals("tv")?"TV preview. Double-tap for landscape fullscreen.":"Video preview. Double-tap for landscape fullscreen.");if(seek!=null)seek.setEnabled(prepared);
        if(expanded&&disc!=null&&current!=null&&!current.url.equals(discUrl)){discUrl=current.url;artwork.bind(disc,current,accent);configureDisc(disc,current);}syncDisc();updateAudioMiniPlayer();
    }
    String format(long ms){long seconds=Math.max(0,ms/1000);return seconds>=3600?String.format(Locale.ROOT,"%d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60):String.format(Locale.ROOT,"%d:%02d",seconds/60,seconds%60);}
    void updateRingProgress(){updateAudioMiniPlayer();if(playButton instanceof RingPlayButton){RingPlayButton ring=(RingPlayButton)playButton;ring.media(current==null?"":current.url);ring.progress(playback==null?0:playback.position(),playback==null?0:playback.duration(),playing);}}
    final Runnable ticker=new Runnable(){public void run(){if(destroyed||!appVisible)return;syncDisc();updateRingProgress();if(!tvMode&&player!=null&&prepared&&seek!=null&&time!=null)try{int duration=player.getDuration(),pos=player.getCurrentPosition();if(!dragging&&duration>0)seek.setProgress((int)((long)pos*1000/duration));seek.setEnabled(duration>0&&!((current!=null)&&(current.kind.equals("radio")||current.kind.equals("tv"))));time.setText(duration>0?format(pos)+" / "+format(duration):"LIVE");}catch(Exception ignored){}else if(time!=null){time.setText("0:00");if(seek!=null)seek.setProgress(0);}main.postDelayed(this,500);}};
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override public boolean dispatchTouchEvent(MotionEvent event){if(event.getActionMasked()==MotionEvent.ACTION_DOWN||event.getActionMasked()==MotionEvent.ACTION_UP){if(mediaSearchVisible||!mediaSearchQuery.trim().isEmpty())touchMediaSearch();}return super.dispatchTouchEvent(event);}
    @Override public void onBackPressed(){if(resetInProgress)return;if(setupOpen){setupBack();return;}if(tvMode){exitTV();return;}if(expanded){expanded=false;render();return;}if(settingsOpen){settingsBack();return;}if(mediaSearchVisible||!mediaSearchQuery.trim().isEmpty()){dismissMediaSearch();return;}if(!homeOpen){openHome();return;}super.onBackPressed();}
    @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);render();updatePlayer();if(player!=null&&prepared&&isVideo)try{fitVideo(player.getVideoWidth(),player.getVideoHeight());}catch(Exception ignored){}}
    @Override public void onStop(){
        cancelSectionTransition();videoOutput.setRetaining(false);super.onStop();rememberSettingsScroll();cancelPlayHolds();appVisible=false;main.removeCallbacks(accentWave);accentWaveScheduled=false;main.removeCallbacks(hideTV);main.removeCallbacks(hideInlineControls);syncDisc();updateArtworkMotion();if(artwork!=null)artwork.setActive(false);main.removeCallbacks(ticker);
        if(list!=null){libraryPosition=list.getFirstVisiblePosition();View first=list.getChildAt(0);libraryOffset=first==null?0:first.getTop();}else if(grid!=null)libraryPosition=grid.getFirstVisiblePosition();
        if(playback!=null){playback.background();if(playback.listener==playbackChanged)playback.listener=null;}
        if(serviceBound){unbindService(playbackConnection);serviceBound=false;}clearVideoOutputCache();playback=null;player=null;
    }
    @Override public void onStart(){super.onStart();searchSongs.clear();searchVideos.clear();appVisible=true;scheduleAccentWave();syncDisc();if(artwork!=null)artwork.setActive(true);main.removeCallbacks(ticker);main.post(ticker);if(!serviceBound)serviceBound=bindService(new Intent(this,PlaybackService.class),playbackConnection,Context.BIND_AUTO_CREATE);}
    @Override public void onTrimMemory(int level){super.onTrimMemory(level);if(level>=android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW){searchSongs.clear();searchVideos.clear();if(artwork!=null)artwork.trimMemory(level);}}
    @Override public void onLowMemory(){super.onLowMemory();searchSongs.clear();searchVideos.clear();if(artwork!=null)artwork.trimMemory(android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE);}
    @Override public void onDestroy(){if(portraitSearchExpansion!=null)portraitSearchExpansion.cancel();if(portraitSearchOverlay!=null)portraitSearchOverlay.animate().cancel();cancelSectionTransition();videoOutput.setRetaining(false);cancelPlayHolds();stopDiscAnimation();if(reactiveGlow!=null)reactiveGlow.stop();destroyed=true;generation++;main.removeCallbacksAndMessages(null);if(playback!=null&&playback.listener==playbackChanged)playback.listener=null;if(serviceBound){unbindService(playbackConnection);serviceBound=false;}if(!serviceEq&&equalizer!=null)equalizer.close();io.shutdownNow();if(artwork!=null)artwork.close();super.onDestroy();}
}
