package com.beproj.bikenav;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

public class GMapV2Direction {
    public final static String MODE_DRIVING = "driving";
    public final static String MODE_WALKING = "walking";
    public Document returningDoc;
    public Context ctxt;
	public ProgressDialog myDialog;

    public GMapV2Direction() { }

    public Document getDocument(LatLng start, LatLng end, String mode, MapActivity activity) {
        String url = "http://maps.googleapis.com/maps/api/directions/xml?" 
                + "origin=" + start.latitude + "," + start.longitude  
                + "&destination=" + end.latitude + "," + end.longitude 
                + "&sensor=false&units=metric&mode="+mode;

        ctxt = activity;
        DirectionsAsync task = new DirectionsAsync();
        task.execute(url);
        
        Document result;
		try {
			result = task.get();
	        return result;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.i("ARrrrrrrrrrrr", "Nai thayu bc!");
        return null;
    }

    public String getDurationText (Document doc) {
        NodeList nl1 = doc.getElementsByTagName("duration");
        Node node1 = nl1.item(nl1.getLength() - 1);
        NodeList nl2 = node1.getChildNodes();
        Node node2 = nl2.item(getNodeIndex(nl2, "text"));
        Log.i("DurationText", node2.getTextContent());
        return node2.getTextContent();
    }

    public int getDurationValue (Document doc) {
        NodeList nl1 = doc.getElementsByTagName("duration");
        Node node1 = nl1.item(nl1.getLength() - 1);
        NodeList nl2 = node1.getChildNodes();
        Node node2 = nl2.item(getNodeIndex(nl2, "value"));
        Log.i("DurationValue", node2.getTextContent());
        return Integer.parseInt(node2.getTextContent());
    }

    public String getDistanceText (Document doc) {
        NodeList nl1 = doc.getElementsByTagName("distance");
        Node node1 = nl1.item(nl1.getLength() - 1);
        NodeList nl2 = node1.getChildNodes();
        Node node2 = nl2.item(getNodeIndex(nl2, "text"));
        Log.i("DistanceText", node2.getTextContent());
        return node2.getTextContent();
    }

    public int getDistanceValue (Document doc) {
        NodeList nl1 = doc.getElementsByTagName("distance");
        Node node1 = nl1.item(nl1.getLength() - 1);
        NodeList nl2 = node1.getChildNodes();
        Node node2 = nl2.item(getNodeIndex(nl2, "value"));
        Log.i("DistanceValue", node2.getTextContent());
        return Integer.parseInt(node2.getTextContent());
    }

    public String getStartAddress (Document doc) {
        NodeList nl1 = doc.getElementsByTagName("start_address");
        Node node1 = nl1.item(0);
        Log.i("StartAddress", node1.getTextContent());
        return node1.getTextContent();
    }

    public String getEndAddress (Document doc) {
        NodeList nl1 = doc.getElementsByTagName("end_address");
        Node node1 = nl1.item(0);
        Log.i("StartAddress", node1.getTextContent());
        return node1.getTextContent();
    }
    
    public String getSummary (Document doc) {
        NodeList nl1 = doc.getElementsByTagName("summary");
        Node node1 = nl1.item(0);
        Log.i("Summary", node1.getTextContent());
        return node1.getTextContent();
    }

    public String getCopyRights (Document doc) {
        NodeList nl1 = doc.getElementsByTagName("copyrights");
        Node node1 = nl1.item(0);
        Log.i("CopyRights", node1.getTextContent());
        return node1.getTextContent();
    }

    public ArrayList<LatLng> getDirection (Document doc) {
        NodeList nl1, nl2, nl3;
        ArrayList<LatLng> listGeopoints = new ArrayList<LatLng>();
        nl1 = doc.getElementsByTagName("step");
        if (nl1.getLength() > 0) {
            for (int i = 0; i < nl1.getLength(); i++) {
                Node node1 = nl1.item(i);
                nl2 = node1.getChildNodes();

                Node locationNode = nl2.item(getNodeIndex(nl2, "start_location"));
                nl3 = locationNode.getChildNodes();
                Node latNode = nl3.item(getNodeIndex(nl3, "lat"));
                double lat = Double.parseDouble(latNode.getTextContent());
                Node lngNode = nl3.item(getNodeIndex(nl3, "lng"));
                double lng = Double.parseDouble(lngNode.getTextContent());
                listGeopoints.add(new LatLng(lat, lng));

                locationNode = nl2.item(getNodeIndex(nl2, "polyline"));
                nl3 = locationNode.getChildNodes();
                latNode = nl3.item(getNodeIndex(nl3, "points"));
                ArrayList<LatLng> arr = decodePoly(latNode.getTextContent());
                for(int j = 0 ; j < arr.size() ; j++) {
                    listGeopoints.add(new LatLng(arr.get(j).latitude, arr.get(j).longitude));
                }

                locationNode = nl2.item(getNodeIndex(nl2, "end_location"));
                nl3 = locationNode.getChildNodes();
                latNode = nl3.item(getNodeIndex(nl3, "lat"));
                lat = Double.parseDouble(latNode.getTextContent());
                lngNode = nl3.item(getNodeIndex(nl3, "lng"));
                lng = Double.parseDouble(lngNode.getTextContent());
                listGeopoints.add(new LatLng(lat, lng));
            }
        }

        return listGeopoints;
    }

    private int getNodeIndex(NodeList nl, String nodename) {
        for(int i = 0 ; i < nl.getLength() ; i++) {
            if(nl.item(i).getNodeName().equals(nodename))
                return i;
        }
        return -1;
    }

    private ArrayList<LatLng> decodePoly(String encoded) {
        ArrayList<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng position = new LatLng((double) lat / 1E5, (double) lng / 1E5);
            poly.add(position);
        }
        return poly;
    }
    
    public ArrayList<RouteStep> getItenary(Document doc)
    {
    	ArrayList<RouteStep> list = new ArrayList<RouteStep>();
    	NodeList steps = doc.getElementsByTagName("step");
		NodeList itemChildren;
		Node currentItem;
		Node currentChild;
		for(int i=0; i<steps.getLength();i++)
		{
			String duration = " 0 min";
			String distance = " 0 m";
			String instructions = "Go nowhere.";
			String maneuver = "NONE";
			LatLng start = null, end = null;
			currentItem = steps.item(i);
			itemChildren = currentItem.getChildNodes();
			for(int j=0; j<itemChildren.getLength(); j++)
			{
				currentChild = itemChildren.item(j);
				//Log.i("Item", currentChild.getNodeName()+"");
				if(currentChild.getNodeName().equalsIgnoreCase("html_instructions"))
				{
					instructions = currentChild.getTextContent()+"";
					Spanned result = Html.fromHtml(instructions);
					instructions = result+"";
					instructions = instructions.replaceAll("\\n+",".\n").trim();
					Log.i("Child Debug", instructions);
				}
				if(currentChild.getNodeName().equalsIgnoreCase("duration"))
				{
					NodeList secondChildren = currentChild.getChildNodes();
					Node currentSecond = secondChildren.item(1);
					duration = currentSecond.getTextContent()+"";
					Log.i("Child Debug", duration);
				}
				if(currentChild.getNodeName().equalsIgnoreCase("distance"))
				{
					NodeList secondChildren = currentChild.getChildNodes();
					Node currentSecond = secondChildren.item(1);
					distance = currentSecond.getTextContent()+"";
					Log.i("Child Debug", distance);
				}
				if(currentChild.getNodeName().equalsIgnoreCase("maneuver"))
				{
					maneuver = currentChild.getTextContent()+"";
					Log.i("Child Debug", maneuver);
				}
				if(currentChild.getNodeName().equalsIgnoreCase("start_location"))
				{
	                NodeList childSubs = currentChild.getChildNodes();
	                Node latNode = childSubs.item(getNodeIndex(childSubs, "lat"));
	                double lat = Double.parseDouble(latNode.getTextContent());
	                Node lngNode = childSubs.item(getNodeIndex(childSubs, "lng"));
	                double lng = Double.parseDouble(lngNode.getTextContent());
	                start = new LatLng(lat, lng);
				}
				if(currentChild.getNodeName().equalsIgnoreCase("end_location"))
				{
	                NodeList childSubs = currentChild.getChildNodes();
	                Node latNode = childSubs.item(getNodeIndex(childSubs, "lat"));
	                double lat = Double.parseDouble(latNode.getTextContent());
	                Node lngNode = childSubs.item(getNodeIndex(childSubs, "lng"));
	                double lng = Double.parseDouble(lngNode.getTextContent());
	                end = new LatLng(lat, lng);
				}
				
			}
			list.add(new RouteStep(duration, distance, instructions, maneuver, start, end));
		}
		return list;
    }
    
    
    //ASYNC TASK STARTS HERE
    public class DirectionsAsync extends AsyncTask<String, Integer, Document>{

		ProgressDialog myDialog;
		Context ct;
		@Override
		protected Document doInBackground(String... params) {
			// TODO Auto-generated method stub
			String url = params[0];
			try {
	            HttpClient httpClient = new DefaultHttpClient();
	            HttpContext localContext = new BasicHttpContext();
	            HttpPost httpPost = new HttpPost(url);
	            HttpResponse response = httpClient.execute(httpPost, localContext);
	            InputStream in = response.getEntity().getContent();
	            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	            Document doc = builder.parse(in);
	            Log.i("OH Feq", "Chaalech");
	            return doc;
	        } catch (Exception e) {
	            e.printStackTrace();
	            Log.i("OH Feq", "nope buddy!");
	        }
	        return null;
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute(); 
			myDialog = ProgressDialog.show(ctxt, "" , "Loading directions...", true);
			//myDialog.setMessage("Loading directions...");
		}

		@Override
		protected void onPostExecute(Document result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			if(myDialog.isShowing())
				myDialog.cancel();
		}
		
		
    }
}