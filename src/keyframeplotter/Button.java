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
	float 	posX, posY, x_max_px, x_min_px, y_max_px, y_min_px, margin;;							
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
		posX = -100;
		posY = -100;
		height = 100;
		width = 200;
		radius = 25;
		weight = 1;
		r_b = 200;
		g_b = 200;
		b_b = 200;
		margin = 10;
		
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
	void init(String p_text, float p_posX, float p_posY, float p_width, float p_height){	
		height = p_height;
		width = p_width;
		radius = 0;
		init(p_text, p_posX, p_posY);		
	}
	
	// Initialize circular button
	void init(String p_text, float p_posX, float p_posY, float p_radius){	
		PApplet.println("Initializing circle");
		height = 0;
		width = 0;
		radius = p_radius;
		init(p_text, p_posX, p_posY);		
	}
	
	// Initialize the button with fewer settings
	private void init(String p_text, float p_posX, float p_posY){		
		label = p_text;
		posX = p_posX;
		posY = p_posY;		
		clicked = false;
		
		x_min_px = posX - width/2;
		x_max_px = posX + width/2;
		y_min_px = posY - height/2;
		y_max_px = posY + height/2;
	}
	
	// Draw the button and label
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
	        p.rect(rectX, rectY, width, height);
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
