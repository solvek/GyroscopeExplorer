package com.kircherelectronics.gyroscopeexplorer.gauge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/*
 * Copyright 2013-2017, Kaleb Kircher - Kircher Engineering, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Draws an analog gauge for displaying rotation measurements in three-space
 * from device sensors.
 * 
 * @author Kaleb
 */
public final class GaugeRotation extends View
{
	private static final String tag = GaugeRotation.class.getSimpleName();

	// Keep track of the rotation of the device
	private float[] rotation = new float[3];

	// Keep static bitmaps of the gauge so we only have to redraw if we have to
	// Static bitmap for the bezel of the gauge
	private Bitmap bezelBitmap;
	// Static bitmap for the face of the gauge
	private Bitmap faceBitmap;

	private Paint backgroundPaint;
	private Paint rimOuterPaint;
	private Paint rimPaint;
	private Paint skyPaint;

	private RectF rimRect;
	private RectF rimOuterRect;
	private RectF skyBackgroundRect;

	/**
	 * Create a new instance.
	 * 
	 * @param context
	 */
	public GaugeRotation(Context context)
	{
		super(context);

		initDrawingTools();
	}

	/**
	 * Create a new instance.
	 * 
	 * @param context
	 * @param attrs
	 */
	public GaugeRotation(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		initDrawingTools();
	}

	/**
	 * Create a new instance.
	 * 
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public GaugeRotation(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		initDrawingTools();
	}

	/**
	 * Update the rotation of the device.
	 * 
	 * @param rotation
	 */
	public void updateRotation(float[] rotation)
	{
		System.arraycopy(rotation, 0, this.rotation, 0, rotation.length);

		this.invalidate();
	}

	private void initDrawingTools()
	{
		// Rectangle for the rim of the gauge bezel
		rimRect = new RectF(0.12f, 0.12f, 0.88f, 0.88f);

		// Paint for the rim of the gauge bezel
		rimPaint = new Paint();
		rimPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		// The linear gradient is a bit skewed for realism
		rimPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));

		float rimOuterSize = -0.04f;
		rimOuterRect = new RectF();
		rimOuterRect.set(rimRect.left + rimOuterSize, rimRect.top
				+ rimOuterSize, rimRect.right - rimOuterSize, rimRect.bottom
				- rimOuterSize);

		rimOuterPaint = new Paint();
		rimOuterPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		rimOuterPaint.setColor(Color.rgb(158,158,158));

		float rimSize = 0.02f;

		skyBackgroundRect = new RectF();
		skyBackgroundRect.set(rimRect.left + rimSize, rimRect.top + rimSize,
				rimRect.right - rimSize, rimRect.bottom - rimSize);

		skyPaint = new Paint();
		skyPaint.setAntiAlias(true);
		skyPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		skyPaint.setColor(Color.rgb(158,158,158));

		backgroundPaint = new Paint();
		backgroundPaint.setFilterBitmap(true);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);

		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		int chosenWidth = chooseDimension(widthMode, widthSize);
		int chosenHeight = chooseDimension(heightMode, heightSize);

		int chosenDimension = Math.min(chosenWidth, chosenHeight);

		setMeasuredDimension(chosenDimension, chosenDimension);
	}

	private int chooseDimension(int mode, int size)
	{
		if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY)
		{
			return size;
		}
		else
		{ // (mode == MeasureSpec.UNSPECIFIED)
			return getPreferredSize();
		}
	}

	// in case there is no size specified
	private int getPreferredSize()
	{
		return 300;
	}

	/**
	 * Draw the gauge rim.
	 * 
	 * @param canvas
	 */
	private void drawRim(Canvas canvas)
	{
		// First draw the most back rim
		canvas.drawOval(rimOuterRect, rimOuterPaint);
		// Then draw the small black line
		canvas.drawOval(rimRect, rimPaint);
	}

	/**
	 * Draw the gauge face.
	 * 
	 * @param canvas
	 */
	private void drawFace(Canvas canvas)
	{
		// free the old bitmap
		if (faceBitmap != null)
		{
			faceBitmap.recycle();
		}

		faceBitmap = Bitmap.createBitmap(getWidth(), getHeight(),
				Bitmap.Config.ARGB_8888);

		Canvas faceCanvas = new Canvas(faceBitmap);
		float scale = (float) getWidth();
		faceCanvas.scale(scale, scale);

		skyBackgroundRect.set(rimRect.left, rimRect.top, rimRect.right,
				rimRect.bottom);

		faceCanvas.drawArc(skyBackgroundRect, 0, 360, true, skyPaint);

		int[] allpixels = new int[faceBitmap.getHeight()
				* faceBitmap.getWidth()];

		faceBitmap.getPixels(allpixels, 0, faceBitmap.getWidth(), 0, 0,
				faceBitmap.getWidth(), faceBitmap.getHeight());

		for (int i = 0; i < faceBitmap.getHeight() * faceBitmap.getWidth(); i++)
		{
			allpixels[i] = Color.TRANSPARENT;
		}

		int height = (int) ((faceBitmap.getHeight() / 2) - ((faceBitmap
				.getHeight() / 2.5) * rotation[1]));

		if (height > faceBitmap.getHeight())
		{
			height = faceBitmap.getHeight();
		}
		
		// Check for 0 case
		if(height == 0)
		{
			height = 1;
		}

		faceBitmap.setPixels(allpixels, 0, faceBitmap.getWidth(), 0, 0,
				faceBitmap.getWidth(), height);

		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		canvas.rotate((float) Math.toDegrees(rotation[2]), faceBitmap.getWidth() / 2f,
				faceBitmap.getHeight() / 2f);

		canvas.drawBitmap(faceBitmap, 0, 0, backgroundPaint);
		canvas.restore();
	}

	/**
	 * Draw the gauge bezel.
	 * 
	 * @param canvas
	 */
	private void drawBezel(Canvas canvas)
	{
		if (bezelBitmap == null)
		{
			Log.w(tag, "Bezel not created");
		}
		else
		{
			canvas.drawBitmap(bezelBitmap, 0, 0, backgroundPaint);
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		Log.d(tag, "Size changed to " + w + "x" + h);

		regenerateBezel();
	}

	/**
	 * Regenerate the background image. This should only be called when the size
	 * of the screen has changed. The background will be cached and can be
	 * reused without needing to redraw it.
	 */
	private void regenerateBezel()
	{
		// free the old bitmap
		if (bezelBitmap != null)
		{
			bezelBitmap.recycle();
		}

		bezelBitmap = Bitmap.createBitmap(getWidth(), getHeight(),
				Bitmap.Config.ARGB_8888);
		Canvas bezelCanvas = new Canvas(bezelBitmap);
		float scale = (float) getWidth();
		bezelCanvas.scale(scale, scale);

		drawRim(bezelCanvas);
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		drawBezel(canvas);
		drawFace(canvas);

		float scale = (float) getWidth();
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		canvas.scale(scale, scale);

		canvas.restore();
	}

}
