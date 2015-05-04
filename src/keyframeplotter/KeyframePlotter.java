package keyframeplotter;

import java.util.ArrayList;
import java.util.Collections;

import processing.core.PApplet;
import processing.serial.*;


public class KeyframePlotter extends PApplet {	
	
	ArrayList<Button> key_frame = new ArrayList<Button>();	// List of buttons that will represent key frames
	Serial myPort;        									// The serial port
	Grid grid = new Grid(this, 50, 30, 30, 105);			// Grid object
	Bounce bounce = new Bounce(this);						// Mouse debouncing object
	Indicator graph_loc = new Indicator(this);
	Indicator mouse_loc = new Indicator(this);
	Button send = new Button(this);
	Button title = new Button(this);
	
	
	// Program states
	final int NONE = -1;
	final int GET_INPUT = 1;
	final int MODIFY_INPUT = 2;
	final int SEND_DATA = 3;
	final int CONFIRM = 4;
	final int CTRL = 5;
	final int INTERP = 6;
	int state = GET_INPUT;
	
	final int MAX_KF = 7;
	
	float x = -1000;   										// x coordinate points retrieved from MCU
	float y = -1000;										// y coordinate points retrieved from MCU
	
	int command = 0;
	
	
	public void setup() {
		size(800, 650);
		background(100, 100, 100);		
		grid.init(55, -5, 5, 380, -20, 20);
		grid.draw();
		
		// Initialize state indicator
		title.init("Key Frame Editor", width/2, 25, 200, 30);
		title.draw();
		
		// Initialize buttons and indicators in right tool bar
		float b_width = 75;
		float b_height = 50;
		float b_margin = 10;
		float b_offset = b_margin + b_height/2;
		graph_loc.init("Xg", "Yg", width - (75/2) - 15, (50/2) + grid.y_min_px, b_width, b_height);
		mouse_loc.init("Xm", "Ym", graph_loc.posX, graph_loc.y_max_px + b_offset, b_width, b_height);	
		send.init("Send", graph_loc.posX, mouse_loc.y_max_px + b_offset, b_width, b_height);
		send.draw();
		
	}

	public void draw() {
		
		// Always do these things		
		updateMousePos();
		checkButtons();		

		// Then execute the current state
		switch(state){
			case GET_INPUT:
				getInput();
				break;
			case SEND_DATA:
				sendData();
				state = GET_INPUT;
				break;
			case CONFIRM:
				break;
			case CTRL:
				break;
			case INTERP:
				break;
			default:
				break;
		}		
	}
	
	void checkButtons(){
		if(mousePressed == true){
			if(mouseButton == LEFT){
				if(send.overButton()){
					println("Send clicked");
					send.colorFill(20, 20, 20);
					send.colorText(200, 200, 200);
					send.draw();
					state = SEND_DATA;
				}
			}
		}	
		// Otherwise un-highlight all buttons
		else{
			send.colorFill(200, 200, 200);
			send.colorText(0, 0, 0);
			send.draw();
		}
			
	}
	
	/** Input state functions **/
	 
	void getInput(){

		// See if an existing key frame is being clicked
		int cur_kf = overKF();

		// If so, modify it
		if(cur_kf > -1){
			println("Modifying key frame");
			modifyKF(cur_kf);
		}
		// Otherwise, add a new point
		else if(cur_kf == -1 && bounce.get() == false){
			println("Adding key frame");
			addKF();
		}
	}
	
	int overKF(){
		
		// Mouse clicked
		if(mousePressed == true){
			// Check if any key frame point is already clicked
			for(int i = 0; i < key_frame.size(); i++){
				Button this_point = key_frame.get(i);
				if(this_point.clicked == true){				
					return i;
				}
			}
			
			// If not, see if one is clicked now
			for(int i = 0; i < key_frame.size(); i++){
				Button this_point = key_frame.get(i);
				if(this_point.overButton()){
					this_point.clicked = true;
					return i;
				}
			}		
			
			// If not, then somewhere other than an existing point is being clicked				
			return -1;
		}
		// Mouse not clicked
		else {
			for(int i = 0; i < key_frame.size(); i++){
				Button this_point = key_frame.get(i);
				this_point.clicked = false;
			}
			bounce.set(false);
			return -2;
		}			
	}
	
	void addKF(){
				
		// Only add key frames if they're being placed on the grid and don't exceed the max KF count
		if(grid.overGrid() && key_frame.size() < MAX_KF){
			key_frame.add(new Button(this));
			Button this_point = key_frame.get(key_frame.size() - 1);
			float posX = (int)grid.x() * grid.x_inc_px + grid.x_zero;			
			this_point.init("temp", posX, mouseY, 20);
			
			// Re-order the points by position
			Collections.sort(key_frame, new PointComparator());
			
			// Label and draw them
			for(int i = 0; i < key_frame.size(); i++){
				this_point = key_frame.get(i);
				this_point.label = Integer.toString(i);
			}
			this_point.draw();			
		}		
	}
	
	void modifyKF(int p_kf){		
		
		// For a left click, cycle through the key frame points
		if(mouseButton == LEFT){
			
			Button this_point = key_frame.get(p_kf);	
			

			int x_last = -1;
			int x_next = (int)grid.x_max;
			int x_this = (int)grid.x();						
			if(p_kf > 0){							
				x_last = (int)grid.x(key_frame.get(p_kf - 1).posX);
			}
			if(p_kf < key_frame.size() - 1)
				x_next = (int)grid.x(key_frame.get(p_kf + 1).posX);
			
			if(x_this > x_last && x_this < x_next){
				// Set the X position where a key frame would fall, not at the exact mouse position
				this_point.posX = x_this * grid.x_inc_px + grid.x_zero;
			}
			if(grid.overGridY()){
				this_point.posY = mouseY;		
			}		
		}
		else if(mouseButton == RIGHT){
			key_frame.remove(p_kf);
		}
		
		// Redraw the grid and buttons
		grid.draw();
		updateMousePos();
		for(int i = 0; i < key_frame.size(); i++){
			Button this_point = key_frame.get(i);
			this_point.label = Integer.toString(i + 1);
			this_point.draw();					
		}	

	
		bounce.set();
	}
	
	
	/** Send state functions **/
	
	void sendData(){
		
	}
	
	/** Helper functions **/
	
	void updateMousePos() {		
		graph_loc.draw(String.format("%.0f", grid.x()), String.format("%.1f", grid.y()));
		mouse_loc.draw(Integer.toString(mouseX), Integer.toString(mouseY));
	}
	
	public static void main(String _args[]) {
		PApplet.main(new String[] { keyframeplotter.KeyframePlotter.class.getName() });
	}
}
