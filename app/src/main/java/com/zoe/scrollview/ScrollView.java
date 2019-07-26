package com.zoe.scrollview;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

public class ScrollView extends ViewGroup {

    private float xDown;    //按下的位置x坐标
    private float xLastMove;    //上一次接触的位置x坐标
    private float xMove;    //现在接触的位置的x坐标
    private long downTime;  //按下的时间
    private long upTime;    //抬起的时间
    private Scroller mScroller;     //Scroller对象
    private int mTouchSlop;       //系统检测最小滑动距离
    private float judgeSpeed=0.8f;     //系统检测生效滑动速度
    private int leftBorder;   //滑动的左边界
    private int rightBorder;  //滑动的右边界

    public ScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScroller = new Scroller(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measureWidth=MeasureSpec.getSize(widthMeasureSpec);
        int measureHeight=MeasureSpec.getSize(heightMeasureSpec);
        int measureWidthMode=MeasureSpec.getMode(widthMeasureSpec);
        int measureHeightMode=MeasureSpec.getMode(heightMeasureSpec);
        int height=0;
        int width=0;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int childCount=getChildCount();
        for(int i=0;i<childCount;++i){
            View childView = getChildAt(i);
            measureChild(childView,widthMeasureSpec,heightMeasureSpec);
        }
        if(childCount != 0){
            height = getChildAt(0).getMeasuredHeight();
            width = getChildAt(0).getMeasuredWidth();
        }
        setMeasuredDimension((measureWidthMode == MeasureSpec.EXACTLY) ? measureWidth : width
                        , (measureHeightMode == MeasureSpec.EXACTLY) ? measureHeight : height);
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
//        Log.d("MyDebug","onInterceptTouchEvent,action="+ev.getAction());
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                xDown = ev.getRawX();
                downTime=System.currentTimeMillis();
                xLastMove = xDown;
                break;
            case MotionEvent.ACTION_MOVE:
                xMove=ev.getRawX();
                int diff= (int) Math.abs(xMove-xLastMove);
                xLastMove=xMove;
                if(diff > mTouchSlop){
                    return true;
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        Log.d("MyDebug","onTouchEvent,event="+event.getAction());
        switch (event.getAction()){
            case MotionEvent.ACTION_MOVE:
//                Log.d("MyDebug","Move");
                xMove=event.getRawX();
                int dx= (int) (xLastMove-xMove);   //要移动的距离
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
//                Log.d("MyDebug","scrollBy "+dx);
                scrollBy(dx,0);
                break;
            case MotionEvent.ACTION_UP:
                upTime=System.currentTimeMillis();
                float speed = (xDown-event.getRawX()) / (upTime-downTime);
//                Log.d("MyDebug","speed="+speed+(speed< -judgeSpeed));
                int judgeX=getWidth();
                View secView=getChildAt(1);
                if(secView != null){
                    judgeX += secView.getWidth()/2;
                }
                if( speed > judgeSpeed ){//滑动到右边界
                    mScroller.startScroll(getScrollX(),0,rightBorder-getWidth()-getScrollX(),0);
                }else if(speed < -judgeSpeed ) { //滑动到左边界
                    mScroller.startScroll(getScrollX(),0,leftBorder-getScrollX(),0);
                }else if(getScrollX() > judgeX-getWidth()){  //滑动到右边界
                    mScroller.startScroll(getScrollX(),0,rightBorder-getWidth()-getScrollX(),0);
                }else if ( getScrollX() <= judgeX-getWidth()){  //滑动到左边界
                    mScroller.startScroll(getScrollX(),0,leftBorder-getScrollX(),0);
                }
                invalidate();
                break;
        }
        return true;
    }

    /**
     * 将控件还原到初始位置
     */
    public void restore(){
        int scrollX=getScrollX();
        if(scrollX != 0){
            mScroller.startScroll(scrollX,0,leftBorder-scrollX,0);
            invalidate();
        }
    }

    @Override
    public void computeScroll() {
        if(mScroller.computeScrollOffset()){
            scrollTo(mScroller.getCurrX(),mScroller.getCurrY());
            invalidate();
        }
    }
}
