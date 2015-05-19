package keyframeplotter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;

import processing.core.PApplet;
import processing.serial.*;


public class KeyframePlotter extends PApplet {	
	
	ArrayList<Button> key_frames = new ArrayList<Button>();	// List of buttons that will represent key frames
	Serial port;        									// The serial port
	Bounce bounce = new Bounce(this);						// Mouse debouncing object
	int grid_top_margin = 50;
	int grid_bottom_margin = 300;
	int grid_left_margin = 30;
	int grid_right_margin = 105;	
	Grid grid = new Grid(this, grid_top_margin, grid_bottom_margin, 
			grid_left_margin, grid_right_margin);			// Grid object
	
	// Serial port vars
	final int PORT_NUM = 4;
	boolean port_open = true;
	boolean timed_out = false;
	
	// Right side tool bar
	Indicator graph_loc = new Indicator(this);
	Indicator mouse_loc = new Indicator(this);
	Button connect = new Button(this);
	Button mem_check = new Button(this);
	Button send = new Button(this);		
	Button get_kf = new Button(this);
	Button lock_x= new Button(this);
	Button lock_y = new Button(this);
	
	// Center object
	Button title = new Button(this);
	Indicator message = new Indicator(this);
	Slider slider = new Slider(this);
	
	// Lower tool bar
	Slider grid_scale_x = new Slider(this);
	Button clear_kf = new Button(this);
	Button go_home = new Button(this);
	Button start_program = new Button(this);
	Button stop_motors = new Button(this);
	
	// Left side tool bar
	Slider grid_scale_y = new Slider(this);
	
	// Graphics variables
	int bgnd_r = 114;
	int bgnd_g = 159;
	int bgnd_b = 192;
	int kf_point_radius = 20;
	
	// Grid variables
	int x_interval = 5;
	int x_min;
	int x_max;
	int y_interval = 1000;
	int y_min;
	int y_max;
	
	// NMX communication constants and vars
	final int BYTE_SIZE = 1;	// Size of a single byte
	final int INT_SIZE = 2;		// Size of an integer
	final int LONG_SIZE = 4;	// Size of a long integer
	final int FLOAT_SIZE = 4;	// Size of a floating point number	
	final int XY_SIZE = 2;		// Represents the two values necessary for an XY location
	String response = "";		// Holds response from NMX commands
	
	// Standard messages
	final String PORT_CLOSED = "Port not open! Must be open for this command.";
	final String TIMED_OUT = "Oh teh noze! Response from NMX timed out!";
	
	// Program states
	final int NONE = -1;
	final int GET_INPUT = 1;
	final int MODIFY_INPUT = 2;
	final int SEND_DATA = 3;
	final int DRAW_POINTS = 4;
	final int RUN = 5;
	final int CTRL = 6;
	final int INTERP = 7;
	int state = GET_INPUT;
	
	// Motor control
	final int MOTOR_COUNT = 3;
	float max_speed = 5000;
	boolean joystick_mode = false;	
	float current_pos = 0;
	boolean moving = false;
	boolean check_moving = false;
	
	// KF and Spline vars
	final int MAX_KF = 7;
	int spline_point_count = 1000;
	float[] spline_point;
	float[] vel_point;
	boolean spline_available = false;
	boolean vel_available = false;
	
	// Timing vars
	long check_time = millis();
	long program_start_time;
	
	// Program run vars
	long run_time = 0;
	float run_time_sec = 0;
	final float MILLIS_PER_SEC = 1000;
	int cur_vel_pnt = -1;
	
	
	public void setup() {
		
		// Open proper com port		
		port = new Serial(this, Serial.list()[PORT_NUM], 9600);
		
		size(800, 900);
		background(bgnd_r, bgnd_g, bgnd_b);
		
		setGridVals();
		grid.init(x_max, x_min, x_interval, y_max, y_min, y_interval);
		grid.draw();
		
		// Initialize buttons and indicators in right toolbar
		float b_width = 90;
		float b_height = 50;
		float b_margin = 10;
		float b_offset = b_margin + b_height/2;
		graph_loc.init("Xg", "Yg", width - (75/2) - 15, (50/2) + grid.y_min_px, b_width, b_height);
		mouse_loc.init("Xm", "Ym", graph_loc.posX, graph_loc.y_max_px + b_offset, b_width, b_height);				
		connect.init("Close Port", graph_loc.posX, mouse_loc.y_max_px + b_offset, b_width, b_height);
		mem_check.init("Mem Check", graph_loc.posX, connect.y_max_px + b_offset, b_width, b_height);
		send.init("Send", graph_loc.posX, mem_check.y_max_px + b_offset, b_width, b_height);			
		get_kf.init("Get KF", graph_loc.posX, send.y_max_px + b_offset, b_width, b_height);
		lock_x.init("Lock X", graph_loc.posX, get_kf.y_max_px + b_offset, b_width, b_height);
		lock_y.init("Lock Y", graph_loc.posX, lock_x.y_max_px + b_offset, b_width, b_height);
		lock_x.clicked = false;
		lock_y.clicked = false;		
		
		// Initialize left side toolbar
		int grid_scale_thickness = 15;		
		grid_scale_y.init(grid.height, grid_scale_thickness, true, grid_left_margin / 2, grid.y_mid_px, 100, 10000, false);
		grid_scale_y.setSlidePercent(0);
		grid_scale_y.draw();
		
		// Initialize mid-screen indicators
		float i_height = 30;
		float i_margin = 10;		
		
		title.init("Key Frame Editor", width/2, 25, 200, i_height);
		title.draw();

		grid_scale_x.init(grid.width, grid_scale_thickness, false, grid.x_mid_px, grid.y_max_px + i_margin + grid_scale_thickness/2, 5, 100, false);
		grid_scale_x.setSlidePercent(0);
		grid_scale_x.draw();
		
		message.init("Message", "NULL", grid.x_center, grid_scale_x.getYMax() + i_margin + i_height/2, grid.width, i_height);		
		message.draw(" ");

		slider.init(grid.width, i_height, false, grid.x_mid_px, message.y_max_px + i_margin + i_height/2, -1, 1, true);
		slider.draw();		
		
		// Initialize lower toolbar buttons
		float lt_width = 100;
		float lt_height = 50;
		float lt_margin = 10;
		float lt_offset = lt_margin + lt_width/2;
		float lt_y_pos = slider.getYMax() + lt_height/2 + lt_margin;
		clear_kf.init("Clear KFs", slider.getXMin() + lt_width / 2, lt_y_pos, lt_width, lt_height);			
		go_home.init("Snd Mot Home", clear_kf.x_max_px + lt_offset, lt_y_pos, lt_width, lt_height);
		start_program.init("Start Program", go_home.x_max_px + lt_offset, lt_y_pos, lt_width, lt_height);
		stop_motors.init("Stop Motors", start_program.x_max_px + lt_offset, lt_y_pos, lt_width, lt_height);
		
		// Enable the motors
		for(int i = 0; i < MOTOR_COUNT; i++){
			NMXCommand(i, 3, BYTE_SIZE, 1);
		}
		
	}

	public void draw() {		
		
		// Always do these things
		timeOutCheck();		
		updateMousePos();	
		updateGraphics();		
		updateMotorSpeed();
		checkButtons();		

		// Then execute the current state
		switch(state){
			case GET_INPUT:
				getInput();
				break;
			case SEND_DATA:
				sendData();				
				break;
			case DRAW_POINTS:
				getSplinePoints();
				getVelPoints();
				state = GET_INPUT;
				break;
			case RUN:
				runProgram();
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
	
	/*** Loop Functions***/
	void timeOutCheck(){
		if(timed_out){
			port.clear();
			port.stop();
			port_open = false;
			message.draw("Command timed out. Unplug and replug NMX, then reopen com port");
			connect.label = "Open Port";			
			connect.draw();
		}
	}
	void updateMotorSpeed(){
		
		// Don't update the speed if a program is running
		if(state == RUN)
			return;
		
		final int CHECK_TIME = 100;
		
		// Only update if we've exceeded the check time
		if(millis() - check_time < CHECK_TIME){
			return;
		}
		
		
		// Slider is being used
		if(slider.clicked()){
			
			// Make sure we're in joystick mode
			if(!joystick_mode){				
				println("Enabling joystick mode");
				NMXCommand(0, 23, BYTE_SIZE, 1);
				if(timed_out){
					message.draw(TIMED_OUT);
					return;
				}
				joystick_mode = true;
			}
			float speed = max_speed * slider.getVal();
			print("Speed: ");
			println(speed);
			println(Float.floatToIntBits(speed));
			// Don't listen for a response from speed command in joystick mode
			NMXCommand(1, 13, FLOAT_SIZE, Float.floatToIntBits(speed), false);
			moving = true;
			check_moving = false;
			
		}
		
		// Slider is not being used
		else{			
			// Make sure motors are stopped and joystick mode is disabled
			if(joystick_mode){
				println("Stopping motors");
				NMXCommand(1, 13, FLOAT_SIZE, Float.floatToIntBits(0), false);
				if(timed_out){
					message.draw("Failed to stop motors");
					return;
				}
				println("Disabling joystick mode");
				NMXCommand(0, 23, BYTE_SIZE, 0);
				if(timed_out){
					message.draw("Failed to exit joystick mode");
					return;
				}				
				check_moving = true;
				joystick_mode = false;		
			}
		}		
		check_time = millis();
		
	}
	void updatePosition(){		
		if(moving){
			// Get the current motor position
			NMXCommand(1, 106);	
			current_pos = parseResponse();
			print("Current grid position: ");
			println(current_pos);
			print("Current px position: ");
			println(grid.y_px(current_pos));
			if(check_moving){
				// Check whether the motors have stopped moving
				NMXCommand(1, 107);
				if(parseResponse() == 0){
					check_moving = false;
					moving  = false;
					return;				
				}
			}
		}		
	}
	
	
	/*** Button Actions **/
	void connectAction(){
		if(port_open){
			if(state == RUN){
				message.draw("Can't close port while program is running");
				return;
			}
			println("Send clicked");
			connect.colorFill(20, 20, 20);
			connect.colorText(200, 200, 200);
			connect.draw();
			port.clear();
			port.stop();
			port_open = false;
			connect.label = "Open Port";
			connect.draw();
		}
		else{
			connect.colorFill(200, 200, 200);
			connect.colorText(0, 0, 0);
			connect.draw();
			port = new Serial(this, Serial.list()[PORT_NUM], 9600);
			port_open = true;
			timed_out = false;
			connect.label = "Close Port";
			connect.draw();
		}
	}
	
	void memCheckAction(){			
		if(port_open){		
			println("Checking memory");
			mem_check.colorFill(20, 20, 20);
			mem_check.colorText(200, 200, 200);
			mem_check.draw();
			NMXCommand(0, 200);
			float mem_val = parseResponse();
			message.draw("Memory available = " + Float.toString(mem_val) + " bytes");
		}
		else
		message.draw(PORT_CLOSED);		
	}
	
	void sendAction(){
		if(port_open){
			if(key_frames.size() < 2){
				message.draw("Must have at least two keyframes");
				return;
			}
			if(key_frames.size() == 3){
				message.draw("Doesn't work with three key frames. Not sure why...");
				return;
			}
			println("Send clicked");
			message.draw("Retrieving movement curve, please wait");
			send.colorFill(20, 20, 20);
			send.colorText(200, 200, 200);			
			state = SEND_DATA;
		}
		else
			message.draw(PORT_CLOSED);
	}
	
	void getKFAction(){
		if(port_open){
			addKF(false);
			println("Done adding adding KF");			
		}
		else
			message.draw(PORT_CLOSED);
	}
	
	void lockXAction(){		
		if(!lock_x.clicked){
			lock_x.colorFill(20, 20, 20);
			lock_x.colorText(200, 200, 200);
			lock_x.clicked = true;
		}
		else{
			lock_x.colorFill(200, 200, 200);
			lock_x.colorText(0, 0, 0);
			lock_x.clicked = false;
		}
	}
	
	void lockYAction(){
		if(!lock_y.clicked){
			lock_y.colorFill(20, 20, 20);
			lock_y.colorText(200, 200, 200);
			lock_y.clicked = true;
		}
		else{
			lock_y.colorFill(200, 200, 200);
			lock_y.colorText(0, 0, 0);
			lock_y.clicked = false;
		}
	}
	
	void clearKFAction(){
		clear_kf.colorFill(20, 20, 20);
		clear_kf.colorText(200, 200, 200);
		clear_kf.draw();
		key_frames.clear();
		spline_available = false;
		vel_available = false;
		message.draw("Key frames cleared");
	}	
	void goHomeAction(){
		NMXCommand(0, 25);
		moving = true;
		check_moving = true;		
	}
	
	void startProgramAction(){
		if(!port_open){
			message.draw("Can't start program while COM port is closed");
			return;
		}
		else if(!spline_available || !vel_available){
			message.draw("Must calculate movement curves before starting program");
			return;
		}
		println("********** Starting program **********");
		state = RUN;
		cur_vel_pnt = -1;		
		program_start_time = millis();
	}
	void stopMotorsAction(){
		for(int i = 0; i < MOTOR_COUNT; i++)
			NMXCommand(i, 4);
		
		// Go back to input state (in case a program is running);
		state = GET_INPUT;
	}
	
	void checkButtons(){
		if(mousePressed == true){
			if(mouseButton == LEFT && bounce.get() ==  false){
				if(send.overButton())
					sendAction();
				else if(connect.overButton())
					connectAction();
				else if(mem_check.overButton())
					memCheckAction();
				else if (get_kf.overButton())
					getKFAction();
				else if(lock_x.overButton())
					lockXAction();
				else if(lock_y.overButton())
					lockYAction();
				else if(clear_kf.overButton())
					clearKFAction();
				else if(go_home.overButton())
					goHomeAction();
				else if(start_program.overButton())
					startProgramAction();
				else if(stop_motors.overButton())
					stopMotorsAction();
			}
		}	
		// Otherwise un-highlight all buttons
		else{
			send.colorFill(200, 200, 200);
			send.colorText(0, 0, 0);
			send.draw();
			mem_check.colorFill(200, 200, 200);
			mem_check.colorText(0, 0, 0);
			mem_check.draw();
			clear_kf.colorFill(200, 200, 200);
			clear_kf.colorText(0, 0, 0);
			clear_kf.draw();
			bounce.set(false);
		}
		
	//	print("X locked: ");
		//println(lock_x.clicked);
			
	}
	
	
	/*** Graphics Management ***/
	
	void updateGraphics(){
		
		// Draw background
		background(bgnd_r, bgnd_g, bgnd_b);
		
		// Draw right toolbar
		mem_check.draw();
		connect.draw();
		send.draw();
		get_kf.draw();
		lock_x.draw();
		lock_y.draw();
				
		// Draw center items
		title.draw();
		message.draw();
		slider.update();
		
		// Draw lower toolbar
		clear_kf.draw();
		go_home.draw();
		start_program.draw();
		stop_motors.draw();
		
		// Draw right toolbar
		grid_scale_x.update();
		grid_scale_y.update();
		
		// Draw grid		
		setGridVals();
		grid.init(x_max, x_min, x_interval, y_max, y_min, y_interval);
		grid.draw();
		updateMousePos();
		
		// Draw key frame points
		for(int i = 0; i < key_frames.size(); i++){			
			Button this_point = key_frames.get(i);			
			this_point.label = Integer.toString(i + 1);
			this_point.posX = grid.x_px(this_point.suX);
			this_point.posY = grid.y_px(this_point.suY);
			this_point.draw();					
		}	
		
		// Draw motor position indicator
		updatePosition();
		
		// Draw position and velocity splines if data is available
		if(spline_available){
		    // If both of the coordinates have now been set, draw the point:
		    for(int i = 0; i < spline_point_count * XY_SIZE; i += 2){
		        // draw the point:
		  	  strokeWeight(2);
		  	  stroke(255, 0, 0);
		  	  if(i == spline_point_count * XY_SIZE - 2)
		  		point(key_frames.get(key_frames.size() - 1).posX, key_frames.get(key_frames.size() - 1).posY);
		  	  else
		  		point(grid.x_px(spline_point[i]), grid.y_px(spline_point[i + 1]));
		    }
		}
		if(vel_available){
			// If both of the coordinates have now been set, draw the point:
		    for(int i = 0; i < spline_point_count * XY_SIZE; i += 2){
		    	// draw the point:
		  	  	strokeWeight(2);
		  	  	stroke(0, 0, 255);
		  	  	point(grid.x_px(vel_point[i]), grid.y_px(vel_point[i + 1]));
		    }			
		}
		
		// If a program is running, draw a progress line
		if(state == RUN){
			stroke(98, 37, 178);
			strokeWeight(3);			
			line(grid.x_px(run_time_sec), grid.y_min_px, grid.x_px(run_time_sec), grid.y_max_px);
		}
		
		// Draw the position indicator
		stroke(0, 255, 0);
		strokeWeight(10);
		if(state == RUN)
			point(grid.x_px(run_time_sec), grid.y_px(current_pos));
		else
			point(grid.x_zero_px, grid.y_px(current_pos));

	}
	void setGridVals(){
		x_interval = (int) (grid_scale_x.getVal() - grid_scale_x.getVal() % 5);
		if(y_interval == 0)
			y_interval = 5;
		x_max = 11 * x_interval;
		x_min = -x_interval;
		
		y_interval = (int) (grid_scale_y.getVal() - grid_scale_y.getVal() % 1000);
		if(y_interval == 0)
			y_interval = 500;
		y_min = -10 * y_interval;
		y_max = 10 * y_interval;
	}
		/** Input state functions **/
	 

	/*** Key Frame Point Functions***/
	void getInput(){

		// See if an existing key frame is being clicked
		int cur_kf = overKF();

		// If so, modify it
		if(cur_kf > -1){
			modifyKF(cur_kf);
		}
		// Otherwise, add a new point
		else if(grid.overGrid() && cur_kf == -1 && bounce.get() == false){
			//println("Adding key frame");
			addKF(true);
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
	
	void addKF(boolean _mouse_input){
		
		// Don't allow more than the maximum key frames to be added
		if(key_frames.size() == MAX_KF){
			message.draw("Max key frames placed. Please modify or delete existing frames.");
			return;
		}
		
		// Add a new button object to the key frame object array and then create a working reference
		key_frames.add(new Button(this));
		Button this_point = key_frames.get(key_frames.size() - 1);
		
		float posX;
		float posY;
		
		// Adding a key frame by clicking on the grid screen
		if(_mouse_input == true){						
			
			posX = (int)grid.x();
			posY = grid.y();			
		}
		
		// Adding a key frame by querying the motor's current position
		else{
			print("Key frames: ");
			println(key_frames.size());
			// If this is the first key frame, reset the motor's home position
			if(key_frames.size() == 1){
				for(int i = 1; i < MOTOR_COUNT+1; i++){
					NMXCommand(i, 9);
					NMXCommand(i, 16);
				}
				current_pos = 0;
				posX = 0;
				posY = 0;
				print("posX: ");
				println(posX);
				print("posY: ");
				println(posY);
				
			}
			// Otherwise check the motor's current position
			else{
				NMXCommand(1, 106);
				float last_x = key_frames.get(key_frames.size() - 2).suX;
				posX = (grid.x_max - last_x) / 4 + last_x;
				posY = parseResponse();
				print("posX: ");
				println(posX);
				print("posY: ");
				println(posY);
			}			
		}
		this_point.suX = posX;
		this_point.suY = posY;
		this_point.init("temp", grid.x_px(posX), grid.y_px(posY), kf_point_radius);
		
		// Re-order the points by position
		Collections.sort(key_frames, new PointComparator());
		
		// Label and draw them
		for(int i = 0; i < key_frames.size(); i++){
			this_point = key_frames.get(i);
			this_point.label = Integer.toString(i);
		}
		this_point.draw();
	}
	
	void modifyKF(int p_kf){		
		
		// For a left click, cycle through the key frame points
		if(mouseButton == LEFT){
			
			Button this_point = key_frames.get(p_kf);	
			

			int x_last = -1;					// X Location of previous point
			int x_next = (int)grid.x_max;		// X Location of next point
			int x_this = (int)grid.x();			// X Location of this point
			
			// Set X position
			// If this is not the first point
			if(p_kf > 0)
				x_last = (int) key_frames.get(p_kf - 1).suX;			
			// If this is not the last point
			if(p_kf < key_frames.size() - 1)
				x_next = (int) key_frames.get(p_kf + 1).suX;			
			// Move the X position if it's not crossing any other points and X axis isn't locked
			if(x_this > x_last && x_this < x_next && !lock_x.clicked){
				// Set the X position where a key frame would fall, not at the exact mouse position
				this_point.suX = x_this; // x_this * grid.x_unit_px + grid.x_zero_px;
			}
			
			// Set Y position
			if(grid.overGridY() && !lock_y.clicked){
				this_point.suY = grid.y();		
			}		
		}
		else if(mouseButton == RIGHT){
			key_frames.remove(p_kf);
			spline_available = false;
			vel_available = false;
		}		
	}
	
	/*** Send state functions ***/
	
	void sendData(){		
		
		if(!port_open){
			println("The port is not open, fool!");
			message.draw("Com port not opened!");
			state = GET_INPUT;
			return;
		}		
		
		// Send key frame count / indicate start of transmission
		println("Sending key frame count");
		NMXCommand(5, 10, INT_SIZE, key_frames.size());
		if(timed_out)
			return;
		
		
		// Verify the frame count
		println("Verifying key frame count");
		NMXCommand(5, 100);
		if(timed_out)
			return;
		
		// **** Send key frame points*** //		
		
		// Loop through each key frame point
		println("Sending key frame point locations");
		for(int i = 0; i < key_frames.size(); i++){
			for(int j = 0; j < XY_SIZE; j++){
				
				// Find position of current point
				float temp_val;
				if(j == 0)
					temp_val = grid.x(key_frames.get(i).posX);
				else
					temp_val = grid.y(key_frames.get(i).posY);
				
				print("Send value: ");
				println(temp_val);
				
				// Convert to intBits
				int out_val = Float.floatToIntBits(temp_val);				
				
				// Send data packet
				NMXCommand(5, 11, FLOAT_SIZE, out_val);			
				if(timed_out)
					return;
			}
		}
		
		// End the transmission
		println("Ending key frame point transmission");
		NMXCommand(5, 10, INT_SIZE, 0);
		if(timed_out)
			return;
		
		// Check point count again
		println("Double checking point count");
		NMXCommand(5, 100);
		if(timed_out)
			return;
		
		state = DRAW_POINTS;				
	}
	
		

	/*** Draw state functions ***/
	
	void getSplinePoints(){		
	
		spline_point = new float[spline_point_count * XY_SIZE];
		
		// Set number of spline points to be retrieved
		println("Setting curve point count");
		NMXCommand(5, 12, INT_SIZE, spline_point_count);
		if(timed_out)
			return;
		
	  	// Check number of spline points to be retrieved
	    println("Getting curve point count");
	    NMXCommand(5, 101);
	    if(timed_out)
			return;
		int spline_point_count = (int)parseResponse();	    
	    
		// Signal start of point retrieval
		println("Signaling start of point retrieval");
		NMXCommand(5, 102);
		println("done");
		if(timed_out)
			return;
		println("blah");
		
       for(int i = 0; i < spline_point_count * XY_SIZE ; i++){
	      println("Retrieval loop");
    	  NMXCommand(5, 103, INT_SIZE, i);
    	  if(timed_out)
				return;
    	  float in_val = parseResponse() / 100;		// This will be a float, so need to divide by 100 on master device side
    	  print("in_val: ");
    	  println(in_val);
         
          if(i % 2 == 0)
        	  spline_point[i] = in_val;
          else
        	  spline_point[i] = in_val;
           
          }	  
       spline_available = true;
	}
	
	void getVelPoints(){		

		vel_point = new float[spline_point_count * XY_SIZE];
		
       for(int i = 0; i < spline_point_count; i++){
	        	    	
    	  NMXCommand(5, 104, INT_SIZE, i);
    	  if(timed_out)
				return;
    	  float in_val = parseResponse() / 100;		// This will be a float, so need to divide by 100 on master device side
    	  print("in_val: ");
    	  println(in_val);
         
          
          vel_point[i*2] = spline_point[i * 2];          
          vel_point[i*2 + 1] = in_val;
           
       }	  
       vel_available = true;
       message.draw("Movement curve retrieval complete");
	}
	
	/*** Run state functions***/
	void runProgram() {
		
		// Update the run time vars
		run_time = millis() - program_start_time;		
		run_time_sec = run_time / MILLIS_PER_SEC;
		
		float speed = 0;
		print("Current vel point: ");
		println(cur_vel_pnt);
		// Before movement starts
		if(cur_vel_pnt == -1){
			
			// Turn on joystick mode							
			println("Enabling joystick mode");
			NMXCommand(0, 23, BYTE_SIZE, 1);
			if(timed_out){
				message.draw(TIMED_OUT);
				return;
			}
			// Set movement vars
			joystick_mode = true;
			moving = true;
			check_moving = false;			
		}
		
		// If we haven't started moving or we've moved past the next update point
		if(run_time_sec > vel_point[(cur_vel_pnt + 1) * 2] || cur_vel_pnt == -1){
			// If this is the last point
			if(cur_vel_pnt == spline_point_count-1)
				speed = 0;
			else{				
				cur_vel_pnt++;
				speed = vel_point[(cur_vel_pnt * 2) + 1];
			}
			// Send the NMX the updated speed
			print("Speed: ");
			println(speed);
			println(Float.floatToIntBits(speed));			
			NMXCommand(1, 13, FLOAT_SIZE, Float.floatToIntBits(speed), false);
		}				
		
		// If this is the end of the program
		if(cur_vel_pnt == spline_point_count-1){
			moving = false;
			println("Disabling joystick mode");
			NMXCommand(0, 23, BYTE_SIZE, 0);
			if(timed_out){
				message.draw(TIMED_OUT);
				return;
			}
			// Turn off joystick mode
			state = GET_INPUT;
		}
	}
	
	/*** Communication functions ***/
	
	void NMXCommand(int _sub_addr, int _command){
		NMXCommand(_sub_addr, _command, 0, 0);	
	}
	void NMXCommand(int _sub_addr, int _command, int _length, int _data){
		NMXCommand(_sub_addr, _command, _length, _data, true);
	}
	// If _response is false, we won't wait for a response from the controller
	void NMXCommand(int _sub_addr, int _command, int _length, int _data, boolean _response){
		
		// Assemble command packet
		String header = "0000000000FF";
		String address = "03";		
		String sub_addr = _sub_addr <= 15 ? "0" + Integer.toHexString(_sub_addr) : Integer.toHexString(_sub_addr);
		String command = _command <= 15 ? "0" + Integer.toHexString(_command) : Integer.toHexString(_command);
		String length = _length <= 15 ? "0" + Integer.toHexString(_length) : Integer.toHexString(_length);
		String data = Integer.toHexString(_data).length()%2 != 0 ? "0" + Integer.toHexString(_data) : Integer.toHexString(_data);
		String packet = header + address + sub_addr + command + length;			
		
		// If the length is non-zero, then append the data
		if(_length != 0){
			// Make sure the data has any necessary leading zeros
			if(data.length() / 2 != _length){
				int leading_zero_byes = _length - (data.length()/2);
				for(int i = 0; i < leading_zero_byes; i++){
					data = "00" + data;
				}
			}
			// Append to the packet
			packet += data;
		}		

		print("Assembled packet: ");
		println(packet);
		
		// Convert hex string to byte array
		byte[] out_command = hexStringToByteArray(packet);
		
		// Send command
		for(int i = 0; i < out_command.length; i++){		
			port.write(out_command[i]);
		}
		
		// The NMX doesn't generate responses in joystick mode
		if(!_response)
			return;
		
		// Wait for response
		final int TIMEOUT = 3000;
		long time = millis();
		while(true){
			// Clear whatever is in the response string
			response = "";
			// Wait for response packet to show up in buffer
			delay(10);
			if(port.available() > 8){
				print("NMX response: ");		
				int size = port.available();
				int[] in_byte = new int[size];
				for(int i = 0; i < in_byte.length; i++){
					in_byte[i] = port.read();
					String debug = in_byte[i] <= 15 ? "0" + Integer.toHexString(in_byte[i]) : Integer.toHexString(in_byte[i]);
					response = response + debug;
					print(debug);
					print(" ");
				}
				println("");	
				print("Response: ");
				println(response);
				return;
			}						
			// Eventually bail if it never shows up
			if(millis() - time > TIMEOUT){
				println("Timed out waiting for NMX response packet");
				timed_out = true;
				return;
			}			
		}	
	}
	
	static byte[] hexStringToByteArray(String s){
		int len = s.length();
		byte[] data = new byte[len/2];
		for(int i = 0; i < len; i += 2){
			data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}
	
	float parseResponse(){
	
		//int length = Integer.decode("0x" + response.substring(18, 20));
		int data_type = 7;
		long data = 0;
		try{
			data_type = Integer.decode("0x" + response.substring(20, 22));
			try{
				data = Long.decode("0x" + response.substring(22, response.length()));
				try{
					// Handle negative longs
					if(data_type == 3 && Integer.decode("0x" + response.substring(22, 24)) == 255)
						data = data - Long.decode("0xffffffff");
				}
				catch(NumberFormatException e){
					println("Error handling negative data value");
				}
			 
			}
			catch(NumberFormatException e){
				println("Error parsing data");
			}
		}
		catch(NumberFormatException e){
			println("Error parsing data type");
		}
		
		
		switch(data_type){
		// Byte
		case 0:			
		// uint
		case 1:			
		// int
		case 2:			
		// long
		case 3:			
		// ulong
		case 4:	
			// In cases 0-4, we shouldn't need to do anything
			break;
		// float
		case 5:
			// This is an error because the NMX shouldn't actually be sending true floats			
			break;
		// string
		case 6:
			// This is an error code, since a string can't convert to a float nicely			
			break;
		// error
		case 7:
			// This happens when the response can't be parsed			
			break;
		}

		return data;
	}
	
	/*** Helper functions ***/
	
	void updateMousePos() {		
		graph_loc.draw(String.format("%.0f", grid.x()), String.format("%.1f", grid.y()));
		mouse_loc.draw(Integer.toString(mouseX), Integer.toString(mouseY));
	}
	
	public static void main(String _args[]) {
		PApplet.main(new String[] { keyframeplotter.KeyframePlotter.class.getName() });
	}
}
