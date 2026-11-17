package com.example.albumpulse;

import android.app.WallpaperManager;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import java.io.InputStream;

final class WallpaperEngine {
    private static final String PREFS="album_pulse_settings", FALLBACK="fallback_uri";
    private static final int WIDTH=1080, HEIGHT=2400;

    static boolean applyFallback(Context c){
        String value=c.getSharedPreferences(PREFS,0).getString(FALLBACK,null);
        if(value==null)return false;
        try{
            Uri uri=Uri.parse(value);
            BitmapFactory.Options bounds=new BitmapFactory.Options();
            bounds.inJustDecodeBounds=true;
            try(InputStream in=c.getContentResolver().openInputStream(uri)){
                BitmapFactory.decodeStream(in,null,bounds);
            }
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inSampleSize=sampleSize(bounds.outWidth,bounds.outHeight,WIDTH,HEIGHT);
            options.inPreferredConfig=Bitmap.Config.RGB_565;
            Bitmap bitmap;
            try(InputStream in=c.getContentResolver().openInputStream(uri)){
                bitmap=BitmapFactory.decodeStream(in,null,options);
            }
            if(bitmap==null)return false;
            boolean ok=set(c,bitmap);
            bitmap.recycle();
            return ok;
        }catch(Exception e){return false;}
    }

    static boolean applyArtwork(Context c,Bitmap art,String title,String artist){
        if(art==null)return false;
        Bitmap out=null;
        try{
            out=Bitmap.createBitmap(WIDTH,HEIGHT,Bitmap.Config.RGB_565);
            Canvas canvas=new Canvas(out);
            Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);

            // Tiny blurred copy keeps background rendering inexpensive.
            Bitmap blur=Bitmap.createScaledBitmap(art,48,107,true);
            canvas.drawBitmap(blur,null,new Rect(0,0,WIDTH,HEIGHT),paint);
            if(blur!=art)blur.recycle();
            canvas.drawColor(0x9906090D);

            float side=WIDTH*.76f,cx=WIDTH/2f,cy=HEIGHT*.46f;
            RectF cover=new RectF(cx-side/2,cy-side/2,cx+side/2,cy+side/2);
            Path clip=new Path();
            clip.addRoundRect(cover,24,24,Path.Direction.CW);
            canvas.save();
            canvas.clipPath(clip);
            float ratio=Math.max(side/art.getWidth(),side/art.getHeight());
            float dw=art.getWidth()*ratio,dh=art.getHeight()*ratio;
            canvas.drawBitmap(art,null,new RectF(cx-dw/2,cy-dh/2,cx+dw/2,cy+dh/2),paint);
            canvas.restore();

            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));
            paint.setTextSize(48);
            canvas.drawText(trim(title,30),cx,cover.bottom+100,paint);
            paint.setTextSize(32);
            paint.setTypeface(Typeface.DEFAULT);
            canvas.drawText(trim(artist,40),cx,cover.bottom+155,paint);

            return set(c,out);
        }catch(Exception e){return false;}
        finally{if(out!=null&&!out.isRecycled())out.recycle();}
    }

    private static int sampleSize(int width,int height,int maxWidth,int maxHeight){
        int sample=1;
        while(width/(sample*2)>=maxWidth&&height/(sample*2)>=maxHeight)sample*=2;
        return sample;
    }
    private static String trim(String s,int n){
        if(s==null)return "";
        return s.length()>n?s.substring(0,n-1)+"…":s;
    }
    private static boolean set(Context c,Bitmap b){
        try{
            WallpaperManager.getInstance(c).setBitmap(b,null,true,WallpaperManager.FLAG_LOCK);
            return true;
        }catch(Exception e){return false;}
    }
}
