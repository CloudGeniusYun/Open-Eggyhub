package com.eggyhub.android;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

public class StatusLoadingView extends View {

    private static final int STATE_LOADING = 0;
    private static final int STATE_SUCCESS = 1;
    private static final int STATE_ERROR = 2;

    private int mState = STATE_LOADING;

    // Paint
    private Paint mPaint;
    private int mColor = Color.parseColor("#0096ff"); // EggyHub Blue
    private float mStrokeWidth = 10f;

    // Loading Animation
    private ValueAnimator mLoadingAnimator;
    private float mLoadingRotation = 0f;
    private RectF mBounds;

    // Success Animation
    private ValueAnimator mSuccessAnimator;
    private float mSuccessProgress = 0f;
    private Path mCheckPath;
    private Path mCheckDstPath;
    
    // Error Animation
    private ValueAnimator mErrorAnimator;
    private float mErrorProgress = 0f;
    private Path mErrorPathLeft;
    private Path mErrorPathRight;
    private Path mErrorDstPathLeft;
    private Path mErrorDstPathRight;
    
    private PathMeasure mPathMeasure;

    public StatusLoadingView(Context context) {
        this(context, null);
    }

    public StatusLoadingView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StatusLoadingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(mStrokeWidth);
        mPaint.setColor(mColor);

        mBounds = new RectF();
        mCheckPath = new Path();
        mCheckDstPath = new Path();
        
        mErrorPathLeft = new Path();
        mErrorPathRight = new Path();
        mErrorDstPathLeft = new Path();
        mErrorDstPathRight = new Path();
        
        mPathMeasure = new PathMeasure();

        mLoadingAnimator = ValueAnimator.ofFloat(0, 360);
        mLoadingAnimator.setDuration(1000);
        mLoadingAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mLoadingAnimator.setInterpolator(new LinearInterpolator());
        mLoadingAnimator.addUpdateListener(animation -> {
            mLoadingRotation = (float) animation.getAnimatedValue();
            invalidate();
        });
        
        mSuccessAnimator = ValueAnimator.ofFloat(0, 1);
        mSuccessAnimator.setDuration(500);
        mSuccessAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mSuccessAnimator.addUpdateListener(animation -> {
            mSuccessProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        
        mErrorAnimator = ValueAnimator.ofFloat(0, 1);
        mErrorAnimator.setDuration(400);
        mErrorAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mErrorAnimator.addUpdateListener(animation -> {
            mErrorProgress = (float) animation.getAnimatedValue();
            invalidate();
        });

        startLoading();
    }

    public void startLoading() {
        mState = STATE_LOADING;
        // Cancel others
        if (mSuccessAnimator != null && mSuccessAnimator.isRunning()) mSuccessAnimator.cancel();
        if (mErrorAnimator != null && mErrorAnimator.isRunning()) mErrorAnimator.cancel();
        
        if (mLoadingAnimator != null && !mLoadingAnimator.isRunning()) {
            mLoadingAnimator.start();
        }
        setColor(Color.parseColor("#0096ff")); // Reset to Blue
        invalidate();
    }

    public void startSuccess() {
        if (mState == STATE_SUCCESS) return;
        mState = STATE_SUCCESS;

        if (mLoadingAnimator != null) mLoadingAnimator.cancel();
        if (mErrorAnimator != null) mErrorAnimator.cancel();
        
        // Ensure path is ready
        if (mCheckPath.isEmpty() && getWidth() > 0) {
            initPaths(getWidth(), getHeight());
        }
        
        setColor(Color.parseColor("#4CAF50")); // Green for Success
        mSuccessAnimator.start();
        invalidate();
    }

    public void startError() {
        if (mState == STATE_ERROR) return;
        mState = STATE_ERROR;

        if (mLoadingAnimator != null) mLoadingAnimator.cancel();
        if (mSuccessAnimator != null) mSuccessAnimator.cancel();
        
        // Ensure path is ready
        if (mErrorPathLeft.isEmpty() && getWidth() > 0) {
            initPaths(getWidth(), getHeight());
        }
        
        setColor(Color.parseColor("#FF5252")); // Red for Error
        mErrorAnimator.start();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = mStrokeWidth / 2f;
        mBounds.set(padding, padding, w - padding, h - padding);
        initPaths(w, h);
    }

    private void initPaths(int w, int h) {
        float cx = w / 2f;
        float cy = h / 2f;
        // Radius for the symbols (slightly smaller than circle)
        float r = Math.min(w, h) / 2f - mStrokeWidth;
        
        // 1. Checkmark
        mCheckPath.reset();
        float startX = cx - r * 0.5f;
        float startY = cy;
        float midX = cx - r * 0.1f;
        float midY = cy + r * 0.4f;
        float endX = cx + r * 0.6f;
        float endY = cy - r * 0.5f;

        mCheckPath.moveTo(startX, startY);
        mCheckPath.lineTo(midX, midY);
        mCheckPath.lineTo(endX, endY);
        
        // 2. Error X
        // Left-Top to Right-Bottom
        mErrorPathLeft.reset();
        float xSize = r * 0.5f; // Size of the X arms from center
        mErrorPathLeft.moveTo(cx - xSize, cy - xSize);
        mErrorPathLeft.lineTo(cx + xSize, cy + xSize);
        
        // Right-Top to Left-Bottom
        mErrorPathRight.reset();
        mErrorPathRight.moveTo(cx + xSize, cy - xSize);
        mErrorPathRight.lineTo(cx - xSize, cy + xSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mState == STATE_LOADING) {
            canvas.save();
            canvas.rotate(mLoadingRotation, getWidth() / 2f, getHeight() / 2f);
            canvas.drawArc(mBounds, 0, 270, false, mPaint);
            canvas.restore();
        } else if (mState == STATE_SUCCESS) {
            // Draw full circle
            canvas.drawArc(mBounds, 0, 360, false, mPaint);
            
            // Draw checkmark animation
            if (!mCheckPath.isEmpty()) {
                mCheckDstPath.reset();
                mPathMeasure.setPath(mCheckPath, false);
                float length = mPathMeasure.getLength();
                mPathMeasure.getSegment(0, length * mSuccessProgress, mCheckDstPath, true);
                canvas.drawPath(mCheckDstPath, mPaint);
            }
        } else if (mState == STATE_ERROR) {
            // Draw full circle
            canvas.drawArc(mBounds, 0, 360, false, mPaint);
            
            // Draw X animation
            if (!mErrorPathLeft.isEmpty()) {
                // Draw first line
                mErrorDstPathLeft.reset();
                mPathMeasure.setPath(mErrorPathLeft, false);
                float length1 = mPathMeasure.getLength();
                // Animate first stroke from 0.0 to 0.5 of total progress time? Or both together?
                // Let's do both together for simplicity or sequential. Sequential looks better.
                
                float p1 = Math.min(1f, mErrorProgress * 2f); // First half of animation
                if (p1 > 0) {
                    mPathMeasure.getSegment(0, length1 * p1, mErrorDstPathLeft, true);
                    canvas.drawPath(mErrorDstPathLeft, mPaint);
                }

                // Draw second line
                float p2 = Math.max(0f, mErrorProgress * 2f - 1f); // Second half
                if (p2 > 0) {
                     mErrorDstPathRight.reset();
                     mPathMeasure.setPath(mErrorPathRight, false);
                     float length2 = mPathMeasure.getLength();
                     mPathMeasure.getSegment(0, length2 * p2, mErrorDstPathRight, true);
                     canvas.drawPath(mErrorDstPathRight, mPaint);
                }
            }
        }
    }
    
    public void setColor(int color) {
        mColor = color;
        mPaint.setColor(mColor);
        invalidate();
    }
}
