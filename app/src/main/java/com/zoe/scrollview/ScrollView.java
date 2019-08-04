package com.zoe.scrollview;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

public class ScrollView extends ViewGroup  {

    private float xDown;    //down的x坐标
    private float xLastMove;    //上一次move的x坐标
    private float xMove;        //当前move的x坐标
    private GestureDetector detector;   //手势判断
    private int mTouchSlop;         //最小滑动判断距离
    private int leftBorder;     //左边界
    private int rightBorder;    //右边界
    private Scroller mScroller;     //Scroller
    private boolean scrollEnable = true;    //滑动开关

    public ScrollView(Context context) {
        this(context, null);
    }

    public ScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScroller = new Scroller(context);
        detector = new GestureDetector(context,new FlingListener());
//        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mTouchSlop = 8;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMeasureSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMeasureSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthMeasureMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMeasureMode = MeasureSpec.getMode(heightMeasureSpec);
        int width = 0, height = 0;

        int childCount = getChildCount();
        for (int i = 0; i < childCount; ++i) {
            View child = getChildAt(i);
            child.setClickable(true);
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
        }
        if(childCount != 0){
            height = getChildAt(0).getMeasuredHeight();
            width = getChildAt(0).getMeasuredWidth();
        }
        setMeasuredDimension(widthMeasureMode == MeasureSpec.EXACTLY ? widthMeasureSize : width
                , heightMeasureMode == MeasureSpec.EXACTLY ? heightMeasureSize : height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed) {
            //第一个childView占据整个可见空间
            View firstChild = getChildAt(0);
            firstChild.layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
            //剩下的childView在右边依次放置
            int childCount = getChildCount();
            int left = firstChild.getRight();
            int height=firstChild.getHeight();
            for (int i = 1; i < childCount; ++i) {
                View childView = getChildAt(i);
                childView.layout(left, 0, left + childView.getMeasuredWidth()
                        , height);
                left = childView.getRight();
            }
        }
        //初始化滑动的左右边界
        leftBorder = getChildAt(0).getLeft();
        rightBorder = getChildAt(getChildCount() - 1).getRight();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        Log.e("MyDebug","action="+ev.getAction());
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                xDown = ev.getRawX();
                xLastMove = xDown;
                break;
            case MotionEvent.ACTION_MOVE:
                xMove=ev.getRawX();
                int diff= (int) (xMove-xLastMove);
//                Log.e("MyDebug","diff="+diff);
                xLastMove=xMove;
                if(Math.abs(diff) > mTouchSlop){
                    if(getScrollX() == 0 && diff > 0){
                        return false;
                    }
                    if(getScrollX() == -rightBorder && diff < 0){
                        return false;
                    }
//                    Log.e("MyDebug","拦截");
                    //屏蔽父View的拦截
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

//        Log.d("MyDebug","action="+event.getAction());
        //如果是fling，则在GestureDetector中处理
        if(detector.onTouchEvent(event)){
            return true;
        }
        //处理不是fling的情况
        switch (event.getAction()){
            case MotionEvent.ACTION_MOVE:
                xMove = event.getRawX();
                int dx = (int) (xLastMove-xMove);
                int xScrolled=getScrollX();     //已经移动过的距离
                xLastMove=xMove;
                if( (xScrolled+dx) <leftBorder){
                    scrollTo(leftBorder,0);
                    return true;
                }
                if( (xScrolled+dx+getWidth()) > rightBorder){
                    scrollTo(rightBorder-getWidth(),0);
                    return true;
                }
                scrollBy(dx,0);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                int judgeX=getWidth();
                View secView=getChildAt(1);
                if(secView != null){
                    judgeX += secView.getWidth()/2;
                }
                if(getScrollX() > judgeX-getWidth()){  //滑动到右边界
                    scrollToRight();
                }else if ( getScrollX() <= judgeX-getWidth()){  //滑动到左边界
                    scrollToLeft();
                }
                break;
        }
        return true;
    }

    private class FlingListener extends GestureDetector.SimpleOnGestureListener{
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//            Log.d("MyDebug","vx="+velocityX);
            if(velocityX > 0){
                scrollToLeft();
            }else{
                scrollToRight();
            }
            return true;
        }
    }

    /**
     * 是否允许滑动
     * @param scrollEnable true允许滑动
     */
    public void setScrollEnable(boolean scrollEnable){
        this.scrollEnable = scrollEnable;
    }

    /**
     * 滑动到左边界
     */
    public void scrollToLeft() {
        int scrolledX = getScrollX();
        mScroller.startScroll(scrolledX, 0, leftBorder - scrolledX, 0);
        invalidate();
    }

    /**
     * 滑动到右边界
     */
    public void scrollToRight() {
        int scrolledX = getScrollX();
        mScroller.startScroll(scrolledX,0,rightBorder - scrolledX -getWidth(),0);
        invalidate();
    }

    @Override
    public void computeScroll() {
        if(mScroller.computeScrollOffset()){
            scrollTo(mScroller.getCurrX(),mScroller.getCurrY());
            invalidate();
        }
    }


}
