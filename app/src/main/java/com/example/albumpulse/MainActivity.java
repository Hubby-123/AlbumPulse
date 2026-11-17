package com.example.albumpulse;

import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class MainActivity extends Activity {
    static final String PREFS="album_pulse_settings", FALLBACK="fallback_uri", ENABLED="dynamic_enabled";
    private Switch toggle;
    private TextView accessStatus, wallpaperStatus, modeStatus;
    private Button accessButton;
    private static final int PICK=410;
    private final int bg=0xFF0A0C0F, surface=0xFF181C20, accent=0xFFD7FF5F, muted=0xFFA4AEB5, white=0xFFF5F7F2;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        buildInterface();
        refresh();
    }

    private void buildInterface() {
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF141A19,bg,bg}));
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22),dp(22),dp(22),dp(30));
        scroll.addView(root);

        LinearLayout brand=new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo=new ImageView(this);
        logo.setImageResource(R.drawable.ic_album_pulse);
        brand.addView(logo,new LinearLayout.LayoutParams(dp(46),dp(46)));
        LinearLayout brandText=new LinearLayout(this);
        brandText.setOrientation(LinearLayout.VERTICAL);
        brandText.setPadding(dp(12),0,0,0);
        TextView name=txt("ALBUMPULSE",13,accent,Typeface.BOLD); name.setLetterSpacing(.16f);
        brandText.addView(name);
        TextView tagline=txt("MUSIC-POWERED WALLPAPERS",10,muted,Typeface.NORMAL); tagline.setLetterSpacing(.09f);
        brandText.addView(tagline);
        brand.addView(brandText,new LinearLayout.LayoutParams(0,-2,1));
        TextView beta=txt("BETA",10,accent,Typeface.BOLD);
        beta.setPadding(dp(11),dp(7),dp(11),dp(7)); beta.setBackground(shape(0xFF27331F,18));
        brand.addView(beta); root.addView(brand);

        TextView hero=txt("Your music.\nYour lock screen.",34,white,Typeface.BOLD);
        hero.setLineSpacing(dp(1),1f);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2); hp.topMargin=dp(30);
        root.addView(hero,hp);
        TextView intro=txt("Album artwork takes over your lock-screen wallpaper while a track is playing. Your chosen wallpaper returns when playback stops.",14,muted,Typeface.NORMAL);
        intro.setLineSpacing(dp(4),1f);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,-2); ip.topMargin=dp(10); ip.bottomMargin=dp(22);
        root.addView(intro,ip);

        LinearLayout mode=card();
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        TextView label=txt("DYNAMIC WALLPAPER",11,muted,Typeface.BOLD); label.setLetterSpacing(.12f);
        row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        modeStatus=txt("INACTIVE",10,muted,Typeface.BOLD);
        modeStatus.setPadding(dp(10),dp(6),dp(10),dp(6)); modeStatus.setBackground(shape(0xFF2A2F33,30));
        row.addView(modeStatus); mode.addView(row);
        toggle=new Switch(this); toggle.setText("Enable album artwork"); toggle.setTextColor(white); toggle.setTextSize(16);
        toggle.setPadding(0,dp(16),0,dp(6));
        toggle.setButtonTintList(android.content.res.ColorStateList.valueOf(accent));
        toggle.setOnCheckedChangeListener((v,on)->setEnabled(on)); mode.addView(toggle);
        TextView hint=txt("Works with music apps that publish Android media-session artwork.",12,muted,Typeface.NORMAL);
        hint.setLineSpacing(dp(3),1f); mode.addView(hint); root.addView(mode);

        section(root,"SETUP","Two quick steps to get started");
        LinearLayout wallpaper=card();
        step(wallpaper,"01","Choose your normal wallpaper","This is restored whenever music stops.");
        wallpaperStatus=txt("No wallpaper selected",12,muted,Typeface.NORMAL);
        LinearLayout.LayoutParams wsp=new LinearLayout.LayoutParams(-1,-2); wsp.topMargin=dp(10);
        wallpaper.addView(wallpaperStatus,wsp);
        Button choose=button("Choose wallpaper"); choose.setOnClickListener(v->chooseWallpaper());
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(48)); cp.topMargin=dp(14);
        wallpaper.addView(choose,cp); root.addView(wallpaper);

        LinearLayout permission=card();
        step(permission,"02","Allow media detection","Android uses Notification Access to read active music-session details and album art.");
        accessStatus=txt("Permission not enabled",12,muted,Typeface.NORMAL);
        LinearLayout.LayoutParams asp=new LinearLayout.LayoutParams(-1,-2); asp.topMargin=dp(10);
        permission.addView(accessStatus,asp);
        accessButton=button("Open access settings");
        accessButton.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(48)); ap.topMargin=dp(14);
        permission.addView(accessButton,ap); root.addView(permission);

        section(root,"GOOD TO KNOW","A couple of Android-specific details");
        LinearLayout info=card();
        info(info,"LOCK-SCREEN NOTIFICATIONS","Android controls whether notifications from other apps appear on the lock screen. AlbumPulse won't dismiss them or change their settings.");
        View line=new View(this); line.setBackgroundColor(0xFF343B3E);
        LinearLayout.LayoutParams linep=new LinearLayout.LayoutParams(-1,dp(1)); linep.topMargin=dp(15); linep.bottomMargin=dp(15);
        info.addView(line,linep);
        info(info,"SUPPORTED PLAYBACK","Spotify, YouTube Music and other players can work when they expose album art through Android media sessions.");
        root.addView(info);
        TextView footer=txt("MADE FOR THE MOMENT BETWEEN PLAY AND PAUSE",9,0xFF6F7B7F,Typeface.BOLD);
        footer.setGravity(Gravity.CENTER); footer.setLetterSpacing(.08f);
        LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2); fp.topMargin=dp(26);
        root.addView(footer,fp);
        setContentView(scroll);
    }

    private void section(LinearLayout root,String title,String subtitle) {
        TextView h=txt(title,10,accent,Typeface.BOLD); h.setLetterSpacing(.16f);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2); hp.topMargin=dp(25); root.addView(h,hp);
        TextView s=txt(subtitle,17,white,Typeface.BOLD);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2); sp.topMargin=dp(5); sp.bottomMargin=dp(12); root.addView(s,sp);
    }

    private LinearLayout card() {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18),dp(18),dp(18),dp(18)); box.setBackground(shape(surface,22)); box.setElevation(dp(2));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(12); box.setLayoutParams(p);
        return box;
    }

    private void step(LinearLayout box,String number,String title,String detail) {
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.TOP);
        TextView n=txt(number,12,accent,Typeface.BOLD); n.setGravity(Gravity.CENTER); n.setBackground(shape(0xFF2B3723,12));
        row.addView(n,new LinearLayout.LayoutParams(dp(36),dp(32)));
        LinearLayout copy=new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); copy.setPadding(dp(12),0,0,0);
        copy.addView(txt(title,16,white,Typeface.BOLD));
        TextView d=txt(detail,12,muted,Typeface.NORMAL); d.setLineSpacing(dp(3),1f);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(5); copy.addView(d,p);
        row.addView(copy,new LinearLayout.LayoutParams(0,-2,1)); box.addView(row);
    }

    private void info(LinearLayout box,String title,String body) {
        TextView h=txt(title,10,accent,Typeface.BOLD); h.setLetterSpacing(.1f); box.addView(h);
        TextView d=txt(body,12,muted,Typeface.NORMAL); d.setLineSpacing(dp(4),1f);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(6); box.addView(d,p);
    }

    private Button button(String label) {
        Button b=new Button(this); b.setText(label); b.setTextAllCaps(false); b.setTextColor(0xFF11160F);
        b.setTextSize(14); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setBackground(shape(accent,14));
        b.setPadding(dp(12),0,dp(12),0); b.setStateListAnimator(null); return b;
    }

    private TextView txt(String value,float size,int color,int style) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT,style); return t;
    }

    private GradientDrawable shape(int color,float radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }

    private int dp(float value) { return (int)(value*getResources().getDisplayMetrics().density+.5f); }

    private void chooseWallpaper() {
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("image/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i,PICK);
    }

    @Override protected void onResume() { super.onResume(); refresh(); }

    private void refresh() {
        String enabled=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");
        boolean access=enabled!=null&&enabled.contains(getPackageName());
        SharedPreferences p=getSharedPreferences(PREFS,0);
        String uri=p.getString(FALLBACK,null); boolean on=p.getBoolean(ENABLED,false);
        accessStatus.setText(access?"Notification Access is connected":"Notification Access is needed");
        accessStatus.setTextColor(access?accent:muted);
        wallpaperStatus.setText(uri==null?"No wallpaper selected":"Your normal wallpaper is ready");
        wallpaperStatus.setTextColor(uri==null?muted:accent);
        modeStatus.setText(on?"NOW ACTIVE":"INACTIVE"); modeStatus.setTextColor(on?accent:muted);
        modeStatus.setBackground(shape(on?0xFF2B3723:0xFF2A2F33,30));
        toggle.setOnCheckedChangeListener(null); toggle.setChecked(on); toggle.setEnabled(access&&uri!=null);
        toggle.setOnCheckedChangeListener((v,checked)->setEnabled(checked));
        accessButton.setText(access?"Manage access settings":"Grant Notification Access");
    }

    @Override protected void onActivityResult(int req,int result,Intent data) {
        super.onActivityResult(req,result,data);
        if(req==PICK&&result==RESULT_OK&&data!=null&&data.getData()!=null) {
            Uri u=data.getData();
            try {
                getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getSharedPreferences(PREFS,0).edit().putString(FALLBACK,u.toString()).apply();
                WallpaperEngine.applyFallback(this);
            } catch(Exception ignored) {}
            refresh();
        }
    }

    private void setEnabled(boolean on) {
        SharedPreferences p=getSharedPreferences(PREFS,0);
        if(on&&p.getString(FALLBACK,null)==null) {
            Toast.makeText(this,"Choose a normal wallpaper first.",Toast.LENGTH_SHORT).show(); refresh(); return;
        }
        p.edit().putBoolean(ENABLED,on).apply();
        if(!on) { WallpaperEngine.applyFallback(this); p.edit().remove("last_dynamic_key").apply(); }
        else try { android.service.notification.NotificationListenerService.requestRebind(new ComponentName(this,MusicListenerService.class)); }
        catch(Exception ignored) {}
        refresh();
    }
}