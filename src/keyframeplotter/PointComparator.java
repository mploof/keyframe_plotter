package keyframeplotter;
import java.util.Comparator;

public class PointComparator implements Comparator<Button> {
	public int compare(Button point1, Button point2){
		return (int) (point1.posX - point2.posX);
	}
}
