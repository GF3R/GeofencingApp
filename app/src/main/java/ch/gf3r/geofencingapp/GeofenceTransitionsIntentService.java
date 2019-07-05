package ch.gf3r.geofencingapp;
import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

public class GeofenceTransitionsIntentService extends IntentService {
    protected static final String TAG = "GeofenceTransitionsIS";
    Context mContext;
    GeoFenceTransitionHandler geoFenceTransitionHandler;

    public GeofenceTransitionsIntentService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mContext = this;
        geoFenceTransitionHandler = new GeoFenceTransitionHandler(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mContext = getApplicationContext();
        return super.onStartCommand(intent, flags, startId);
    }

    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = "" + geofencingEvent.getErrorCode();
            Log.e(TAG, errorMessage);
            return;
        }

        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
        Location loc = geofencingEvent.getTriggeringLocation();
        LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
        String name = getGeofenceIds(triggeringGeofences);
        switch(geofencingEvent.getGeofenceTransition()){
            case Geofence.GEOFENCE_TRANSITION_ENTER :
                sendNotification("ENTERED " + name);
                geoFenceTransitionHandler.HandleTransition(true, name, latLng);
                return;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                name = getGeofenceIds(triggeringGeofences);
                sendNotification("EXITED " + name);
                geoFenceTransitionHandler.HandleTransition(false, name, latLng);
                return;
            default:
                System.out.println("error while transitioning");
                return;

        }
    }

    private String getGeofenceIds(List<Geofence> triggeringGeofences) {
        ArrayList<String> triggeringGeofencesIdsList = new ArrayList<>();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());

        }
        String triggeringGeofencesIdsString = TextUtils.join(", ",  triggeringGeofencesIdsList);
        return triggeringGeofencesIdsString;
    }

    private void sendNotification(String notificationDetails) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy hh:mm");
        String timestamp = simpleDateFormat.format(new Date());
        String CHANNEL_ID = "com.ch.bfh";
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(mChannel);
        }
        Intent notificationIntent = new Intent(getApplicationContext(), MapsActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MapsActivity.class);
        stackBuilder.addNextIntent(notificationIntent);
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.common_google_signin_btn_icon_light)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.common_full_open_on_phone))
                .setColor(Color.BLUE)
                .setContentTitle(notificationDetails)
                .setContentText(timestamp)
                .setContentIntent(notificationPendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID); // Channel ID
        }
        builder.setAutoCancel(true);
        mNotificationManager.notify(0, builder.build());
    }


}