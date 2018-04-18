import java.lang.Math;

public class DistanceMetres {

	private static final double DegToRad = Math.PI/180;
	private static final double EarthRadiusMetrs = 6372797.560856;

	public double calcDistance(VertCoord begin, VertCoord end) {
		double latArc, lonArc, latHav, lonHav;
		double tmpLatCos, arcDist;
		latArc = (begin.getLat() - end.getLat()) * DegToRad;
		lonArc = (begin.getLon() - end.getLon()) * DegToRad;
		latHav = Math.sin(latArc * 0.5) * Math.sin(latArc * 0.5);
		lonHav = Math.sin(lonArc * 0.5) * Math.sin(lonArc * 0.5);
		tmpLatCos = Math.cos(begin.getLat() * DegToRad) * Math.cos(end.getLat() * DegToRad);
		arcDist = 2.0 * Math.asin(Math.sqrt(latHav + (tmpLatCos * lonHav)));
		return EarthRadiusMetrs * arcDist;
	}
}
