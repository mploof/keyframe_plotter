package keyframeplotter;
import java.util.Comparator;

public class PointComparator implements Comparator<ControlHandle> {
	public int compare(ControlHandle point1, ControlHandle point2){
		return (int) (point1.getGridX() - point2.getGridX());
	}
}
