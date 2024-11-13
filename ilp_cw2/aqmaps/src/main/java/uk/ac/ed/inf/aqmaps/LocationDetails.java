package uk.ac.ed.inf.aqmaps;

public class LocationDetails {
	String country;
	Square square;
	public static class Square{
		Southwest southwest;
		public static class Southwest{
			double lng;
			double lat;
		}
		Northeast northeast;
		public static class Northeast{
			double lng;
			double lat;
		}
	}
	String nearestPlace;
	Coordinates coordinates;
	public static class Coordinates{
		double lng;
		double lat;
	}
	String words;
	String language;
	String map;

}
