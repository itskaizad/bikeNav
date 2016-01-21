package com.beproj.bikenav;

import com.google.android.gms.maps.model.LatLng;

public class RouteStep {
	
	String duration;
	String distance;
	String instructions;
	String maneuver;
	LatLng start;
	LatLng end;
	
	
	
	public RouteStep(String duration, String distance, String instructions,
			String maneuver, LatLng start, LatLng end) {
		super();
		this.duration = duration;
		this.distance = distance;
		this.instructions = instructions;
		this.maneuver = maneuver;
		this.start = start;
		this.end = end;
	}

	public LatLng getStart() {
		return start;
	}

	public LatLng getEnd() {
		return end;
	}

	public String getDuration() {
		return duration;
	}

	public String getDistance() {
		return distance;
	}

	public String getInstructions() {
		return instructions;
	}

	public String getManeuver() {
		return maneuver;
	}

}
