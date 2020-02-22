package com.example.vivien.calendar;

import com.google.android.gms.common.server.converter.StringToIntConverter;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;


        import com.google.android.gms.common.ConnectionResult;
        import com.google.android.gms.common.GoogleApiAvailability;
        import com.google.api.client.extensions.android.http.AndroidHttp;
        import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
        import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
        import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

        import com.google.api.client.http.HttpTransport;
        import com.google.api.client.json.JsonFactory;
        import com.google.api.client.json.jackson2.JacksonFactory;
        import com.google.api.client.util.ExponentialBackOff;

        import com.google.api.services.calendar.CalendarScopes;
        import com.google.api.client.util.DateTime;

        import com.google.api.services.calendar.model.*;
import com.google.api.services.calendar.model.Calendar;

import android.Manifest;
        import android.accounts.AccountManager;
        import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
        import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
        import android.content.Intent;
        import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
        import android.net.NetworkInfo;
        import android.os.AsyncTask;
        import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
        import android.text.TextUtils;
        import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
        import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.json.JSONArray;

import java.io.IOException;
import java.util.*;

import pub.devrel.easypermissions.AfterPermissionGranted;
        import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends Activity implements EasyPermissions.PermissionCallbacks {
    GoogleAccountCredential mCredential;
    private TextView mOutputText;
    private Button mCallApiButton;
    ProgressDialog mProgress;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String BUTTON_TEXT = "Call Google Calendar API";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { CalendarScopes.CALENDAR_READONLY };

    TextView s1_tv1, s1_tv2;
    Button s1_bt1, s1_bt2, s1_bt3;
    EditText s1_et1;
    String date1 = "", date2 = "", time1 = "", time2 = "";
    ServerConnection sc;
    JSONArray user, other;
    private int mYear1, mMonth1, mDay1, mHour1, mMinute1 ,mYear2, mMonth2, mDay2, mHour2, mMinute2;

    protected LocationManager locationManager;
    Location location;
    int month[]={31,28,31,30,31,30,31,31,30,31,30,31};
    SimpleAdapter adapter;
    private List<Map<String,Object>> list;
    private List<String> A;
    String[]box={"C","D","E","F","G","H","I","J"};

    /**
     * Create the main activity.
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout activityLayout = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        activityLayout.setLayoutParams(lp);
        activityLayout.setOrientation(LinearLayout.VERTICAL);
        activityLayout.setPadding(16, 16, 16, 16);

        ViewGroup.LayoutParams tlp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        mCallApiButton = new Button(this);
        mCallApiButton.setText(BUTTON_TEXT);
        mCallApiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallApiButton.setEnabled(false);
                mOutputText.setText("");
                getResultsFromApi();
                mCallApiButton.setEnabled(true);
            }
        });
        activityLayout.addView(mCallApiButton);

        mOutputText = new TextView(this);
        mOutputText.setLayoutParams(tlp);
        mOutputText.setPadding(16, 16, 16, 16);
        mOutputText.setVerticalScrollBarEnabled(true);
        mOutputText.setMovementMethod(new ScrollingMovementMethod());
        mOutputText.setText(
                "Click the \'" + BUTTON_TEXT +"\' button to test the API.");
        activityLayout.addView(mOutputText);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Google Calendar API ...");

        setContentView(activityLayout);

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
        sc = new ServerConnection();
    }

    private TimerTask task = new TimerTask () {
        public void run() {
            //每3秒更新一次對方的位置
            Message message = new Message ();
            message.what = 3;
            h1.sendMessage (message);
        }
    };
    private ListView.OnItemClickListener itemclick = new ListView.OnItemClickListener()
    {
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
        {
            Map<String, Object> item = list.get(arg2);
            int count = (int)item.get("count");
            int price = (int)item.get("price");
            int total = (int)item.get("total");
            count--;
            if(count==0)
                list.remove(arg2);
            else
            {
                item.put("total",total-price);
                item.put("count",count);
                item.put("note","單價:"+price+"      個數:"+count);
                list.set(arg2,item);

            }

            adapter.notifyDataSetChanged();
        }
    };


    Handler h1 = new Handler () {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                int day = 0;
                //算開始日到結束日共多少天
                for (int i = 0; i < 12; i++) {
                    if (mMonth2 == mMonth1 + i) {
                        day += mDay2 - mDay1 + 1;
                        break;
                    } else {
                        day += month[mMonth1 + i];
                    }
                }
                final int days = day;
                final int time = mHour2 - mHour1;
                new Thread() {
                    public void run() {
                        sc.insert("imf30", "imf30", "A,B,C,D,E,F,G,H,I", "'5','" + s1_et1.getText() + "','Vivi','" + date1 + "','" + date2 + "','" + time1 + "','" + time2 + "','" + days + "','" + time + "'");
                        sc.delete("imf30","imf30","A=5 OR A=6");
                    }
                }.start();
                new Thread() {
                    public void run() {
                        int num = 0;
                        for (int i = 0; i * 40 < days * time; i++) {
                            String[] OK = {"", "", "", "", "", "", "", ""};
                            for (int j = 0; j * 5 < 40; j++) {
                                for (int k = 0; k < 5; k++) {
                                    if (num == days * time)
                                        break;
                                    OK[j] += 1;
                                    num++;
                                }
                                if (num == days * time)
                                    break;
                            }
                            sc.insert("imf30", "imf30", "A,B,C,D,E,F,G,H,I,J", "'6','" + i + "','" + OK[0] + "','" + OK[1] + "','" + OK[2] + "','" + OK[3] + "','" + OK[4] + "','" + OK[5] + "','" + OK[6] + "','" + OK[7] + "'");
                        }
                    }
                }.start();

            }

            if (msg.what == 2) {
                //更新自己的位置(DB)
                new Thread () {
                    public void run() {
                        sc.update ("imf30", "imf30", "A='4' AND B='Vivi'", "F='" + location.getLongitude () + "',E='" + location.getLatitude ()+ "'");
                        //因為使用共用的資料庫，所以對座標位置進行了一點加密
                    }
                }.start ();
            }
            if (msg.what == 3) {
                //取得對方的位置(DB)
                new Thread () {
                    public void run() {
                        other = sc.query ("imf30", "imf30", "B,C,D,E,F", "A='6'");
                        //更新地圖
                    }
                }.start ();
            }
        }
    };


    public void showDatePickerDialog1() {
        // 設定初始日期
        final java.util.Calendar c = java.util.Calendar.getInstance();
        mYear2 = c.get(java.util.Calendar.YEAR);
        mMonth2 = c.get(java.util.Calendar.MONTH);
        mDay2 = c.get(java.util.Calendar.DAY_OF_MONTH);
        // 跳出日期選擇器
        DatePickerDialog dpd = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {
                        mYear2= year;
                        mMonth2= monthOfYear + 1;
                        mDay2= dayOfMonth;
                        date2=mYear2 + "-" + mMonth2 + "-" + mDay2;
                        // 完成選擇，顯示日期
                        s1_tv1.setText(date1+" ~ "+date2);
                    }
                }, mYear2, mMonth2, mDay2);
        dpd.show();
    }

    public void showDatePickerDialog2() {
        // 設定初始日期
        final java.util.Calendar c = java.util.Calendar.getInstance();
        mYear1 = c.get(java.util.Calendar.YEAR);
        mMonth1 = c.get(java.util.Calendar.MONTH);
        mDay1 = c.get(java.util.Calendar.DAY_OF_MONTH);
        // 跳出日期選擇器
        DatePickerDialog dpd = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    public void onDateSet(DatePicker view, int year,
                                          int monthOfYear, int dayOfMonth) {
                        mYear1 = year;
                        mMonth1 = monthOfYear + 1;
                        mDay1 = dayOfMonth;
                        date1=mYear1 + "-" + mMonth1 + "-" + mDay1;
                        // 完成選擇，顯示日期

                    }
                }, mYear1, mMonth1, mDay1);
        dpd.show();
    }

    public void showTimePickerDialog1() {
        // 設定初始時間
        final java.util.Calendar c = java.util.Calendar.getInstance();
        mHour2 = c.get(java.util.Calendar.HOUR_OF_DAY);
        mMinute2 = 0;

        // 跳出時間選擇器
        TimePickerDialog tpd = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    public void onTimeSet(TimePicker view, int hourOfDay,
                                          int minute) {
                        // 完成選擇，顯示時間
                        if(minute<10){
                            time2=hourOfDay + ":0" + minute;
                        }
                        else {
                            time2=hourOfDay + ":" + minute;
                        }
                        s1_tv2.setText(time1+" ~ "+time2);
                        mHour2=hourOfDay;
                    }
                }, mHour2, mMinute2, false);
        tpd.show();
    }

    public void showTimePickerDialog2() {
        // 設定初始時間
        final java.util.Calendar c = java.util.Calendar.getInstance();
        mHour1 = c.get(java.util.Calendar.HOUR_OF_DAY);
        mMinute1 = 0;

        // 跳出時間選擇器
        TimePickerDialog tpd = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    public void onTimeSet(TimePicker view, int hourOfDay,
                                          int minute) {
                        // 完成選擇，顯示時間
                        if(minute<10){
                            time1=hourOfDay + ":0" + minute;
                        }
                        else {
                            time1=hourOfDay + ":" + minute;
                        }
                        mHour1=hourOfDay;
                    }
                }, mHour1, mMinute1, false);
        tpd.show();
    }




    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            mOutputText.setText("No network connection available.");
        } else {
            new MakeRequestTask(mCredential).execute();
            newpage(1);
        }
    }
    private void newpage(int page){
        if(page ==1) {
            setContentView(R.layout.activity_main);
            s1_bt1 = (Button) findViewById(R.id.s1_bt1);
            s1_bt2 = (Button) findViewById(R.id.s1_bt2);
            s1_bt3 = (Button) findViewById(R.id.s1_bt3);
            s1_tv1 = (TextView) findViewById(R.id.s1_tv1);
            s1_tv2 = (TextView) findViewById(R.id.s1_tv2);
            s1_et1 = (EditText) findViewById(R.id.s1_et1);
            list = new ArrayList<Map<String, Object>>();
            s1_bt1.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    showDatePickerDialog1();
                    showDatePickerDialog2();
                }
            });
            s1_bt3.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    setContentView(R.layout.layout);
                    ListView lv = (ListView) findViewById(R.id.listView);
                    list.clear();
                    adapter = new SimpleAdapter(MainActivity.this, list, R.layout.listitem, new String[]{"name", "total", "note"}, new int[]{R.id.itemname, R.id.itemtotal, R.id.itemnote});
                    lv.setAdapter(adapter);
                    lv.setOnItemClickListener(itemclick);
                    //insert活動資料
                    Message message = new Message();
                    message.what = 1;
                    h1.sendMessage(message);
                    Timer timer01 = new Timer ();
                    timer01.schedule (task, 0, 3000);
                    if (A == null || A.size() == 0) {}
                    else {
                        int day=0, time=0;
                        for (int i = 0; i < 12; i++) {
                            if (mMonth2 == mMonth1 + i) {
                                day += mDay2 - mDay1 + 1;
                                break;
                            } else {
                                day += month[mMonth1 + i];
                            }
                        }
                        time = mHour2 - mHour1;
                        for (int i=0;i<A.size();i++) {
                            String a[] = A.get(i).split(";");//分date1 and date2
                            String b[] = a[0].split("T");//day and time
                            String day1[] = b[0].split("-");//YYY-MM-DD
                            String h1 = b[1].substring(0, 2);//hour
                            String b1[] = a[1].split("T");//day and time
                            String day2[] = b1[0].split("-");//YYY-MM-DD
                            String h2 = b1[1].substring(0, 2);//hour
                            //算開始日到結束日共多少天
                            int test=0;
                            if(Integer.valueOf(h1)>=mHour1&&Integer.valueOf(h1)<mHour2&&Integer.valueOf(h2)<=mHour2 && Integer.valueOf(h2)>=mHour1)
                                test=4;
                            else if(Integer.valueOf(h1)>=mHour1&&Integer.valueOf(h1)<mHour2)
                                test=3;
                            else if(Integer.valueOf(h2)<=mHour2 && Integer.valueOf(h2)>=mHour1)
                                test=2;
                            else if(Integer.valueOf(h1)<=mHour1&&Integer.valueOf(h2)>=mHour2)
                                test=1;
                            if(test!=0){
                                int d1 = 0, d2=0;
                                for (int j = 0; j < 12; j++) {
                                    if ( Integer.valueOf(day1[1])== mMonth1 + j) {
                                        d1 += Integer.valueOf(day1[2]) - mDay1 + 1;
                                        break;
                                    } else {
                                        d1 += month[mMonth1 + j];
                                    }
                                }
                                for (int j = 0; j < 12; j++) {
                                    if (Integer.valueOf(day2[1]) == mMonth1 + j) {
                                        d2 += Integer.valueOf(day2[2]) - mDay1 + 1;
                                        break;
                                    } else {
                                        d2 += month[mMonth1 + j];
                                    }
                                }
                                String e=String.valueOf(d1);
                                Log.e("d1",e);
                                String f=String.valueOf(d2);
                                Log.e("d2",f);
                                Log.e("d3",String.valueOf(day));
                                int x,y;
                                if((d1>0&&d1<=day)||(d2>0&&d2<=day)){
                                    if (test == 4) {
                                        x=(d1-1)*time;
                                        y=d2*time;
                                    }else if(test==3){
                                        x=(d1-1)*time+(Integer.valueOf(h1)-mHour1);
                                        y=d2*time;
                                    }else if(test==2){
                                        x=(d1-1)*time+(Integer.valueOf(h1)-mHour1);
                                        y=(d2-1)*time+(Integer.valueOf(h2)-mHour1);
                                    }
                                    else{
                                        x=(d1-1)*time;
                                        y=(d2-1)*time+(Integer.valueOf(h2)-mHour1);
                                    }

                                    for(int z = x; z <=y; z++){
                                        Log.e("d","dd");
                                        final int u=z%40;//第幾個6
                                        int u1=x-40*u;//6入面第幾個數
                                        final int u2=u1%5;//6入面第幾格
                                        final int u3=u1-5*u2;//5個格的第幾格
                                        //目前未做的部分
                                        /*new Thread () {
                                            public void run() {
                                                //String data =sc.query("imf30","imf30",box[u2],"A='6' AND B='"+u+"'").toString();
                                                String data="11111";
//                                                data= data.substring(7,12);//hour
                                                char[] DATA=data.toCharArray();
                                                DATA[u3]='0';
                                                DATA[1]='0';
                                                String data1=new String(DATA);
                                                //sc.update("imf30","imf30","A=6 AND B="+u,box[u2]+"='"+data1+"'");
                                            }
                                        }.start ();*/
                                    }
                                }
                            }
                        }
                    }
                    //showCurrentLocation();
                }
            });
            s1_bt2.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    showTimePickerDialog1();
                    showTimePickerDialog2();
                }
            });
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    mOutputText.setText(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * An asynchronous task that handles the Google Calendar API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;

        public MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Google Calendar API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of the next 10 events from the primary calendar.
         * @return List of Strings describing returned events.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            // List the next 10 events from the primary calendar.
            DateTime now = new DateTime(System.currentTimeMillis());
            //DateTime end = new DateTime(mYear1+"-"+mMonth1+"-"+mDay1+"T"+mHour1+":00:00.000+08:00");
            DateTime end = new DateTime("2016-12-31T14:00:00.000+08:00");
            String dateString = end.toString();


            List<String> eventStrings = new ArrayList<String>();

            Events events = mService.events().list("primary")
                    .setTimeMin(now)
                    .setTimeMax(end)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            List<Event> items = events.getItems();

            for (Event event : items) {
                DateTime start = event.getStart().getDateTime();
                DateTime after;
                if(start!=null) {
                    after = event.getEnd().getDateTime();
                    //從DateTime分出date和time~~~
                    /*
                    String[] token = start.toString().split("T");//[呼叫函式]將字串作切割,今天是以空白" " 來切該字串
                    String[] date1 = token[0].split("-");
                    String time1 = token[1].substring(0, 1);
                    token = after.toString().split("T");//[呼叫函式]將字串作切割,今天是以空白" " 來切該字串
                    String[] date2 = token[0].split("-");
                    String time2 = token[1].substring(0, 1);
                    */
                }
                else {
                    // All-day events don't have start times, so just use
                    // the start date.
                    start = event.getStart().getDate();
                    after = start;
                   // String[] date1 = start.toString().split("-");
                }
                //將日期放入list
                eventStrings.add(start.toString()+";"+after.toString());
            }
            return eventStrings;
        }


        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
            A=output;
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    mOutputText.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }
    }
}
