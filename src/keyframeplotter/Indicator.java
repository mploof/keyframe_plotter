package keyframeplotter;

import processing.core.PApplet;
import processing.core.PConstants;

public class Indicator {

	
	/*************************************
	 *  							 	 *
	 *       Variables and Objects       *
	 * 									 *
	 * ***********************************/		

	// Processing object
	private PApplet p;							
	

	// Indicator properties
	float height, width, posX, posY, x_max_px, x_min_px, y_max_px, y_min_px, margin;							
	int		weight, r_b, g_b, b_b;	
	String label_0, label_1, value_0, value_1, output_0, output_1;	// Button label text
	int text_size, text_margin, r_t, g_t, b_t, alignment;			// Label parameters
	boolean clicked;
	
	
	/*************************************
	 *  					             *
	 *             Functions             *
	 * 						             *
	 * ********************************* */
		
	Indicator(PApplet _p){
		
		// Copy the Processing object
		p = _p;
		
		// Set default button parameters		
		posX = -100;
		posY = -100;
		height = 100;
		width = 200;
		weight = 1;
		r_b = 200;
		g_b = 200;
		b_b = 200;
		margin = 10;
		
		// Set default label parameters
		label_0 = "";
		label_1 = "";
		output_0 = "";
		output_1 = "";
		text_size = 14;
		r_t = 0;
		g_t = 0;
		b_t = 0;
		text_margin = 5;
		alignment = PConstants.CENTER;
		
	}
	
	void init(PApplet _p){
		p = _p;
	}
	
	// Initialize indicator
	void init(String _label_0, String _label_1, float _posX, float _posY, float _width, float _height){	
		height = _height;
		width = _width;			
		label_0 = _label_0.concat(": ");
		label_1 = _label_1.concat(": ");
		posX = _posX;
		posY = _posY;	
		
		x_min_px = posX - width/2;
		x_max_px = posX + width/2;
		y_min_px = posY - height/2;
		y_max_px = posY + height/2;
	}
	
	private void prepDraw(){
		
		// Set drawing parameters
		p.stroke(0);
        p.strokeWeight(weight);
		p.fill(r_b, g_b, b_b);
		
        // Determine corner location
        float rectX = posX - (width/ 2);
        float rectY = posY - (height / 2);
        
        // Draw button	        
        p.rect(rectX, rectY, width, height);
        
        // Set text parameters
        p.fill(r_t, g_t, b_t);
		p.textSize(text_size);
		p.textAlign(PConstants.LEFT, PConstants.CENTER);		
	}
	
	void draw(){
		if(output_1 == "")
			draw(output_0);
		else
			draw(output_0, output_1);
	}

	// Draw single line indicator
	void draw(String _value_0){
		output_0 = _value_0;
		prepDraw();
		p.text(label_0.concat(_value_0), posX - width/2 + text_margin, posY);        
	}

		
	// Draw two line indicator
	void draw(String _value_0, String _value_1){
		
		output_0 = _value_0;
		output_1 = _value_1;
		
		prepDraw();
		p.text(label_0.concat(_value_0), posX - width/2 + text_margin, posY - height/4);
		p.text(label_1.concat(_value_1), posX - width/2 + text_margin, posY + height/4);
	}
}
