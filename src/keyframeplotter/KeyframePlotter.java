package keyframeplotter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;

import processing.core.PApplet;
import processing.serial.*;


public class KeyframePlotter extends PApplet {	
	
	ArrayList<Button> key_frames = new ArrayList<Button>();	// List of buttons that will represent key frames
	Serial port;        									// The serial port
	Grid grid = new Grid(this, 50, 50, 30, 105);			// Grid object
	Bounce bounce = new Bounce(this);						// Mouse debouncing object
	Indicator graph_loc = new Indicator(this);
	Indicator mouse_loc = new Indicator(this);
	Button send = new Button(this);
	Button title = new Button(this);
	Indicator message = new Indicator(this);
	
	
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
		
		// Open proper com port
		port = new Serial(this, Serial.list()[4], 9600);
		
		size(800, 650);
		background(100, 100, 100);		
		grid.init(55, -5, 5, 380, -20, 20);
		grid.draw();
		
		// Initialize state indicator
		title.init("Key Frame Editor", width/2, 25, 200, 30);
		title.draw();
		message.init("Message", "NULL", grid.x_center, height-25, grid.width, 30);		
		message.draw(" ");
		
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
		bounce.set();
	}
	
	void checkButtons(){
		if(mousePressed == true){
			if(mouseButton == LEFT && bounce.get() ==  false){
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
			bounce.set(false);
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
			for(int i = 0; i < key_frames.size(); i++){
				Button this_point = key_frames.get(i);
				if(this_point.clicked == true){				
					return i;
				}
			}
			
			// If not, see if one is clicked now
			for(int i = 0; i < key_frames.size(); i++){
				Button this_point = key_frames.get(i);
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
			for(int i = 0; i < key_frames.size(); i++){
				Button this_point = key_frames.get(i);
				this_point.clicked = false;
			}
			return -2;
		}			
	}
	
	void addKF(){
				
		// Only add key frames if they're being placed on the grid and don't exceed the max KF count
		if(grid.overGrid() && key_frames.size() < MAX_KF){
			key_frames.add(new Button(this));
			Button this_point = key_frames.get(key_frames.size() - 1);
			float posX = (int)grid.x() * grid.x_unit_px + grid.x_zero_px;			
			this_point.init("temp", posX, mouseY, 20);
			
			// Re-order the points by position
			Collections.sort(key_frames, new PointComparator());
			
			// Label and draw them
			for(int i = 0; i < key_frames.size(); i++){
				this_point = key_frames.get(i);
				this_point.label = Integer.toString(i);
			}
			this_point.draw();			
		}
		else if(key_frames.size() == MAX_KF){
			message.draw("Max key frames placed");
		}
	}
	
	void modifyKF(int p_kf){		
		
		// For a left click, cycle through the key frame points
		if(mouseButton == LEFT){
			
			Button this_point = key_frames.get(p_kf);	
			

			int x_last = -1;
			int x_next = (int)grid.x_max;
			int x_this = (int)grid.x();						
			if(p_kf > 0){							
				x_last = (int)grid.x(key_frames.get(p_kf - 1).posX);
			}
			if(p_kf < key_frames.size() - 1)
				x_next = (int)grid.x(key_frames.get(p_kf + 1).posX);
			
			if(x_this > x_last && x_this < x_next){
				// Set the X position where a key frame would fall, not at the exact mouse position
				this_point.posX = x_this * grid.x_unit_px + grid.x_zero_px;
			}
			if(grid.overGridY()){
				this_point.posY = mouseY;		
			}		
		}
		else if(mouseButton == RIGHT){
			key_frames.remove(p_kf);
		}
		
		// Redraw the grid and buttons
		grid.draw();
		updateMousePos();
		for(int i = 0; i < key_frames.size(); i++){
			Button this_point = key_frames.get(i);
			this_point.label = Integer.toString(i + 1);
			this_point.draw();					
		}	

	
		//bounce.set();
	}
	
	
	/** Send state functions **/
	
	void sendData(){
		
		// Make sure this function isn't called a bunch of times in a row
		if(bounce.get() == true)
			return;
		
		final int SEND = 1;
		final int XY_SIZE = 2;
		final int FLOAT_BYTES = 4;
		int response_check;
		
		// Send code to indicate incoming points
		port.write(SEND);
		
		// Quit sending if an error is received
		response_check = responseListener();
		if(response_check == -1)			
			return;
		else if(response_check == 0){
			println("Controller-side error");
			return;			
		}
		else
			println("Command accepted");
		
		
		// Send number of incoming points
		print("Point count: ");
		println(key_frames.size());
		port.write(key_frames.size());
		
		response_check = responseListener();
		if(response_check == -1)			
			return;
		else if(response_check == 0){
			println("Controller-side error");
			return;			
		}
		else
			println("Point count accepted");
		
		
		// Loop
		for(int i = 0; i < key_frames.size(); i++){
			for(int j = 0; j < XY_SIZE; j++){
				
				// Find position of current point
				float temp_val;
				if(j == 0)
					temp_val = grid.x(key_frames.get(i).posX);
				else
					temp_val = grid.y(key_frames.get(i).posY);
				//temp_val = (float)5616.55;
				print("Temp val: ");
				println(temp_val);
				
				// Convert to intBits
				int out_val = Float.floatToIntBits(temp_val);				
				print("out_val: ");
				println(out_val);
			
				// Break float into bytes and send			
				for(int k = 0; k < FLOAT_BYTES; k++){				
					byte out_byte= (byte) ((out_val >> (8 * k)) & 0xFF);
					port.write(out_byte);
					print("out_byte: ");
					println(out_byte);
				}
				
				// Wait for response and bail if we timeout
				response_check = responseListener();
				if(response_check == -1){					
					return;
				}
				else if(response_check == -5000){
					println("Error reading value");
					port.clear();
					return;
				}				
				else
					println("Value accepted");				
			}
		}
		
		// Wait for spline calculations to finish
		response_check = responseListener();
		if(response_check == -1)			
			return;
		else if(response_check == 0){
			println("Controller-side error");
			return;			
		}
		else
			println("Spline calculations completed");
	
		
		// Request spline information
		getPoints(5, 255, 0, 0);
		
	}
	
	void getPoints(int p_size, int p_r, int p_g, int p_b){
		  println("Getting points");
		  println("delay");
		  delay(500);
		  		 
		  println("Entering point retrieval loop");		  
		  int i = 0;
		  float cur_x = -1000;
		  float cur_y = -1000;
		  int point_count = 10;
		  
		  while(true){
		    delay(10);		    
		    if(port.available() > 0){
		      // get the ASCII string:
		       String inString = port.readStringUntil('\n');
		       print("inString: ");
		       println(inString);
		       
		       if (inString != null) {		       	  	   
		    	 
		         // trim off any whitespace:
		         inString = trim(inString);
		         float inFloat = -1;
		         try{
		         // convert to an int and map to the screen height:
		        	 inFloat = Float.parseFloat(inString);
		         }
		         catch(NumberFormatException e){
		        	 println("Oh teh noze!");
		         }
		        
		        if(inFloat > 5000){
		           println("reached stop code");		           
		           return;
		         }
		         
		        if(i%2 == 0)
		        	cur_x = grid.x_px(inFloat);
		        else
		        	cur_y = grid.y_px(inFloat);       
		       
		           
		         // If both of the coordinates have now been set, draw the point:
		         if(cur_x != -1000 && cur_y != -1000){
		           // draw the point:
		           strokeWeight(p_size);
		           stroke(p_r, p_g, p_b);
		           point(cur_x, cur_y);
		           // reset the point vars:
		           cur_x = -1000;
		           cur_y = -1000;		         
		         }
		         i++;
		         if(i == point_count * 2){
		        	 break;
		         }
		       }		       
		    }  
		  }
		}
	
	int responseListener(){
		long time = millis();
		final int TIMEOUT = 4000;
		// Wait for response
		while(true){
			delay(10);
			if(port.available() > 0){
				float response = readPort();
				// The response was a null string, keep trying
				if(response != -5000){
					print("Response:");
					println(response);
					return (int)response;
				}
			}
			if(millis() - time > TIMEOUT){
				println("Sending timed out");
				message.draw("Sending timed out, try again");
				return -1;
			}
		}
	}
	
	float readPort(){		
		String in_string = port.readStringUntil('\n');
		if(in_string != null){
			in_string = trim(in_string);
			return Float.parseFloat(in_string);
		}
		return -5000;
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
