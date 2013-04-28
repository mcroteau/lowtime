package org.agius.lowtime;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;

import org.agius.lowtime.domain.LowtimeSettings;

import static org.agius.lowtime.LowtimeConstants.*;


@SuppressLint("SimpleDateFormat")
public class HomeIntent  extends Activity{

	private TextView alarm;
	private TextView range;
	private TextView waketone;
	private TextView snooze;
	private TextView timerange;
	
	private TextView status;
	
	private TableRow valuesRow;
	private TableRow welcomeRow;
	
	
	private Button hideButton;
	private Button toggleButton;
	private Button createButton;
	
	private LowtimeSettings settings;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);
        
    	if(serviceRunning())
        	System.out.println("running");
    	
        settings = new LowtimeSettings(getSharedPreferences(LOWTIME_SETTINGS, 0));

        alarm = (TextView) findViewById(R.id.alarm);
        range = (TextView) findViewById(R.id.range);
        waketone = (TextView) findViewById(R.id.waketone);
        snooze = (TextView) findViewById(R.id.snooze);
        timerange = (TextView) findViewById(R.id.timerange);
        
        status = (TextView) findViewById(R.id.status);
        welcomeRow = (TableRow) findViewById(R.id.welcome_row);
        valuesRow = (TableRow) findViewById(R.id.values_row);
        
        
        resetValues();
        resetValuesUnset();
    	initializeLowtimeButtons();
    	reinitializeView();
    	
    }
    

    
    @Override
    protected void onStart() {
        super.onStart();  
    	settings.reinitialize(getSharedPreferences(LOWTIME_SETTINGS, 0));
    	if(settings.settingsSet()){
    		if(settings.isActive()){
        		setActiveViewState();
        		setSettingsValues();
    			restartService();
    		}else{
        		setInactiveViewState();
    		}
    	}else{
    		setInactiveViewState();
    	}
    }
    
    
    
    private void restartService(){
        stopService(new Intent(HomeIntent.this, LowtimeService.class));
        startService(new Intent(HomeIntent.this, LowtimeService.class));
    }
    
    
    private void displayLowtimeSetView(){
    	
    	valuesRow.setVisibility(View.VISIBLE);
    	welcomeRow.setVisibility(View.GONE);
    	
    	setSettingsValues();
        
    	if(settings.isActive()){
    		setActiveViewState();
    	}else{
    		setInactiveViewState();
    	}
    	
    }
    
    
    private void setSettingsValues(){
        Calendar lowtimeCalendar = Calendar.getInstance();
        lowtimeCalendar.set(Calendar.HOUR_OF_DAY, settings.getHour());
        lowtimeCalendar.set(Calendar.MINUTE, settings.getMinutes());
        
        Calendar lowtimeBackCalendar = Calendar.getInstance();
        lowtimeBackCalendar.set(Calendar.HOUR_OF_DAY, settings.getHour());
        lowtimeBackCalendar.set(Calendar.MINUTE, settings.getMinutes());;
        lowtimeBackCalendar.add(Calendar.MINUTE, (-settings.getRange()));
        
        
        Date date = lowtimeCalendar.getTime();
        Date datePre = lowtimeBackCalendar.getTime();
        String formattedTime = new SimpleDateFormat("hh:mm a").format(date);	
        String formattedTimePre = new SimpleDateFormat("hh:mm a").format(datePre);     
        
        alarm.setText(formattedTime);
        alarm.setBackgroundColor(Color.TRANSPARENT);
        
        range.setText(settings.getRange() + " Minutes");
        range.setBackgroundColor(Color.TRANSPARENT);
        
        waketone.setText(settings.getWaketone());
        waketone.setBackgroundColor(Color.TRANSPARENT);
        
        snooze.setText(settings.getSnoozeDuration() + " Minutes");
        snooze.setBackgroundColor(Color.TRANSPARENT);
        
        timerange.setText(formattedTimePre + "-" + formattedTime);
        timerange.setBackgroundColor(Color.TRANSPARENT);
    }
    
    
    private void displayLowtimeUnsetView(){

    	valuesRow.setVisibility(View.GONE);
    	welcomeRow.setVisibility(View.VISIBLE);
    	
        if(settings.isActive()){
    		setActiveViewState();
    	}else{
    		setInactiveViewState();
    	}
        
    }
    
    
    private void setActiveViewState(){
        status.setText(LOWTIME_ACTIVE_LABEL);
        status.setBackgroundColor(Color.parseColor(ACTIVE_COLOR));
        toggleButton.setText(LOWTIME_BUTTON_DISABLE);
    }
    
    
    private void setInactiveViewState(){
    	status.setText(LOWTIME_INACTIVE_LABEL);
    	status.setBackgroundColor(Color.parseColor(INACTIVE_COLOR));
        toggleButton.setText(LOWTIME_BUTTON_ENABLE);
    }
    
    
    private void resetValues(){
    	settings.resetValues();
    }
    
    
    private void reinitializeView(){
    	if(settings.settingsSet()){
        	displayLowtimeSetView();
        	setAlarm();
        }else{
        	displayLowtimeUnsetView();
        }
    }
    
    
    private void setAlarm(){

        Calendar cal = Calendar.getInstance();
        
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.HOUR_OF_DAY, settings.getHour());
        cal.set(Calendar.MINUTE, settings.getMinutes());
        
        if(cal.getTimeInMillis() < System.currentTimeMillis()){
            cal.add(Calendar.DATE, 1);  
        }
 
        Intent intent = new Intent(this, AlarmIntent.class);
        intent.putExtra("onetime", Boolean.TRUE);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(this, ALARM_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager =  (AlarmManager)getSystemService(Activity.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
        
    }
    
    
    private void updateStatus(){

    	if(serviceRunning())
    		stopService(new Intent(HomeIntent.this, LowtimeService.class));
    	
    	String statusString = LOWTIME_ACTIVE_LABEL;
    	String buttonText = LOWTIME_BUTTON_DISABLE;
    	if(settings.isActive()){
    		settings.setActive(false);
    		statusString = LOWTIME_INACTIVE_LABEL;
    		status.setBackgroundColor(Color.parseColor(INACTIVE_COLOR));
    		buttonText = LOWTIME_BUTTON_ENABLE;
        	stopService(new Intent(HomeIntent.this, LowtimeService.class));
    	}else{
    		settings.setActive(true);
    		status.setBackgroundColor(Color.parseColor(ACTIVE_COLOR));
        	startService(new Intent(HomeIntent.this, LowtimeService.class));
    	}    	

    	status.setText(statusString);
        toggleButton.setText(buttonText);
        
        settings.commit();
    }
    
    
    private void resetValuesUnset(){
    	
        alarm.setText(UNSET);
        alarm.setBackgroundColor(Color.parseColor(INACTIVE_COLOR));
        
        range.setText(UNSET);
        range.setBackgroundColor(Color.parseColor(INACTIVE_COLOR));
        
        waketone.setText(UNSET);
        waketone.setBackgroundColor(Color.parseColor(INACTIVE_COLOR));
        
        snooze.setText(UNSET);
        snooze.setBackgroundColor(Color.parseColor(INACTIVE_COLOR));
        
        timerange.setText(UNSET);   
        timerange.setBackgroundColor(Color.parseColor(INACTIVE_COLOR));
        	
    }
    
    
    private void initializeLowtimeButtons(){
        Button editButton = (Button) findViewById(R.id.editButton);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
			public void onClick(View v) {
                Intent i = new Intent(HomeIntent.this, LowtimeSettingIntent.class);
                startActivity(i);
            }
        });
        
//        hideButton = (Button) findViewById(R.id.hideButton);
//        hideButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//			public void onClick(View v) {
//            	moveTaskToBack(true);
//            }
//        });
        
        
        toggleButton = (Button) findViewById(R.id.toggleButton);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
			public void onClick(View v) {
            	updateStatus();
            }
        });	  
        
        createButton = (Button) findViewById(R.id.createButton);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
			public void onClick(View v) {
                Intent i = new Intent(HomeIntent.this, LowtimeSettingIntent.class);
                startActivity(i);
            }
        });
        
    }
    
    
    /*
     * http://stackoverflow.com/questions/600207/android-check-if-a-service-is-running
     */
    private boolean serviceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LowtimeService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    
    private void showSettings(){
        Intent i = new Intent(HomeIntent.this, LowtimeSettingIntent.class);
        startActivity(i);
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                showSettings();
                return true;
        }
		return false;
    }
    
}
