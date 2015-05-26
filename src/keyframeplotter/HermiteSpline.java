package keyframeplotter;

import processing.core.PApplet;

public class HermiteSpline {
	
	// Processing object
	private PApplet p;
	
	private float f, d, s;
	private float xn[], fn[], dn[];
		
	HermiteSpline(PApplet _p){
		p = _p;		
	}
	
	public void init(float _xn[], float _fn[], float _dn[]){
		xn = _xn;
		fn = _fn;
		dn = _dn;
	}
	
	public void cubic_value(float x1, float f1, float d1, float x2,
		float f2, float d2, float x)
	{
		float c2;
		float c3;
		float df;
		float h;

		h = x2 - x1;
		df = (f2 - f1) / h;

		c2 = -((float)2.0 * d1 - (float)3.0 * df + d2) / h;
		c3 = (d1 - (float)2.0 * df + d2) / h / h;
		
		f = f1 + (x - x1) * (d1
			+ (x - x1) * (c2
			+ (x - x1) *   c3));
		d = d1 + (x - x1) * ((float)2.0 * c2
			+ (x - x1) * (float)3.0 * c3);
		s = (float)2.0 * c2 + (x - x1) * (float)6.0 * c3;
		
		return;
	}


	public void cubic_spline_value(float x)
	{
		if(x > xn[xn.length - 1])
			x = xn[xn.length - 1];
		int curve = findCurve(x);

		cubic_value(xn[curve], fn[curve], dn[curve], xn[curve + 1],
			fn[curve + 1], dn[curve + 1], x);
		
		return;
	}	
	
	public float getPos(){
		return f;
	}
	
	public float getVel(){
		return d;
	}
	
	public float getAccel(){
		return s;
	}
	
	private int findCurve(float _x){
		
		int which = 0;
		
		for(int i = 0; i < xn.length; i++){
			if(_x >= xn[i] && _x <= xn[i+1])
				return i;
		}		
		
		return which;
	}
}