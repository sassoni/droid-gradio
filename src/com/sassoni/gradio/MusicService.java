package com.sassoni.gradio;

import java.io.IOException;

import io.vov.vitamio.MediaMetadataRetriever;
import io.vov.vitamio.MediaPlayer;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

// Put player.start() in a separate thread?

public class MusicService extends Service {

	private static final int NOTIFICATION_ID = R.id.notification;

//	private NotificationManager notificationManager;
	public static String BROADCAST_ACTION = "com.sassoni.gradio.displayevent";
	LocalBroadcastManager localBroadcastManager;
	private Notification notification;
	private MediaPlayer mediaPlayer;
	private String stationChosen;
	Intent broadcastIntent;
	Context context;
	public static String STATION_NAME_KEY = "station_name_key";
	public static String STATION_URL_KEY = "station_url_key";
	public static String PLAYER_STATE_KEY = "player_state_intent_key";
	public enum PlayerState {
		ERROR, START, STOP
	}
	
	@Override
	public void onCreate() {
		
//		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		localBroadcastManager = LocalBroadcastManager.getInstance(this);
		broadcastIntent = new Intent(BROADCAST_ACTION);	
		
		mediaPlayer = new MediaPlayer(this);
		mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				broadcastIntent.putExtra(PLAYER_STATE_KEY, PlayerState.ERROR);
				localBroadcastManager.sendBroadcast(broadcastIntent);
//		    	mediaPlayer.reset();
				return true;
			}	
		} );
		mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {	    
			@Override
		    public void onPrepared(MediaPlayer mp) {
		    	mediaPlayer.start();
		    	broadcastIntent.putExtra(PLAYER_STATE_KEY, PlayerState.START);
				localBroadcastManager.sendBroadcast(broadcastIntent);
		    	showNotification(); 
		    }
		});
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		final String url = intent.getExtras().getString(STATION_URL_KEY);
		stationChosen = intent.getExtras().getString(STATION_NAME_KEY);
		
		mediaPlayer.reset();
		try {
			mediaPlayer.setDataSource(url);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		mediaPlayer.prepareAsync();
		// Put buffering broadcast here
		
		return START_NOT_STICKY;
	}

    private void stopPlayer() {
    	broadcastIntent.putExtra(PLAYER_STATE_KEY, PlayerState.STOP);
		localBroadcastManager.sendBroadcast(broadcastIntent);
    	
    	if (mediaPlayer != null) {
    		if (mediaPlayer.isPlaying()) {
    			mediaPlayer.stop();
    		}
//    		mediaPlayer.reset();
    		mediaPlayer.release();
    		mediaPlayer = null;
    	}	
    }
	
	@Override
	public void onDestroy() {
		stopPlayer();
		// notificationManager.cancel(NOTIFICATION);
	}

	private void showNotification() {
		NotificationCompat.Builder builder = new Builder(this);
		builder.setSmallIcon(R.drawable.ic_launcher);
		builder.setContentTitle("Gradio");
		builder.setContentText("Playing " + stationChosen);
		Intent notificationIntent = new Intent(this, MainActivity.class);
		PendingIntent pi = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		builder.setContentIntent(pi);
		notification = builder.build();
		// notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;
		// notification.flags |= Notification.FLAG_NO_CLEAR;
		// notification.flags |= Notification.FLAG_ONGOING_EVENT;
 
		startForeground(NOTIFICATION_ID, notification);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}