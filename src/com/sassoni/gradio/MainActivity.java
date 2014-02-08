package com.sassoni.gradio;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import com.sassoni.gradio.MusicService.PlayerState;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaMetadataRetriever;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

public class MainActivity extends Activity {
	String theUrl;
	TableLayout layout; 
	private String currentTag; 

	private Intent musicServiceIntent;
	private MenuItem editStationMenuItem;
	SharedPreferences settings;
	private static final String SETTINGS_FILE = "GradioSettingsFile";

	private static ButtonMode BUTTON_MODE;
	public enum ButtonMode {
		EDIT, PLAY, STOP
	}

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context content, Intent intent) {

			PlayerState pState = (PlayerState) intent.getSerializableExtra(MusicService.PLAYER_STATE_KEY);

			switch(pState) {
			case ERROR:
				Toast.makeText(getApplicationContext(), "Streaming Error!", 
						Toast.LENGTH_LONG).show();
				break;
			case START:
				BUTTON_MODE = ButtonMode.STOP; 
				// Disable all buttons except the one playing
				for (int i = 0; i < layout.getChildCount(); i++) {
					final TableRow row = (TableRow) layout.getChildAt(i);
					for (int j = 0; j < row.getChildCount(); j++) {
						final View child = row.getChildAt(j);
						if (child instanceof Button && child.getTag().toString() != currentTag) {
							child.setEnabled(false);
						}
					} 
				}
				editStationMenuItem.setEnabled(false);
				break;
			case STOP:
				BUTTON_MODE = ButtonMode.PLAY;
				// Enable all buttons again
				for (int i = 0; i < layout.getChildCount(); i++) {
					final TableRow row = (TableRow) layout.getChildAt(i);
					for (int j = 0; j < row.getChildCount(); j++) {
						final View child = row.getChildAt(j);
						if (child instanceof Button) {
							child.setEnabled(true);
						}
					} 
				}
				editStationMenuItem.setEnabled(true);
				break;
			default:
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (!LibsChecker.checkVitamioLibs(this)) {
			Toast.makeText(getApplicationContext(), "No Vitamio Libs!", 
					Toast.LENGTH_LONG).show();
			return;
		}

		setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		BUTTON_MODE = ButtonMode.PLAY;

		settings = getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE);	

		layout = (TableLayout) findViewById(R.id.main_table_layout);

		refreshStationNames();
	}

	@Override
	protected void onResume() {
		super.onResume();
		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
				new IntentFilter(MusicService.BROADCAST_ACTION));
	}

	@Override
	protected void onPause() {
		super.onPause();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_actions, menu);
		editStationMenuItem = menu.getItem(0);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {  // Maybe we will add more settings later
		case R.id.edit_station_action:
			if (BUTTON_MODE != ButtonMode.EDIT) {
				BUTTON_MODE = ButtonMode.EDIT;
				setButtonColors(R.drawable.btn_selector_edit_mode);			
			} else {
				BUTTON_MODE = ButtonMode.PLAY;
				setButtonColors(R.drawable.btn_selector);
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void editPlayStop(View view) {
		String tag = view.getTag().toString();

		switch(BUTTON_MODE) {
		case EDIT:
			editStation(tag);
			break;
		case PLAY:
			playStation(tag);
			break;
		case STOP:
			stopStation();
			break;
		default:
			break;
		}
	}

	private void editStation(final String tag) {
		LayoutInflater inflater = getLayoutInflater();
		final View dialoglayout = inflater.inflate(R.layout.edit_station, (ViewGroup) getCurrentFocus());

		final EditText editName = (EditText) dialoglayout.findViewById(R.id.edit_station_name);
		final EditText editUrl = (EditText) dialoglayout.findViewById(R.id.edit_station_url);

		// Check if this station has existing settings
		String existingName = settings.getString(tag + "_name", null);
		String existingUrl = settings.getString(tag + "_url", null);

		// And show them
		if (existingName != null) { 
			editName.setText(existingName); 
		}
		if (existingUrl != null) { 
			editUrl.setText(existingUrl); 
		}

		// Show a dialog with text fields
		AlertDialog alertDialogBuilder = new AlertDialog.Builder(this)
		.setTitle("Edit station")
		.setView(dialoglayout)
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int whichButton) {
				String newName = editName.getText().toString();
				String newUrl = editUrl.getText().toString();

				// Save them
				settings.edit().putString(tag + "_name", newName).commit();
				settings.edit().putString(tag + "_url", newUrl).commit();

				// And refresh the interface
				refreshStationNames();
			}
		}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int whichButton) {
				// Do nothing.
			}
		}).show();
	}

	private void playStation(String tag) {
		currentTag = tag;
		musicServiceIntent = new Intent(this, MusicService.class);

		String name = settings.getString(tag + "_name", null);
		String url = settings.getString(tag + "_url", null);

		// Can't play with empty name or url
		if (isEmpty(url) || isEmpty(name)) {
			Toast.makeText(getApplicationContext(), "Station name or url is empty!", 
					Toast.LENGTH_LONG).show();
		} else {theUrl = url;
		musicServiceIntent.putExtra(MusicService.STATION_NAME_KEY, name);
		musicServiceIntent.putExtra(MusicService.STATION_URL_KEY, url);
		startService(musicServiceIntent); 
		}
	}

	private void stopStation() {
		stopService(musicServiceIntent);
	}

	private void refreshStationNames() {

		for (int i = 0; i < layout.getChildCount(); i++) {
			final TableRow row = (TableRow) layout.getChildAt(i);
			for (int j = 0; j < row.getChildCount(); j++) {
				final View child = row.getChildAt(j);
				if (child instanceof Button) {
					String name = settings.getString(child.getTag().toString() + "_name", null); 
					if (name != null) {
						((Button) child).setText(name);
					}
				}
			} 
		}
	}

	private void setButtonColors(int drawableId) {
		for (int i = 0; i < layout.getChildCount(); i++) {
			final TableRow row = (TableRow) layout.getChildAt(i);
			for (int j = 0; j < row.getChildCount(); j++) {
				final View child = row.getChildAt(j);
				if (child instanceof Button) {
					child.setBackgroundResource(drawableId);
				}
			} 
		}
	}

	private boolean isEmpty(String string) {
		if (string == null) {
			return true;
		} else {
			string.replace(" ", "");
			if (string.length() == 0) {
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public void onBackPressed() {
		this.moveTaskToBack(true);
	}

}
