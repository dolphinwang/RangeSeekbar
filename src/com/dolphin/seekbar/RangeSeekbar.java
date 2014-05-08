/*
 * Copyright (C) 2014 Roy Wang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dolphin.seekbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import com.dolphin.multitouchseekbar.R;

/**
 * A seekbar contains two cursor(left and right). Multiple touch supported.
 * 
 * @author dolphinWang
 * @time 2014/05/07
 */
public class RangeSeekbar extends View {

    private static final String DEBUG_TAG = "RangeSeekbar.java";

    private static final int DEFAULT_DURATION = 100;

    private enum DIRECTION {
        LEFT, RIGHT;
    }

    private int mDuration;

    /**
     * Scrollers for left and right cursor
     */
    private Scroller mLeftScroller;
    private Scroller mRightScroller;

    /**
     * Background drawables for left and right cursor. State list supported.
     */
    private Drawable mLeftCursorBG;
    private Drawable mRightCursorBG;

    /**
     * Represent states.
     */
    private int[] mPressedEnableState = new int[] {
            android.R.attr.state_pressed, android.R.attr.state_enabled };
    private int[] mUnPresseEanabledState = new int[] {
            -android.R.attr.state_pressed, android.R.attr.state_enabled };

    /**
     * Colors of text and seekbar in different states.
     */
    private int mTextColorNormal;
    private int mTextColorSelected;
    private int mSeekbarColorNormal;
    private int mSeekbarColorSelected;

    /**
     * Height of seekbar
     */
    private int mSeekbarHeight;

    /**
     * Size of text mark.
     */
    private int mTextSize;

    /**
     * Space between the text and the seekbar
     */
    private int mMarginBetween;

    /**
     * Length of every part. As we divide some parts according to marks.
     */
    private int mPartLength;

    /**
     * Contents of text mark.
     */
    private CharSequence[] mTextArray;

    /**
     * 
     */
    private float[] mTextWidthArray;

    private Rect mPaddingRect;
    private Rect mLeftCursorRect;
    private Rect mRightCursorRect;

    private RectF mSeekbarRect;
    private RectF mSeekbarRectSelected;

    private float mLeftCursorIndex = 0;
    private float mRightCursorIndex = 1.0f;
    private int mLeftCursorNextIndex = 0;
    private int mRightCursorNextIndex = 1;

    private Paint mPaint;

    private int mLeftPointerLastX;
    private int mRightPointerLastX;

    private int mLeftPointerID = -1;
    private int mRightPointerID = -1;

    private boolean mLeftHited;
    private boolean mRightHited;

    private int mRightBoundary;

    private OnCursorChangeListener mListener;

    public RangeSeekbar(Context context) {
        this(context, null, 0);
    }

    public RangeSeekbar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RangeSeekbar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        applyConfig(context, attrs);

        mPaddingRect = new Rect(getPaddingLeft(), getPaddingTop(),
                getPaddingRight(), getPaddingBottom());
        mLeftCursorRect = new Rect();
        mRightCursorRect = new Rect();

        mSeekbarRect = new RectF();
        mSeekbarRectSelected = new RectF();

        mTextWidthArray = new float[mTextArray.length];

        mLeftScroller = new Scroller(context, new DecelerateInterpolator());
        mRightScroller = new Scroller(context, new DecelerateInterpolator());

        initPaint();

        setWillNotDraw(false);
        setFocusable(true);
        setClickable(true);
    }

    private void applyConfig(Context context, AttributeSet attrs) {
        if (attrs == null) {
            return;
        }

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.RangeSeekbar);

        mDuration = a.getInteger(R.styleable.RangeSeekbar_autoMoveDuration,
                DEFAULT_DURATION);

        mLeftCursorBG = a
                .getDrawable(R.styleable.RangeSeekbar_leftCursorBackground);
        mRightCursorBG = a
                .getDrawable(R.styleable.RangeSeekbar_rightCursorBackground);

        mTextColorNormal = a.getColor(R.styleable.RangeSeekbar_textColorNormal,
                Color.BLACK);
        mTextColorSelected = a.getColor(
                R.styleable.RangeSeekbar_textColorSelected,
                Color.rgb(242, 79, 115));

        mSeekbarColorNormal = a.getColor(
                R.styleable.RangeSeekbar_seekbarColorNormal,
                Color.rgb(218, 215, 215));
        mSeekbarColorSelected = a.getColor(
                R.styleable.RangeSeekbar_seekbarColorSelected,
                Color.rgb(242, 79, 115));

        mSeekbarHeight = (int) a.getDimension(
                R.styleable.RangeSeekbar_seekbarHeight, 10);
        mTextSize = (int) a.getDimension(R.styleable.RangeSeekbar_textSize, 15);
        mMarginBetween = (int) a.getDimension(
                R.styleable.RangeSeekbar_spaceBetween, 15);

        mTextArray = a.getTextArray(R.styleable.RangeSeekbar_markTextArray);
        if (mTextArray != null && mTextArray.length > 0) {
            mLeftCursorIndex = 0;
            mRightCursorIndex = mTextArray.length - 1;
            mRightCursorNextIndex = (int) mRightCursorIndex;
        } else {
            throw new IllegalArgumentException(
                    "Text array is null, how can i do...");
        }

        a.recycle();
    }

    private void initPaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mPaint.setAntiAlias(true);
        mPaint.setStyle(Style.FILL);
        mPaint.setTextSize(mTextSize);

        final int length = mTextArray.length;
        for (int i = 0; i < length; i++) {
            mTextWidthArray[i] = mPaint.measureText(mTextArray[i].toString());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int leftPointerH = mLeftCursorBG.getIntrinsicHeight();
        final int rightPointerH = mRightCursorBG.getIntrinsicHeight();

        // Get max height between left and right cursor.
        final int maxOfPointer = Math.max(leftPointerH, rightPointerH);
        // Than get max height between seekbar and cursor.
        final int maxOfPointerAndSeekbar = Math.max(mSeekbarHeight,
                maxOfPointer);
        // So we get the need height.
        int heightNeeded = maxOfPointerAndSeekbar + mMarginBetween + mTextSize
                + mPaddingRect.top + mPaddingRect.bottom;

        heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightNeeded,
                MeasureSpec.EXACTLY);

        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        mSeekbarRect.left = mPaddingRect.left
                + mLeftCursorBG.getIntrinsicWidth() / 2;
        mSeekbarRect.right = widthSize - mPaddingRect.right
                - mRightCursorBG.getIntrinsicWidth() / 2;
        mSeekbarRect.top = mPaddingRect.top + mTextSize + mMarginBetween;
        mSeekbarRect.bottom = mSeekbarRect.top + mSeekbarHeight;

        mSeekbarRectSelected.top = mSeekbarRect.top;
        mSeekbarRectSelected.bottom = mSeekbarRect.bottom;

        mPartLength = ((int) (mSeekbarRect.right - mSeekbarRect.left))
                / (mTextArray.length - 1);

        mRightBoundary = (int) (mSeekbarRect.right + mRightCursorBG
                .getIntrinsicWidth() / 2);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        /*** Draw text marks ***/
        final int length = mTextArray.length;
        mPaint.setTextSize(mTextSize);
        for (int i = 0; i < length; i++) {
            if ((i > mLeftCursorIndex && i < mRightCursorIndex)
                    || (i == mLeftCursorIndex || i == mRightCursorIndex)) {
                mPaint.setColor(mTextColorSelected);
            } else {
                mPaint.setColor(mTextColorNormal);
            }

            final String text2draw = mTextArray[i].toString();
            final float textWidth = mTextWidthArray[i];

            float textDrawLeft = mSeekbarRect.left + i * mPartLength
                    - textWidth / 2;

            canvas.drawText(text2draw, textDrawLeft, mPaddingRect.top
                    + mTextSize, mPaint);
        }

        /*** Draw seekbar ***/
        final float radius = (float) mSeekbarHeight / 2;
        mSeekbarRectSelected.left = mSeekbarRect.left + mPartLength
                * mLeftCursorIndex;
        mSeekbarRectSelected.right = mSeekbarRect.left + mPartLength
                * mRightCursorIndex;
        // If whole of seekbar is selected, just draw seekbar with selected
        // color.
        if (mLeftCursorIndex == 0 && mRightCursorIndex == length - 1) {
            mPaint.setColor(mSeekbarColorSelected);
            canvas.drawRoundRect(mSeekbarRect, radius, radius, mPaint);
        } else {
            // Draw background first.
            mPaint.setColor(mSeekbarColorNormal);
            canvas.drawRoundRect(mSeekbarRect, radius, radius, mPaint);

            // Draw selected part.
            mPaint.setColor(mSeekbarColorSelected);
            // Can draw rounded rectangle, but original rectangle is enough.
            // Because edges of selected part will be covered by cursors.
            canvas.drawRect(mSeekbarRectSelected, mPaint);
        }

        /*** Draw cursors ***/
        // left cursor first
        final int leftWidth = mLeftCursorBG.getIntrinsicWidth();
        final int leftHieght = mLeftCursorBG.getIntrinsicHeight();
        final int leftLeft = (int) (mSeekbarRectSelected.left - (float) leftWidth / 2);
        final int leftTop = (int) ((mSeekbarRect.top + mSeekbarHeight / 2) - (leftHieght / 2));
        mLeftCursorRect.left = leftLeft;
        mLeftCursorRect.top = leftTop;
        mLeftCursorRect.right = leftLeft + leftWidth;
        mLeftCursorRect.bottom = leftTop + leftHieght;
        mLeftCursorBG.setBounds(mLeftCursorRect);
        mLeftCursorBG.draw(canvas);

        // right cursor second
        final int rightWidth = mRightCursorBG.getIntrinsicWidth();
        final int rightHeight = mRightCursorBG.getIntrinsicHeight();
        final int rightLeft = (int) (mSeekbarRectSelected.right - (float) rightWidth / 2);
        final int rightTop = (int) ((mSeekbarRectSelected.top + mSeekbarHeight / 2) - (rightHeight / 2));
        mRightCursorRect.left = rightLeft;
        mRightCursorRect.top = rightTop;
        mRightCursorRect.right = rightLeft + rightWidth;
        mRightCursorRect.bottom = rightTop + rightHeight;
        mRightCursorBG.setBounds(mRightCursorRect);
        mRightCursorBG.draw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }

        // For multiple touch
        final int action = event.getActionMasked();
        switch (action) {
        case MotionEvent.ACTION_DOWN:

            handleTouchDown(event);

            break;
        case MotionEvent.ACTION_POINTER_DOWN:

            handleTouchDown(event);

            break;
        case MotionEvent.ACTION_MOVE:

            handleTouchMove(event);

            break;
        case MotionEvent.ACTION_POINTER_UP:

            handleTouchUp(event);

            break;
        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_UP:

            handleTouchUp(event);

            break;
        }

        return super.onTouchEvent(event);
    }

    private void handleTouchDown(MotionEvent event) {
        final int actionIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int downX = (int) event.getX(actionIndex);
        final int downY = (int) event.getY(actionIndex);

        if (mLeftCursorRect.contains(downX, downY)) {
            if (mLeftHited) {
                return;
            }

            // If hit, change state of drawable, and record id of touch pointer.
            mLeftPointerLastX = downX;
            mLeftCursorBG.setState(mPressedEnableState);
            mLeftPointerID = event.getPointerId(actionIndex);
            mLeftHited = true;

            invalidate();
        } else if (mRightCursorRect.contains(downX, downY)) {
            if (mRightHited) {
                return;
            }

            mRightPointerLastX = downX;
            mRightCursorBG.setState(mPressedEnableState);
            mRightPointerID = event.getPointerId(actionIndex);
            mRightHited = true;

            invalidate();
        }
    }

    private void handleTouchUp(MotionEvent event) {
        final int actionIndex = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int actionID = event.getPointerId(actionIndex);

        if (actionID == mLeftPointerID) {
            if (!mLeftHited) {
                return;
            }

            // If cursor between in tow mark locations, it should be located on
            // the lower or higher one.

            // step 1:Calculate the offset with lower mark.
            final int lower = (int) Math.floor(mLeftCursorIndex);
            final int higher = (int) Math.ceil(mLeftCursorIndex);

            final float offset = mLeftCursorIndex - lower;
            if (offset != 0) {

                // step 2:Decide which mark will go to.
                if (offset < 0.5f) {
                    // If left cursor want to be located on lower mark, go ahead
                    // guys.
                    // Because right cursor will never appear lower than the
                    // left one.
                    mLeftCursorNextIndex = lower;
                } else if (offset > 0.5f) {
                    mLeftCursorNextIndex = higher;
                    // If left cursor want to be located on higher mark,
                    // situation becomes a little complicated.
                    // We should check that whether distance between left and
                    // right cursor is less than 1, and next index of left
                    // cursor is difference with current
                    // of right one.
                    if (Math.abs(mLeftCursorIndex - mRightCursorIndex) <= 1
                            && mLeftCursorNextIndex == mRightCursorNextIndex) {
                        // Left can not go to the higher, just to the lower one.
                        mLeftCursorNextIndex = lower;
                    }
                }

                // step 3: Move to.
                if (!mLeftScroller.computeScrollOffset()) {
                    final int fromX = (int) (mLeftCursorIndex * mPartLength);

                    mLeftScroller.startScroll(fromX, 0, mLeftCursorNextIndex
                            * mPartLength - fromX, 0, mDuration);

                    triggleCallback(true, mLeftCursorNextIndex);
                }
            }

            // Reset values of parameters
            mLeftPointerLastX = 0;
            mLeftCursorBG.setState(mUnPresseEanabledState);
            mLeftPointerID = -1;
            mLeftHited = false;

            invalidate();
        } else if (actionID == mRightPointerID) {
            if (!mRightHited) {
                return;
            }

            final int lower = (int) Math.floor(mRightCursorIndex);
            final int higher = (int) Math.ceil(mRightCursorIndex);

            final float offset = mRightCursorIndex - lower;
            if (offset != 0) {

                if (offset > 0.5f) {
                    mRightCursorNextIndex = higher;
                } else if (offset < 0.5f) {
                    mRightCursorNextIndex = lower;
                    if (Math.abs(mLeftCursorIndex - mRightCursorIndex) <= 1
                            && mRightCursorNextIndex == mLeftCursorNextIndex) {
                        mRightCursorNextIndex = higher;
                    }
                }

                if (!mRightScroller.computeScrollOffset()) {
                    final int fromX = (int) (mRightCursorIndex * mPartLength);

                    mRightScroller.startScroll(fromX, 0, mRightCursorNextIndex
                            * mPartLength - fromX, 0, mDuration);

                    triggleCallback(false, mRightCursorNextIndex);
                }
            }

            mRightPointerLastX = 0;
            mLeftCursorBG.setState(mUnPresseEanabledState);
            mRightPointerID = -1;
            mRightHited = false;

            invalidate();
        }
    }

    private void handleTouchMove(MotionEvent event) {

        if (mLeftHited && mLeftPointerID != -1) {

            final int index = event.findPointerIndex(mLeftPointerID);
            final float x = event.getX(index);

            float deltaX = x - mLeftPointerLastX;
            mLeftPointerLastX = (int) x;

            DIRECTION direction = (deltaX < 0 ? DIRECTION.LEFT
                    : DIRECTION.RIGHT);

            if (direction == DIRECTION.LEFT && mLeftCursorIndex == 0) {
                return;
            }

            // Check whether cursor will move out of boundary
            if (mLeftCursorRect.left + deltaX < mPaddingRect.left) {
                mLeftCursorIndex = 0;
                invalidate();
                return;
            }

            // Check whether left and right cursor will collision.
            if (mLeftCursorRect.right + deltaX >= mRightCursorRect.left) {
                // Check whether right cursor is in "Touch" mode( if in touch
                // mode, represent that we can not move it at will), or right
                // cursor reach the boundary.
                if (mRightHited || mRightCursorIndex == mTextArray.length - 1
                        || mRightScroller.computeScrollOffset()) {
                    // Just move left cursor to the left side of right one.
                    deltaX = mRightCursorRect.left - mLeftCursorRect.right;
                } else {
                    // Move right cursor to higher location.
                    final int maxMarkIndex = mTextArray.length - 1;

                    if (mRightCursorIndex <= maxMarkIndex - 1) {
                        mRightCursorNextIndex = (int) (mRightCursorIndex + 1);

                        if (!mRightScroller.computeScrollOffset()) {
                            final int fromX = (int) (mRightCursorIndex * mPartLength);

                            mRightScroller
                                    .startScroll(fromX, 0,
                                            mRightCursorNextIndex * mPartLength
                                                    - fromX, 0, mDuration);
                            triggleCallback(false, mRightCursorNextIndex);
                        }
                    }
                }
            }

            // After some calculate, if deltaX is still be zero, do quick
            // return.
            if (deltaX == 0) {
                return;
            }

            // Calculate the movement.
            final float moveX = deltaX / mPartLength;
            mLeftCursorIndex += moveX;

            invalidate();
        }

        if (mRightHited && mRightPointerID != -1) {

            final int index = event.findPointerIndex(mRightPointerID);
            final float x = event.getX(index);

            float deltaX = x - mRightPointerLastX;
            mRightPointerLastX = (int) x;

            DIRECTION direction = (deltaX < 0 ? DIRECTION.LEFT
                    : DIRECTION.RIGHT);

            final int maxIndex = mTextArray.length - 1;
            if (direction == DIRECTION.RIGHT && mRightCursorIndex == maxIndex) {
                return;
            }

            if (mRightCursorRect.right + deltaX > mRightBoundary) {
                deltaX = mRightBoundary - mRightCursorRect.right;
            }

            final int maxMarkIndex = mTextArray.length - 1;
            if (direction == DIRECTION.RIGHT
                    && mRightCursorIndex == maxMarkIndex) {
                return;
            }

            if (mRightCursorRect.left + deltaX < mLeftCursorRect.right) {
                if (mLeftHited || mLeftCursorIndex == 0
                        || mLeftScroller.computeScrollOffset()) {
                    deltaX = mLeftCursorRect.right - mRightCursorRect.left;
                } else {
                    if (mLeftCursorIndex >= 1) {
                        mLeftCursorNextIndex = (int) (mLeftCursorIndex - 1);

                        if (!mLeftScroller.computeScrollOffset()) {
                            final int fromX = (int) (mLeftCursorIndex * mPartLength);
                            mLeftScroller.startScroll(fromX, 0,
                                    mLeftCursorNextIndex * mPartLength - fromX,
                                    0, mDuration);
                            triggleCallback(true, mLeftCursorNextIndex);
                        }
                    }
                }
            }

            if (deltaX == 0) {
                return;
            }

            final float moveX = deltaX / mPartLength;
            mRightCursorIndex += moveX;

            invalidate();
        }
    }

    @Override
    public void computeScroll() {
        if (mLeftScroller.computeScrollOffset()) {
            final int deltaX = mLeftScroller.getCurrX();

            mLeftCursorIndex = (float) deltaX / mPartLength;

            invalidate();
        }

        if (mRightScroller.computeScrollOffset()) {
            final int deltaX = mRightScroller.getCurrX();

            mRightCursorIndex = (float) deltaX / mPartLength;

            invalidate();
        }
    }

    private void triggleCallback(boolean isLeft, int location) {
        if (mListener == null) {
            return;
        }

        if (isLeft) {
            mListener.onLeftCursorChanged(location,
                    mTextArray[location].toString());
        } else {
            mListener.onRightCursorChanged(location,
                    mTextArray[location].toString());
        }
    }

    public void setLeftSelection(int partIndex) {
        if (partIndex >= mTextArray.length - 1 || partIndex <= 0) {
            throw new IllegalArgumentException(
                    "Index should from 0 to size of text array minus 2!");
        }

        if (partIndex != mLeftCursorIndex) {
            if (!mLeftScroller.isFinished()) {
                mLeftScroller.abortAnimation();
            }
            mLeftCursorNextIndex = partIndex;
            final int leftFromX = (int) (mLeftCursorIndex * mPartLength);
            mLeftScroller.startScroll(leftFromX, 0, mLeftCursorNextIndex
                    * mPartLength - leftFromX, 0, mDuration);
            triggleCallback(true, mLeftCursorNextIndex);

            if (mRightCursorIndex <= mLeftCursorNextIndex) {
                if (!mRightScroller.isFinished()) {
                    mRightScroller.abortAnimation();
                }
                mRightCursorNextIndex = mLeftCursorNextIndex + 1;
                final int rightFromX = (int) (mRightCursorIndex * mPartLength);
                mRightScroller.startScroll(rightFromX, 0, mRightCursorNextIndex
                        * mPartLength - rightFromX, 0, mDuration);
                triggleCallback(false, mRightCursorNextIndex);
            }

            invalidate();
        }
    }

    public void setRightSelection(int partIndex) {
        if (partIndex >= mTextArray.length || partIndex <= 0) {
            throw new IllegalArgumentException(
                    "Index should from 1 to size of text array minus 1!");
        }

        if (partIndex != mRightCursorIndex) {
            if (!mRightScroller.isFinished()) {
                mRightScroller.abortAnimation();
            }

            mRightCursorNextIndex = partIndex;
            final int rightFromX = (int) (mPartLength * mRightCursorIndex);
            mRightScroller.startScroll(rightFromX, 0, mRightCursorNextIndex
                    * mPartLength - rightFromX, 0, mDuration);
            triggleCallback(false, mRightCursorNextIndex);

            if (mLeftCursorIndex >= mRightCursorNextIndex) {
                if (!mLeftScroller.isFinished()) {
                    mLeftScroller.abortAnimation();
                }

                mLeftCursorNextIndex = mRightCursorNextIndex - 1;
                final int leftFromX = (int) (mLeftCursorIndex * mPartLength);
                mLeftScroller.startScroll(leftFromX, 0, mLeftCursorNextIndex
                        * mPartLength - leftFromX, 0, mDuration);
                triggleCallback(true, mLeftCursorNextIndex);
            }
            invalidate();
        }
    }

    public void setLeftCursorBackground(Drawable drawable) {
        if (drawable == null) {
            throw new IllegalArgumentException(
                    "Do you want to make left cursor invisible?");
        }

        mLeftCursorBG = drawable;
    }

    public void setLeftCursorBackground(int resID) {
        if (resID < 0) {
            throw new IllegalArgumentException(
                    "Do you want to make left cursor invisible?");
        }

        mLeftCursorBG = getResources().getDrawable(resID);
    }

    public void setRightCursorBackground(Drawable drawable) {
        if (drawable == null) {
            throw new IllegalArgumentException(
                    "Do you want to make right cursor invisible?");
        }

        mRightCursorBG = drawable;
    }

    public void setRightCursorBackground(int resID) {
        if (resID < 0) {
            throw new IllegalArgumentException(
                    "Do you want to make right cursor invisible?");
        }

        mRightCursorBG = getResources().getDrawable(resID);
    }

    public void setTextMarkColorNormal(int color) {
        if (color <= 0 || color == Color.TRANSPARENT) {
            throw new IllegalArgumentException(
                    "Do you want to make text mark invisible?");
        }

        mTextColorNormal = color;
    }

    public void setTextMarkColorSelected(int color) {
        if (color <= 0 || color == Color.TRANSPARENT) {
            throw new IllegalArgumentException(
                    "Do you want to make text mark invisible?");
        }

        mTextColorSelected = color;
    }

    public void setSeekbarColorNormal(int color) {
        if (color <= 0 || color == Color.TRANSPARENT) {
            throw new IllegalArgumentException(
                    "Do you want to make seekbar invisible?");
        }

        mSeekbarColorNormal = color;
    }

    public void setSeekbarColorSelected(int color) {
        if (color <= 0 || color == Color.TRANSPARENT) {
            throw new IllegalArgumentException(
                    "Do you want to make seekbar invisible?");
        }

        mSeekbarColorSelected = color;
    }

    /**
     * In pixels
     * 
     * @param height
     */
    public void setSeekbarHeight(int height) {
        if (height <= 0) {
            throw new IllegalArgumentException(
                    "Height of seekbar can not less than 0!");
        }

        mSeekbarHeight = height;
    }

    /**
     * To set space between text mark and seekbar.
     * 
     * @param space
     */
    public void setSpaceBetween(int space) {
        if (space < 0) {
            throw new IllegalArgumentException(
                    "Space between text mark and seekbar can not less than 0!");
        }

        mMarginBetween = space;
    }

    public void setTextMarks(CharSequence... marks) {
        if (marks == null || marks.length == 0) {
            throw new IllegalArgumentException(
                    "Text array is null, how can i do...");
        }

        mTextArray = marks;
        mLeftCursorIndex = 0;
        mRightCursorIndex = mTextArray.length - 1;
        mRightCursorNextIndex = (int) mRightCursorIndex;
    }

    public int getLeftCursorIndex() {
        return (int) mLeftCursorIndex;
    }

    public int getRightCursorIndex() {
        return (int) mRightCursorIndex;
    }

    public void setOnCursorChangeListener(OnCursorChangeListener l) {
        mListener = l;
    }

    public interface OnCursorChangeListener {
        void onLeftCursorChanged(int location, String textMark);

        void onRightCursorChanged(int location, String textMark);
    }
}
