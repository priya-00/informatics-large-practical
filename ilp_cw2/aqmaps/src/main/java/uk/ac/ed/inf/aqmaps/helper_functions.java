package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.turf.TurfJoins;

public class helper_functions {
	
	// list of coordinates of drone confinement area
	public List<Point> list_confinement_coordinates()
	{
		//initialise drone confinement coordinates
		Point point = Point.fromLngLat(-3.192473, 55.946233); //Forrest Hill
		Point point2 = Point.fromLngLat(-3.184319, 55.946233); //KFC
		Point point3 = Point.fromLngLat(-3.184319, 55.942617); //Buccleuch St
		Point point4 = Point.fromLngLat(-3.192473, 55.942617); //Top of Meadows

		//List of Points of the coordinates above
		List<Point> coordinates = new ArrayList<Point>();
		coordinates.add(point);
		coordinates.add(point2);
		coordinates.add(point3);
		coordinates.add(point4);
		coordinates.add(point);
		
		return coordinates;
	}

	// drone confinement area
	public void drone_confinement(List<Feature> features_list)
	{
		
		//List of Points of the coordinates for the confinement area
		List<Point> coordinates = new ArrayList<Point>();
		coordinates = list_confinement_coordinates();

		//creating the border of the drone confinement area
		LineString border = LineString.fromLngLats(coordinates);
		Feature feature = Feature.fromGeometry(border);

		//add to feature list
		features_list.add(feature);
	}
	
	// check point is inside drone confinement area
	public boolean drone_confinement(double lng, double lat)
	{
		Point p = Point.fromLngLat(lng, lat);
		List<Point> coordinates = new ArrayList<Point>();
		coordinates = list_confinement_coordinates();
		List<List<Point>> polygon_coordinates = new ArrayList<List<Point>>();
		polygon_coordinates.add(coordinates);
		Polygon confinement_polygon = Polygon.fromLngLats(polygon_coordinates);
		if(TurfJoins.inside(p, confinement_polygon)==true)
		{
			return true;
		}
		else
		{
			return false;
		}
		
	}

	// find distance between current position of drone and a sensor
	public double findDistance(double x1_lng, double x1_lat, double x2_lng, double x2_lat)
	{
		double dist = 0.0;
		dist = Math.sqrt((Math.pow((x1_lng-x2_lng),2)) + (Math.pow((x1_lat-x2_lat),2)));
		return dist;		
	}

	// add properties to sensor marker
	public void marker_properties(double lng,double lat,String reading1,double battery,String location, List<Feature> features_list)
	{
		Point point = Point.fromLngLat(lng, lat);
		Feature feature = Feature.fromGeometry(point);
		//double reading = Double.parseDouble(reading1);
		if(battery<10.0 || reading1==null)
		{
			feature.addStringProperty("location", location);
			feature.addStringProperty("rgb-string", "#000000");
			feature.addStringProperty("marker-color", "#000000");
			feature.addStringProperty("marker-symbol", "cross");
		}
		else
		{
			double reading = Double.parseDouble(reading1);
			if(reading>=0.0 && reading<32.0)
			{
				feature.addStringProperty("location", location);
				feature.addStringProperty("rgb-string", "#00ff00");
				feature.addStringProperty("marker-color", "#00ff00");
				feature.addStringProperty("marker-symbol", "lighthouse");
			}
			else if(reading>=32.0 && reading<64.0)
			{
				feature.addStringProperty("location", location);
				feature.addStringProperty("rgb-string", "#40ff00");
				feature.addStringProperty("marker-color", "#40ff00");
				feature.addStringProperty("marker-symbol", "lighthouse");
			}
			else if(reading>=64.0 && reading<96.0)
			{
				feature.addStringProperty("location", location);
				feature.addStringProperty("rgb-string", "#80ff00");
				feature.addStringProperty("marker-color", "#80ff00");
				feature.addStringProperty("marker-symbol", "lighthouse");
			}
			else if(reading>=96.0 && reading<128.0)
			{
				feature.addStringProperty("location", location);
				feature.addStringProperty("rgb-string", "#c0ff00");
				feature.addStringProperty("marker-color", "#c0ff00");
				feature.addStringProperty("marker-symbol", "lighthouse");
			}
			else if(reading>=128.0 && reading<160.0)
			{
				feature.addStringProperty("location", location);
				feature.addStringProperty("rgb-string", "#ffc000");
				feature.addStringProperty("marker-color", "#ffc000");
				feature.addStringProperty("marker-symbol", "danger");
			}
			else if(reading>=160.0 && reading<192.0)
			{
				feature.addStringProperty("location", location);
				feature.addStringProperty("rgb-string", "#ff8000");
				feature.addStringProperty("marker-color", "#ff8000");
				feature.addStringProperty("marker-symbol", "danger");
			}
			else if(reading>=192.0 && reading<224.0)
			{
				feature.addStringProperty("location", location);
				feature.addStringProperty("rgb-string", "#ff4000");
				feature.addStringProperty("marker-color", "#ff4000");
				feature.addStringProperty("marker-symbol", "danger");
			}
			else if(reading>=224.0 && reading<256.0)
			{
				feature.addStringProperty("location", location);
				feature.addStringProperty("rgb-string", "#ff0000");
				feature.addStringProperty("marker-color", "#ff0000");
				feature.addStringProperty("marker-symbol", "danger");
			}
		}

		features_list.add(feature);
	}

	// function to round theta to the nearest 10
	public int round_to_nearest10(double theta)
	{
		int lwr = (int)(theta / 10) * 10;
		int upr = (int)lwr + 10;
		// Return of closest of two
		return (theta - lwr >= upr - theta)? upr : lwr;
	}

	// check if given coordinates are in any of the no-fly-zones
	public boolean in_no_fly_zones(double lng, double lat, String geojsonString) throws IOException, InterruptedException
	{
		boolean in_zone;
		int cnt = 0;
		Point point = Point.fromLngLat(lng, lat);
		
		FeatureCollection noflyzones = FeatureCollection.fromJson(geojsonString);
		if (noflyzones.features() != null) 
		{
			for(Feature singleNoFlyZone : noflyzones.features()) {
				if (singleNoFlyZone.geometry() instanceof Polygon) 
				{
					Polygon p = (Polygon)singleNoFlyZone.geometry();
					if(TurfJoins.inside(point, p)==false)
					{
						cnt++;
					}

				}
			}
		}
		if(cnt==4)
		{
			in_zone = false;
		}
		else
		{
			in_zone = true;
		}

		return in_zone;


	} 
	
}
