package net.nodedevelopment.nodetv;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;

/** Transparent video-only fullscreen touch layer. Native Android system Back gestures stay intact. */
public final class FullscreenGestureView extends View {
    public interface Listener {
        void onSwipeStart(boolean brightnessSide);
        void onSwipe(boolean brightnessSide, float delta);
        void onCenterTap();
    }
    private static final int UNDECIDED=0, VERTICAL=1, OTHER=2;
    private final Listener listener;
    private final float threshold;
    private float downX,downY;
    private int gesture=UNDECIDED;
    private boolean leftSide;

    public FullscreenGestureView(Context context, Listener listener) {
        super(context);
        this.listener=listener;
        threshold=18f*getResources().getDisplayMetrics().density;
        setClickable(true);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX=event.getX();downY=event.getY();gesture=UNDECIDED;
                leftSide=downX<getWidth()/2f;
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                // Ignore multitouch, so pinch and system multitouch do not modify settings.
                gesture=OTHER;
                return true;
            case MotionEvent.ACTION_MOVE:
                if(gesture==OTHER)return true;
                float dx=event.getX()-downX,dy=event.getY()-downY;
                if(gesture==UNDECIDED&&(Math.abs(dx)>threshold||Math.abs(dy)>threshold)) {
                    if(Math.abs(dy)>Math.abs(dx)*1.15f){
                        gesture=VERTICAL;
                        listener.onSwipeStart(leftSide);
                    } else gesture=OTHER;
                }
                if(gesture==VERTICAL){
                    // Up is positive. A swipe covering 60% of the view traverses full range.
                    listener.onSwipe(leftSide, (downY-event.getY())/Math.max(1f,getHeight())*1.65f);
                }
                return true;
            case MotionEvent.ACTION_UP:
                if(gesture==UNDECIDED){
                    float x=event.getX(),y=event.getY();
                    if(Math.abs(x-downX)<threshold&&Math.abs(y-downY)<threshold
                        &&x>=getWidth()*.25f&&x<=getWidth()*.75f
                        &&y>=getHeight()*.20f&&y<=getHeight()*.80f){
                        listener.onCenterTap();
                    }
                }
                performClick();
                gesture=UNDECIDED;
                return true;
            case MotionEvent.ACTION_CANCEL:
                gesture=UNDECIDED;
                return true;
            default: return true;
        }
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
