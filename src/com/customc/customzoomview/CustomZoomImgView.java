/**
 * Copyright 2014 
 * 
 * Yogesh Pangam  
 *  
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this CustomZoomImgView software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.customc.customzoomview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

/**
 * <b>ImageView<br/>
 * └CustomZoomImgView<br/>
 * </b> This class is custom image view capable of zoom in and zoom out on pinch
 * in & pinch out events.
 * 
 * @author Yogesh Pangam.
 * 
 */
public class CustomZoomImgView extends ImageView implements OnTouchListener {
	public enum TranslatePos {
		CENTER, TOPLEFT, TOPRIGHT, BOTTOMLEFT, BOTTOMRIGHT
	}

	public enum PivotPoint {
		CENTER, TOPLEFT, TOPRIGHT, BOTTOMLEFT, BOTTOMRIGHT
	}

	public enum ZoomStyle {
		HORIZONTALONLY, VERTICALONLY, BOTHDIRACTION
	}

	ZoomStyle objZoomStyle;
	PivotPoint objPivotPoint;
	TranslatePos objTranslatePos = TranslatePos.CENTER;
	private boolean isZoomVerticleOnly = false, isZoomHorizontalOnly = false;

	private Matrix matrix;
	private Matrix savedMatrix = new Matrix();
	private volatile float defaultBase = 1, minZoomLevel = 0.2f,
			maxZoomLevel = 2;
	private boolean atStart = true;
	boolean isResizeBM = true;
	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	private boolean shouldCentered = false, isCentered = false;
	private int mode = NONE;

	private PointF mStartPoint = new PointF();
	private PointF mMiddlePoint = new PointF();
	private boolean isPivotpointSetExpl = false;
	private float oldDist = 1f;
	private float matrixValues[] = { 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f };
	private float scale;
	private int mViewWidth = -1, mOriViewWidth;
	private int mViewHeight = -1, mOriViewHeight;
	private int mBitmapWidth;
	private int mBitmapHeight;

	private boolean mScrollable = false, mHScrollable = false,
			mVScrollable = false;
	boolean isZoomEnabled = true, isScrollEnabled = true;
	Bitmap bitmap;

	public CustomZoomImgView(Context context, Bitmap bm) {
		super(context);
		Log.d("app", "constructor CustomZoomImgView1");
		this.setScaleType(ScaleType.MATRIX);
		this.setOnTouchListener(this);
		this.setBackgroundColor(Color.parseColor("#00000000"));
		this.setMinZoomLevel(100);
		this.setMaxZoomLevel(2000);
		setImageBitmap(bm);
	}

	public CustomZoomImgView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CustomZoomImgView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.setScaleType(ScaleType.MATRIX);
		this.setOnTouchListener(this);
		this.setBackgroundColor(Color.parseColor("#00000000"));
		this.setMinZoomLevel(100);
		this.setMaxZoomLevel(2000);
		Log.d("app", "constructor CustomZoomImgView2");
		setImageBitmap(((BitmapDrawable) this.getDrawable()).getBitmap());
	}

	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mViewWidth = w;
		mViewHeight = h;
		Log.d("appOnSiaeChanged", "" + w);
	}

	@Override
	protected void onDetachedFromWindow() {
		// TODO Auto-generated method stub
		this.setImageBitmap(null);
		if (bitmap != null)
			bitmap.recycle();
		Log.d("app", "View is detached from window.");
		super.onDetachedFromWindow();
	}

	public void setAlignmentWhenSmaller(TranslatePos objTranslatePos){
		this.objTranslatePos=objTranslatePos;		
	}
	/**
	 * <b>public void setImageBitmap(Bitmap bm) </b><br/>
	 * This method from ImageView is overridden in CustomZoomImgView. It sets
	 * the bitmap bm as the content of CustomZoomImgView. <b><br/>
	 * Note : Any previous bitmap of the view will be recycled. </b>
	 * */
	@Override
	public void setImageBitmap(Bitmap bm) {
		// TODO Auto-generated method stub
		atStart = true;
		Log.d("app", "set Image Bitmap called" + bm);
		if (bitmap != null && bitmap != bm) {
			Log.d("app", "BitMap Recycled");
			bitmap.recycle();
			bitmap = null;
		}
		System.gc();
		if (!isResizeBM) {
			ViewGroup.LayoutParams lp = this.getLayoutParams();
			lp.height = LayoutParams.WRAP_CONTENT;
			lp.width = LayoutParams.WRAP_CONTENT;
			this.setLayoutParams(lp);
		}
		bitmap = bm;
		super.setImageBitmap(bitmap);
		if (bitmap != null) {
			matrix = new Matrix();
			mBitmapWidth = bitmap.getWidth();
			mBitmapHeight = bitmap.getHeight();
			this.setImageMatrix(matrix);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (atStart) {
			float m[] = new float[9];
			matrix.getValues(m);
			RectF src = new RectF(0, 0, mBitmapWidth, mBitmapHeight);
			RectF dst = new RectF(0, 0, mViewWidth, mViewHeight);
			if (isResizeBM) {
				matrix.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER);
			} else {
				matrix.setRectToRect(src, src, Matrix.ScaleToFit.START);
			}
			matrix.getValues(m);
			defaultBase = m[0];

			Log.d("trace", "" + mBitmapWidth + " " + mBitmapHeight + " "
					+ mViewWidth + " " + mViewHeight + " " + m[0] + " " + m[4]);
			this.setImageMatrix(matrix);
			atStart = false;
		}
		super.onDraw(canvas);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// Log.d("trace","ontouchImage");
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			Log.d("app", "Actiondown");
			mScrollable = isImageLarger();
			mHScrollable = isImageHLarger();
			mVScrollable = isImageVLarger();
			savedMatrix.set(matrix);
			mStartPoint.set(event.getX(), event.getY());
			mode = DRAG;
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			Log.d("app", "ActionPointerdown");
			mOriViewWidth = mViewWidth;
			if (getParent() != null) {
				getParent().requestDisallowInterceptTouchEvent(true);
			}
			oldDist = spacing(event);
			if (oldDist > 10f) {
				if (!isImageLarger()) {
					shouldCentered = true;
				} else {
					shouldCentered = false;
				}
				savedMatrix.set(matrix);
				midPoint(mMiddlePoint, event);
				mode = ZOOM;
			}
			break;
		case MotionEvent.ACTION_UP:
			Log.d("app", "ActionUP");
			break;
		case MotionEvent.ACTION_POINTER_UP:
			Log.d("app", "ActionPointerUP");
			if (!isImageLarger()) {
				isCentered = false;
				shouldCentered = true;
			} else {
				isCentered = true;
				shouldCentered = false;
			}
			if (shouldCentered) {
				if (!isCentered) {
					matrix.getValues(matrixValues);
					int bitmapCurrWidth = (int) (matrixValues[0] * mBitmapWidth);
					int bitmapCurrHeight = (int) (matrixValues[4] * mBitmapHeight);

					if (objTranslatePos == TranslatePos.CENTER) {
						matrixValues[2] = (int) ((mViewWidth - bitmapCurrWidth) / 2);
						matrixValues[5] = (int) ((mViewHeight - bitmapCurrHeight) / 2);
					} else if (objTranslatePos == TranslatePos.BOTTOMLEFT) {
						matrixValues[2] = 0;
						matrixValues[5] = (int) ((mViewHeight - bitmapCurrHeight));
					} else if (objTranslatePos == TranslatePos.TOPLEFT) {
						matrixValues[2] = 0;
						matrixValues[5] = 0;
					} else if (objTranslatePos == TranslatePos.BOTTOMRIGHT) {
						matrixValues[2] = (int) ((mViewWidth - bitmapCurrWidth));
						matrixValues[5] = (int) ((mViewHeight - bitmapCurrHeight));
					} else if (objTranslatePos == TranslatePos.TOPRIGHT) {
						matrixValues[2] = (int) ((mViewWidth - bitmapCurrWidth));
						matrixValues[5] = 0;
					}

					Log.d("appM", "centered " + matrixValues[2] + " "
							+ matrixValues[5]);
					matrix.setValues(matrixValues);
					this.setImageMatrix(matrix);
					invalidate();
					isCentered = true;
					shouldCentered = false;
				}
			}

			mode = NONE;
			break;
		case MotionEvent.ACTION_MOVE:
			if (mode == DRAG) {
				if (isScrollEnabled)
					drag(event);
			} else if (mode == ZOOM) {
				if (isZoomEnabled)
					zoom(event);
			}
			break;
		}
		return true;
	}

	private void drag(MotionEvent event) {
		savedMatrix.getValues(matrixValues);
		float left = matrixValues[2];
		float top = matrixValues[5];
		float bottom = (0 + (matrixValues[4] * mBitmapHeight)) - mViewHeight;
		float right = (0 + (matrixValues[0] * mBitmapWidth)) - mViewWidth;
		float dx = event.getX() - mStartPoint.x;
		float dy = event.getY() - mStartPoint.y;
		if (mScrollable) {// Draggable in both directions
			if ((left + dx) > 0) {
				dx = 0;
				savedMatrix.getValues(matrixValues);
				matrixValues[2] = 0.0000f;
				savedMatrix.setValues(matrixValues);
			}
			if ((top + dy) > 0) {
				dy = 0;
				savedMatrix.getValues(matrixValues);
				matrixValues[5] = 0.0000f;
				savedMatrix.setValues(matrixValues);
			}
			if ((left + dx) < -right) {
				dx = 0;
				savedMatrix.getValues(matrixValues);
				matrixValues[2] = -right;
				savedMatrix.setValues(matrixValues);
			}
			if ((top + dy) < -bottom) {
				dy = 0;
				savedMatrix.getValues(matrixValues);
				matrixValues[5] = -bottom;
				savedMatrix.setValues(matrixValues);
			}
			matrix.set(savedMatrix);
			matrix.postTranslate(dx, dy);
			this.setImageMatrix(matrix);
		} else if (mHScrollable) {// Draggable Horizontally
			if ((left + dx) > 0) {
				dx = 0;
				savedMatrix.getValues(matrixValues);
				matrixValues[2] = 0.0000f;
				savedMatrix.setValues(matrixValues);
			}
			if ((left + dx) < -right) {
				dx = 0;
				savedMatrix.getValues(matrixValues);
				matrixValues[2] = -right;
				savedMatrix.setValues(matrixValues);
			}
			matrix.set(savedMatrix);
			matrix.postTranslate(dx, 0);
			this.setImageMatrix(matrix);

		} else if (mVScrollable) {// Draggable Vertically
			if ((top + dy) > 0) {
				dy = 0;
				savedMatrix.getValues(matrixValues);
				matrixValues[5] = 0.0000f;
				savedMatrix.setValues(matrixValues);
			}
			if ((top + dy) < -bottom) {
				dy = 0;
				savedMatrix.getValues(matrixValues);
				matrixValues[5] = -bottom;
				savedMatrix.setValues(matrixValues);
			}
			matrix.set(savedMatrix);
			matrix.postTranslate(0, dy);
			this.setImageMatrix(matrix);
		}
	}

	private void zoom(MotionEvent event) {
		float newDist = spacing(event);
		boolean zoomIn = newDist > oldDist;
		matrix.set(savedMatrix);

		savedMatrix.getValues(matrixValues);
		scale = newDist / oldDist;

		float temp = matrixValues[0];
		;
		if (isZoomHorizontalOnly) {
			temp = matrixValues[0];
		} else if (isZoomVerticleOnly) {
			temp = matrixValues[4];
		}

		if (!zoomIn && (temp * scale) < defaultBase * (minZoomLevel)) {
			scale = (defaultBase * (minZoomLevel)) / temp;
		}
		if (zoomIn && (temp * scale) > defaultBase * (maxZoomLevel)) {
			scale = (defaultBase * (maxZoomLevel)) / temp;
		}
		float midX = mMiddlePoint.x;
		float midY = mMiddlePoint.y;
		if (isPivotpointSetExpl) {
			if (objPivotPoint == PivotPoint.CENTER) {
				midX = (mViewWidth / 2);
				midY = (mViewHeight / 2);
			} else if (objPivotPoint == PivotPoint.BOTTOMLEFT) {
				midX = 0;
				midY = (mViewHeight);
			} else if (objPivotPoint == PivotPoint.TOPLEFT) {
				midX = 0;
				midY = 0;
			} else if (objPivotPoint == PivotPoint.BOTTOMRIGHT) {
				midX = mViewWidth;
				midY = mViewHeight;
			} else if (objPivotPoint == PivotPoint.TOPRIGHT) {
				midX = mViewWidth;
				midY = 0;
			}
		}
		float scaleX = scale, scaleY = scale;

		if (isZoomHorizontalOnly) {
			scaleY = 1;
		} else if (isZoomVerticleOnly) {
			scaleX = 1;
		}
		if (!isPivotpointSetExpl) {
			if (shouldCentered) {
				midX = (mViewWidth / 2);
				midY = (mViewHeight / 2);
				matrix.postScale(scaleX, scaleY, midX, midY);
			} else {
				matrix.postScale(scaleX, scaleY, mMiddlePoint.x, mMiddlePoint.y);
			}
		} else {
			matrix.postScale(scaleX, scaleY, midX, midY);
		}
		this.setImageMatrix(matrix);
		if (!isResizeBM) {
			int width = Math.round((mOriViewWidth * scale));
			ViewGroup.LayoutParams lp = this.getLayoutParams();
			lp.width = width;
			this.setLayoutParams(lp);

			this.requestLayout();
		}
	}

	/**
	 * <b>private float spacing(MotionEvent event) <br/>
	 * </b>Determine the space between the first two fingers
	 */
	private float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float) Math.sqrt(x * x + y * y);
	}

	/**
	 * <b>private void midPoint(PointF point, MotionEvent event) <br/>
	 * </b> Calculate the mid point of the first two fingers
	 */
	private void midPoint(PointF point, MotionEvent event) {
		float x = event.getX(0) + event.getX(1);
		float y = event.getY(0) + event.getY(1);
		point.set(x / 2, y / 2);
	}

	/*
	 * private void displayMatrix(Matrix mat) { if (mat != null) { float m[] =
	 * new float[9]; mat.getValues(m); Log.d("appM", "-----------------");
	 * Log.d("appM", " " + m[0] + "\t " + m[1] + "\t " + m[2]); Log.d("appM",
	 * " " + m[3] + "\t " + m[4] + "\t " + m[5]); Log.d("appM", " " + m[6] +
	 * "\t " + m[7] + "\t " + m[8]); } }
	 */

	private boolean isImageLarger() {
		if (isZoomHorizontalOnly)
			return isImageHLarger();
		if (isZoomVerticleOnly)
			return isImageVLarger();
		else
			return (isImageHLarger() && isImageVLarger());
	}

	private boolean isImageHLarger() {
		if (matrix != null) {
			float m[] = new float[9];
			matrix.getValues(m);
			float bitmapWidth = m[0] * mBitmapWidth;
			if (bitmapWidth > mViewWidth) {
				return true;
			} else
				return false;
		}
		return false;
	}

	private boolean isImageVLarger() {
		if (matrix != null) {
			float m[] = new float[9];
			matrix.getValues(m);
			float bimtapHeight = m[4] * mBitmapHeight;
			if (bimtapHeight > mViewHeight) {
				return true;
			} else
				return false;
		}
		return false;
	}

	/**
	 * <b>public float getMinZoomLevel ()</b><br/>
	 * Returns minimum zoom level for the image
	 * */
	public float getMinZoomLevel() {
		return minZoomLevel;
	}

	/**
	 * <b>public void setMinZoomLevel ( float minZoomLevel )</b><br/>
	 * Sets minimum zoom level for the image
	 * */
	public void setMinZoomLevel(float minZoomLevel) {
		this.minZoomLevel = minZoomLevel / 100;
	}

	/**
	 * <b>public float getMaxZoomLevel ()</b><br/>
	 * Returns maximum zoom level for the image
	 * */
	public float getMaxZoomLevel() {
		return maxZoomLevel;
	}

	/**
	 * <b>public void setMaxZoomLevel ( float maxZoomLevel )</b><br/>
	 * Sets maximum zoom level for the image
	 * */
	public void setMaxZoomLevel(float maxZoomLevel) {
		this.maxZoomLevel = maxZoomLevel / 100;
	}

	/**
	 * <b>public void zoomImage ( float scale )</b><br/>
	 * Zooms the image by scale about center point of the bitmap.
	 * */
	public void zoomImage(float scale) {
		scale = scale / 100;

		if ((defaultBase * scale) < defaultBase * (minZoomLevel)
				|| (defaultBase * scale) > defaultBase * (maxZoomLevel))
			return;

		matrix.setScale(defaultBase * scale, defaultBase * scale);

		float m[] = new float[9];
		matrix.getValues(m);
		int bitmapCurrWidth = (int) (m[0] * mBitmapWidth);
		int bitmapCurrHeight = (int) (m[4] * mBitmapHeight);

		m[2] = (int) ((mViewWidth - bitmapCurrWidth) / 2);
		m[5] = (int) ((mViewHeight - bitmapCurrHeight) / 2);
		Log.d("appM", "centered " + matrixValues[2] + " " + matrixValues[5]);
		matrix.setValues(m);

		this.setImageMatrix(matrix);
		invalidate();
	}

	public void setPivotPoint(PivotPoint objPivotPoint) {
		if (objPivotPoint == null) {
			this.objPivotPoint = objPivotPoint;
			isPivotpointSetExpl = false;
		} else {
			this.objPivotPoint = objPivotPoint;
			isPivotpointSetExpl = true;
		}
	}

	public void setZoomStyle(ZoomStyle objZoomStyle) {
		this.objZoomStyle = objZoomStyle;
		if (objZoomStyle == ZoomStyle.HORIZONTALONLY) {
			isZoomHorizontalOnly = true;
			isZoomVerticleOnly = false;
		} else if (objZoomStyle == ZoomStyle.VERTICALONLY) {
			isZoomHorizontalOnly = false;
			isZoomVerticleOnly = true;
		} else {
			isZoomHorizontalOnly = false;
			isZoomVerticleOnly = false;
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		// Log.d("trace", "DispatchImage ");
		boolean flag = super.dispatchTouchEvent(ev);
		return flag;
	}
}