package keyframeplotter;

import processing.core.PApplet;
import java.lang.Math;

public class ControlHandle{
	// Processing object
	private PApplet p;
	private Grid grid;
	
	private float center_radius, center_x, center_y, handle_radius, handle_x[], handle_y[], angle;
	private boolean handle_clicked[];
	private final int HANDLE_COUNT = 2;
	
	/** Constructor **/
	
	ControlHandle(PApplet _p, Grid _grid){
		p = _p;
		grid = _grid;
		angle = 0;
		center_radius = 40;		
		handle_radius = 10;
		handle_x = new float[HANDLE_COUNT];
		handle_y = new float[HANDLE_COUNT];
		handle_clicked = new boolean[HANDLE_COUNT];
		for(int i = 0; i < HANDLE_COUNT; i++){
			handle_clicked[i] = false;	
		}
	}		
	
	/** Public Functions **/
	
	
	public void setLoc(float _x, float _y){
		center_x = _x;
		center_y = _y;
		
		// Find the new handle locations
		double radians = Math.toRadians(angle);
		float x_offset = (float) (center_radius/2 * Math.cos(radians));
		float y_offset = (float) (center_radius/2 * Math.sin(radians));

		handle_x[0] = center_x - x_offset;
		handle_y[0] = center_y - y_offset;
		handle_x[1] = center_x + x_offset;
		handle_y[1] = center_y + y_offset;
	}
	
	public boolean update(){
		
		// If the mouse isn't pressed, then don't do anything
		if(p.mousePressed == false){
			for(int i = 0; i < HANDLE_COUNT; i++){
				handle_clicked[i] = false;
			}			
			return false;		
		}

		int which = -1;
		
		// Check if a handle is already clicked
		for(int i = 0; i < HANDLE_COUNT; i++){
			if(handle_clicked[i]){
				which = i;
				break;
			}
		}
		
		// If one is not already clicked, check which one is
		if(which == -1){
			which = overHandle();
			
			if(which == -1)
				return false;
			else
				handle_clicked[which] = true;
		}		
		
		float x = p.mouseX;
		float y = p.mouseY;
		
		// Update the position of the clicked handle
		double radians = Math.atan((p.mouseY - center_y) / (p.mouseX - center_x));
		angle = (float) Math.toDegrees(radians);
		
		return true;
	}
	
	/** Private Functions **/
	
	public void draw(){
		
		// Draw the outline circle
		p.noFill();
		p.stroke(0);
		p.strokeWeight(2);
		p.ellipse(center_x, center_y, center_radius, center_radius);
		
		// Draw the line connecting the handles
		p.stroke(0);
		p.strokeWeight(2);
		p.line(handle_x[0], handle_y[0], handle_x[1], handle_y[1]);
		
		// Draw the two control handles
		p.fill(100, 20, 227);
		p.stroke(0);
		p.strokeWeight(1);		
		for(int i = 0; i < 2; i++){
			p.ellipse(handle_x[i], handle_y[i], handle_radius, handle_radius);
		}
		
		
	}
	
	public int overHandle(){
		
		for(int i = 0; i < HANDLE_COUNT; i++){
			float disX = handle_x[i] - p.mouseX;
			float disY = handle_y[i] - p.mouseY;
			
			if (PApplet.sqrt(PApplet.sq(disX) + PApplet.sq(disY)) < handle_radius ) {
			    return i;
			}
		}
		return -1;		
	}
	
	public float getAngle(){
		return angle;
	}
	
	public float getSlope(){
		
		float slope;
		
		if(handle_x[1] > handle_x[0])			
			slope = (grid.y(handle_y[1]) - grid.y(handle_y[0])) / (grid.x(handle_x[1]) - grid.x(handle_x[0]));
		else
			slope = (grid.y(handle_y[0]) - grid.y(handle_y[1])) / (grid.x(handle_x[0]) - grid.x(handle_x[1]));

		return slope;
	}
}
