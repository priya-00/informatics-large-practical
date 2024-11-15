package uk.ac.ed.inf.aqmaps;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.*;
import com.mapbox.geojson.*;
import java.util.*;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;

/**
 * Hello world!
 *
 */
public class App 
{
	
	public static void main( String[] args ) throws Exception
	{
		// input
		String day = args[0];
    	String month = args[1];
    	String year = args[2];
    	double start_lat = Double.parseDouble(args[3]);
    	double start_lng = Double.parseDouble(args[4]);
    	long seed = Long.parseLong(args[5]);
    	String port = args[6];
    	
		String urlString = "http://localhost:"+port+"/maps/"+year+"/"+month+"/"+day+"/air-quality-data.json";

		dronePath dp = new dronePath();
		String readings_geojson = dp.find_dronePath(urlString, start_lng, start_lat, port);
		String[][] flightpath = dp.flight_path();
		
		// converting flightpath array into string
		String flightpath_txt = "";
		for(String[] row : flightpath)
		{
			flightpath_txt = flightpath_txt + String.join(",", row) + "\n";
		}
		
		// create required output files
		create_file("readings-"+day+"-"+month+"-"+year+".geojson",readings_geojson);
		create_file("flightpath-"+day+"-"+month+"-"+year+".txt",flightpath_txt);

	}//main

	public static void create_file(String filename, String file_content)
	{
		//creating and writing readings_geojson to readings-DD-MM-YYYY.geojson file
		try
		{
			File file = new File(filename);
			file.createNewFile();
			System.out.println(file.getName() + " file created.");
		}
		catch(IOException e)
		{
			System.out.println("An error occured.");
			e.printStackTrace();
		}

		try
		{
			FileWriter myWriter = new FileWriter(filename);
			myWriter.write(file_content);
			myWriter.close();
		}
		catch(IOException e)
		{
			System.out.println("An error occured.");
			e.printStackTrace();
		}
	}//create_file

}//class
