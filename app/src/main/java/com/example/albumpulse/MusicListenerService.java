package com.example.albumpulse;

import android.content.*;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.*;
import android.service.notification.*;
import java.util.*;

public class MusicListenerService extends NotificationListenerService {
    private static final String PREFS="album_pulse_settings", ENABLED="dynamic_enabled", LAST="last_dynamic_key";
    private MediaSessionManager manager;
    private final Map<MediaController,MediaController.Callback> callbacks=new HashMap<>();
    private MediaSessionManager.OnActiveSessionsChangedListener listener;
    private boolean applied=false;

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
            if(!active.contains(c)){
                MediaController.Callback cb=callbacks.remove(c);
                if(cb!=null)c.unregisterCallback(cb);
            }
        }
        for(MediaController c:active)if(!callbacks.containsKey(c)){
            MediaController.Callback cb=new MediaController.Callback(){
                @Override public void onPlaybackStateChanged(PlaybackState s){update();}
                @Override public void onMetadataChanged(MediaMetadata m){update();}
                @Override public void onSessionDestroyed(){refresh();}
            };
            callbacks.put(c,cb);
            c.registerCallback(cb);
        }
        update();
    }

    // Event-driven: Android notifies us when playback or track metadata changes.
    // No polling/timer is needed, which avoids waking the app every second.
    private void update(){
        SharedPreferences prefs=getSharedPreferences(PREFS,0);
        if(!prefs.getBoolean(ENABLED,false)){restore();return;}

        for(MediaController c:callbacks.keySet()){
            PlaybackState state=c.getPlaybackState();
            if(state==null||state.getState()!=PlaybackState.STATE_PLAYING)continue;
            MediaMetadata metadata=c.getMetadata();
            if(metadata==null)continue;

            Bitmap art=null;
            try{
                art=metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
                if(art==null)art=metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                if(art==null)art=metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
            }catch(Exception ignored){}
            if(art==null)continue;

            String title=metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
            String artist=metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
            String album=metadata.getString(MediaMetadata.METADATA_KEY_ALBUM);
            String mediaId=metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
            String id=c.getPackageName()+"|"+mediaId+"|"+album+"|"+title+"|"+artist;

            // Render only when the track changes; never rewrite the wallpaper every second.
            if(id.equals(prefs.getString(LAST,"")))return;
            if(WallpaperEngine.applyArtwork(this,art,title,artist)){
                prefs.edit().putString(LAST,id).apply();
                applied=true;
            }
            return;
        }
        restore();
    }

    private void restore(){
        SharedPreferences prefs=getSharedPreferences(PREFS,0);
        if(applied||!prefs.getString(LAST,"").isEmpty()){
            WallpaperEngine.applyFallback(this);
            prefs.edit().remove(LAST).apply();
        }
        applied=false;
    }
    private void cleanup(){
        for(Map.Entry<MediaController,MediaController.Callback> entry:callbacks.entrySet()){
            try{entry.getKey().unregisterCallback(entry.getValue());}catch(Exception ignored){}
        }
        callbacks.clear();
        if(manager!=null&&listener!=null){
            try{manager.removeOnActiveSessionsChangedListener(listener);}catch(Exception ignored){}
        }
        manager=null;
        listener=null;
    }
}
