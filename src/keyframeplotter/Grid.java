package keyframeplotter;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PFont;

public class Grid {
	
	/*************************************
	 *  							 	 *
	 *       Variables and Objects       *
	 * 									 *
	 * ***********************************/
	
	// Processing object
	private PApplet p;
	
	// Class variables
	float x_max, x_min, x_inc, y_max, y_min, y_inc, vert_lines, horiz_lines, x_zero, y_zero;
	
	/*************************************
	 *  					             *
	 *             Functions             *
	 * 						             *
	 * ********************************* */
	 
	Grid(PApplet _p){
		p = _p;
	}
	

	/** Initializer **
	 * 
	 * @param _x_max = maximum value of grid in x direction (int)
	 * @param _x_min = minimum value of grid in x direction (int)
	 * @param _y_max = maximum value of grid in y direction (int)
	 * @param _y_min = minimum value of grid in y direction (int)
	 * @param _inc   = grid line increment size (int)
	 *
	 */
	void init(int _x_max, int _x_min, int _x_inc, int _y_max, int _y_min, int _y_inc){
		x_max = _x_max;
		x_min = _x_min;
		y_max = _y_max;
		y_min = _y_min;
		x_inc = _x_inc;
		y_inc = _y_inc;
		
		vert_lines = (x_max - x_min) / x_inc;
		horiz_lines = (y_max - y_min) / y_inc;
	}
	
	void draw() {
		
		// Clear the screen and set white background
		p.clear();
		p.background(255, 255, 255);
		
		// Draw grid lines:
		p.stroke(200);
	    p.strokeWeight(1);
	    int x_line = 0;
	    int y_line = 0; 
	    	// Horizontals
		for(byte i = 0; i < horiz_lines; i++){
			p.line(0, (p.height / horiz_lines) * i, p.width, (p.height / horiz_lines) * i);
		}
			// Verticals
		for(byte i = 0; i < vert_lines; i++){
		   p.line((p.width / vert_lines) * i, 0, (p.width / vert_lines) * i, p.height); 
		}
		
			
		// Draw zero lines		
	    p.stroke(0);
	    p.strokeWeight(2);
	    	// Find location of lines
	    x_zero = (-x_min) / x_inc * (p.width / vert_lines);
	    y_zero = p.height - ((-y_min) / y_inc * (p.height / horiz_lines));
	    p.fill(0);
	   
	       	// Don't draw the lines if they're off the screen
	    if(x_zero >= 0)
	    	p.line(x_zero, 0, x_zero, p.height);
	    if(y_zero >= 0)
	    	p.line(0, y_zero, p.width, y_zero);
	
		// Draw the numbers
		PFont f;
		f = p.createFont("Arial", 12, true);
		p.textFont(f, 12);
		p.fill(0);
		
		// X Scale
		p.textAlign(PConstants.LEFT, PConstants.TOP);
		for(byte i = 0; i < vert_lines; i++){
			// Don't print values less than 0
			if((int)((i * x_inc) + x_min) >=0)
				p.text((int)((i * x_inc) + x_min), (p.width / vert_lines) * i + 5, y_zero);     
		}		
		// Y Scale
		p.textAlign(PConstants.RIGHT);
		for(byte i = 0; i < horiz_lines; i++){
		     p.text((int)(y_max - (i * y_inc)), x_zero - 5, (p.height / horiz_lines) * i - 5);			          
		}
	}
}
