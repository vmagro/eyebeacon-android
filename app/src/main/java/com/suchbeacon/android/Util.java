package com.suchbeacon.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.suchbeacon.android.activities.MainActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by vmagro on 2/15/14.
 */
public class Util {

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static void toTheCloudAsync(final Context context, final int major, final int minor) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    String account = PreferenceManager.getDefaultSharedPreferences(context).getString("account", null);
                    Log.i("cloud", "account = " + account);
                    String token = GoogleAuthUtil.getToken(context, account, Constants.SCOPE);
                    String url = "http://suchbeacon.com/content?majorId=" + major + "&minorId=" + minor + "&email=" + account + "&accessToken=" + token;
                    Log.i("cloud", "url = " + url);
                    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                    Log.i("cloud", "response code = " + connection.getResponseCode());

                     BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String data = "";
                    String line;
                    while ((line = reader.readLine()) != null) {
                        data += line;
                    }

                    JSONObject json = new JSONObject(data).getJSONObject("data");
                    String name = json.getString("name");
                    String imageUrl = json.getString("imageUrl");
                    String pTitle= "eyeBeacon";
                    String pBody = "eyeBeacon: ";
                    String template = json.getString("template");
                    if(template.equals("intro")){
                        pTitle= name;
                        pBody = pBody.concat(name);
                    }
                    else if(template.equals("info")){
                        JSONArray infos = json.getJSONArray("infos");
                        pTitle= name;
                        pBody = pBody.concat(((JSONObject)infos.get(0)).getString("desc"));
                    }
                    else if(template.equals("payment")){
                        pTitle= name;
                        pBody = "Location: " + json.getString("location")+ "\n";
                        pBody = pBody.concat("Price: $" + json.getString("price") + "\n");
                        pBody = pBody.concat("\n"+json.getString("description"));
                    }
                    Notification notif = new Notification.Builder(context)
                            .setContentTitle(name)
                            .setContentText("Beacon nearby " + major + ":" + minor)
                            .setSmallIcon(R.drawable.notif_small)
//                            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop searching", Util.getStopServicePendingIntent(context))
                            .build();
                    Intent notificationIntent = new Intent(context.getApplicationContext(), MainActivity.class);
                    PendingIntent contentIntent = PendingIntent.getActivity(context.getApplicationContext(), 0, notificationIntent, 0);
                    notif.contentIntent = contentIntent;
                    NotificationManager notificationMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationMgr.notify(3309, notif);

                    Intent i = new Intent("com.getpebble.action.SEND_NOTIFICATION");

                    final Map pebbleData = new HashMap();
                    pebbleData.put("title", pTitle);
                    pebbleData.put("body", pBody);
                    final JSONObject jsonData = new JSONObject(pebbleData);
                    final String notificationData = new JSONArray().put(jsonData).toString();

                    i.putExtra("messageType", "PEBBLE_ALERT");
                    i.putExtra("sender", "EyeBeacon");
                    i.putExtra("notificationData", notificationData);
                    Log.i("pebble", "Sending notification to pebble");

                    /* Send to activity */
                    Intent intent = new Intent("BeaconInfo");
                    // You can also include some extra data.
                    intent.putExtra("Name", name);
//                    Bundle b = new Bundle();
//                    b.putParcelable("Location", l);
//                    intent.putExtra("Location", b);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                    context.sendBroadcast(i);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (GoogleAuthException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    public static PendingIntent getStopServicePendingIntent(Context context) {
        Intent intent = new Intent("com.suchbeacon.android.stop");
        intent.setClass(context.getApplicationContext(), StopServiceReceiver.class);
        return PendingIntent.getBroadcast(context.getApplicationContext(), 23461245, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static PendingIntent getScanPendingIntent(Context context) {
        Intent serviceIntent = new Intent(context, BeaconMonitor.class);
        return PendingIntent.getService(context, 7824, serviceIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

}
