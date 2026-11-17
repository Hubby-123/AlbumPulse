package com.example.albumpulse;

import android.content.*;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.*;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.notification.*;
import java.util.*;

public class MusicListenerService extends NotificationListenerService {
    private static final String PREFS="album_pulse_settings", ENABLED="dynamic_enabled", LAST="last_dynamic_key";
    private MediaSessionManager manager;
    private final Map<MediaController,MediaController.Callback> callbacks=new HashMap<>();
    private MediaSessionManager.OnActiveSessionsChangedListener listener;
    private boolean applied=false;
    private final Handler handler=new Handler(Looper.getMainLooper());

    @Override public void onListenerConnected(){
        super.onListenerConnected();
        manager=(MediaSessionManager)getSystemService(MEDIA_SESSION_SERVICE);
        if(manager==null)return;
        listener=this::sync;
        try{manager.addOnActiveSessionsChangedListener(listener,new ComponentName(this,MusicListenerService.class));}catch(SecurityException ignored){}
        refresh();
    }
    @Override public void onNotificationPosted(StatusBarNotification n){refresh();}
    @Override public void onNotificationRemoved(StatusBarNotification n){refresh();}
    @Override public void onListenerDisconnected(){cleanup();restore();super.onListenerDisconnected();}
    @Override public void onDestroy(){cleanup();restore();super.onDestroy();}

    private void refresh(){
        if(manager==null)return;
        try{sync(manager.getActiveSessions(new ComponentName(this,MusicListenerService.class)));}catch(SecurityException ignored){}
    }
    private void sync(List<MediaController> active){
        if(active==null)active=Collections.emptyList();
        for(MediaController c:new ArrayList<>(callbacks.keySet())){
            if(!active.contains(c)){MediaController.Callback cb=callbacks.remove(c);if(cb!=null)c.unregisterCallback(cb);}
        }
        for(MediaController c:active)if(!callbacks.containsKey(c)){
            MediaController.Callback cb=new MediaController.Callback(){
                @Override public void onPlaybackStateChanged(PlaybackState s){update();}
                @Override public void onMetadataChanged(MediaMetadata m){update();}
                @Override public void onSessionDestroyed(){refresh();}
            };
            callbacks.put(c,cb);c.registerCallback(cb);
        }
        update();
    }
    private void update(){
        if(!getSharedPreferences(PREFS,0).getBoolean(ENABLED,false)){restore();return;}
        for(MediaController c:callbacks.keySet()){
            PlaybackState s=c.getPlaybackState();
            if(s==null||s.getState()!=PlaybackState.STATE_PLAYING)continue;
            MediaMetadata m=c.getMetadata();if(m==null)continue;
            Bitmap art=null;
            try{
                art=m.getBitmap(MediaMetadata.METADATA_KEY_ART);
                if(art==null)art=m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                if(art==null)art=m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
            }catch(Exception ignored){}
            if(art==null)continue;
            String title=m.getString(MediaMetadata.METADATA_KEY_TITLE);
            String artist=m.getString(MediaMetadata.METADATA_KEY_ARTIST);
            long duration=m.getLong(MediaMetadata.METADATA_KEY_DURATION);
            long position=Math.max(0,s.getPosition());
            if(s.getState()==PlaybackState.STATE_PLAYING)
                position+=(long)((SystemClock.elapsedRealtime()-s.getLastPositionUpdateTime())*s.getPlaybackSpeed());
            if(duration>0)position=Math.min(position,duration);
            String id=c.getPackageName()+"|"+title+"|"+artist;
            SharedPreferences p=getSharedPreferences(PREFS,0);
            // Do not call setBitmap repeatedly for the same track: Android visibly
            // flashes/recomposes the lock-screen wallpaper on every bitmap update.
            if(id.equals(p.getString(LAST,"")))return;
            if(WallpaperEngine.applyArtwork(this,art,title,artist,position,duration)){
                p.edit().putString(LAST,id).apply();applied=true;
            }
            return;
        }
        restore();
    }
    private void restore(){
        SharedPreferences p=getSharedPreferences(PREFS,0);
        if(applied||!p.getString(LAST,"").isEmpty()){
            WallpaperEngine.applyFallback(this);p.edit().remove(LAST).apply();
        }
        applied=false;
    }
    private void cleanup(){
        for(Map.Entry<MediaController,MediaController.Callback> e:callbacks.entrySet()){
            try{e.getKey().unregisterCallback(e.getValue());}catch(Exception ignored){}
        }
        callbacks.clear();
        if(manager!=null&&listener!=null)try{manager.removeOnActiveSessionsChangedListener(listener);}catch(Exception ignored){}
    }
}