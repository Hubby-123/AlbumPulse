package com.example.albumpulse;

import android.app.Activity;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadata;
import android.media.session.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.*;
import java.util.List;

public class MainActivity extends Activity {
    static final String PREFS="album_pulse_settings", FALLBACK="fallback_uri", ENABLED="dynamic_enabled";
    private final int bg=0xFF0B0D0E, panel=0xFF171A1B, accent=0xFFD7FF5F, muted=0xFF929A99, white=0xFFF2F4EF;
    private static final int PICK=410;
    private Switch toggle;
    private TextView permissionText,wallpaperText,trackTitle,trackArtist,elapsedText,durationText,playButton,modeText;
    private SeekBar progress;
    private Button permissionButton;
    private MediaSessionManager sessions;
    private MediaController controller;
    private MediaController.Callback callback;
    private boolean dragging=false;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable ticker=new Runnable(){public void run(){updateProgress();handler.postDelayed(this,500);}};

    @Override public void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);
        sessions=(MediaSessionManager)getSystemService(MEDIA_SESSION_SERVICE);buildUi();
    }
    private void buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(bg);
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(22),dp(24),dp(22),dp(28));scroll.addView(root);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.ic_album_pulse);top.addView(logo,new LinearLayout.LayoutParams(dp(38),dp(38)));
        TextView brand=label("AlbumPulse",20,white,Typeface.BOLD);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1);bp.leftMargin=dp(11);top.addView(brand,bp);
        modeText=label("OFF",10,muted,Typeface.BOLD);modeText.setLetterSpacing(.12f);top.addView(modeText);root.addView(top);
        TextView headline=label("Your music,\nyour wallpaper.",31,white,Typeface.BOLD);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=dp(34);root.addView(headline,hp);
        TextView sub=label("Album art on your lock screen. Nothing extra.",14,muted,Typeface.NORMAL);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(8);root.addView(sub,sp);

        LinearLayout player=panel();LinearLayout ph=new LinearLayout(this);ph.setGravity(Gravity.CENTER_VERTICAL);
        TextView now=label("NOW PLAYING",10,accent,Typeface.BOLD);now.setLetterSpacing(.14f);ph.addView(now,new LinearLayout.LayoutParams(0,-2,1));
        TextView source=label("MEDIA SESSION",9,muted,Typeface.BOLD);source.setLetterSpacing(.08f);ph.addView(source);player.addView(ph);
        trackTitle=label("Nothing playing",18,white,Typeface.BOLD);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=dp(22);player.addView(trackTitle,tp);
        trackArtist=label("Start music in a supported player",13,muted,Typeface.NORMAL);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=dp(5);player.addView(trackArtist,ap);
        progress=new SeekBar(this);progress.setMax(1000);progress.setPadding(0,0,0,0);
        progress.setProgressTintList(ColorStateList.valueOf(accent));progress.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF3A403E));progress.setThumbTintList(ColorStateList.valueOf(accent));
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(28));pp.topMargin=dp(18);player.addView(progress,pp);
        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int v,boolean u){}
            public void onStartTrackingTouch(SeekBar s){dragging=true;}
            public void onStopTrackingTouch(SeekBar s){dragging=false;if(controller!=null){long d=duration();if(d>0)controller.getTransportControls().seekTo(d*s.getProgress()/1000);}}
        });
        LinearLayout times=new LinearLayout(this);times.setGravity(Gravity.CENTER_VERTICAL);
        elapsedText=label("0:00",11,muted,Typeface.NORMAL);durationText=label("0:00",11,muted,Typeface.NORMAL);
        times.addView(elapsedText,new LinearLayout.LayoutParams(0,-2,1));times.addView(durationText);player.addView(times);
        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);controls.setPadding(0,dp(14),0,0);
        controls.addView(control("Ⅱ‹",()->{if(controller!=null)controller.getTransportControls().skipToPrevious();}));
        playButton=control("▶",()->togglePlayback());LinearLayout.LayoutParams playp=new LinearLayout.LayoutParams(dp(64),dp(54));playp.leftMargin=dp(26);playp.rightMargin=dp(26);controls.addView(playButton,playp);
        controls.addView(control("›Ⅱ",()->{if(controller!=null)controller.getTransportControls().skipToNext();}));player.addView(controls);
        LinearLayout.LayoutParams plp=new LinearLayout.LayoutParams(-1,-2);plp.topMargin=dp(28);root.addView(player,plp);

        LinearLayout settings=panel();TextView st=label("WALLPAPER",10,accent,Typeface.BOLD);st.setLetterSpacing(.14f);settings.addView(st);
        LinearLayout tr=new LinearLayout(this);tr.setGravity(Gravity.CENTER_VERTICAL);tr.addView(label("Album artwork wallpaper",15,white,Typeface.BOLD),new LinearLayout.LayoutParams(0,-2,1));
        toggle=new Switch(this);toggle.setButtonTintList(ColorStateList.valueOf(accent));tr.addView(toggle);settings.addView(tr);toggle.setOnCheckedChangeListener((v,on)->setEnabled(on));
        wallpaperText=label("Choose a fallback image to begin.",12,muted,Typeface.NORMAL);LinearLayout.LayoutParams wtp=new LinearLayout.LayoutParams(-1,-2);wtp.topMargin=dp(5);settings.addView(wallpaperText,wtp);
        Button choose=actionButton("Choose fallback wallpaper");LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(44));cp.topMargin=dp(13);settings.addView(choose,cp);choose.setOnClickListener(v->chooseWallpaper());
        LinearLayout.LayoutParams setp=new LinearLayout.LayoutParams(-1,-2);setp.topMargin=dp(14);root.addView(settings,setp);

        LinearLayout access=panel();TextView at=label("MEDIA ACCESS",10,accent,Typeface.BOLD);at.setLetterSpacing(.14f);access.addView(at);
        permissionText=label("Notification access is required to detect playback.",12,muted,Typeface.NORMAL);LinearLayout.LayoutParams apt=new LinearLayout.LayoutParams(-1,-2);apt.topMargin=dp(8);access.addView(permissionText,apt);
        permissionButton=actionButton("Open access settings");LinearLayout.LayoutParams abp=new LinearLayout.LayoutParams(-1,dp(44));abp.topMargin=dp(12);access.addView(permissionButton,abp);
        permissionButton.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        LinearLayout.LayoutParams acp=new LinearLayout.LayoutParams(-1,-2);acp.topMargin=dp(12);root.addView(access,acp);
        TextView foot=label("While locked, use the media controls shown by your music app to skip tracks.",11,muted,Typeface.NORMAL);foot.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2);fp.topMargin=dp(20);root.addView(foot,fp);setContentView(scroll);refreshSettings();
    }
    private LinearLayout panel(){LinearLayout b=new LinearLayout(this);b.setOrientation(1);b.setPadding(dp(18),dp(18),dp(18),dp(16));GradientDrawable d=new GradientDrawable();d.setColor(panel);d.setCornerRadius(dp(18));b.setBackground(d);return b;}
    private TextView label(String s,float size,int color,int style){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.DEFAULT,style);return t;}
    private TextView control(String glyph,Runnable action){TextView t=label(glyph,22,white,Typeface.BOLD);t.setGravity(Gravity.CENTER);t.setOnClickListener(v->action.run());return t;}
    private Button actionButton(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(13);b.setTextColor(white);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);GradientDrawable d=new GradientDrawable();d.setColor(0xFF292F2D);d.setCornerRadius(dp(12));b.setBackground(d);b.setStateListAnimator(null);return b;}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private void chooseWallpaper(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("image/*");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,PICK);}
    @Override protected void onResume(){super.onResume();refreshSettings();connectMedia();handler.removeCallbacks(ticker);handler.post(ticker);}
    @Override protected void onPause(){handler.removeCallbacks(ticker);disconnectMedia();super.onPause();}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==PICK&&res==RESULT_OK&&data!=null&&data.getData()!=null){Uri u=data.getData();try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);getSharedPreferences(PREFS,0).edit().putString(FALLBACK,u.toString()).apply();WallpaperEngine.applyFallback(this);}catch(Exception ignored){}refreshSettings();}}
    private boolean hasAccess(){String e=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");return e!=null&&e.contains(getPackageName());}
    private void refreshSettings(){if(toggle==null)return;SharedPreferences p=getSharedPreferences(PREFS,0);boolean a=hasAccess(),on=p.getBoolean(ENABLED,false),has=p.getString(FALLBACK,null)!=null;
        permissionText.setText(a?"Connected":"Allow Notification Access to detect music.");permissionText.setTextColor(a?accent:muted);permissionButton.setText(a?"Manage access":"Grant access");
        wallpaperText.setText(has?"Fallback wallpaper is set.":"Choose a fallback image to begin.");wallpaperText.setTextColor(has?accent:muted);modeText.setText(on?"ON":"OFF");modeText.setTextColor(on?accent:muted);
        toggle.setOnCheckedChangeListener(null);toggle.setChecked(on);toggle.setEnabled(a&&has);toggle.setOnCheckedChangeListener((v,c)->setEnabled(c));}
    private void setEnabled(boolean on){SharedPreferences p=getSharedPreferences(PREFS,0);if(on&&p.getString(FALLBACK,null)==null){Toast.makeText(this,"Choose a fallback wallpaper first.",Toast.LENGTH_SHORT).show();refreshSettings();return;}
        p.edit().putBoolean(ENABLED,on).apply();if(!on){WallpaperEngine.applyFallback(this);p.edit().remove("last_dynamic_key").apply();}else try{android.service.notification.NotificationListenerService.requestRebind(new ComponentName(this,MusicListenerService.class));}catch(Exception ignored){}refreshSettings();}
    private void connectMedia(){if(!hasAccess()||sessions==null){showNoTrack();return;}try{List<MediaController> list=sessions.getActiveSessions(new ComponentName(this,MusicListenerService.class));MediaController chosen=null;
        for(MediaController c:list){PlaybackState s=c.getPlaybackState();if(s!=null&&s.getState()!=PlaybackState.STATE_NONE&&s.getState()!=PlaybackState.STATE_STOPPED){chosen=c;break;}}
        if(chosen==null&&!list.isEmpty())chosen=list.get(0);if(chosen!=controller){disconnectMedia();controller=chosen;if(controller!=null){callback=new MediaController.Callback(){@Override public void onMetadataChanged(MediaMetadata m){runOnUiThread(()->showTrack());}@Override public void onPlaybackStateChanged(PlaybackState s){runOnUiThread(()->showTrack());}};controller.registerCallback(callback);}}showTrack();
        }catch(SecurityException e){showNoTrack();}}
    private void disconnectMedia(){if(controller!=null&&callback!=null)try{controller.unregisterCallback(callback);}catch(Exception ignored){}controller=null;callback=null;}
    private void showNoTrack(){trackTitle.setText("Nothing playing");trackArtist.setText("Start music in a supported player");playButton.setText("▶");progress.setProgress(0);elapsedText.setText("0:00");durationText.setText("0:00");}
    private void showTrack(){if(controller==null){showNoTrack();return;}MediaMetadata m=controller.getMetadata();if(m==null){showNoTrack();return;}CharSequence title=m.getText(MediaMetadata.METADATA_KEY_TITLE),artist=m.getText(MediaMetadata.METADATA_KEY_ARTIST);
        trackTitle.setText(title==null?"Unknown track":title);trackArtist.setText(artist==null?controller.getPackageName():artist);updateProgress();}
    private long duration(){return controller==null||controller.getMetadata()==null?0:controller.getMetadata().getLong(MediaMetadata.METADATA_KEY_DURATION);}
    private void updateProgress(){if(controller==null)return;PlaybackState s=controller.getPlaybackState();long d=duration(),pos=s==null?0:Math.max(0,s.getPosition());
        if(s!=null&&s.getState()==PlaybackState.STATE_PLAYING)pos+=(long)((SystemClock.elapsedRealtime()-s.getLastPositionUpdateTime())*s.getPlaybackSpeed());if(d>0)pos=Math.min(pos,d);
        if(!dragging)progress.setProgress(d>0?(int)(pos*1000/d):0);elapsedText.setText(format(pos));durationText.setText(format(d));playButton.setText(s!=null&&s.getState()==PlaybackState.STATE_PLAYING?"Ⅱ":"▶");}
    private String format(long ms){long s=Math.max(0,ms)/1000;return String.format(java.util.Locale.getDefault(),"%d:%02d",s/60,s%60);}
    private void togglePlayback(){if(controller==null)return;PlaybackState s=controller.getPlaybackState();if(s!=null&&s.getState()==PlaybackState.STATE_PLAYING)controller.getTransportControls().pause();else controller.getTransportControls().play();}
}