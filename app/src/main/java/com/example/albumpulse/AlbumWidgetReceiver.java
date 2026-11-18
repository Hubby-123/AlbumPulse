package com.example.albumpulse;
import android.content.*;
public class AlbumWidgetReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){Intent s=new Intent(c,MusicListenerService.class);s.setAction(i.getAction());try{c.startService(s);}catch(Exception ignored){}}
}