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
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.R.integer;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.trandi.opentld.TLDView.ViewUpdateEventListener;
import com.trandi.opentld.tld.Util;

public class MainActivity extends Activity {
	private TLDView _tldView;
	private ArrayList<Rect> boxes = new ArrayList<Rect>();
	private ArrayList<ImageView> boxViews = new ArrayList<ImageView>();
	private ArrayList<RelativeLayout.LayoutParams> originalParams = new ArrayList<RelativeLayout.LayoutParams>();
	private int _mainBoxIndex;
	private double displayRatio;
    
	private static final String TAG = "TLDActivity";
	
    private BaseLoaderCallback _openCVCallBack = new BaseLoaderCallback(this) {
    	@Override
    	public void onManagerConnected(int status) {
    		switch (status) {
				case LoaderCallbackInterface.SUCCESS:
				{
					Log.i(Util.TAG, "OpenCV loaded successfully");
					
			        setContentView(R.layout.activity_main);
			        			        
					_tldView = (TLDView) findViewById(R.id.tld_view);
					_tldView.setVisibility(SurfaceView.VISIBLE);
					_tldView.enableView();
					_tldView.setEventListener(new ViewUpdateEventListener(){

						@Override
						public void onViewUpdated() {
							Log.v(TAG, "view updated!");
							if (!_tldView.getViewReady()) {
								return;
							} else if (_tldView.getTrackedBox() == null) {
						        double bgwidth = _tldView.getFrameSize().width;
						        String jsonString = loadJSONFromAsset();
								try {
									JSONObject obj = new JSONObject(jsonString);
									int mainIndex = Integer.parseInt(obj.getString("main"));
									_mainBoxIndex = mainIndex;
									int imgwidth = Integer.parseInt(obj.getJSONArray("resolution").get(0).toString());
									//int imgheight = Integer.parseInt(obj.getJSONArray("resolution").get(1).toString());
									double workratio = (double)imgwidth/(double)bgwidth;
									JSONArray boxesJsonArray = obj.getJSONArray("box");
									if (boxesJsonArray != null) {
										int len = boxesJsonArray.length();
										for (int i=0 ; i < len; i++){
											JSONArray thisbox = boxesJsonArray.getJSONArray(i);
											boxes.add(new Rect((int)(Integer.parseInt(thisbox.getString(0))/workratio), 
													(int)(Integer.parseInt(thisbox.getString(1))/workratio), 
													(int)(Integer.parseInt(thisbox.getString(2))/workratio), 
													(int)(Integer.parseInt(thisbox.getString(3))/workratio)));
										}
										Rect mainBox = boxes.get(mainIndex);
										_tldView.setMainBox(mainBox);
										
										Handler mainHandler = new Handler(Looper.getMainLooper());
										Runnable myRunnable = new Runnable(){

											@Override
											public void run() {
												// TODO Auto-generated method stub
												for (int i=0 ; i<boxes.size();i++){
													ImageView iv;
													RelativeLayout.LayoutParams params;
													Rect thisRect = boxes.get(i);
													

													RelativeLayout rl = (RelativeLayout) findViewById(R.id.rlay);
													displayRatio = rl.getHeight()/_tldView.getFrameSize().height;
													double xoffset = (rl.getWidth() - _tldView.getFrameSize().width*displayRatio)/2;
													//Log.v(TAG, "rl width:" + rl.getWidth() + "," + _tldView.getFrameSize().width);
													//Log.v(TAG, "rl height:" + rl.getHeight() + "," + _tldView.getFrameSize().height);

													double yoffset = (rl.getHeight() - _tldView.getFrameSize().height*displayRatio)/2;


													iv = new ImageView(getApplicationContext());
													iv.setImageResource(R.drawable.ic_launcher);
													//if (i==_mainBoxIndex) {
														iv.setBackgroundColor(Color.GREEN);
													//}

													params = new RelativeLayout.LayoutParams(thisRect.right-thisRect.left,thisRect.bottom-thisRect.top);
													params.leftMargin = (int) (thisRect.left*displayRatio+xoffset);
													params.topMargin = (int) (thisRect.top*displayRatio+yoffset);
													params.height = (int) ((double)params.height*displayRatio);
													params.width = (int) ((double)params.width*displayRatio);
													
													rl.addView(iv, params);
													originalParams.add(params);
													iv.setFocusable(true);
													iv.setAlpha(1);
													boxViews.add(iv);
													iv.setContentDescription(""+i);
													for (int j=0; j<i ; j++){
														if(thisRect.contains(boxes.get(j))){
															boxViews.get(j).bringToFront();
														}
													}
													///iv.bringToFront();
												}
											}
											
										};
										mainHandler.post(myRunnable);
									}

										
									
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							} else {
								org.opencv.core.Rect mainRect = _tldView.getMainBox();
								org.opencv.core.Rect trackedRect = _tldView.getTrackedBox();
								if (trackedRect == null) {
									return;
								}
								if (mainRect == null) {
									mainRect = trackedRect;
								}
								//final double xoffset = trackedRect.x + (double)trackedRect.width/2 - mainRect.x - (double)mainRect.width/2;
								//final double yoffset = trackedRect.y + (double)trackedRect.height/2 - mainRect.y - (double)mainRect.height/2;
								
								final double xoffset = trackedRect.x - mainRect.x;
								final double yoffset = trackedRect.y - mainRect.y;

								final double widthPer = (double)trackedRect.width/(double)mainRect.width;
								final double heightPer = (double)trackedRect.height/(double)mainRect.height;
								Log.v(TAG, "update tracked" + trackedRect.x+","+trackedRect.y+","+trackedRect.width+","+trackedRect.height);
								Log.v(TAG, "update main" + mainRect.x+","+mainRect.y+","+mainRect.width+","+mainRect.height);
								Log.v(TAG, "update change" + xoffset+","+yoffset+","+widthPer+","+heightPer);

								Handler mainHandler = new Handler(Looper.getMainLooper());
								Runnable myRunnable = new Runnable(){

									@Override
									public void run() {
										RelativeLayout relativeL = (RelativeLayout) findViewById(R.id.rlay);
										for(int i=0 ; i<boxViews.size();i++){

											ImageView thisImageView = boxViews.get(i);
											/*if(Integer.parseInt((String) thisImageView.getContentDescription()) != _mainBoxIndex){
												continue;
											}*/

											RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(originalParams.get(i));

											params.width = (int) (params.width / widthPer);
											params.height = (int) (params.height / heightPer);
											params.leftMargin = (int) (params.leftMargin - xoffset*displayRatio);
											params.topMargin = (int) (params.topMargin - yoffset*displayRatio);
											//Hide none main views
											if (!_tldView.isTracked()) {
												thisImageView.setVisibility(View.INVISIBLE);
												Log.v(TAG, "set to invisible!!!!!");
												//params.leftMargin = 10000;
												//thisImageView.requestLayout();
												//continue;
											}
											else if (!_tldView.isMainBoxClose() && Integer.parseInt((String) thisImageView.getContentDescription()) != _mainBoxIndex ) {
												thisImageView.setVisibility(View.INVISIBLE);
												//params.leftMargin = 10000;

												//if(relativeL.add)
											} else {
												thisImageView.setVisibility(View.VISIBLE);
												 
											}
											thisImageView.setLayoutParams(params);
											//relativeL.updateViewLayout(thisImageView, params);	
										}
									}
									
								};
								
								mainHandler.post(myRunnable);


								
								
							}
						}
						
					});
				} break;
				default:
				{
					Log.e(Util.TAG, "OpenCV can NOT be loaded");
					super.onManagerConnected(status);
				} break;
			}
    	}
    };	

    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Log.i(Util.TAG, "Trying to load OpenCV library");
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, _openCVCallBack)) {
        	Log.e(Util.TAG, "Cannot connect to OpenCV Manager");
        }
        
        String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
        File imgDir = new File(extStorageDirectory, "tldimgs");
        if(!imgDir.isDirectory()) {
        	imgDir.mkdir();
        }
    }    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
	public String loadJSONFromAsset() {
        String json = null;
        try {

            InputStream is = getAssets().open("testjson.json");

            int size = is.available();

            byte[] buffer = new byte[size];

            is.read(buffer);

            is.close();

            json = new String(buffer, "UTF-8");


        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;

    }
}
