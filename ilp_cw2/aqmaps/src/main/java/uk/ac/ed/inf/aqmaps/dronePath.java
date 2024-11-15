package uk.ac.ed.inf.aqmaps;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.*;
import java.nio.file.*;
import com.mapbox.geojson.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class dronePath {

	List<Feature> features_list = new ArrayList<Feature>();
	List<Point> drone_path_coordinates = new ArrayList<Point>();
	String[][] flightpath = new String[150][7];
	String[][] fp;
	
	helper_functions hf = new helper_functions();

	public String find_dronePath(String urlString, double longitude, double latitude, String port) throws IOException, InterruptedException
	{
		// Getting data from web server for specified day
		
		// One HttpClient shared between all HttpRequests
		HttpClient client = HttpClient.newHttpClient();
		
		// HttpClient assumes that it is a GET request by default.
		var request = HttpRequest.newBuilder()
				.uri(URI.create(urlString))
				.build();
		
		// The response object is of class HttpResponse<String>
		var response = client.send(request, BodyHandlers.ofString());
		String jsonListString = response.body();
		
		Type listType = new TypeToken<ArrayList<Sensor>>(){}.getType();
		// Use the "fromJson(String, Type)" method
		ArrayList<Sensor> sensorList = new Gson().fromJson(jsonListString, listType);

		// Got list of all sensors (location,battery,reading) in sensorList

		// Get no-fly-zones data
		String urlString1 = "http://localhost:80/buildings/no-fly-zones.geojson";
		var request3 = HttpRequest.newBuilder()
				.uri(URI.create(urlString1))
				.build();
		var response3 = client.send(request3, BodyHandlers.ofString());
		String geojsonString = response3.body();

		// defining drone confinement area
		//hf.drone_confinement(features_list);
		
		int k = -1;

		// boolean array that is true for sensors that have been visited
		Boolean[] visited = new Boolean[33];
		for(int i=0;i<33;i++)
		{
			visited[i]=false;
		}

		// current position of drone (starts at specified coordinates) 
		double[] cur_position = new double[2];
		cur_position[0] = longitude;
		cur_position[1] = latitude;

		int sensor_number=0;

		// counter variable to check if all sensors have been visited
		int cnt = 0;

		// count the number of moves
		int moves = 0;

		double min_dist = 100.0;
		double[] next_sensor = new double[2];
		String next_reading = "";
		double next_battery = 0.0;
		String next_location = "";
		while(cnt!=33 && moves<=150)
		{
			min_dist = 100;
			// iterate over sensors
			for(int i=0; i<33; i++)
			{
				if(visited[i]==false)
				{
					Sensor cur_sensor = sensorList.get(i);
					// get the location of the sensor
					String tmp = cur_sensor.location;
					String[] w3w = tmp.split("\\.");

					String urlString2 = "http://localhost:"+port+"/words/"+w3w[0]+"/"+w3w[1]+"/"+w3w[2]+"/details.json";
					var request2 = HttpRequest.newBuilder()
							.uri(URI.create(urlString2))
							.build();
					// The response object is of class HttpResponse<String>
					var response2 = client.send(request2, BodyHandlers.ofString());
					String jsonDetailsString = response2.body();
					var details = new Gson().fromJson(jsonDetailsString,LocationDetails.class);
					// get corresponding lng,lat for the What3Words location of sensor
					double sensor_lng = details.coordinates.lng;
					double sensor_lat = details.coordinates.lat;
					// find the distance between current position of drone and sensor
					double dist = hf.findDistance(cur_position[0],cur_position[1],sensor_lng,sensor_lat);
					// find the sensor that is closest to drone's current position
					if(dist<min_dist && dist>=0.0003)
					{
						min_dist = dist;
						next_sensor[0] = sensor_lng;
						next_sensor[1] = sensor_lat;
						sensor_number = i;
						next_reading = cur_sensor.reading;
						next_battery = cur_sensor.battery;
						next_location = cur_sensor.location;

					}
				}
			}

			// make drone move in lengths of 0.0003 degrees towards the chosen sensor
			double[] next_position = new double[2];
			long num_moves = Math.round(min_dist/0.0003);
			for(long i=1; i<=num_moves; i++)
			{
				double theta_rad = Math.atan2((next_sensor[1]-cur_position[1]),(next_sensor[0]-cur_position[0]));
				double theta3 = Math.toDegrees(theta_rad);
				int theta = hf.round_to_nearest10(theta3);
				double theta1 = Math.toRadians(theta);
				next_position[0] = cur_position[0] + ((0.0003)*Math.cos(theta1));
				next_position[1] = cur_position[1] + ((0.0003)*Math.sin(theta1));
				if(hf.in_no_fly_zones(next_position[0], next_position[1], geojsonString)==false && hf.drone_confinement(next_position[0], next_position[1])==true && moves<150)
				{
					Point point = Point.fromLngLat(cur_position[0], cur_position[1]);
					drone_path_coordinates.add(point);
					Point point1 = Point.fromLngLat(next_position[0], next_position[1]);
					drone_path_coordinates.add(point1);
					int th=theta;
					if(theta<0 && theta>=-180 && theta<=-90)
					{
						th = 180 + (180 - (Math.abs(theta)));
					}
					else if(theta<0 && theta>-90)
					{
						th = 360 - (Math.abs(theta));
					}
					else if(theta==360)
					{
						th = 0;
					}
					k++;
					flightpath[k][0]=Integer.toString(k+1);
					flightpath[k][1]=Double.toString(cur_position[0]);
					flightpath[k][2]=Double.toString(cur_position[1]);
					flightpath[k][3]=Integer.toString(th);
					flightpath[k][4]=Double.toString(next_position[0]);
					flightpath[k][5]=Double.toString(next_position[1]);
					
					cur_position[0] = next_position[0];
					cur_position[1] = next_position[1];
					moves++;
				}
				else
				{
					// change theta until next_position is not in no fly zones and continue moves
					while(hf.in_no_fly_zones(next_position[0], next_position[1], geojsonString)==true || hf.drone_confinement(next_position[0], next_position[1])==false)
					{
						theta = theta+10;
						double theta2 = Math.toRadians(theta);
						next_position[0] = cur_position[0] + ((0.0003)*Math.cos(theta2));
						next_position[1] = cur_position[1] + ((0.0003)*Math.sin(theta2));
					}
					if(moves<150)
					{
						Point point = Point.fromLngLat(cur_position[0], cur_position[1]);
						drone_path_coordinates.add(point);
						Point point1 = Point.fromLngLat(next_position[0], next_position[1]);
						drone_path_coordinates.add(point1);
						int th=theta;
						if(theta<0 && theta>=-180 && theta<=-90)
						{
							th = 180 + (180 - (Math.abs(theta)));
						}
						else if(theta<0 && theta>-90)
						{
							th = 360 - (Math.abs(theta));
						}
						else if(theta==360)
						{
							th = 0;
						}
						k++;
						flightpath[k][0]=Integer.toString(k+1);
						flightpath[k][1]=Double.toString(cur_position[0]);
						flightpath[k][2]=Double.toString(cur_position[1]);
						flightpath[k][3]=Integer.toString(th);
						flightpath[k][4]=Double.toString(next_position[0]);
						flightpath[k][5]=Double.toString(next_position[1]);
						cur_position[0] = next_position[0];
						cur_position[1] = next_position[1];
						moves++;
					}//if 
				}//else
			}//num_moves for loop
			if(hf.findDistance(cur_position[0],cur_position[1],next_sensor[0],next_sensor[1])<0.0002)
			{
				// add marker properties to sensor
				hf.marker_properties(next_sensor[0],next_sensor[1],next_reading,next_battery,next_location,features_list);
				visited[sensor_number] = true;
				cnt++;
				flightpath[k][6]=next_location;
			}
		}// cnt, moves while loop
		
		// move drone to make path a closed loop
		if(moves<150)
		{
			// find distance between drone's starting and current coordinates
			double dist = hf.findDistance(cur_position[0], cur_position[1], longitude, latitude);
			// no. of moves required to return to starting position
			long num_moves = Math.round(dist/0.0003);
			double[] next_position = new double[2];
			for(long i=1; i<=num_moves; i++)
			{
				double theta_rad = Math.atan2((latitude-cur_position[1]),(longitude-cur_position[0]));
				double theta3 = Math.toDegrees(theta_rad);
				int theta = hf.round_to_nearest10(theta3);
				double theta1 = Math.toRadians(theta);
				next_position[0] = cur_position[0] + ((0.0003)*Math.cos(theta1));
				next_position[1] = cur_position[1] + ((0.0003)*Math.sin(theta1));
				if(hf.in_no_fly_zones(next_position[0], next_position[1], geojsonString)==false && hf.drone_confinement(next_position[0], next_position[1])==true && moves<150)
				{
					Point point = Point.fromLngLat(cur_position[0], cur_position[1]);
					drone_path_coordinates.add(point);
					Point point1 = Point.fromLngLat(next_position[0], next_position[1]);
					drone_path_coordinates.add(point1);
					int th=theta;
					if(theta<0 && theta>=-180 && theta<=-90)
					{
						th = 180 + (180 - (Math.abs(theta)));
					}
					else if(theta<0 && theta>-90)
					{
						th = 360 - (Math.abs(theta));
					}
					else if(theta==360)
					{
						th = 0;
					}
					k++;
					flightpath[k][0]=Integer.toString(k+1);
					flightpath[k][1]=Double.toString(cur_position[0]);
					flightpath[k][2]=Double.toString(cur_position[1]);
					flightpath[k][3]=Integer.toString(th);
					flightpath[k][4]=Double.toString(next_position[0]);
					flightpath[k][5]=Double.toString(next_position[1]);
					
					cur_position[0] = next_position[0];
					cur_position[1] = next_position[1];
					moves++;
				}
				else
				{
					// change theta until next_position is not in no fly zones and continue moves
					while(hf.in_no_fly_zones(next_position[0], next_position[1], geojsonString)==true || hf.drone_confinement(next_position[0], next_position[1])==false)
					{
						theta = theta+10;
						double theta2 = Math.toRadians(theta);
						next_position[0] = cur_position[0] + ((0.0003)*Math.cos(theta2));
						next_position[1] = cur_position[1] + ((0.0003)*Math.sin(theta2));
					}
					if(moves<150)
					{
						Point point = Point.fromLngLat(cur_position[0], cur_position[1]);
						drone_path_coordinates.add(point);
						Point point1 = Point.fromLngLat(next_position[0], next_position[1]);
						drone_path_coordinates.add(point1);
						int th=theta;
						if(theta<0 && theta>=-180 && theta<=-90)
						{
							th = 180 + (180 - (Math.abs(theta)));
						}
						else if(theta<0 && theta>-90)
						{
							th = 360 - (Math.abs(theta));
						}
						else if(theta==360)
						{
							th = 0;
						}
						k++;
						flightpath[k][0]=Integer.toString(k+1);
						flightpath[k][1]=Double.toString(cur_position[0]);
						flightpath[k][2]=Double.toString(cur_position[1]);
						flightpath[k][3]=Integer.toString(th);
						flightpath[k][4]=Double.toString(next_position[0]);
						flightpath[k][5]=Double.toString(next_position[1]);
						cur_position[0] = next_position[0];
						cur_position[1] = next_position[1];
						moves++;
					}//if 
				}//else
			}//num_moves for loop	
		}// if
		
		//System.out.println("moves = " + moves);
		//System.out.println("cnt = " + cnt);
		
		fp = new String[moves][7];
		for(int i=0; i<moves; i++)
		{
			for(int j=0; j<7; j++)
			{
				fp[i][j] = flightpath[i][j];
			}
		}

		LineString drone_path = LineString.fromLngLats(drone_path_coordinates);
		Feature feature = Feature.fromGeometry(drone_path);
		
		//add to feature list
		features_list.add(feature);
		
		//creating geojson feature collection
		FeatureCollection dronepath_features = FeatureCollection.fromFeatures(features_list);
		String dronepath = dronepath_features.toJson();

		return dronepath;
	}
	
	// function to return flight path of the drone
	public String[][] flight_path()
	{
		return fp;
	}
}
