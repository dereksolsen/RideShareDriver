package com.cs3370.android.lrs_driverapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class SignInActivity extends AppCompatActivity {

    private EditText mUserEmail;
    private EditText mPassword;
    private TextView mErrorMessage, mTextView_signingIn;
    private Button mSignInButton, cancelButton;
    private RequestQueue mRequestQueue;

    public static Driver mDriver;
    private String mDriverID;

    SQLiteDatabase myDatabase;

    private ProgressBar progressBar;

    //Auto login
    SharedPreferences sharedPreferences;
    public static final String MYPREFERENCES = "user_details";
    public static final String NAME = "nameKey";
    public static final String PHONE = "phoneKey";
    public static final String EMAIL = "emailKey";
    public static final String PASSWORD = "emailKey";
    public static final String ID = "idKey";
    public static final String AUTHORIZED = "authorizedKey";
    public static final String ROLE = "roleKey";
    public static final String CREATED_AT = "created_at_key";
    public static final String UPDATED_AT = "updated_at_key";
    public static final String ONLINE = "onlineKey";
    public static final String RATING = "rating";
    public static final String HISTORY = "historyKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        initViews();
        showForm();

        sharedPreferences = getSharedPreferences(MYPREFERENCES, Context.MODE_PRIVATE);
        if (sharedPreferences.contains(EMAIL) && sharedPreferences.contains(PASSWORD)) {
            getMyPreferences();
            Intent intent = new Intent(SignInActivity.this, RecyclerViewActivity.class);
            startActivity(intent);
            finish();
        }

        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideForm();
                validate(mUserEmail.getText().toString(), mPassword.getText().toString());
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    public void initViews() {
        mUserEmail = (EditText) findViewById(R.id.editText_email);
        mPassword = (EditText) findViewById(R.id.password);
        mErrorMessage = (TextView) findViewById(R.id.errorMessage);
        mTextView_signingIn = (TextView) findViewById(R.id.signing_in);
        mSignInButton = (Button) findViewById(R.id.signInButton);
        cancelButton = (Button) findViewById(R.id.button_cancel);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
    }

    //checks if they are in the database, identifies if they are a driver or a passenger, directs them to the proper screen
    private void validate(String userName, final String userPassword) {
        mRequestQueue = Volley.newRequestQueue(this);
        String url = getResources().getString(R.string.server_addr) + "/api/login?email=" + userName + "&password=" + userPassword;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {

                    JSONObject user = response.getJSONObject("user");

                    boolean authorized = response.getBoolean("authorized");
                    String role = response.getString("role");

                    if (authorized && role.equals("driver")) {
                        String id = user.getString("id");
                        String name = user.getString("name");
                        String email = user.getString("email");
                        String phone_number = user.getString("phone_number");

                        String history = user.getString("history");
                        String created_at = user.getString("created_at");
                        String updated_at = user.getString("updated_at");
                        boolean online = user.getBoolean("online");
                        mDriver = Driver.getInstance();
                        mDriver.set("authorized", authorized);
                        mDriver.set("role", role);
                        mDriver.set("id", id);
                        mDriver.set("name", name);
                        mDriver.set("email", email);
                        mDriver.set("phoneNumber", phone_number);

                        mDriver.set("history", history);
                        mDriver.set("createdAt", created_at);
                        mDriver.set("updatedAt", updated_at);
                        mDriver.set("online", online);

                        //Save into preferences
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(NAME, name);
                        editor.putString(PHONE, phone_number);
                        editor.putString(EMAIL, email);
                        editor.putString(PASSWORD, userPassword);
                        editor.putString(ID, id);
                        editor.putString(HISTORY, history);
                        editor.putString(CREATED_AT, created_at);
                        editor.putString(ROLE, role);
                        editor.putString(UPDATED_AT, updated_at);
                        editor.putBoolean(AUTHORIZED, authorized);
                        editor.putBoolean(ONLINE, online);
                        editor.commit();

                        myDatabase = new DBHelper(SignInActivity.this).getWritableDatabase();

                        Intent intent;
                        //If statement for DriverMapsActivity
                        if (isTableExist(myDatabase, DBSchema.MapsActivityTable.TABLE)) {
                            getAllData(myDatabase, DBSchema.MapsActivityTable.TABLE);
                            startActivity(new Intent(SignInActivity.this, DriverMapsActivity.class));
                            finish();
                        } else {
                            intent = new Intent(SignInActivity.this, RecyclerViewActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        showForm();
                        mErrorMessage.setVisibility(View.VISIBLE);
                        mErrorMessage.setText("Invalid Username or Password");
                    }
                } catch (JSONException e1) {
                    showForm();
                    mErrorMessage.setVisibility(View.VISIBLE);
                    mErrorMessage.setText("Invalid Username or Password");
                    e1.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                showForm();
                error.printStackTrace();
            }
        });
        mRequestQueue.add(request);
    }

    public void getMyPreferences() {
        boolean authorized = sharedPreferences.getBoolean(AUTHORIZED, false);
        String role = sharedPreferences.getString(ROLE, "");
        String id = sharedPreferences.getString(ID, "");
        String name = sharedPreferences.getString(NAME, "");
        String email = sharedPreferences.getString(EMAIL, "");
        String phone_number = sharedPreferences.getString(PHONE, "");
        String history = sharedPreferences.getString(HISTORY, "");
        String created_at = sharedPreferences.getString(CREATED_AT, "");
        String updated_at = sharedPreferences.getString(UPDATED_AT, "");
        boolean online = sharedPreferences.getBoolean(ONLINE, false);

        mDriver = Driver.getInstance();
        mDriver.set("authorized", authorized);
        mDriver.set("role", role);
        mDriver.set("id", id);
        mDriver.set("name", name);
        mDriver.set("email", email);
        mDriver.set("phoneNumber", phone_number);

        mDriver.set("history", history);
        mDriver.set("createdAt", created_at);
        mDriver.set("updatedAt", updated_at);
        mDriver.set("online", online);
    }

    //******************************FOR DRIVERSMAPACTIVITY*******************************

    public boolean isTableExist(SQLiteDatabase db, String tableName) {
        String query = "SELECT * FROM " + tableName;
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.close();
                return true;
            }
            cursor.close();
        }
        cursor.close();
        return false;
    }

    public void getAllData(SQLiteDatabase db, String tableName) {
        String query = "SELECT * FROM " + tableName;
        Cursor cursor = db.rawQuery(query, null);

        double latitude, longitude;
        if (cursor.getCount() == 0) {
            System.out.println("Local database empty.");
        } else {
            System.out.println("Local database not empty.");
            //StringBuffer buffer = new StringBuffer();
            while (cursor.moveToNext()) {
                DataHolder.mId = cursor.getString(0);
                DataHolder.clientName = cursor.getString(1);
                DataHolder.clientPhoneNumber = cursor.getString(2);

                latitude = Double.parseDouble(cursor.getString(3));
                longitude = Double.parseDouble(cursor.getString(4));
                DataHolder.mPickUp_latlng = new com.google.android.gms.maps.model.LatLng(latitude, longitude);

                latitude = Double.parseDouble(cursor.getString(5));
                longitude = Double.parseDouble(cursor.getString(6));
                DataHolder.mDropOff_latlng = new com.google.android.gms.maps.model.LatLng(latitude, longitude);

                DataHolder.mPickUpAddress = cursor.getString(7);
                DataHolder.mDropOffAddress = cursor.getString(8);

                if (cursor.getString(9).equals("1"))
                    DataHolder.isClientPickedUp = true;
                else
                    DataHolder.isClientPickedUp = false;
            }
        }
    }

    public void hideForm() {
        mUserEmail.setVisibility(View.INVISIBLE);
        mPassword.setVisibility(View.INVISIBLE);
        mErrorMessage.setVisibility(View.INVISIBLE);
        mSignInButton.setVisibility(View.INVISIBLE);
        cancelButton.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        mTextView_signingIn.setVisibility(View.VISIBLE);
    }

    public void showForm() {
        mUserEmail.setVisibility(View.VISIBLE);
        mPassword.setVisibility(View.VISIBLE);
        mErrorMessage.setVisibility(View.VISIBLE);
        mSignInButton.setVisibility(View.VISIBLE);
        cancelButton.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        mTextView_signingIn.setVisibility(View.INVISIBLE);
    }
}
