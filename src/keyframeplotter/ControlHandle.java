package keyframeplotter;

import processing.core.PApplet;
import processing.core.PConstants;

import java.lang.Math;
import java.util.ArrayList;

public class ControlHandle{
	// Processing object
	private PApplet p;
	private static Grid grid;
	private static ArrayList<ControlHandle> control_handles;
	private String label;
	private float center_radius, center_x, center_y, gridX, gridY, handle_radius, handle_x[], handle_y[], line_margin, angle;
	private boolean handle_clicked[], center_clicked;
	private int r_t, g_t, b_t, text_size, id;
	private final int HANDLE_COUNT = 2;
	
	/** Constructor **/
	
	ControlHandle(PApplet _p, Grid _grid, ArrayList<ControlHandle> _control_handles){
		p = _p;
		grid = _grid;
		control_handles = _control_handles;
		angle = 0;
		center_radius = 20;		
		handle_radius = 10;
		line_margin = 10;
		handle_x = new float[HANDLE_COUNT];
		handle_y = new float[HANDLE_COUNT];
		handle_clicked = new boolean[HANDLE_COUNT];
		for(int i = 0; i < HANDLE_COUNT; i++){
			handle_clicked[i] = false;	
		}
		center_clicked = false;
		
		label = "";
		text_size = 12;				
		r_t = 0;
		g_t = 0;
		b_t = 0;
	}		
	
	/********************** 
	 * 		 			  *
	 *  Public Functions  *
	 * 					  *
	 **********************/	
	
	public void setLocPx(float _x, float _y){
		center_x = _x;
		center_y = _y;
		gridX = grid.x(center_x);
		gridY = grid.y(center_y);
		
		updateHandles();
	}	
	
	public void setLocGrid(float _x, float _y){
		setLocPx(grid.x_px(_x), grid.y_px(_y));
	}
	
	public void setLabel(String _label){
		label = _label;
	}
	
	public boolean update(){
		
		// If the mouse isn't pressed, then don't do anything
		if(p.mousePressed == false){
			for(int i = 0; i < HANDLE_COUNT; i++){
				handle_clicked[i] = false;
				center_clicked = false;
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
		
		// Check if the center circle has already been clicked
		if(!center_clicked){		
			
			// If neither the center nor a handle was clicked, see if a handle is now being clicked
			if(which == -1){
				which = overHandle();
			}
				
			// If a handle is clicked, update its position
			if(which > -1){				
				if(p.mouseButton == PConstants.LEFT){
					// Update the position of the clicked handle
					double radians = Math.atan((p.mouseY - center_y) / (p.mouseX - center_x));
					angle = (float) Math.toDegrees(radians);
				}
				// If the handle was right-clicked, reset the angle to 0
				else{
					angle = 0;
				}			
				updateHandles();
				handle_clicked[which] = true;
				return true;
			}			
			// Otherwise check whether a center point is now being clicked
			else if(overCenter() && p.mouseButton == PConstants.LEFT){
				center_clicked = true;
			}
			// If the mouse is not over the center or it was a right click, return false
			else
				return false;				
		}
		
		// If the center has been clicked, update its position
		if(center_clicked){
			float x;
			float y;
			
			float min_x;
			float max_x;
			float min_y = grid.y_min_px;
			float max_y = grid.y_max_px;
			
			// The first point cannot be modified
			if(id == 0){
				return true;				
			}
			// The last point is bounded by the previous point and the edge of the grid
			else if(id == control_handles.size() - 1){
				min_x = control_handles.get(id - 1).getPxX();
				max_x = grid.x_max_px;
			}
			// All other points are bounded by the points on either side
			else{
				min_x = control_handles.get(id - 1).getPxX();
				max_x = control_handles.get(id + 1).getPxX();
			}
			
						
			if(p.mouseX >= min_x && p.mouseX <= max_x)
				x = p.mouseX;
			else
				x = center_x;
			if(p.mouseY>= min_y && p.mouseY <= max_y)
				y = p.mouseY;
			else
				y = center_y;
			
			setLocPx(x, y);
			return true;
		}	
		
		return false;
	}
	
	public void draw(){		
		
		// Draw the line connecting the handles
		p.stroke(0);
		p.strokeWeight(2);
		p.line(handle_x[0], handle_y[0], handle_x[1], handle_y[1]);
		
		// Draw the center circle
		p.fill(200);
		p.stroke(0);
		p.strokeWeight(1);
		p.ellipse(center_x, center_y, center_radius, center_radius);
		
		// Draw the two control handles
		p.fill(100, 20, 227);
		p.stroke(0);
		p.strokeWeight(1);		
		for(int i = 0; i < 2; i++){
			p.ellipse(handle_x[i], handle_y[i], handle_radius, handle_radius);
		}		
		
		// Draw the label
		p.fill(r_t, g_t, b_t);
		p.textSize(text_size);
		p.textAlign(PConstants.CENTER, PConstants.CENTER);        
		p.text(label, center_x, center_y);
	}

	public float getAngle(){
		return angle;
	}

	public float getGridSlope(){
		
		float slope;
		
		if(handle_x[1] > handle_x[0])			
			slope = (grid.y(handle_y[1]) - grid.y(handle_y[0])) / (grid.x(handle_x[1]) - grid.x(handle_x[0]));
		else
			slope = (grid.y(handle_y[0]) - grid.y(handle_y[1])) / (grid.x(handle_x[0]) - grid.x(handle_x[1]));

		return slope;
	}
	
	public float getGridX(){
		return gridX;
	}
	
	public float getGridY(){
		return gridY;
	}
	
	public float getPxX(){
		return center_x;
	}
	
	public float getPxY(){
		return center_y;
	}
	
	/** overCenter
	 * 
	 * @return Returns true if the mouse is over the center circle
	 * 
	 */
	public boolean overCenter(){
		float disX = center_x - p.mouseX;
		float disY = center_y - p.mouseY;
		
		if (PApplet.sqrt(PApplet.sq(disX) + PApplet.sq(disY)) < handle_radius ) {
		    return true;
		}
		else 
			return false;
	}
	
	public void setID(int _id){
		id = _id;
	}
	
	/********************* 
	 * 					 *
	 * Private Functions *
	 * 					 *
	 *********************/
		
	/** overCenter
	 * 
	 * @return Returns 0 or 1 if the mouse is over control handle 0 or 1, and -1 otherwise
	 * 
	 */
	private int overHandle(){
		
		for(int i = 0; i < HANDLE_COUNT; i++){
			float disX = handle_x[i] - p.mouseX;
			float disY = handle_y[i] - p.mouseY;
			
			if (PApplet.sqrt(PApplet.sq(disX) + PApplet.sq(disY)) < handle_radius ) {
			    return i;
			}
		}
		return -1;		
	}
	
	private void updateHandles(){
		
		// Find the new handle locations
		double radians = Math.toRadians(angle);
		float x_offset = (float) ((center_radius + line_margin) * Math.cos(radians));
		float y_offset = (float) ((center_radius + line_margin) * Math.sin(radians));

		handle_x[0] = center_x - x_offset;
		handle_y[0] = center_y - y_offset;
		handle_x[1] = center_x + x_offset;
		handle_y[1] = center_y + y_offset;				
	}
}
