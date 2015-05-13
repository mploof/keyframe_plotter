package keyframeplotter;

import processing.core.PApplet;

public class Slider {

	/*************************************
	 *  							 	 *
	 *       Variables and Objects       *
	 * 									 *
	 * ***********************************/		

	// Processing object
	private PApplet p;	
	
	private boolean vertical, clicked, snap;
	private float 
		s_length, s_thickness, 								// Slide Drawing parameters
		s_x_max_px, s_x_min_px, s_x_mid_px,			 
		s_y_max_px, s_y_min_px, s_y_mid_px, 
		c_size, c_length, c_slide_percent, 					// Cursor Drawing parameters 
		c_x_min_px, c_x_max_px, c_x_mid_px, 
		c_y_min_px, c_y_max_px, c_y_mid_px,
		c_snap_percent,
		min_val, max_val;									// Value output vars
	private int
		s_r, s_g, s_b,	// Slider color vars  
		c_r, c_g, c_b;	// Cursor color vars
	
	
	/*************************************
	 *  					             *
	 *             Functions             *
	 * 						             *
	 * ********************************* */
	
	Slider(PApplet _p){
		p = _p;		
	}
	
	void init(float _length, float _thickness, boolean _vertical, float _x_pos, float _y_pos, float _min_val, float _max_val, boolean _snap){
							
		s_length = _length;
		s_thickness = _thickness;
		vertical = _vertical;
		s_x_mid_px = _x_pos;
		s_y_mid_px = _y_pos;
		max_val = _max_val;
		min_val = _min_val;
		snap = _snap;
		
		// Set default slider color
		s_r = 175;
		s_g = 175;
		s_b = 175;
		
		// Set default cursor color
		c_r = 80;
		c_g = 105;
		c_b = 175;
		
		// Set default slide position vars
		if(!vertical){
			s_x_max_px = s_x_mid_px + s_length / 2; 
			s_x_min_px = s_x_mid_px - s_length / 2;
			s_y_max_px = s_y_mid_px + s_thickness / 2;
			s_y_min_px = s_y_mid_px - s_thickness / 2;
		}
		else{
			s_x_max_px = s_x_mid_px + s_thickness / 2; 
			s_x_min_px = s_x_mid_px - s_thickness / 2;
			s_y_max_px = s_y_mid_px + s_length / 2;
			s_y_min_px = s_y_mid_px - s_length / 2;
		}
		
		// Set default cursor size and position	
		c_size = (float) 0.05;
		c_length = c_size * s_length;
		c_slide_percent = (float) 0.5;		
		c_snap_percent = (float) 0.5;
		clicked = false;
		
		// Draw the slider given the initial parameters
		draw();
	}
	
	public void update(){		
		if(cursorClicked()){		
			float temp_percent;
			if(vertical)
				temp_percent  = (p.mouseY - s_y_min_px) / (s_y_max_px - s_y_min_px);
			else
				temp_percent = (p.mouseX - s_x_min_px) / (s_x_max_px - s_x_min_px);
			
			if(temp_percent < 0)
				c_slide_percent = 0;
			else if(temp_percent > 1)
				c_slide_percent = 1;
			else
				c_slide_percent = temp_percent;
			
			//PApplet.print("Slide percent: ");
			//PApplet.println(c_slide_percent);
		}		
		else if(snap)
			c_slide_percent = c_snap_percent;		
		
		draw();
		
	}
	
	public boolean clicked(){
		return clicked;
	}
	
	private boolean cursorClicked(){				
		if(p.mousePressed){
			if(clicked || (p.mousePressed 
					&&p.mouseX > c_x_min_px && p.mouseX < c_x_max_px 
					&& p.mouseY > c_y_min_px && p.mouseY < c_y_max_px)){
					clicked = true;
					return true;
			}
			return false;
		}
		else{
			clicked = false;
			return false;
		}
	}
	
	// Returns current slider value
	public float val(){
		return min_val + (c_slide_percent * (max_val - min_val));
	}
	
	public void draw(){		
		
		// Draw slider
		p.stroke(0);
		p.strokeWeight(1);
		p.fill(s_r, s_g, s_b);
		
		float s_width;
		float s_height;
		float s_x_rect;
		float s_y_rect;
		if(vertical){
			s_width = s_thickness;
			s_height = s_length;			 
		}
		else{
			s_width = s_length;
			s_height = s_thickness;
		}
		s_x_rect = s_x_mid_px - s_width/2;
		s_y_rect = s_y_mid_px - s_height/2;
			
		p.rect(s_x_rect, s_y_rect, s_width, s_height);
		
		
		// Draw cursor		
		float c_width;
		float c_height;
		float c_x_rect;
		float c_y_rect;
		
		// For vertical bar
		if(vertical){
			c_width = s_thickness;
			c_height = c_length;
			c_x_rect = s_x_rect;
			c_x_mid_px = s_x_mid_px;
			c_y_mid_px = s_y_min_px + (c_length/2) + c_slide_percent * (s_length - c_length);
			c_y_rect = c_y_mid_px - c_length/ 2;
			
			// Update the boundaries of the cursor
			c_x_max_px = c_x_mid_px + c_length / 2; 
			c_x_min_px = c_x_mid_px - c_length / 2;
			c_y_max_px = c_y_mid_px + s_thickness / 2;
			c_y_min_px = c_y_mid_px - s_thickness / 2;
		}
		// For horizontal bar
		else{
			c_width = c_length;
			c_height = s_thickness;
			c_x_mid_px = s_x_min_px + (c_length/2) + c_slide_percent * (s_length - c_length);
			c_y_mid_px = s_y_mid_px;
			c_x_rect = c_x_mid_px - c_length / 2; 
			c_y_rect = s_y_rect;
			
			// Update the boundaries of the cursor
			c_x_max_px = c_x_mid_px + s_thickness / 2; 
			c_x_min_px = c_x_mid_px - s_thickness / 2;
			c_y_max_px = c_y_mid_px + c_length / 2;
			c_y_min_px = c_y_mid_px - c_length / 2;
		}
		p.fill(c_r, c_g, c_b);
		p.rect(c_x_rect, c_y_rect, c_width, c_height);		
	}
}