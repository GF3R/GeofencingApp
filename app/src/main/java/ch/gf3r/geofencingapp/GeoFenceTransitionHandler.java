package ch.gf3r.geofencingapp;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class GeoFenceTransitionHandler {

    private Context context;
    private String Url = "https://geofencinggrothoff.azurewebsites.net/api/handlepost";

    GeoFenceTransitionHandler(Context context){
        this.context = context;
    }

    public void HandleTransition(boolean isEntry, String name, LatLng latLng){
        sendPost(Url, latLng, name, isEntry);
        switch(name){
            case "Home":
                changeWifiStatus(isEntry);
                return;
            case "Work":
                return;
        }
    }

    public static void sendPost(String urlAddress, final LatLng latLng, String name, final boolean isEnter) {
        final String urlAdr  = urlAddress;
        final String nameStr = name;
        final LatLng latLngGeo = latLng;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(urlAdr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept","application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("timestamp", System.currentTimeMillis()/1000);
                    jsonParam.put("GeoFenceName", nameStr);
                    jsonParam.put("latitude", latLngGeo.latitude);
                    jsonParam.put("longitude", latLngGeo.longitude);
                    if(isEnter) {
                        jsonParam.put("status","ENTERED");
                    }else{
                        jsonParam.put("status","EXITED");
                    }
                    Log.i("JSON", jsonParam.toString());
                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                    os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                    os.flush();
                    os.close();

                    Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                    Log.i("MSG" , conn.getResponseMessage());

                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }

    private void changeWifiStatus(boolean status){
        WifiManager wifiManager = (WifiManager)this.context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(status);
    }


}
