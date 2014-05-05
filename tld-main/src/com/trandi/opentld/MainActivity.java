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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.R.integer;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.trandi.opentld.TLDView.ViewUpdateEventListener;
import com.trandi.opentld.tld.Util;

public class MainActivity extends Activity {
	private TLDView _tldView;
	private ArrayList<Rect> boxes = new ArrayList<Rect>();
	private ArrayList<ImageView> boxViews = new ArrayList<ImageView>();
	private ArrayList<String> boxLabels = new ArrayList<String>();
	private ArrayList<RelativeLayout.LayoutParams> originalParams = new ArrayList<RelativeLayout.LayoutParams>();
	private int _mainBoxIndex;
	private double displayRatio;
	
	private String sessionName;
	
    private AsyncHttpClient mHttpclient = new AsyncHttpClient();
    private ProgressDialog sendingDialog = null;
    
    private Button mCaptureButton;
    
    private String jsonFromHttp;
    
	private static final String TAG = "TLDActivity";
	
	
	private void uploadPicture() throws FileNotFoundException{
		RequestParams params = new RequestParams();
		String storageStatusString = Environment.getExternalStorageState();
        Log.d("MainActivity", storageStatusString);

        String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
        File imgDir = new File(extStorageDirectory, "tldimgs");
	    File mediaFile = new File(imgDir, sessionName+".jpg");
	    params.put("pictures[0]", mediaFile);

		//File imgFile = new File(passedImgPathString);

		mHttpclient.post(this, "http://roc.cs.rochester.edu/ccvideo/RegionSpeak/php/upload.php?session="+sessionName, params, new AsyncHttpResponseHandler() {
		    @Override
		    public void onSuccess(String response) {
			    sendingDialog.dismiss();
			    mCaptureButton.setText("Start tracking");
			    //mCaptureButton.setEnabled(false);
				_tldView.frameSelected = true;
			    Toast.makeText(getApplicationContext(), "Please wait for us to process the photo.", Toast.LENGTH_SHORT).show();
		    }
		    @Override
		    public void onFailure(int statusCode, org.apache.http.Header[] headers, byte[] responseBody, java.lang.Throwable error){
		    	 Log.v("Main", "fail"+statusCode+responseBody+","+error.toString());
		    	 Toast.makeText(getApplicationContext(), "Sending failed", Toast.LENGTH_SHORT).show();
		    	 sendingDialog.dismiss();
		    }
		});
	}
	
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
					

					//_tldView.connectCamera(640,480);
					
					Handler mainHandler = new Handler(Looper.getMainLooper());
					Runnable myRunnable = new Runnable(){

						@Override
						public void run() {
							// TODO Auto-generated method stub
							
							mCaptureButton = new Button(getApplicationContext());
							mCaptureButton.setText("Capture a photo");
							//captureButton.setBackgroundColor(getResources().getColor(android.R.color.black));
							
							RelativeLayout rl = (RelativeLayout) findViewById(R.id.rlay);
							RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(_tldView.getWidth(), _tldView.getHeight());
							params.leftMargin = 0;
							params.topMargin = 0;
							params.height = rl.getHeight();
							params.width = rl.getWidth();
							
							mCaptureButton.setOnClickListener(new View.OnClickListener() {
								@Override
								
								public void onClick(View v) {
									//_tldView.setResolution(640, 480);
									
									if (!_tldView.frameSelected && !_tldView.bbgot) {
										
										String storageStatusString = Environment.getExternalStorageState();
								        Log.d("MainActivity", storageStatusString);

								        String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
								        File imgDir = new File(extStorageDirectory, "tldimgs");
									    if (!imgDir.exists()){
									    	Boolean dirBool = imgDir.mkdirs();
									        if (dirBool)
									            Log.i(Util.TAG, "SUCCESS creating folder");
									           else
									            Log.i(Util.TAG, "Fail creating folder");
									    }
								        sessionName = new SimpleDateFormat("hh:mm:ssa-MMM-dd-yy", Locale.US).format(new Date());
									    File mediaFile = new File(imgDir, sessionName+".jpg");
									    Mat fileMat = new Mat();
										Imgproc.cvtColor(_tldView._frozeFrame, fileMat, Imgproc.COLOR_BGR2RGB);
									    Boolean bool = Highgui.imwrite(mediaFile.getAbsolutePath(), fileMat);
								        if (bool)
								            Log.i(Util.TAG, "SUCCESS writing image to external storage");
								           else
								            Log.i(Util.TAG, "Fail writing image to external storage");
								        sendingDialog = ProgressDialog.show(MainActivity.this, "Uploading, please wait",
											    null, true);
										sendingDialog.setOnDismissListener(new OnDismissListener() {
											
											@Override
											public void onDismiss(DialogInterface dialog) {
												// TODO Auto-generated method stub
												mHttpclient.cancelRequests(getApplicationContext(), true);
											}
										});
										
										
										sendingDialog.show();
										try {
											uploadPicture();
										} catch (FileNotFoundException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										
									} else if (_tldView.frameSelected && !_tldView.bbgot) {
										//get json return string
										
										String jsonURL = "http://roc.cs.rochester.edu/ccvideo/RegionSpeak/php/getMasksForPhone.php?&session="+sessionName;
										mHttpclient.get(MainActivity.this, jsonURL, new AsyncHttpResponseHandler() {			
										    @Override
										    public void onSuccess(String response) {
										    	jsonFromHttp = response;
												_tldView.jsonBoxReady = true;
												mCaptureButton.setVisibility(View.INVISIBLE);
										    	Log.e("JSONPHP", "Success to get JSON back");

										    }
										    
										    @Override
										    public void onFailure(int statusCode, org.apache.http.Header[] headers, byte[] responseBody, java.lang.Throwable error){
										    	Log.e("JSONPHP", "Failed to get JSON back");
										    }
										    
										    
										});
									}
								}
							});
							
							Log.v("MainActivity", "added button" + params.width);
							  
							rl.addView(mCaptureButton, params);
						}
						
						
					};
					mainHandler.post(myRunnable);
					
					_tldView.setEventListener(new ViewUpdateEventListener(){

						@Override
						public void onViewUpdated() {
							Log.v(TAG, "view updated!");
							if (!_tldView.jsonBoxReady || !_tldView.getViewReady()) {
								return;
							} else if (_tldView.getTrackedBox() == null) {
						        double bgwidth = _tldView.getFrameSize().width;
						        String jsonString = loadJSONFromHttp();
						        //String jsonString = loadJSONFromAsset();
								try {
									JSONObject obj = new JSONObject(jsonString);
									int mainIndex = 0;
									int maxSize = 0;
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
											boxLabels.add(thisbox.getString(4));
											int size = boxes.get(i).width() * boxes.get(i).height();
											if (size > maxSize) {
												mainIndex = i;
												maxSize = size;
											}
										}
										_mainBoxIndex = mainIndex;
										Rect mainBox = boxes.get(mainIndex);
										_tldView.setMainBox(mainBox);
										
										Handler mainHandler = new Handler(Looper.getMainLooper());
										Runnable myRunnable = new Runnable(){

											@Override
											public void run() {
												// TODO Auto-generated method stub
												//ImageView bg = 
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
														// iv.setBackgroundColor(Color.GREEN);
													//}

													params = new RelativeLayout.LayoutParams(thisRect.right-thisRect.left,thisRect.bottom-thisRect.top);
													params.leftMargin = (int) (thisRect.left*displayRatio+xoffset);
													params.topMargin = (int) (thisRect.top*displayRatio+yoffset);
													params.height = (int) ((double)params.height*displayRatio);
													params.width = (int) ((double)params.width*displayRatio);
													boxes.set(i, new Rect(params.leftMargin, params.topMargin, params.leftMargin+params.width, params.topMargin+params.height));
													  
													rl.addView(iv, params);
													originalParams.add(params);
													iv.setFocusable(true);
													iv.setAlpha(1);
													boxViews.add(iv);
													iv.setContentDescription(boxLabels.get(i));
													iv.setTag(Integer.valueOf(i));
													iv.setFocusable(true); 
													
													iv.setOnHoverListener(new View.OnHoverListener() {
														
														@Override
														public boolean onHover(View v, MotionEvent event) {
															// TODO Auto-generated method stub
															switch (event.getAction()) {
															case MotionEvent.ACTION_HOVER_EXIT:
																Boolean contained = false;

																for(int i=0;i<boxes.size();i++){
																	//String cd = v.getContentDescription().toString();
																	//Find out if it's the same
																	//int thisIndex = cd.charAt(cd.length()-1)-48;
																	int thisIndex = ((Integer) v.getTag()).intValue();
																	if (boxes.get(i).contains((int)event.getRawX(), (int)event.getRawY()) && i!=thisIndex) {
																		boxViews.get(i).requestFocus();
																		boxViews.get(i).sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
																		contained = true;
																		break;
																		}
																}
																if (!contained) {
																	TLDView bg = (TLDView)findViewById(R.id.tld_view);
																	bg.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
																}
																break;
  
															default:
																break;
															}
															return false;
														}
													});
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
											/*else if (!_tldView.isMainBoxClose() && ((Integer) thisImageView.getTag()).intValue() != _mainBoxIndex ) {
												//thisImageView.setVisibility(View.INVISIBLE);
												//params.leftMargin = 10000;

												//if(relativeL.add)
											}*/
											else {
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

    protected void onUserLeaveHint () {
    	
		mHttpclient.get(this, "http://roc.cs.rochester.edu/ccvideo/RegionSpeak/php/setInactive.php?session="+sessionName, new AsyncHttpResponseHandler() {
		    @Override
		    public void onSuccess(String response) {
		    	finish();
		    }
		    @Override
		    public void onFailure(int statusCode, org.apache.http.Header[] headers, byte[] responseBody, java.lang.Throwable error){
		    	 Log.v("Main", "fail"+statusCode+responseBody+","+error.toString());
		    	 Toast.makeText(getApplicationContext(), "Check internet connection.", Toast.LENGTH_SHORT).show();
		    	 //sendingDialog.dismiss();
		    }
		});
    }
    
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
    
    public String loadJSONFromHttp(){
    	return jsonFromHttp;
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
