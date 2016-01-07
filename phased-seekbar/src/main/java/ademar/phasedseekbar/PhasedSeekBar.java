package ademar.phasedseekbar;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class PhasedSeekBar extends View {

    protected static final int[] STATE_NORMAL = new int[] {}; //Unselected anchor of Seek bar
    protected static final int[] STATE_SELECTED = new int[] { android.R.attr.state_selected }; //selected anchor of SeekBar
    protected static final int[] STATE_PRESSED = new int[] { android.R.attr.state_pressed }; //Pressed anchor of seekBar

    protected int[] mState = STATE_SELECTED;

    protected boolean mModeIsHorizontal = true; //Flag for Horizontal Seek bar
    protected boolean mFirstDraw = true; //Flag to check first time background drawing
    protected boolean mUpdateFromPosition = false;
    protected boolean mDrawOnOff = true; // Flag to check the state of Anchor
    protected boolean mFixPoint = true; //Flag for fixed point(discrete) or floating point (continuous) seek bar

    protected Drawable mBackgroundDrawable;
    protected RectF mBackgroundPaddingRect;

    protected int mCurrentX, mCurrentY; // current coordinates of anchor
    protected int mPivotX, mPivotY; // Pivot points of Handle
    protected int mItemHalfWidth, mItemHalfHeight; // Half Height and width of part of seek bar
    protected int mItemAnchorHalfWidth, mItemAnchorHalfHeight; //
    protected int[][] mAnchors; //Array to store the position of Anchors
    protected int mCurrentItem; // Position of current item

    protected PhasedAdapter mAdapter; // Adapter for seek bar
    protected PhasedListener mListener; // Listen the Phase of Seek Bar
    protected PhasedInteractionListener mInteractionListener; //

    boolean isDragging, isAnimating = false; //Flag to check Animation

    //constructors
    public PhasedSeekBar(Context context) {
        super(context);
        init(null, 0);
    }

    public PhasedSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public PhasedSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    //Initialize the view of seek bar
    protected void init(AttributeSet attrs, int defStyleAttr) {
        mBackgroundPaddingRect = new RectF();
        if (attrs != null) {
            //Initialize the attribute array
            TypedArray a = getContext().obtainStyledAttributes(
                    attrs, R.styleable.PhasedSeekBar, defStyleAttr, 0);

            //setting the flags for DrawOnOff, FixPoint and ModeHorizontal
            setDrawOnOff(a.getBoolean(R.styleable.PhasedSeekBar_phased_draw_on_off, mDrawOnOff));
            setFixPoint(a.getBoolean(R.styleable.PhasedSeekBar_phased_fix_point, mFixPoint));
            setModeIsHorizontal(a.getInt(R.styleable.PhasedSeekBar_phased_mode, 0) != 2);

            //Initializing the Dimensions of the seek bar Background
            mBackgroundPaddingRect.left = a.getDimension(R.styleable.PhasedSeekBar_phased_base_margin_left, 0.0f);
            mBackgroundPaddingRect.top = a.getDimension(R.styleable.PhasedSeekBar_phased_base_margin_top, 0.0f);
            mBackgroundPaddingRect.right = a.getDimension(R.styleable.PhasedSeekBar_phased_base_margin_right, 0.0f);
            mBackgroundPaddingRect.bottom = a.getDimension(R.styleable.PhasedSeekBar_phased_base_margin_bottom, 0.0f);

            // Initializing the midpoint cordinate of seek bar BackGround
            mItemHalfWidth = (int) (a.getDimension(R.styleable.PhasedSeekBar_phased_item_width, 0.0f) / 2.0f);
            mItemHalfHeight = (int) (a.getDimension(R.styleable.PhasedSeekBar_phased_item_width, 0.0f) / 2.0f);

            //Initializing the midpoint coordinate of Anchor
            mItemAnchorHalfWidth = (int) (a.getDimension(R.styleable.PhasedSeekBar_phased_anchor_width, 0.0f) / 2.0f);
            mItemAnchorHalfHeight = (int) (a.getDimension(R.styleable.PhasedSeekBar_phased_anchor_height, 0.0f) / 2.0f);

            //Initializing the Background of seek bar
            mBackgroundDrawable = a.getDrawable(R.styleable.PhasedSeekBar_phased_base_background);

            // Recycle the TypedArray, to be re-used by a later caller
            a.recycle();
        }
    }

    // Called on first draw of the BackGround
    protected void configure() {
        Rect rect = new Rect((int) mBackgroundPaddingRect.left,
                (int) mBackgroundPaddingRect.top,
                (int) (getWidth() - mBackgroundPaddingRect.right),
                (int) (getHeight() - mBackgroundPaddingRect.bottom));
        if (mBackgroundDrawable != null) {
            mBackgroundDrawable.setBounds(rect);
        }
        mCurrentX = mPivotX = getWidth() / 2;
        mCurrentY = mPivotY = getHeight() / 2;

        /*int count = getCount();
        int widthBase = rect.width() / count;
        int widthHalf = widthBase / 2;
        int heightBase = rect.height() / count;
        int heightHalf = heightBase / 2;
        mAnchors = new int[count][2];

        *//*todo change this logic so that we can start from the beginning of the seek bar
        Logic : for count = 3 and length = 10 for horizontal
        1. Get the count of Drawables count = 3
        2. put the first one at index 0.
        3. baseWidth =
        *//*
        for (int i = 0, j = 1; i < count; i++, j++) {
            mAnchors[i][0] = mModeIsHorizontal ? widthBase * j - widthHalf + rect.left : mPivotX;
            mAnchors[i][1] = !mModeIsHorizontal ? heightBase * j - heightHalf + rect.top : mPivotY;
        }*/

        int countOfAnchors = getCount();
        int seekBarWidth = rect.width() + 2 * mItemAnchorHalfWidth;
        int seekBarLength = rect.height() + 2 * mItemAnchorHalfHeight;
        mAnchors = new int[countOfAnchors][2];
        mAnchors[0][0] = mModeIsHorizontal ? rect.left + mItemAnchorHalfWidth : mPivotX;
        mAnchors[0][1] = !mModeIsHorizontal ? rect.top + mItemAnchorHalfHeight : mPivotY;
        mAnchors[countOfAnchors-1][0] = mModeIsHorizontal ? rect.right - mItemAnchorHalfWidth : mPivotX;
        mAnchors[countOfAnchors-1][1] = !mModeIsHorizontal ? rect.bottom - mItemAnchorHalfHeight : mPivotY;

        for (int index = 1,jIndex = 1; index <= countOfAnchors - 2; index++,jIndex++) {
            int widthBaseSeekBar = (Math.abs(Math.abs(rect.right) - Math.abs(rect.left))) / (countOfAnchors-1);
            int heightBaseSeekBar = (Math.abs(Math.abs(rect.bottom) - Math.abs(rect.top))) / (countOfAnchors-1);
            mAnchors[index][0] = mModeIsHorizontal ? widthBaseSeekBar * jIndex + rect.left : mPivotX;
            mAnchors[index][1] = !mModeIsHorizontal ? heightBaseSeekBar * jIndex + rect.top : mPivotY;
        }
    }

    // Called when the current configuration of the resources being used by the application have changed.
    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setFirstDraw(true);
    }

    //used for drawing the background
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mFirstDraw) configure();
        if (mBackgroundDrawable != null) mBackgroundDrawable.draw(canvas);
        if (isInEditMode()) return;

        Drawable itemOff;
        Drawable itemOn;
        StateListDrawable stateListDrawable;
        int count = getCount();

        if (!mUpdateFromPosition) {
            int distance;
            int minIndex = 0;
            int minDistance = Integer.MAX_VALUE;
            for (int i = 0; i < count; i++) {
                distance = Math.abs(mModeIsHorizontal ? mAnchors[i][0] - mCurrentX : mAnchors[i][1] - mCurrentY);
                if (minDistance > distance) {
                    minIndex = i;
                    minDistance = distance;
                }
            }

            setCurrentItem(minIndex);
            stateListDrawable = mAdapter.getItem(minIndex);
        } else {
            mUpdateFromPosition = false;
            mCurrentX = mAnchors[mCurrentItem][0];
            mCurrentY = mAnchors[mCurrentItem][1];
            stateListDrawable = mAdapter.getItem(mCurrentItem);
        }
        stateListDrawable.setState(mState);
        itemOn = stateListDrawable.getCurrent();

        for (int i = 0; i < count; i++) {
            if (!mDrawOnOff && i == mCurrentItem) continue;
            stateListDrawable = mAdapter.getItem(i);
            stateListDrawable.setState(STATE_NORMAL);
            itemOff = stateListDrawable.getCurrent();
            itemOff.setBounds(
                    mAnchors[i][0] - mItemHalfWidth,
                    mAnchors[i][1] - mItemHalfHeight,
                    mAnchors[i][0] + mItemHalfWidth,
                    mAnchors[i][1] + mItemHalfHeight);
            itemOff.draw(canvas);
        }

        itemOn.setBounds(
                mCurrentX - mItemHalfWidth,
                mCurrentY - mItemHalfHeight,
                mCurrentX + mItemHalfWidth,
                mCurrentY + mItemHalfHeight);
        itemOn.draw(canvas);

        setFirstDraw(false);
    }


    //Implement this method to handle touch screen motion events.
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int previousX = mCurrentX;
        int previousY = mCurrentY;
        mCurrentX = mModeIsHorizontal ? getNormalizedX(event) : mPivotX;
        mCurrentY = !mModeIsHorizontal ? getNormalizedY(event) : mPivotY;
        int action = event.getAction();
        mUpdateFromPosition = mFixPoint && action == MotionEvent.ACTION_UP;
        mState = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL ? STATE_SELECTED : STATE_PRESSED;
        invalidate();

        if (mInteractionListener != null) {
            mInteractionListener.onInteracted(mCurrentX, mCurrentY, mCurrentItem, event);
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP:
                if (previousX != mCurrentX) {
                    startAnimation(previousX);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return super.onTouchEvent(event);
    }

    private int indexOfMinDistanceAnchor() {
        int distance;
        int minIndex = 0;
        int count = mAdapter.getCount();
        int minDistance = Integer.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            distance = Math.abs(mModeIsHorizontal ? mAnchors[i][0] - mCurrentX : mAnchors[i][1] - mCurrentY);
            if (minDistance > distance) {
                minIndex = i;
                minDistance = distance;
            }
        }
        return minIndex;
    }

    private void startAnimation(int previousX) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ValueAnimator anim = ValueAnimator.ofInt(previousX, mAnchors[indexOfMinDistanceAnchor()][0]);
            anim.setDuration(800);
            anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        mCurrentX = (Integer) animation.getAnimatedValue();
                        invalidate();
                    }
                }
            });
            anim.start();
        }
    }

    //return the X coordinate of seek bar or a part of the seek bar
    protected int getNormalizedX(MotionEvent event) {
        return Math.min(Math.max((int) event.getX(), mItemHalfWidth), getWidth() - mItemHalfWidth);
    }

    //return the Y coordinate of seek bar or a part of the seek bar
    protected int getNormalizedY(MotionEvent event) {
        return Math.min(Math.max((int) event.getY(), mItemHalfHeight), getHeight() - mItemHalfHeight);
    }

    //Return the count of number of drawables on seek bar
    protected int getCount() {
        return isInEditMode() ? 3 : mAdapter.getCount();
    }

    // set the adapter to seek bar
    public void setAdapter(PhasedAdapter adapter) {
        mAdapter = adapter;
    }

    // Set the flag for first draw
    public void setFirstDraw(boolean firstDraw) {
        mFirstDraw = firstDraw;
    }

    // set the listener for SeekBar
    public void setListener(PhasedListener listener) {
        mListener = listener;
    }

    //set the Interaction Listener
    public void setInteractionListener(PhasedInteractionListener interactionListener) {
        mInteractionListener = interactionListener;
    }

    // Set the current Position of the seek bar anchor
    public void setPosition(int position) {
        position = position < 0 ? 0 : position;
        position = position >= mAdapter.getCount() ? mAdapter.getCount() - 1 : position;
        mCurrentItem = position;
        mUpdateFromPosition = true;
        //invalidate the position and onDraw is called
        invalidate();
    }

    public boolean isModeIsHorizontal() {
        return mModeIsHorizontal;
    }

    public void setModeIsHorizontal(boolean modeIsHorizontal) {
        mModeIsHorizontal = modeIsHorizontal;
    }

    public boolean isDrawOnOff() {
        return mDrawOnOff;
    }

    public void setDrawOnOff(boolean drawOnOff) {
        mDrawOnOff = drawOnOff;
    }

    public boolean isFixPoint() {
        return mFixPoint;
    }

    public void setFixPoint(boolean fixPoint) {
        mFixPoint = fixPoint;
    }

    public int getCurrentX() {
        return mCurrentX;
    }

    public int getCurrentY() {
        return mCurrentY;
    }

    public int getCurrentItem() {
        return mCurrentItem;
    }

    protected void setCurrentItem(int currentItem) {
        if (mCurrentItem != currentItem && mListener != null) {
            mListener.onPositionSelected(currentItem);
        }
        mCurrentItem = currentItem;
    }
}
