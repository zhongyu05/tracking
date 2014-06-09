package com.trandi.opentld;

import java.util.Comparator;

public class RecordComparator implements Comparator<QueueRecord> {

	@Override
	public int compare(QueueRecord arg0, QueueRecord arg1) {

		if ((arg0.getOldIndex() < arg1.getOldIndex())) {
			return 1;
		
		} else if ((arg0.getOldIndex() == arg1.getOldIndex())) {
			if (arg0.getIndex() < arg1.getIndex()) {
				return 1;
			} else if (arg0.getIndex() == arg1.getIndex()) {
				return 0;
			} else {
				return -1;
			}
		} else {
			return -1;
		}	

		
/*
		if (arg0.getIndex() < arg1.getIndex()) {
			return -1;
			
		} else if (arg0.getIndex() > arg1.getIndex()) {
			return 1;
			
		} else {
			return 0;
			
		}
*/
/*
		if (arg0.getOldIndex() < arg1.getOldIndex()) {
			return -1;
			
		} else if (arg0.getOldIndex() > arg1.getOldIndex()) {
			return 1;
			
		} else {
			return 0;
			
		}
*/
/*
		if (arg0.getIndex() < arg1.getIndex()) {
			return -1;
			
		} else if (arg0.getIndex() > arg1.getIndex()) {
			return 1;
			
		} else {
			return 0;
			
		}
*/
	}
}