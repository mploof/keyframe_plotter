package keyframeplotter;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.serial.*;


public class KeyframePlotter extends PApplet {	
		
	ArrayList<ControlHandle> control_handles = 
			new ArrayList<ControlHandle>();
	Serial port;        									// The serial port
	Bounce bounce = new Bounce(this);						// Mouse debouncing object
	int grid_top_margin = 50;
	int grid_bottom_margin = 200;
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
	Slider update_rate = new Slider(this);
	Indicator update_display = new Indicator(this);
	
	// Center object
	Button title = new Button(this);
	Indicator message = new Indicator(this);
	Slider motor_slider = new Slider(this);
	
	// Lower tool bar
	Slider grid_scale_x = new Slider(this);
	final int min_x_inc = 1;
	final int max_x_inc = 10;
	Button clear_kf = new Button(this);
	Button go_home = new Button(this);
	Button start_program = new Button(this);
	Button stop_motors = new Button(this);
	
	// Left side tool bar
	Slider grid_scale_y = new Slider(this);
	final int min_y_inc = 1000;
	final int max_y_inc = 10000;
	
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
	final int MAX_KF = 20;
	int spline_point_count = 4000;
	float[] spline_abscissa;
	float[] spline_pos_y;	
	float[] spline_vel_y;
	boolean spline_available = false;
	boolean data_sent = false;	
	
	// Timing vars
	long check_time = millis();
	long program_start_time;
	
	// Program run vars
	long run_time = 0;
	float run_time_sec = 0;
	final float MILLIS_PER_SEC = 1000;
	int cur_vel_pnt = -1;
	float max_error = 0;
	float min_error = 0;
	int vel_update_rate; 
	
	// Spline interpolation
	HermiteSpline hermite = new HermiteSpline(this);
	
	
	public void setup() {
		
		// Open proper com port		
		port = new Serial(this, Serial.list()[PORT_NUM], 9600);
		
		size(800, 800);
		background(bgnd_r, bgnd_g, bgnd_b);
		
		setGridVals();
		grid.init(x_max, x_min, x_interval, y_max, y_min, y_interval);
		grid.draw();
		
		// Initialize grid scales and mid-screen indicators
		int grid_scale_thickness = 15;
		float i_height = 30;
		float i_margin = 10;
		grid_scale_y.init(grid.height, grid_scale_thickness, true, grid_left_margin / 2, grid.y_mid_px, min_y_inc, max_y_inc, false);
		grid_scale_y.setSlidePercent(0);
		grid_scale_y.draw();
		title.init("Key Frame Editor", width/2, 25, 200, i_height);
		title.draw();
		grid_scale_x.init(grid.width, grid_scale_thickness, false, grid.x_mid_px, grid.y_max_px + i_margin + grid_scale_thickness/2, min_x_inc, max_x_inc, false);
		grid_scale_x.setSlidePercent(0);
		grid_scale_x.draw();
		message.init("Message", "NULL", grid.x_center, grid_scale_x.getYMax() + i_margin + i_height/2, grid.width, i_height);		
		message.draw(" ");
		motor_slider.init(grid.width, i_height, false, grid.x_mid_px, message.y_max_px + i_margin + i_height/2, -1, 1, true);
		motor_slider.draw();	
		
		// Initialize lower toolbar buttons
		int lt_count = 4;
		float lt_height = 50;
		float lt_margin = 10;
		float lt_width = (grid.width - (lt_count - 1) * lt_margin) / lt_count;		
		float lt_offset = lt_margin + lt_width/2;
		float lt_y_pos = motor_slider.getYMax() + lt_height/2 + lt_margin;
		clear_kf.init("Clear KFs", motor_slider.getXMin() + lt_width / 2, lt_y_pos, lt_width, lt_height);			
		go_home.init("Snd Mot Home", clear_kf.x_max_px + lt_offset, lt_y_pos, lt_width, lt_height);
		start_program.init("Start Program", go_home.x_max_px + lt_offset, lt_y_pos, lt_width, lt_height);
		stop_motors.init("Stop Motors", start_program.x_max_px + lt_offset, lt_y_pos, lt_width, lt_height);
		
		// Initialize buttons and indicators in right toolbar
		int b_count = 8;
		float b_margin = 10;
		float b_width = 90;
		float b_height = (grid.height - (b_count - 1) * b_margin) / b_count;		
		float b_offset = b_margin + b_height/2;
		float b_column_x = width - (75/2) - 15;
		graph_loc.init("Xg", "Yg", b_column_x, (b_height/2) + grid.y_min_px, b_width, b_height);
		mouse_loc.init("Xm", "Ym", b_column_x, graph_loc.y_max_px + b_offset, b_width, b_height);				
		connect.init("Close Port", b_column_x, mouse_loc.y_max_px + b_offset, b_width, b_height);
		mem_check.init("Mem Check", b_column_x, connect.y_max_px + b_offset, b_width, b_height);
		send.init("Send", b_column_x, mem_check.y_max_px + b_offset, b_width, b_height);			
		get_kf.init("Get KF", b_column_x, send.y_max_px + b_offset, b_width, b_height);
		lock_x.init("Lock X", b_column_x, get_kf.y_max_px + b_offset, b_width, b_height);
		lock_y.init("Lock Y", b_column_x, lock_x.y_max_px + b_offset, b_width, b_height);
		lock_x.clicked = false;
		lock_y.clicked = false;		
		update_rate.init(clear_kf.y_max_px - grid_scale_x.getYMin(), 20, true, b_column_x - b_width/4, 
				(clear_kf.y_max_px + grid_scale_x.getYMin()) / 2, 0, 1, false);
		update_rate.setSlidePercent((float)0.25);
		update_rate.draw();
		update_display.init("", "NULL", b_column_x + b_width/4, update_rate.getYMid(), 40, 30);
				
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
		if(motor_slider.clicked()){
			
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
			float speed = max_speed * motor_slider.getVal();
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
			if(check_moving){
				// Check whether the motors have stopped moving
				NMXCommand(1, 107);
				if(parseResponse() == 0){
					check_moving = false;
					moving  = false;
					return;				
				}
			}
			// If a program is running, show the current error
			if(state == RUN){
				
				hermite.cubic_spline_value(run_time_sec);
				float error = current_pos - hermite.getPos();
				if(error > max_error)
					max_error = error;
				else if (error < min_error)
					min_error = error;
				message.draw(Float.toString(error));
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
			if(control_handles.size() < 2){
				message.draw("Must have at least two keyframes");
				return;
			}
			println("Send clicked");
			message.draw("Sending key frame data to NMX");
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
		control_handles.clear();
		spline_available = false;
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
		else if(!data_sent){
			message.draw("Must send key frame data to NMX before running program");
			return;
		}
		println("********** Starting program **********");
		cur_vel_pnt = -1;
		runProgram();
	}
	
	void stopMotorsAction(){
		for(int i = 0; i < MOTOR_COUNT; i++){
			NMXCommand(i, 4);
			moving = false;
			joystick_mode = false;
			NMXCommand(0, 23, BYTE_SIZE, 0);
		}
		
		// Go back to input state (in case a program is running);
		state = GET_INPUT;
	}
	
	void updateSpline(){		
		int size = control_handles.size();
		float xn[] = new float[size];
		float fn[] = new float[size];
		float dn[] = new float[size];
		
		for(int i = 0; i < size; i++){			
			ControlHandle this_handle = control_handles.get(i);
			xn[i] = this_handle.getGridX();
			fn[i] = this_handle.getGridY();
			dn[i] = this_handle.getGridSlope();
		}
		
		spline_abscissa = new float[spline_point_count];
		spline_pos_y= new float[spline_point_count];
		spline_vel_y = new float[spline_point_count];
				
		float interval = control_handles.get(control_handles.size()-1).getGridX() / (float)(spline_point_count - 1);
				
		hermite.init(xn, fn, dn);
		for(int i = 0; i < spline_point_count; i++){
			
			float location = (float)i * interval;
			
			// Don't exceed the maximum x position due to rounding
			if(location > control_handles.get(control_handles.size()-1).getGridX())
				location = control_handles.get(control_handles.size()-1).getGridX();
			
			hermite.cubic_spline_value((float)i * interval);
			spline_abscissa[i] = location;
			spline_pos_y[i] =  hermite.getPos();
			spline_vel_y[i] =  hermite.getVel();			
		}
		spline_available = true;		
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
		update_rate.update();
		
		// Find the new update rate and indicate it
		vel_update_rate = floor((float)(Math.pow(update_rate.getVal(), 2) * 99) + 1);
		update_display.draw(Integer.toString(vel_update_rate));
				
		// Draw center items
		title.draw();
		message.draw();
		motor_slider.update();
		
		// Draw lower toolbar
		clear_kf.draw();
		go_home.draw();
		start_program.draw();
		stop_motors.draw();
		
		// Draw grid scales
		grid_scale_x.update();
		grid_scale_y.update();
		
		// Draw grid		
		setGridVals();
		grid.init(x_max, x_min, x_interval, y_max, y_min, y_interval);
		grid.draw();
		updateMousePos();
		
		// Draw control handles
		for(int i = 0; i < control_handles.size(); i++){			
			ControlHandle this_handle = control_handles.get(i);
			this_handle.setLabel(Integer.toString(i + 1));
			this_handle.setLocGrid(this_handle.getGridX(), this_handle.getGridY());
			this_handle.draw();
		}		
		
		// Update the spline calculation
		if(control_handles.size() >= 2)
			updateSpline();
		
		// Draw motor position indicator
		updatePosition();
		
		// Draw position and velocity splines if data is available
		if(spline_available){

		    for(int i = 0; i < spline_point_count; i ++){
		    	// draw the displacement points
		  	  	strokeWeight(2);
		  	  	stroke(255, 0, 0);
	  	  		point(grid.x_px(spline_abscissa[i]), grid.y_px(spline_pos_y[i]));

		    	// draw the velocity points
	  	  		stroke(0, 0, 255);
	  	  		point(grid.x_px(spline_abscissa[i]), grid.y_px(spline_vel_y[i]));
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
		x_interval = (int) (grid_scale_x.getVal() - grid_scale_x.getVal() % min_x_inc);
		if(y_interval == 0)
			y_interval = 5;
		x_max = 11 * x_interval;
		x_min = -x_interval;
		
		y_interval = (int) (grid_scale_y.getVal() - grid_scale_y.getVal() % min_y_inc);
		if(y_interval == 0)
			y_interval = 500;
		y_min = -10 * y_interval;
		y_max = 10 * y_interval;
	}
	
	/** Input state functions **/
	 
	void getInput(){		
		
		// If a control handle is clicked, update it
		for(int i = 0; i < control_handles.size(); i++){
			ControlHandle this_handle = control_handles.get(i);
			if(this_handle.update() == true){
				data_sent = false;
				message.draw("");
				return;					
			}
		}
		
		// If a control handle is right clicked, delete it
		for(int i = 0; i < control_handles.size(); i++){
			ControlHandle this_handle = control_handles.get(i);
			if(this_handle.overCenter() == true && mousePressed && mouseButton == RIGHT && bounce.get() == false){
				control_handles.remove(i);				
				data_sent = false;
				message.draw("");
				// Reset the control handle id numbers
				for(int j = 0; j < control_handles.size(); j++){
					control_handles.get(j).setID(j);
				}
				return;
			}								
		}		
		
		// Otherwise, add a new point if the grid was clicked
		if(grid.overGrid() && mousePressed && mouseButton == LEFT && bounce.get() == false){			
				addKF(true);
				data_sent = false;
				message.draw("");
		}
	}	
	
	void addKF(boolean _mouse_input){
		
		// Don't allow more than the maximum key frames to be added
		if(control_handles.size() == MAX_KF){
			message.draw("Max key frames placed. Please modify or delete existing frames.");
			return;
		}
		
		// Add a new button object to the key frame object array and then create a working reference
		control_handles.add(new ControlHandle(this, grid, control_handles));
		ControlHandle this_handle = control_handles.get(control_handles.size() - 1);		
		
		float posX;
		float posY;
		
		// If this is the first key frame, reset the motor's home position
		if(control_handles.size() == 1){
			print("Adding first point");
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
		// Adding a key frame by clicking on the grid screen
		else if(_mouse_input == true){			
			println("Adding new point");
			posX = (int)grid.x();
			posY = grid.y();		
			println("X: ");
			print(posX);
			print(" Y: ");
			println(posY);
		}		
		// Adding a key frame by querying the motor's current position
		else{
			print("Key frames: ");
			println(control_handles.size());
			
			// Check the motor's current position		
			NMXCommand(1, 106);
			float last_x = control_handles.get(control_handles.size() - 2).getGridX();
			posX = (grid.x_max - last_x) / 4 + last_x;
			posY = parseResponse();
			print("posX: ");
			println(posX);
			print("posY: ");
			println(posY);					
		}
		this_handle.setLocGrid(posX, posY);		
		
		// Re-order the points by position
		Collections.sort(control_handles, new PointComparator());
		
		// Label and draw them
		for(int i = 0; i < control_handles.size(); i++){
			this_handle = control_handles.get(i);
			this_handle.setLabel(Integer.toString(i));
			this_handle.setID(i);
		}
		this_handle.draw();

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
		NMXCommand(5, 10, INT_SIZE, control_handles.size());
		if(timed_out)
			return;
		
		// Select axis 0
		NMXCommand(5, 11, INT_SIZE, 0);
		if(timed_out)
			return;
				
		// Verify the frame count
		println("Verifying key frame count");
		NMXCommand(5, 100);
		if(timed_out)
			return;
		print("Key frame count set: ");
		println(parseResponse());
		
		// **** Send key frame points*** //		
		
		// Set the abscissas of the key frame points
		for(int i = 0; i < control_handles.size(); i++){
			float temp_val = control_handles.get(i).getGridX();
			int out_val = Float.floatToIntBits(temp_val);
			print("Abscissa ");
			print(i);
			print(" value: ");
			println(temp_val);
			
			NMXCommand(5, 12, FLOAT_SIZE, out_val);
			if(timed_out)
				return;
		}
		
		// Set the positions of the key frame points
		for(int i = 0; i < control_handles.size(); i++){
			float temp_val = control_handles.get(i).getGridY();
			int out_val = Float.floatToIntBits(temp_val);
			print("Position ");
			print(i);
			print(" value: ");
			println(temp_val);
			
			NMXCommand(5, 13, FLOAT_SIZE, out_val);
			if(timed_out)
				return;
		}
				
		// Set the velocities of the key frame points
		for(int i = 0; i < control_handles.size(); i++){
			float temp_val = control_handles.get(i).getGridSlope();
			int out_val = Float.floatToIntBits(temp_val);
			print("Velocity ");
			print(i);
			print(" value: ");
			println(temp_val);
			
			NMXCommand(5, 14, FLOAT_SIZE, out_val);
			if(timed_out)
				return;
		}		
		
		// End the transmission
		println("Ending key frame point transmission");
		NMXCommand(5, 10, INT_SIZE, 0);
		if(timed_out)
			return;
		
		data_sent = true;		
		state = GET_INPUT;
		message.draw("Ready to start program!");
	}	
	
	/*** Run state functions***/
	void runProgram() {
						
		// Start the program on the NMX
		if(state != RUN){
			println("Sending start command");
			state = RUN;
			// Set the update rate			
			NMXCommand(5, 15, INT_SIZE, vel_update_rate);
			
			// Set the flag to update the motor position indicator
			moving = true;
			
			// Start the program
			NMXCommand(5, 20);
			
			// Set the start time
			program_start_time = millis();
			
		}
		
		// Update the running time
		else{
			run_time = millis() - program_start_time;		
			run_time_sec = run_time / MILLIS_PER_SEC;
			// Check whether the program has finished
			if(run_time_sec > control_handles.get(control_handles.size()-1).getGridX()){
				moving = false;
				String error_message = "Max error: " + Float.toString(max_error) + " Min error: " + Float.toString(min_error);
				max_error = 0;
				min_error = 0;
				message.draw(error_message);
				state = GET_INPUT;
			}
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
		catch(StringIndexOutOfBoundsException e){
			println("Out of bounds!!!");
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
