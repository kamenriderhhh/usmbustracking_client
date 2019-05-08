package wow.usmbustracking_client;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

import static android.content.ContentValues.TAG;

public class GeofenceTransitionsIntentService extends IntentService{
    public static final int ENTRY = 1;
    public static final int EXIT = 2;
    public static final int ERROR = 3;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @parameter name Used to name the worker thread, important only for debugging.
     */
    public GeofenceTransitionsIntentService() {
        super(GeofenceTransitionsIntentService.class.getSimpleName());
    }

    protected void onHandleIntent(Intent intent) {
        final ResultReceiver receiver = MainActivity.nearBusStopReceiver;
        Bundle bundle = new Bundle();
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            //String errorMessage = GeofenceErrorMessages.getErrorString(this, geofencingEvent.getErrorCode());
            Log.e(TAG, "geofence error");
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER
            /* || geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT */) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            bundle.putString("nearBusStop", triggeringGeofences.get(0).getRequestId());
            receiver.send(ENTRY, bundle);
            // Get the transition details as a String.
            /*
            String geofenceTransitionDetails = getGeofenceTransitionDetails(
                    this,
                    geofenceTransition,
                    triggeringGeofences
            );
            */

            // Send notification and log the transition details.
            //sendNotification(geofenceTransitionDetails);
            //Log.i(TAG, geofenceTransitionDetails);
            Log.d("Enter", triggeringGeofences.get(0).getRequestId());
        }
        else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT)
        {
            receiver.send(EXIT, bundle);
            Log.d("Exit", "exit");
        }
        else {
            // Log the error.
            Log.d(TAG, "geofence error"/*getString(R.string.geofence_transition_invalid_type, geofenceTransition)*/);
            geofencingEvent.hasError();
            geofencingEvent.getErrorCode();
            bundle.putString("Error", "errir");
            receiver.send(ERROR, bundle);

        }
    }
}
