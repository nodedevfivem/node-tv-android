package net.nodedevelopment.nodetv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import java.util.Random;

/** CPU-bounded 96x64 CRT noise, upscaled by Canvas. Does not animate while hidden. */
public final class RetroNoiseView extends View {
    private final Bitmap bitmap=Bitmap.createBitmap(96,64,Bitmap.Config.ARGB_8888);
    private final int[] pixels=new int[96*64];
    private final Paint paint=new Paint();
    private final Random rng=new Random();
    private boolean receiving=false;
    private int frameCount;
    public RetroNoiseView(Context context){super(context);setVisibility(GONE);paint.setFilterBitmap(false);}
    public void showNoise(boolean show) {
        receiving=show;setVisibility(show?VISIBLE:GONE);
        if(show)postInvalidateOnAnimation();
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if(!receiving)return;
        int band = ((frameCount++ * 3) % 64);
        for(int y=0;y<64;y++)for(int x=0;x<96;x++){
            int v=rng.nextInt(256);
            if(Math.abs(y-band)<2) v=Math.min(255,v+55);
            pixels[y*96+x]=Color.rgb(v,v,v);
        }
        bitmap.setPixels(pixels,0,96,0,0,96,64);
        c.drawBitmap(bitmap,null,new android.graphics.Rect(0,0,getWidth(),getHeight()),paint);
        postInvalidateDelayed(85);
    }
}
