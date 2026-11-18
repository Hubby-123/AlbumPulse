package com.example.albumpulse;
import android.appwidget.*;
import android.content.*;
public class AlbumWidgetProvider extends AppWidgetProvider {
 static void refresh(Context c){ AppWidgetManager m=AppWidgetManager.getInstance(c); int[] ids=m.getAppWidgetIds(new ComponentName(c,AlbumWidgetProvider.class)); for(int id:ids) update(c,m,id); }
 static void update(Context c,AppWidgetManager m,int id){
  RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.album_widget);
  v.setOnClickPendingIntent(R.id.prev,action(c,"PREV",id));v.setOnClickPendingIntent(R.id.play,action(c,"TOGGLE",id));v.setOnClickPendingIntent(R.id.next,action(c,"NEXT",id));
  Intent open=new Intent(c,MainActivity.class); PendingIntent pi=PendingIntent.getActivity(c,id,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);v.setOnClickPendingIntent(R.id.widget_root,pi);m.updateAppWidget(id,v);
 }
 static PendingIntent action(Context c,String action,int id){Intent i=new Intent(c,AlbumWidgetReceiver.class);i.setAction(action);i.putExtra("widget_id",id);return PendingIntent.getBroadcast(c,id+action.hashCode(),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids){for(int id:ids)update(c,m,id);}
}