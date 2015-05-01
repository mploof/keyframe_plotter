package keyframeplotter;

import processing.core.PApplet;

public class Bounce {
	
	/*************************************
	 *  							 	 *
	 *       Variables and Objects       *
	 * 									 *
	 * ***********************************/
	
	// Processing object
	private PApplet p;
	
	
	/*************************************
	 *  					             *
	 *             Functions             *
	 * 						             *
	 * ********************************* */	
	
	Bounce(PApplet _p){
		p = _p;
	}
	
	boolean mouse_bounce;
	
	public void set(){
		if(p.mousePressed == true)
			mouse_bounce = true;
		else
			mouse_bounce = false;
	}
	
	public void set(boolean p_bounce){
		mouse_bounce = p_bounce;
	}
	
	public boolean get(){
		return mouse_bounce;
	}
}
