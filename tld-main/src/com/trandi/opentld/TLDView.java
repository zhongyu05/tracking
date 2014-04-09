/**
 * Copyright 2013 Dan Oprescu
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.trandi.opentld;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.R.bool;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Toast;

import com.trandi.opentld.tld.Tld;
import com.trandi.opentld.tld.Tld.ProcessFrameStruct;
import com.trandi.opentld.tld.Util;


public class TLDView extends JavaCameraView implements CameraBridgeViewBase.CvCameraViewListener {
	final private SurfaceHolder _holder;
    private int _canvasImgYOffset;
    private int _canvasImgXOffset;
    
    private Boolean frameSelected = false;
    private Boolean bbgot = false;
    private Boolean fileWritten = false;
	
	private Mat _currentGray = new Mat();
	private Mat _readImg = new Mat();
	private Mat _lastGray = new Mat();
	private Mat _frozeFrame = new Mat();
	private Mat _finalgray = new Mat();
	private Tld _tld = null;
	private Rect _trackedBox = null;
	private ProcessFrameStruct _processFrameStruct = null;
	private Properties _tldProperties;
	
	private static final Size WORKING_FRAME_SIZE = new Size(144, 80);
	private Mat _workingFrame = new Mat();
	private String _errMessage;
	
	public TLDView(Context context, AttributeSet attrs) {
		super(context, attrs);
		_holder = getHolder();
		
		// Init the PROPERTIES
		InputStream propsIS = null;
		try{
			propsIS = context.getResources().openRawResource(R.raw.parameters);
			_tldProperties = new Properties();
			_tldProperties.load(propsIS);
		} catch (IOException e) {
			Log.e(Util.TAG, "Can't load properties", e);
		}finally{
			if(propsIS != null){
				try {
					propsIS.close();
				} catch (IOException e) {
					Log.e(Util.TAG, "Can't close props", e);
				}
			}
		}
		
		//File testImage = new File("/storage/sdcard0/DCIM/Camera/)
		_readImg = Highgui.imread("/storage/sdcard0/DCIM/Camera/IMG_20140322_183024.jpg");
		//Toast.makeText(context, "Height: " + _frozeFrame.height() + " Width: " + _frozeFrame.width(), Toast.LENGTH_LONG).show();
		// listens to its own events
		setCvCameraViewListener(this);
		
		
		// DEBUG
		//_trackedBox = new BoundingBox(165,93,51,54, 0, 0);
		
		// LISTEN for touches of the screen, to define the BOX to be tracked
		final AtomicReference<Point> trackedBox1stCorner = new AtomicReference<Point>();
		final Paint rectPaint = new Paint();
		rectPaint.setColor(Color.rgb(0, 255, 0));
		rectPaint.setStrokeWidth(5);
		rectPaint.setStyle(Style.STROKE);
		
		setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// re-init
				_errMessage = null;
				//_tld = null;
				
				final Point corner = new Point(event.getX() - _canvasImgXOffset, event.getY() - _canvasImgYOffset);
				switch(event.getAction()){
				case MotionEvent.ACTION_DOWN:
					if(frameSelected && !bbgot){
						Log.i(Util.TAG, "1st corner: " + corner);
						trackedBox1stCorner.set(corner);
						break;
					}
					break;
				case MotionEvent.ACTION_UP:
					if(!frameSelected && !bbgot){
						frameSelected = true;
						break;
					} else if (frameSelected && !bbgot) {
						_trackedBox = new Rect(trackedBox1stCorner.get(), corner);
						//bbgot = true;
					}
					//Log.i(Util.TAG, "bbgot" + bbgot);
					Log.i(Util.TAG, "Tracked box DEFINED: " + _trackedBox);
					break;
				case MotionEvent.ACTION_MOVE:
					if (frameSelected && !bbgot) {
						final android.graphics.Rect rect = new android.graphics.Rect(
								(int) trackedBox1stCorner.get().x
										+ _canvasImgXOffset,
								(int) trackedBox1stCorner.get().y
										+ _canvasImgYOffset, (int) corner.x
										+ _canvasImgXOffset, (int) corner.y
										+ _canvasImgYOffset);
						final Canvas canvas = _holder.lockCanvas(rect);
						canvas.drawColor(Color.TRANSPARENT,
								PorterDuff.Mode.CLEAR); // remove old rectangle
						canvas.drawRect(rect, rectPaint);
						_holder.unlockCanvasAndPost(canvas);
						break;
					}
				}
				return true;
			}
		});
	}

	@Override
	public Mat onCameraFrame(Mat originalFrame) {
		if (!frameSelected) {
			//_frozeFrame = originalFrame;
			originalFrame.copyTo(_frozeFrame);
			//Imgproc.resize(_readImg, _frozeFrame, originalFrame.size());
			//Imgproc.cvtColor(_frozeFrame, _frozeFrame, Imgproc.COLOR_BGR2RGB);
		} /*else if(!fileWritten) {
	        String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
	        File imgDir = new File(extStorageDirectory, "tldimgs");
	        File imgToWrite = new File(imgDir.getAbsoluteFile(), "image.jpg");
	        Boolean bool = Highgui.imwrite(imgToWrite.getAbsolutePath(), _frozeFrame);
	        fileWritten = true;
	        if (bool){
	            Log.i(Util.TAG, "SUCCESS writing image to external storage");
	           else
	            Log.i(Util.TAG, "Fail writing image to external storage");
		}*/
		try{
			// Image is too big and this requires too much CPU for a phone, so scale everything down...			
			if (frameSelected && !bbgot) {
				Imgproc.resize(_frozeFrame, _workingFrame, WORKING_FRAME_SIZE);
			} else {
				Imgproc.resize(originalFrame, _workingFrame, WORKING_FRAME_SIZE);
			}
			final Size workingRatio = new Size(originalFrame.width() / WORKING_FRAME_SIZE.width, originalFrame.height() / WORKING_FRAME_SIZE.height);
			// usefull to see what we're actually working with...
			/*if (frameSelected && _trackedBox == null) {
				_workingFrame.copyTo(_frozeFrame.submat(_frozeFrame.rows() - _workingFrame.rows(), _frozeFrame.rows(), 0, _workingFrame.cols()));
			} else {
				//_workingFrame.copyTo(originalFrame.submat(originalFrame.rows() - _workingFrame.rows(), originalFrame.rows(), 0, _workingFrame.cols()));
				_finalgray.copyTo(originalFrame.submat(originalFrame.rows() - _finalgray.rows(), originalFrame.rows(), 0, _finalgray.cols()));
			}*/
			if(_trackedBox != null){
				if(_tld == null){ // run the 1st time only
					Imgproc.cvtColor(_workingFrame, _lastGray, Imgproc.COLOR_RGB2GRAY);
					_tld = new Tld(_tldProperties);
					final Rect scaledDownTrackedBox = scaleDown(_trackedBox, workingRatio);
					//_workingFrame.copyTo(_finalgray);
					Log.i(Util.TAG, "Final:"+_finalgray.size().height);
					//Log.i(Util.TAG, "Working Ration: " + workingRatio + " / Tracking Box: " + _trackedBox + " / Scaled down to: " + scaledDownTrackedBox);
					try {
						_tld.init(_lastGray, scaledDownTrackedBox);
					}catch(Exception eInit){
				        // start from scratch, you have to select an init box again !
						_trackedBox = null;
						_tld = null;
						throw eInit; // re-throw it as it will be dealt with later
					}
					bbgot = true;
				}else{
					Imgproc.cvtColor(_workingFrame, _currentGray, Imgproc.COLOR_RGB2GRAY);
				
					_processFrameStruct = _tld.processFrame(_lastGray, _currentGray);
					drawPoints(originalFrame, _processFrameStruct.lastPoints, workingRatio, new Scalar(255, 0, 0));
					drawPoints(originalFrame, _processFrameStruct.currentPoints, workingRatio, new Scalar(0, 255, 0));
					drawBox(originalFrame, scaleUp(_processFrameStruct.currentBBox, workingRatio), new Scalar(0, 0, 255));
						
					_currentGray.copyTo(_lastGray);
					
					// overlay the current positive examples on the real image(needs converting at the same time !)
					//copyTo(_tld.getPPatterns(), originalFrame);
				}
				Log.i(Util.TAG, "Box size"+_trackedBox.size());
			}
		} catch(Exception e) {
	        _errMessage = e.getClass().getSimpleName() + " / " + e.getMessage();
	        Log.e(Util.TAG, "TLDView PROBLEM", e);
		}

		
		if(_errMessage !=  null){
			Core.putText(originalFrame, _errMessage, new Point(0, 300), Core.FONT_HERSHEY_PLAIN, 1.3d, new Scalar(255, 0, 0), 2);
		}
		if(frameSelected && !bbgot){
			return _frozeFrame;
		}
		else {
			return originalFrame;
		}
	}

	private static void copyTo(List<Mat> patterns, Mat dest) {
		if(patterns == null || patterns.isEmpty() || dest == null) return;
		
		final int patternRows = patterns.get(0).rows();
		final int patternCols = patterns.get(0).cols();
		final int vertCount = dest.rows() / patternRows;
		final int horizCount = patterns.size() / vertCount + 1;
		
		int patchIdx = 0;
		for(int col = dest.cols() - horizCount * patternCols - 1; col < dest.cols()  && patchIdx < patterns.size(); col += patternCols){
			for(int row = 0; row < dest.rows() && patchIdx < patterns.size(); row += patternRows) {
				Imgproc.cvtColor(patterns.get(patchIdx), dest.submat(row, row + patternRows, col, col + patternCols), Imgproc.COLOR_GRAY2RGBA);
				patchIdx++;
			}
		}	
	}
	
	@Override
	public void onCameraViewStarted(int width, int height) {
    	_canvasImgXOffset = (getWidth() - width) / 2;
    	_canvasImgYOffset = (getHeight() - height) / 2;
	}

	@Override
	public void onCameraViewStopped() {
		// TODO Auto-generated method stub
	}
	
	
	private static void drawPoints(Mat image, final Point[] points, final Size scale, final Scalar colour){
		if(points != null){
			for(Point point : points){
				Core.circle(image, scaleUp(point, scale), 2, colour);
			}
		}
	}
	
	private static void drawBox(Mat image, final Rect box, final Scalar colour){
		if(box != null){
			Core.rectangle(image, box.tl(), box.br(), colour);
		}
	}
	
	
	/* SCALING */
	
	private static Point scaleUp(Point point, Size scale){
		if(point == null || scale == null) return null;
		return new Point(point.x * scale.width, point.y * scale.height);
	}
	
	private static Point scaleDown(Point point, Size scale){
		if(point == null || scale == null) return null;
		return new Point(point.x / scale.width, point.y / scale.height);
	}
	
	private static Rect scaleUp(Rect rect, Size scale) {
		if(rect == null || scale == null) return null;
		return new Rect(scaleUp(rect.tl(), scale), scaleUp(rect.br(), scale));
	}
	
	private static Rect scaleDown(Rect rect, Size scale) {
		if(rect == null || scale == null) return null;
		return new Rect(scaleDown(rect.tl(), scale), scaleDown(rect.br(), scale));
	}
}
