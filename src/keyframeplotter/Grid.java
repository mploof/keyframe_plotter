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
	float
	top_margin, bottom_margin, left_margin, right_margin, x_center, y_center,
	x_max_px, x_min_px, y_max_px, y_min_px, height, width,
	x_max, x_min, x_inc, x_range, x_inc_px, x_zero, vert_lines, 
	y_max, y_min, y_inc, y_range, y_inc_px, y_zero, horiz_lines;
	 
	
	/*************************************
	 *  					             *
	 *             Functions             *
	 * 						             *
	 * ********************************* */
	 
	Grid(PApplet _p, float _top_margin, float _bottom_margin, float _left_margin, float _right_margin){
		p = _p;
		top_margin = _top_margin;
		bottom_margin = _bottom_margin;
		left_margin = _left_margin;
		right_margin = _right_margin;
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
		
		// Graph frame variables
		x_max_px = p.width - right_margin;
		x_min_px = left_margin;
		y_max_px = p.height - bottom_margin;
		y_min_px = top_margin;
		x_center = (x_max_px + x_min_px) / 2;
		y_center = (y_max_px + y_min_px) / 2;
		height = y_max_px - y_min_px;
		width = x_max_px - x_min_px;
		
		x_max = _x_max;							// Max x value in graph units
		x_min = _x_min;							// Min x value in graph units
		x_range = x_max - x_min;
		
		y_max = _y_max;							// Max y value in graph units
		y_min = _y_min;							// Min y value in graph units
		y_range = y_max - y_min;				// Width in graph units
		
		x_inc = _x_inc;							// x increment size in graph units
		y_inc = _y_inc;							// y increment size in graph units
		
		x_inc_px = width / (x_max - x_min);		// Width in pixels of each x increment
		y_inc_px = height / (y_max - y_min);	// Height in pixels of each y increment
		
		vert_lines = (x_max - x_min) / x_inc;
		horiz_lines = (y_max - y_min) / y_inc;
		
		
		
		x_zero = (-x_min) / x_inc * (width / vert_lines) + x_min_px;
	    y_zero = (height - ((-y_min) / y_inc * (height / horiz_lines))) + y_min_px;
		
	    
	    
	}
	
	// Returns x graph value of mouse position
	float x(){
		return x(p.mouseX);		
	}
	
	// Returns x graph value of arbitrary input
	float x(float _input){
		
		if(_input > x_max_px)
			return x_max;
		else if(_input < x_min_px)
			return x_min;
		else
			return (_input - x_zero) / x_inc_px;
	}
	
	
	// Returns y graph value of mouse position
	float y(){
		return y(p.mouseY);
	}
	
	// Returns y graph value of arbitrary input
	float y(float _input){
		if(_input > y_max_px)
			return y_min;
		else if(_input < y_min_px)
			return y_max;
		else
			return -(_input - y_zero) / y_inc_px;
	}
	
	boolean overGrid(){
		
		if(overGridX() && overGridY())
			return true;
		else
			return false;
	}
	
	boolean overGridX(){
		
		if(p.mouseX >= x_zero && p.mouseX <= x_max_px)
			return true;
		else
			return false;		
	}
	
	boolean overGridY(){
		
		if(p.mouseY >= y_min_px && p.mouseY <= y_zero)
			return true;
		else
			return false;		
	}
	
	
	void draw() {
		
		// Clear the screen and set white background
		p.stroke(0);
		p.strokeWeight(3);
		p.fill(255, 255, 255);
		p.rect(left_margin, top_margin, x_max_px - x_min_px, y_max_px - y_min_px);
		
		// Draw grid lines
		p.stroke(200);
	    p.strokeWeight(1);

	    	// Horizontals
		for(byte i = 0; i < horiz_lines; i++){
			p.line(x_min_px, ((height / horiz_lines) * i) + y_min_px, x_max_px, ((height / horiz_lines) * i) + y_min_px);
		}
			// Verticals
		for(byte i = 0; i < vert_lines; i++){
		   p.line(((width / vert_lines) * i) + x_min_px, y_min_px, ((width / vert_lines) * i) + x_min_px, y_max_px); 
		}		
			
		// Draw zero lines		
	    p.stroke(0);
	    p.strokeWeight(2);
	    p.fill(0);


	   
	       	// Don't draw the lines if they're off the screen
	    if(x_zero >= 0)
	    	p.line(x_zero, y_min_px, x_zero, y_max_px);
	    if(y_zero >= 0)
	    	p.line(x_min_px, y_zero, x_max_px, y_zero);
	
		// Draw the numbers
		PFont f;
		f = p.createFont("Arial", 12, true);
		p.textFont(f, 12);
		p.fill(0);
		
		// X Scale
		p.textAlign(PConstants.LEFT, PConstants.TOP);
		for(byte i = 0; i < vert_lines; i++){
			// Don't print values less than 0 or values that are out of the graph boundary
			float location = ((width / vert_lines) * i + 5) + x_min_px;
			if((int)((i * x_inc) + x_min) >=0 && location > x_min_px && location < x_max_px )
				p.text((int)((i * x_inc) + x_min), location, y_zero);     
		}		
		// Y Scale
		p.textAlign(PConstants.RIGHT);
		for(byte i = 0; i < horiz_lines; i++){
			float location = ((height / horiz_lines) * i - 5) + y_min_px;
			if(location > y_min_px && location < y_max_px)
				p.text((int)(y_max - (i * y_inc)), x_zero - 5, location);			          
		}
	}
}
