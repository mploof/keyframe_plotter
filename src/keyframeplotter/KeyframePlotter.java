package keyframeplotter;

import processing.core.PApplet;
import processing.serial.*;


public class KeyframePlotter extends PApplet {
	
	Button title;
	int cur = 0;
	int point_count = 4;
	Button[] points = new Button[point_count];	
	Serial myPort;        	// The serial port
	Grid grid;
	
	final int NONE = -1;
	final int DRAW_INPUT_COUNT = 0;
	final int INPUT_COUNT = 1;
	final int INPUT = 2;
	final int CONFIRM = 3;
	final int CTRL = 4;
	final int INTERP = 5;
	
	
	float x = -1000;   		// x coordinate points retrieved from MCU
	float y = -1000;		// y coordinate points retrieved from MCU
	
	
	boolean do_this = true;
	boolean mouse_bounce = false;
	boolean load_vals = false;
	String val = "null";
	int command = 0;
	int state = NONE;
	
	public void setup() {
		size(800, 600);
		grid = new Grid(this);
		grid.init(55, -5, 5, 180, -180, 10);
		grid.draw();		
		
		for(int i = 0; i < points.length; i++){
			points[i] = new Button(this);
		}
	}

	public void draw() {
		updateMousePos();
		if(mouse_bounce == false && mousePressed == true){
			println("Mouse clicked");
			if(cur <= point_count - 1){				
				points[cur].init("Test", mouseX, mouseY, 20);
				points[cur].label = Integer.toString(cur);
				points[cur].draw();
				cur++;
			}
			mouse_bounce = true;
		}
		if(mousePressed == false){
		    mouse_bounce = false;
		}
	}
	
	public static void main(String _args[]) {
		PApplet.main(new String[] { keyframeplotter.KeyframePlotter.class.getName() });
	}
	
	void updateMousePos() {
		 
		  // Draw rectangle
		  stroke(0);
		  strokeWeight(2);
		  fill(220, 220, 220);
		  rect(width - 60, 0, 60, 50);
		  
		  // Determine graph location
		  float x = (mouseX - grid.x_zero) / (width / (grid.x_max - grid.x_min));
		  float y = -(mouseY - grid.y_zero) / (height / (grid.y_max - grid.y_min));
		  
		  // Update text
		  fill(0);
		  textAlign(CENTER, CENTER);
		  text(String.format("%.2f", x), width - 30, 15);
		  text(String.format("%.2f", y), width - 30, 35);
		}

	

}
