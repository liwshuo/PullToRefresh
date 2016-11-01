package com.bupt.cherry;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Scroller;
import android.widget.TextView;

/**
 * Created by lishuo on 16/11/1.
 */

public class PullToRefreshView extends ViewGroup {

    public static enum State {

        RESET(0x0),
        PULL_TO_REFRESH(0x1),
        RELEASE_TO_REFRESH(0x2),
        REFRESHING(0x8),
        REFRESH_FINISH(0x9);
//        MANUAL_REFRESHING(0x9),
//        OVERSCROLLING(0x10);

        private int mIntValue;

        State(int intValue) {
            mIntValue = intValue;
        }

        static State mapIntToValue(final int stateInt) {
            for (State value : State.values()) {
                if (stateInt == value.getIntValue()) {
                    return value;
                }
            }
            return RESET;
        }

        int getIntValue() {
            return mIntValue;
        }
    }


    private View mRefreshableView; //可刷新的View，比如listview，recyclerview
    private FrameLayout mInfoViewContainer; //
    private TextView mInfoView;  //顶部的下拉后展示信息的View

    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int RELEASE_TO_REFRESH_SLOP = 100;

    private State mState;

    private int mInfoViewHeight = 0;
    private int mTouchSlop;

    private float mInitialMotionY;

    private boolean mIsBeingDragged;


    private Interpolator mDecelerateInterpolator;

    Scroller mScroller;

    private int mRefreshableViewPaddingLeft;
    private int mRefreshableViewPaddingTop;
    private int mRefreshableViewPaddingBottom;
    private int mRefreshableViewPaddingRight;


    public PullToRefreshView(Context context) {
        super(context);
        init();
    }

    public PullToRefreshView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PullToRefreshView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public PullToRefreshView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        initInfoView();
        setState(State.RESET);
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);
        mScroller = new Scroller(getContext());
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

    }

    private void initRefreshableView() {
        if (mRefreshableView != null) {
            return;
        }
        if (getChildCount() > 0) {
            for (int i = 0; i < getChildCount(); i++) {
                View view = getChildAt(i);
                if (view != mInfoView) {
                    mRefreshableViewPaddingTop = view.getPaddingTop();
                    mRefreshableViewPaddingLeft = view.getPaddingLeft();
                    mRefreshableViewPaddingBottom = view.getPaddingBottom();
                    mRefreshableViewPaddingRight = view.getPaddingRight();
                    mRefreshableView = view;


                }
            }
        }
    }

    private void initInfoView() {
        mInfoViewContainer = new FrameLayout(getContext());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        mInfoViewContainer.setLayoutParams(lp);
        addView(mInfoViewContainer);
        mInfoView = new TextView(getContext());
        FrameLayout.LayoutParams lp1 = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, 200);
        mInfoView.setLayoutParams(lp1);
        mInfoView.setGravity(Gravity.CENTER);
        mInfoViewContainer.addView(mInfoView);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        initRefreshableView();
        if (mRefreshableView == null) {
            return;
        }
        measureChild(mInfoViewContainer, widthMeasureSpec, heightMeasureSpec);
        if (mInfoViewContainer.getMeasuredHeight() != 0) {
            mInfoViewHeight = mInfoViewContainer.getMeasuredHeight();
        }
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingRight() - getPaddingLeft(), MeasureSpec.EXACTLY);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY);
        mRefreshableView.measure(widthMeasureSpec, heightMeasureSpec);
//        mInfoView.measure(widthMeasureSpec, mInfoViewHeight);
    }

    @Override
    protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
        initRefreshableView();
        if (mRefreshableView == null) {
            return;
        }

        int height = getMeasuredHeight();
        int width = getMeasuredWidth();
        int left = getPaddingLeft();
        int top = getPaddingTop();
        int right = getPaddingRight();
        int bottom = getPaddingBottom();

        mRefreshableView.layout(left, top, left + width - right, top + height - bottom);
        mInfoViewContainer.layout(left, top - mInfoViewHeight, left + width - right, top);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (canChildScrollUp()) {
            return false;
        }
        final int action = MotionEventCompat.getActionMasked(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
//                setTargetOffsetTop(0, true);
                mIsBeingDragged = false;
                mInitialMotionY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float y = ev.getY();
                final float yDiff = y - mInitialMotionY;
                if (Math.abs(getScrollY()) < mInfoViewHeight && yDiff > mTouchSlop && !mIsBeingDragged) {
                    mIsBeingDragged = true;
                    setState(State.PULL_TO_REFRESH);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                break;
        }

        return mIsBeingDragged;
    }

    private boolean canChildScrollUp() {
        return ViewCompat.canScrollVertically(mRefreshableView, -1);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    boolean mIsScrollingToTop;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                int scrollY = getScrollY();
                float y = event.getY();
                int deltaY = (int) (mInitialMotionY - y);
                if ((scrollY <= 0 || deltaY < 0) && Math.abs(getScrollY()) < mInfoViewHeight) {
                    float v = (float) (mInfoViewHeight - Math.abs(scrollY)) / (float) mInfoViewHeight;
                    float interpolator = mDecelerateInterpolator.getInterpolation(v);
                    scrollBy(0, (int) (deltaY * interpolator));
                    mInitialMotionY = y;
                    if (Math.abs(scrollY) > RELEASE_TO_REFRESH_SLOP) {
                        setState(State.RELEASE_TO_REFRESH);
                    }else {
                        setState(State.PULL_TO_REFRESH);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mState == State.RELEASE_TO_REFRESH) {
                    mScroller.startScroll(getScrollX(), getScrollY(), getScrollX(), -(mInfoViewHeight - (-getScrollY())));
                    setState(State.REFRESHING);
                    mIsScrollingToTop = true;
                }else if (mState == State.PULL_TO_REFRESH){
                    mScroller.startScroll(getScrollX(), getScrollY(), 0 - getScrollX(), 0 - getScrollY());
                    setState(State.RESET);
                }
                invalidate();
                break;
        }
        return true;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        } else {
            if (mIsScrollingToTop) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setState(State.REFRESH_FINISH);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                setState(State.RESET);
                                mScroller.startScroll(getScrollX(), getScrollY(), 0 - getScrollX(), 0 - getScrollY());
                                invalidate();
                            }
                        }, 1000);

                    }
                }, 3000);
                mIsScrollingToTop = false;
            }
        }
    }

    private void setState(State state) {
        mState = state;
        switch (state) {
            case PULL_TO_REFRESH:
                mInfoView.setText("下拉刷新");
                break;
            case RELEASE_TO_REFRESH:
                mInfoView.setText("松手刷新");
                break;
            case REFRESHING:
                mInfoView.setText("刷新中...");
                break;
            case REFRESH_FINISH:
                mInfoView.setText("刷新完成");
                break;
        }
    }
}
