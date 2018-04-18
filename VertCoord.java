/*
 * klasa przechowujaca wspolrzedne wierzcholka
 */

public class VertCoord {
	private double latitude;
	private double longitude;

	VertCoord (double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public double getLat() { return latitude; }

	public double getLon() { return longitude; }
}
