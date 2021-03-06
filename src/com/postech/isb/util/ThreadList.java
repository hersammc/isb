package com.postech.isb.util;
import java.io.Serializable;

/**
 * Route information class. Serializable class to send over activity switching.
 *  
 * @author Jeongbong Seo.
 *
 */
public class ThreadList implements Serializable, Comparable {
	private static final long serialVersionUID = 1L;
	public int num;
	public boolean notdel;
	public boolean newt;
	public boolean comment;
	public String writer;
	public String date;
	public int cnt;
	public String title;
	public boolean highlight;
	
	public ThreadList () {
		writer = "Error";
		date = "Error";
		newt = false;
		comment = false;			
		title = "Error";
		highlight = false;
		notdel = false;
	}

	@Override
	public int compareTo(Object another) {
		// TODO Auto-generated method stub
		return (((ThreadList)another).num) - num;
	}
}
