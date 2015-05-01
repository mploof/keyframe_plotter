package keyframeplotter;

import java.util.ArrayList;

import processing.core.PApplet;
import processing.serial.*;


public class KeyframePlotter extends PApplet {	
	
	ArrayList<Button> key_point = new ArrayList<Button>();	// List of buttons that will represent key frames
	Serial myPort;        									// The serial port
	Grid grid = new Grid(this, 50, 30, 30, 105);			// Grid object
	Bounce bounce = new Bounce(this);						// Mouse debouncing object
	Indicator graph_loc = new Indicator(this);
	Indicator mouse_loc = new Indicator(this);
	Button input = new Button(this);
	Button modify = new Button(this);
	Button send = new Button(this);
	Button state_ind = new Button(this);
	
	
	// Program states
	final int NONE = -1;
	final int GET_INPUT = 1;
	final int MODIFY_INPUT = 2;
	final int SEND_DATA = 3;
	final int CONFIRM = 4;
	final int CTRL = 5;
	final int INTERP = 6;
	int state = GET_INPUT;
	
	final int MAX_KEYFRAMES = 7;
	
	float x = -1000;   										// x coordinate points retrieved from MCU
	float y = -1000;										// y coordinate points retrieved from MCU
	
	int command = 0;
	
	
	public void setup() {
		size(800, 650);
		background(255, 255, 255);		
		grid.init(55, -5, 5, 380, -20, 20);
		grid.draw();
		
		// Initialize state indicator
		state_ind.init("Input Key Points", width/2, 25, 200, 30);
		state_ind.draw();
		
		// Initialize buttons and indicators in right tool bar
		float b_width = 75;
		float b_height = 50;
		float b_margin = 10;
		float b_offset = b_margin + b_height/2;
		graph_loc.init("Xg", "Yg", width - (75/2) - 15, (50/2) + grid.y_min_px, b_width, b_height);
		mouse_loc.init("Xm", "Ym", graph_loc.posX, graph_loc.y_max_px + b_offset, b_width, b_height);
		input.init("Input", graph_loc.posX, mouse_loc.y_max_px + b_offset, b_width, b_height);
		input.draw();
		modify.init("Modify", input.posX, input.y_max_px + b_offset, b_width, b_height);
		modify.draw();
		send.init("Send", modify.posX, modify.y_max_px + b_offset, b_width, b_height);
		send.draw();
		
	}

	public void draw() {
		
		// Always do these things		
		updateMousePos();
		//checkButtons();

		// Then execute the current state
		switch(state){
			case GET_INPUT:
				getInput();
				break;
			case MODIFY_INPUT:
				modifyInput();
				break;
			case SEND_DATA:
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
		
	}
	
	void getInput(){
		
		// Set state indicator
		state_ind.label = "Input Key Points";
		state_ind.draw();
		
		if(bounce.get() == false && mousePressed == true){

			// Right click to finish adding points
			if(mouseButton == RIGHT || key_point.size() == MAX_KEYFRAMES){
				state++;
				print("Done adding key frames");
			}
			
			// Left Click to add point
			else if(mouseButton == LEFT){
				println("Mouse clicked");
				int cur_point = key_point.size() - 1;
				int x_last = -1;
				int x_max = (int)grid.x_max;
				int x_this = (int)grid.x();						
				if(cur_point > 0){							
					x_last = (int)grid.x(key_point.get(key_point.size()-1).posX);
				}
				
				if(x_this > x_last && x_this < x_max){
					key_point.add(new Button(this));
					Button this_point = key_point.get(key_point.size() - 1);
					float posX = (int)grid.x() * grid.x_inc_px + grid.x_zero;
					this_point.init("Test", posX, mouseY, 20);
					this_point.label = Integer.toString(key_point.size());
					this_point.draw();			
				}
			}
			bounce.set();
		}
		
		bounce.set();
	}
	
	void modifyInput(){
		
		// Set state indicator
		state_ind.label = "Modify Key Points";
		state_ind.draw();

		if(mousePressed == false){
			bounce.set();
			
			// Set the click state for all buttons false
			for(int i = 0; i < key_point.size(); i++){
				Button this_point = key_point.get(i);
				this_point.clicked = false;
			}
		}
		
		else if(mousePressed == true){

			// Right click to modifying points
			if(bounce.get() == false && mouseButton == RIGHT){
				state++;
				print("Done modifying key frames");
				bounce.set();
			}
			
			// Left click to add point
			else if(mouseButton == LEFT){
				
				boolean clicked = false;
				
				// Cycle through the key frame points				
				for(int i = 0; i < key_point.size(); i++){
					Button this_point = key_point.get(i);	
					
					// See if a button has already been clicked
					if(this_point.clicked){
						int x_last = -1;
						int x_next = (int)grid.x_max;
						int x_this = (int)grid.x();						
						if(i > 0){							
							x_last = (int)grid.x(key_point.get(i-1).posX);
						}
						if(i < key_point.size() - 1)
							x_next = (int)grid.x(key_point.get(i+1).posX);
						
						if(x_this > x_last && x_this < x_next){
							// Set the X position where a key frame would fall, not at the exact mouse position
							this_point.posX = x_this * grid.x_inc_px + grid.x_zero;
						}
						if(grid.y() >= 0 && grid.y() <= 360){
							this_point.posY = mouseY;		
						}
						clicked = true;
						break;
					}
				}
				
				// If no button was already clicked, see if one is clicked now
				if(!clicked){					
					for(int i = 0; i < key_point.size(); i++){
						Button this_point = key_point.get(i);										
						if(this_point.overButton()){
							println("Clicked a point");
							this_point.clicked = true;
						}
					}		
				}
				
				// Redraw the grid and buttons
				grid.draw();
				updateMousePos();
				for(int i = 0; i < key_point.size(); i++){
					Button this_point = key_point.get(i);
					this_point.draw();					
				}	
			} // done with left click action
		}
		bounce.set();
	}
	

	void updateMousePos() {		
		graph_loc.draw(String.format("%.0f", grid.x()), String.format("%.1f", grid.y()));
		mouse_loc.draw(Integer.toString(mouseX), Integer.toString(mouseY));
	}
	
	public static void main(String _args[]) {
		PApplet.main(new String[] { keyframeplotter.KeyframePlotter.class.getName() });
	}
}
