package com.chat.imapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.chat.imapp.interfaces.OnAsyncTaskListener;
import com.chat.imapp.items.UserDetailItem;
import com.chat.imapp.sessions.Sessions;
import com.chat.imapp.utility.AlertDialogManager;
import com.chat.imapp.utility.AsyncLoadVolley;
import com.chat.imapp.utility.AsyncResponse;
import com.chat.imapp.utility.ConnectionDetector;
import com.chat.imapp.utility.Constant;
import com.chat.imapp.utility.ScreenSize;
import com.chat.imapp.utility.Validate;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
 
public class LoginActivity extends Activity {
	
	protected static final String TAG = null;
	
	private Context context = LoginActivity.this;
	
    // alert dialog manager
    AlertDialogManager alert = new AlertDialogManager();
    
    // Internet detector
    ConnectionDetector cd;
    
    // UI elements
    EditText txtPassword;
    EditText txtEmail;
    
    // Register button
    Button btnLogin;
    
    private ImageView logoImageView1;
    
    private AsyncLoadVolley asyncLoadVolley;
	
	ArrayList<UserDetailItem> list;
	
	private ScreenSize screenSize;
	
	// GCM
	private GoogleCloudMessaging gcm;    
    private String regid;
    
    private String email, password;
	
    private static final int FROM_START = 1;
    private static final int FROM_LOGIN = 2;
    
    @SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      //Initialize TestFlight with your app token.
        if(Sessions.isLoggedIn(context)) {
        	Intent i = new Intent(getApplicationContext(), FriendsListActivity.class);
            startActivity(i);
            finish();
        }
        else
        {
        	requestWindowFeature(Window.FEATURE_NO_TITLE);
        	setContentView(R.layout.login);
        	
            if (checkPlayServices()) {
                gcm = GoogleCloudMessaging.getInstance(this);
                regid = GcmRegistration.getRegistrationId(context);
                
                Log.e(TAG, " 1 -> Reg id : " + regid);
                
                if (regid.isEmpty()) {
                	Log.e(TAG, "  str b4 bg : ");
                    registerInBackgroundMy(FROM_START);
                }
            } else {
                Log.i(TAG, "No valid Google Play Services APK found.");
            }
            
        	String filename = getResources().getString(R.string.login_php);
			asyncLoadVolley = new AsyncLoadVolley(context, filename);		
			asyncLoadVolley.setOnAsyncTaskListener(asyncTaskListener);
		
        cd = new ConnectionDetector(getApplicationContext());
        
        txtEmail = (EditText) findViewById(R.id.loginEmail);
        txtPassword = (EditText) findViewById(R.id.loginPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        
        logoImageView1 = (ImageView) findViewById(R.id.logo_image1);
        
        screenSize = new ScreenSize(context);
		
		int duration = 1300;
        
        Animation animationTop = new TranslateAnimation(0, 0, (int)(screenSize.getScreenHeightPixel()), 0); 
        animationTop.setDuration(duration);
        animationTop.setFillAfter(true);
        animationTop.setInterpolator(new DecelerateInterpolator());
        logoImageView1.setAnimation(animationTop);
        
        list = new ArrayList<UserDetailItem>();
        
        /*
         * Click event on Register button
         * */
        btnLogin.setOnClickListener(new View.OnClickListener() {
             
            @Override
            public void onClick(View arg0) {
                // Read EditText dat
                email = txtEmail.getText().toString();
                password = txtPassword.getText().toString();
                boolean check = true;
            	
            	Validate validate = new Validate();
                
                if(validate.isNotEmpty(email))
        			email = txtEmail.getText().toString();
        		else
        		{
        			email = "";
        			check = false;
        			txtEmail.setError("Please enter your email");
        		}
        		int passwordLength = 1;
        		if(validate.isAtleastValidLength(password, passwordLength))			
        			password = txtPassword.getText().toString();
        		else
        		{
        			password = "";
        			check = false;
        			txtPassword.setError("Please enter a password");
        		}
                
                // Check if user filled the form
                if(check) {
                	
                	if (regid.isEmpty()) {
                		Log.i(TAG, "Regid empty. So register again.");
                        registerInBackgroundMy(FROM_LOGIN);
                    }
        			else {
        				startRegistration();
        			}   
                } else {
                    // user doen't filled that data
                    // ask him to fill the form
                    alert.showAlertDialog(LoginActivity.this, "Login Error!", "Please enter correct email and/or password", false);
                }
            }
        });
        }
    }
    
    private void startRegistration() {
    	btnLogin.setClickable(false);
    	Map<String, String> map = new HashMap<String, String>();
		map.put(Constant.EMAIL, email);
		map.put(Constant.PASSWORD, password);
		map.put(Constant.REGID, GcmRegistration.getRegistrationId(context));
		asyncLoadVolley.setBasicNameValuePair(map);
    	asyncLoadVolley.beginTask();
    	
	}
    
	private boolean checkPlayServices() {
	    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
	    if (resultCode != ConnectionResult.SUCCESS) {
	        if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
	            GooglePlayServicesUtil.getErrorDialog(resultCode, this,
	                    GcmRegistration.PLAY_SERVICES_RESOLUTION_REQUEST).show();
	        } else {
	            Log.i(TAG, "This device is not supported.");
	            finish();
	        }
	        return false;
	    }
	    return true;
	}
	
	private void registerInBackgroundMy(final int from) {
		
		new AsyncTask<Void, Void, Boolean>() {
	    	
	    	@Override
	    	protected void onPreExecute() {
	    		super.onPreExecute();
	    		Log.i(TAG, "Regid started.");
	    	}
	    	
	    	@Override
	    	protected Boolean doInBackground(Void... params) {
	    		String msg = "";
	    		try {
	    			Log.i(TAG, "befor GoogleCloudMessaging.");
	                if (gcm == null) {
	                    gcm = GoogleCloudMessaging.getInstance(context);
	                }
	                Log.i(TAG, "after GoogleCloudMessaging.");
	                regid = gcm.register(GcmRegistration.SENDER_ID);
	                msg = "Device registered, registration ID=" + regid;
	                
	                Log.e(TAG, "Reg id : " + regid);
	                GcmRegistration.storeRegistrationId(context, regid);
	                return true;
	            } catch (IOException ex) {
	                msg = "Error :" + ex.getMessage();
	                return false;
	            }	    		
	    	}
	    	
	    	@Override
	    	protected void onPostExecute(Boolean result) {	    
	    		Log.i(TAG, "Regid end 1.");
	    		super.onPostExecute(result);
	    		
	    		Log.i(TAG, "Regid end 2.");
	    		if(result) {
	    			Log.i(TAG, "Start server registration.");
	    			if(from==FROM_LOGIN)
	    				startRegistration();
	    		}
	    		else {
	    			Log.i(TAG, "Fail.");
	    			showToast("Please try again");
	    		}
	    	}
		}.execute();
		
	}
    
    private String response = "";
	OnAsyncTaskListener asyncTaskListener = new OnAsyncTaskListener() {
		
		@Override
		public void onTaskComplete(boolean isComplete, String message) {
			Log.e(TAG, "mess : "+message);
			response = message;
			
			btnLogin.setClickable(true);
			 
				AsyncResponse asyncResponse = new AsyncResponse(response);
				if(asyncResponse.ifSuccess())
				{	
					list = asyncResponse.getUserDetail();
					
					String id = list.get(0).getId();
					String name = list.get(0).getName();
					String lname = list.get(0).getLname();
					String email = list.get(0).getEmail();
					String status = list.get(0).getStatus();
					String online = list.get(0).getOnline();
					String image = list.get(0).getImage();
					Log.e(TAG, "image : "+image);
					storeId(context, id, name, lname, email, status, online, image);
					
					// Launch Main Activity
			        Intent i = new Intent(getApplicationContext(), FriendsListActivity.class);
			        startActivity(i);
			        finish();
				}
				else
				{
					showToast(""+asyncResponse.getMessage());
				}
		}
		
		@Override
		public void onTaskBegin() {
			
		}
	};
	
	private void storeId(Context context, String id, String name, String lname, String email, String status, String online, String image) {
	    Sessions.save(context, id, name, lname, email, status, online, image);
	}
	
	public void onSkipClick(View view) {
		Intent i = new Intent(getApplicationContext(), RegisterActivity.class);
        startActivity(i);
        finish();
	}	
	
	
	
	protected void showToast(String message) {
		Toast.makeText(context, ""+message, Toast.LENGTH_SHORT).show();
	}		
    
    protected void showToastLong(String message) {
		Toast.makeText(context, ""+message, Toast.LENGTH_LONG).show();
	}
}
