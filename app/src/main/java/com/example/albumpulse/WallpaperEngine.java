package com.example.albumpulse;

import android.app.WallpaperManager;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import java.io.InputStream;

final class WallpaperEngine {
    private static final String PREFS="album_pulse_settings", FALLBACK="fallback_uri";
    static boolean applyFallback(Context c){
        String value=c.getSharedPreferences(PREFS,0).getString(FALLBACK,null);
        if(value==null)return false;
        try(InputStream in=c.getContentResolver().openInputStream(Uri.parse(value))){
            Bitmap b=BitmapFactory.decodeStream(in);if(b==null)return false;
            boolean ok=set(c,b);b.recycle();return ok;
        }catch(Exception e){return false;}
    }
    static boolean applyArtwork(Context c,Bitmap art,String title,String artist){
        if(art==null)return false;
        try{
            int w=1080,h=2400;
            Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
            Canvas canvas=new Canvas(out);
            Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
            Bitmap blur=Bitmap.createScaledBitmap(art,48,Math.max(72,(int)(48f*h/w)),true);
            canvas.drawBitmap(blur,null,new Rect(0,0,w,h),p);
            if(blur!=art)blur.recycle();
            canvas.drawColor(0x9906090D);
            float side=w*.76f,cx=w/2f,cy=h*.49f;
            RectF cover=new RectF(cx-side/2,cy-side/2,cx+side/2,cy+side/2);
            Path clip=new Path();clip.addRoundRect(cover,24,24,Path.Direction.CW);
            canvas.save();canvas.clipPath(clip);
            float ratio=Math.max(side/art.getWidth(),side/art.getHeight());
            float dw=art.getWidth()*ratio,dh=art.getHeight()*ratio;
            canvas.drawBitmap(art,null,new RectF(cx-dw/2,cy-dh/2,cx+dw/2,cy+dh/2),p);
            canvas.restore();
            p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);
            p.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));p.setTextSize(48);
            canvas.drawText(trim(title,30),cx,cover.bottom+100,p);
            p.setTextSize(32);p.setTypeface(Typeface.DEFAULT);
            canvas.drawText(trim(artist,40),cx,cover.bottom+155,p);
            boolean ok=set(c,out);out.recycle();return ok;
        }catch(Exception e){return false;}
    }
    private static String trim(String s,int n){if(s==null)return "";return s.length()>n?s.substring(0,n-1)+"…":s;}
    private static boolean set(Context c,Bitmap b){
        try{WallpaperManager.getInstance(c).setBitmap(b,null,true,WallpaperManager.FLAG_LOCK);return true;}
        catch(Exception e){return false;}
    }
}
