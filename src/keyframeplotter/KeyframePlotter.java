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
	Button connect = new Button(this);
	Button send = new Button(this);
	Button title = new Button(this);
	Button mem_check = new Button(this);
	Indicator message = new Indicator(this);
	
	// General use constants
	final int BYTE_SIZE = 1;	// Size of a single byte
	final int INT_SIZE = 2;		// Size of an integer
	final int LONG_SIZE = 4;	// Size of a long integer
	final int FLOAT_SIZE = 4;	// Size of a floating point number	
	final int XY_SIZE = 2;		// Represents the two values necessary for an XY location
	
	// Program states
	final int NONE = -1;
	final int GET_INPUT = 1;
	final int MODIFY_INPUT = 2;
	final int SEND_DATA = 3;
	final int DRAW_POINTS = 4;
	final int CONFIRM = 5;
	final int CTRL = 6;
	final int INTERP = 7;
	int state = GET_INPUT;
	
	final int MAX_KF = 7;
	
	float x = -1000;   										// x coordinate points retrieved from MCU
	float y = -1000;										// y coordinate points retrieved from MCU
	int spline_pnt_count = 60;
	boolean port_open = true;
	boolean timed_out = false;
	
	int spline_point_count;
	float[] spline_point;
	boolean spline_available = false;
	
	int command = 0;
	final int PORT_NUM = 4;
	
	
	public void setup() {
		
		// Open proper com port		
		port = new Serial(this, Serial.list()[PORT_NUM], 9600);
		
		size(800, 650);
		background(100, 100, 100);		
		grid.init(55, -5, 5, 10000, -10000, 1000);
		grid.draw();
		
		// Initialize state indicator
		title.init("Key Frame Editor", width/2, 25, 200, 30);
		title.draw();
		message.init("Message", "NULL", grid.x_center, height-25, grid.width, 30);		
		message.draw(" ");
		
		// Initialize buttons and indicators in right tool bar
		float b_width = 90;
		float b_height = 50;
		float b_margin = 10;
		float b_offset = b_margin + b_height/2;
		graph_loc.init("Xg", "Yg", width - (75/2) - 15, (50/2) + grid.y_min_px, b_width, b_height);
		mouse_loc.init("Xm", "Ym", graph_loc.posX, graph_loc.y_max_px + b_offset, b_width, b_height);
		mem_check.init("Mem Check", graph_loc.posX, mouse_loc.y_max_px + b_offset, b_width, b_height);		
		connect.init("Close Port", graph_loc.posX, mem_check.y_max_px + b_offset, b_width, b_height);		
		send.init("Send", graph_loc.posX, connect.y_max_px + b_offset, b_width, b_height);				
	}

	public void draw() {
		
		// Always do these things
		timeOutCheck();
		updateGraphics();		
		updateMousePos();		
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
	
	/*** Button Actions ***/
	void memCheckAction(){			
		if(port_open){		
			println("Checking memory");
			mem_check.colorFill(20, 20, 20);
			mem_check.colorText(200, 200, 200);
			mem_check.draw();
			String response = NMXCommand(0, 200);
			float mem_val = parseResponse(response);
			message.draw("Memory available = " + Float.toString(mem_val) + " bytes");
		}
		else
		message.draw("Port not open, can't send mem check request!");		
	}
	
	void sendAction(){
		println("Send clicked");
		send.colorFill(20, 20, 20);
		send.colorText(200, 200, 200);
		send.draw();
		state = SEND_DATA;
	}
	
	void connectAction(){
		if(port_open){
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
	
	void checkButtons(){
		if(mousePressed == true){
			if(mouseButton == LEFT && bounce.get() ==  false){
				if(send.overButton()){
					sendAction();
				}
				else if(connect.overButton()){
					connectAction();
												
				}
				else if(mem_check.overButton())
					memCheckAction();				
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
			bounce.set(false);
		}
			
	}
	
	void updateGraphics(){
		background(100, 100, 100);
		mem_check.draw();
		connect.draw();
		send.draw();
				
		title.draw();
		message.draw();
		
		// Redraw the grid and buttons
		grid.draw();
		updateMousePos();
		for(int i = 0; i < key_frames.size(); i++){
			Button this_point = key_frames.get(i);
			this_point.label = Integer.toString(i + 1);
			this_point.draw();					
		}	
		
		if(spline_available){
		    // If both of the coordinates have now been set, draw the point:
		    for(int i = 0; i < spline_point_count * XY_SIZE; i += 2){
		        // draw the point:
		  	  strokeWeight(5);
		  	  stroke(255, 0, 0);
		  	  if(i == spline_point_count * XY_SIZE - 2)
		  		point(key_frames.get(key_frames.size() - 1).posX, key_frames.get(key_frames.size() - 1).posY);
		  	  else
		  		point(spline_point[i], spline_point[i+1]);
		    }
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
		else if(grid.overGrid() && cur_kf == -1 && bounce.get() == false){
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
		if(key_frames.size() < MAX_KF){
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

	
		//bounce.set();
	}
	
	
	/** Send state functions **/
	
	void sendData(){		
		
		if(!port_open){
			println("The port is not open, fool!");
			message.draw("Com port not opened!");
			state = GET_INPUT;
			return;
		}
			
		
		final int FLUSH_COUNT = 0;	// Number of times to spam the controller before sending real info 
		
		// Flush the NMX
		println("Sending clearing commands to NMX");
		for(int i = 0; i < FLUSH_COUNT; i++){
			NMXCommand(5, 100);
			if(timed_out)
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
	
	String NMXCommand(int _sub_addr, int _command){
		return NMXCommand(_sub_addr, _command, 0, 0);	
	}
	
	String NMXCommand(int _sub_addr, int _command, int _length, int _data){
		
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

		//print("Assembled packet: ");
		//println(packet);
		
		// Convert hex string to byte array
		byte[] out_command = hexStringToByteArray(packet);
		
		// Send command
		for(int i = 0; i < out_command.length; i++){		
			port.write(out_command[i]);
		}
		
		// Wait for response
		final int TIMEOUT = 3000;
		long time = millis();
		while(true){
			// Wait for response packet to show up in buffer
			delay(10);			
			String response = "";
			if(port.available() > 0){
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
				return response;
			}						
			// Eventually bail if it never shows up
			if(millis() - time > TIMEOUT){
				println("Timed out waiting for NMX response packet");
				timed_out = true;
				return "-1";
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
	
	
	/** Draw state functions **/
	
	void getSplinePoints(){		
		String response;
		spline_point_count = 150;
		spline_point = new float[spline_point_count * XY_SIZE];
		
		// Set number of spline points to be retrieved
		println("Setting curve point count");
		NMXCommand(5, 12, INT_SIZE, spline_point_count);
		if(timed_out)
			return;
		
	  	// Check number of spline points to be retrieved
	    println("Getting curve point count");
	    response = NMXCommand(5, 101);
	    if(timed_out)
			return;
		int spline_point_count = (int)parseResponse(response);	    
	    
		// Signal start of point retrieval
		println("Signaling start of point retrieval");
		NMXCommand(5, 102);
		if(timed_out)
			return;
		
       for(int i = 0; i < spline_point_count * XY_SIZE ; i++){
	        	    	
    	  response = NMXCommand(5, 103, INT_SIZE, i);
    	  if(timed_out)
				return;
    	  float in_val = parseResponse(response) / 100;		// This will be a float, so need to divide by 100 on master device side
    	  print("in_val: ");
    	  println(in_val);
         
          if(i % 2 == 0)
        	  spline_point[i] = grid.x_px(in_val);
          else
        	  spline_point[i] = grid.y_px(in_val);
           
          }	  
       spline_available = true;
	}
	
	float parseResponse(String input){
				
		int length = Integer.decode("0x" + input.substring(18, 20));
		int data_type = Integer.decode("0x" + input.substring(20, 22));		
		long data = Long.decode("0x" + input.substring(22, input.length()));	

		// Handle negative longs
		if(data_type == 3 && Integer.decode("0x" + input.substring(22, 24)) == 255)
			data = data - Long.decode("0xffffffff"); 
		
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
			data = -5000;
			break;
		// string
		case 6:
			// This is an error code, since a string can't convert to a float nicely
			data = -5000;
			break;
		}

		return data;
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
