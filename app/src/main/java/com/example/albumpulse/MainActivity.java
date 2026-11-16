package com.example.albumpulse;

import android.app.Activity;
import android.content.*;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import android.graphics.Color;

public class MainActivity extends Activity {
    static final String PREFS="album_pulse_settings", FALLBACK="fallback_uri", ENABLED="dynamic_enabled";
    private Switch toggle;
    private TextView status;
    private static final int PICK=410;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32,48,32,32);
        root.setBackgroundColor(Color.rgb(9,11,13));
        TextView title=new TextView(this);
        title.setText("ALBUMPULSE\n\nYour music. Your lock screen.");
        title.setTextColor(Color.WHITE); title.setTextSize(27);
        root.addView(title);
        TextView desc=new TextView(this);
        desc.setText("\nAlbum art becomes your lock-screen wallpaper while music plays. Your normal wallpaper returns when playback stops.\n");
        desc.setTextColor(0xFFB0B7BC); root.addView(desc);
        Button choose=new Button(this); choose.setText("Choose normal wallpaper");
        choose.setOnClickListener(v -> {
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("image/*");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(i,PICK);
        }); root.addView(choose);
        Button access=new Button(this); access.setText("Open Notification Access settings");
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        root.addView(access);
        status=new TextView(this); status.setTextColor(Color.WHITE); root.addView(status);
        toggle=new Switch(this); toggle.setText("Enable dynamic lock-screen artwork");
        toggle.setTextColor(Color.WHITE);
        toggle.setOnCheckedChangeListener((v,on)->setEnabled(on)); root.addView(toggle);
        setContentView(root); refresh();
    }
    @Override protected void onResume(){super.onResume();refresh();}
    private void refresh(){
        boolean access=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners")!=null &&
            Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners").contains(getPackageName());
        String uri=getSharedPreferences(PREFS,0).getString(FALLBACK,null);
        status.setText(access?"Notification access: enabled":"Notification access: not enabled");
        toggle.setOnCheckedChangeListener(null);
        toggle.setChecked(getSharedPreferences(PREFS,0).getBoolean(ENABLED,false));
        toggle.setEnabled(access && uri!=null);
        toggle.setOnCheckedChangeListener((v,on)->setEnabled(on));
    }
    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);
        if(req==PICK && result==RESULT_OK && data!=null && data.getData()!=null){
            Uri u=data.getData();
            try{
                getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getSharedPreferences(PREFS,0).edit().putString(FALLBACK,u.toString()).apply();
                WallpaperEngine.applyFallback(this);
            }catch(Exception ignored){}
            refresh();
        }
    }
    private void setEnabled(boolean on){
        SharedPreferences p=getSharedPreferences(PREFS,0);
        if(on && p.getString(FALLBACK,null)==null){Toast.makeText(this,"Choose a normal wallpaper first.",Toast.LENGTH_SHORT).show();refresh();return;}
        p.edit().putBoolean(ENABLED,on).apply();
        if(!on){WallpaperEngine.applyFallback(this);p.edit().remove("last_dynamic_key").apply();}
        else {try{android.service.notification.NotificationListenerService.requestRebind(new ComponentName(this,MusicListenerService.class));}catch(Exception ignored){}}
        refresh();
    }
}
