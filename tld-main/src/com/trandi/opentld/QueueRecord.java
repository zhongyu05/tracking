package com.trandi.opentld;

import org.opencv.core.Mat;

public class QueueRecord {
	private Mat working_frame, prev_frame;
	private long frame_index, old_index;
	
	public QueueRecord(Mat wframe, Mat pframe, long fi, long oi) {
 		working_frame = new Mat();
 		prev_frame = new Mat();
		wframe.copyTo(working_frame);
		pframe.copyTo(prev_frame);
 		//working_frame = wframe;
 		//prev_frame = pframe;
		frame_index = fi;
		old_index = oi;
	}
	
	public Mat getWorkingFrame() {
		return working_frame;
	}
	
	public Mat getPrevFrame() {
		return prev_frame;
	}
	
	public long getIndex() {
		return frame_index;
	}
	
	public long getOldIndex() {
		return old_index;
	}
}
