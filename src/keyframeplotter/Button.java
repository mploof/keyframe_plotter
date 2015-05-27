package keyframeplotter;

import processing.core.PApplet;
import processing.core.PConstants;

public class Button {
	
	/*************************************
	 *  							 	 *
	 *       Variables and Objects       *
	 * 									 *
	 * ***********************************/		

	// Processing object
	private PApplet p;							
	
	// Rectangle button parameters
	float height, width;							
	
	// Circle button parameters
	float radius;									
	
	// Common parameters
	float 	posX, posY, suX, suY, x_max_px, x_min_px, y_max_px, y_min_px, margin;							
	int		weight, r_b, g_b, b_b;	
	String label;								// Button label text
	int text_size, r_t, g_t, b_t;				// Label parameters
	boolean clicked;
	
	
	/*************************************
	 *  					             *
	 *             Functions             *
	 * 						             *
	 * ********************************* */
		
	Button(PApplet _p){
		
		// Copy the Processing object
		p = _p;
		
		// Set default button parameters		
		posX = -100;						// X Screen position in px
		posY = -100;						// Y Screen position in px
		height = 100;						// Rectangular height in px
		width = 200;						// Rectangular width in px
		radius = 25;						// Circular radius in px
		weight = 1;							// Weight of outline
		r_b = 200;							// Background R value
		g_b = 200;							// Background G value
		b_b = 200;							// Background B value
		margin = 10;						// Margin from other items (must be implemented manually)
		
		// Set default label parameters
		label = "";
		text_size = 14;
		r_t = 0;
		g_t = 0;
		b_t = 0;
		
	}
	
	void init(PApplet _p){
		p = _p;
	}
	
	// Initialize rectangular button
	void init(String _text, float _posX, float _posY, float _width, float _height){	
		height = _height;
		width = _width;
		radius = 0;
		init(_text, _posX, _posY);		
	}
	
	// Initialize circular button
	void init(String _text, float _posX, float _posY, float _radius){		
		height = 0;
		width = 0;
		radius = _radius;
		init(_text, _posX, _posY);		
	}
	
	// Initialize the button with fewer settings
	private void init(String _text, float _posX, float _posY){		
		label = _text;
		posX = _posX;
		posY = _posY;		
		clicked = false;
		
		x_min_px = posX - width/2;
		x_max_px = posX + width/2;
		y_min_px = posY - height/2;
		y_max_px = posY + height/2;
	}
	
	void colorFill(int _r, int _g, int _b){
		r_b = _r;
		g_b = _g;
		b_b = _b;
	}
	
	void colorText(int _r, int _g, int _b){
		r_t = _r;
		g_t = _g;
		b_t = _b;
	}
	
	// Draw the button and label at a point other than the stored X and Y values
	void draw(){
		
		// Set drawing parameters
		p.stroke(0);
        p.strokeWeight(weight);
		p.fill(r_b, g_b, b_b);
		
		// Draw rectangular button
		if(radius == 0) {		
	        // Determine corner location
	        float rectX = posX - (width/ 2);
	        float rectY = posY - (height / 2);
	        
	        // Draw button	        
	        p.rect(rectX, rectY, width, height, height/8);
		}
		// Draw circular button
		else
			p.ellipse(posX, posY, radius, radius);
        
        // Draw label
        p.fill(r_t, g_t, b_t);
		p.textSize(text_size);
		p.textAlign(PConstants.CENTER, PConstants.CENTER);
        p.text(label, posX, posY);
        
	}
	

	// Determine whether the mouse is over the button
	boolean overButton(){	      
		
		if(radius == 0){
		      if(p.mouseX >= (posX - width/2) && p.mouseX <= (posX + width/2) &&
		    		  p.mouseY >= (posY - height/2) && p.mouseY <= (posY + height/2))
		        return true;
		      else
		        return false;  
		}
		else{
			float disX = posX - p.mouseX;
			float disY = posY - p.mouseY;
			if (PApplet.sqrt(PApplet.sq(disX) + PApplet.sq(disY)) < radius ) {
			    return true;
			  } else {
			    return false;
			  }
		}
	}
}
