package com.cs3370.android.lrs_driverapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.google.android.material.navigation.NavigationView;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.cs3370.android.lrs_driverapp.SignInActivity.mDriver;

//all lists are displayed in this activity

public class RecyclerViewActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private RecyclerView mRecyclerView;
    private MyAdapter mAdapter;
    private TextView mListTitle, textViewNavUserName;
    private RequestQueue mRequestQueue;

    //********************************Drawer Menu***********************************
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;
    private View navHeader;

    final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view);

        //******************* Init the drawer menu ******************************
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navHeader = navigationView.getHeaderView(0);
        textViewNavUserName = navHeader.findViewById(R.id.textView_navUserName);
        toggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        //**************** Set the drawer menu ************************************
        navigationView.setItemIconTintList(null);
        navigationView.setNavigationItemSelectedListener(this);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (mDriver != null && mDriver.get("name") != null)
            textViewNavUserName.setText(mDriver.get("name"));
        //**************************************************************************

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mListTitle = (TextView) findViewById(R.id.list_title);

        if (DataHolder.isClientDroppedOff){
            DataHolder.isClientDroppedOff = false;
            RideListHandler.getInstance().updateList(this, "/api/driver-history?id=" + Driver.getInstance().get("id"), "History");
        }

        if (DataHolder.navSelected == null) {
            mListTitle.setText("Accepted Rides");
            RideListHandler.getInstance().updateList(this, "/api/driver-requests?id=" + Driver.getInstance().get("id"), "Accepted");
        } else if (DataHolder.navSelected.equals("Accepted")) {
            mListTitle.setText("Accepted Rides");
        } else if (DataHolder.navSelected.equals("Pending")) {
            mListTitle.setText("Pending Rides");
        } else if (DataHolder.navSelected.equals("History")) {
            mListTitle.setText("History");
        }

        showListItems();

        /*
        DataHolder.showProgressDialog(this);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                    showListItems();
            }
        }, 1500);

         */

    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    public void showListItems() {
        mAdapter = new MyAdapter();
        mRecyclerView.setAdapter(mAdapter);
        RecyclerSectionItemDecoration sectionItemDecoration =
                new RecyclerSectionItemDecoration(getResources().getDimensionPixelSize(R.dimen.recycler_section_header_height),
                        true, getSectionCallback(RideListHandler.getInstance().getList()));
        mRecyclerView.addItemDecoration(sectionItemDecoration);

        if (RideListHandler.getInstance().getList().isEmpty()) {
            if (DataHolder.navSelected == null)
                mListTitle.setText("No Accepted rides recorded.");
            else if (DataHolder.navSelected.equals("Accepted"))
                mListTitle.setText("No Accepted rides recorded.");
            else if (DataHolder.navSelected.equals("History"))
                mListTitle.setText("Your history is empty");
            else if (DataHolder.navSelected.equals("Pending"))
                mListTitle.setText("No pending rides recorded");
        }
        //DataHolder.dismissProgressDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DataHolder.dismissProgressDialog();
    }

    private void RecyclerSectionItemDecorationHelper() {
        mRecyclerView.removeItemDecorationAt(0);
        RecyclerSectionItemDecoration sectionItemDecoration =
                new RecyclerSectionItemDecoration(getResources().getDimensionPixelSize(R.dimen.recycler_section_header_height),
                        true, getSectionCallback(RideListHandler.getInstance().getList()));
        mRecyclerView.addItemDecoration(sectionItemDecoration);
    }

    private RecyclerSectionItemDecoration.SectionCallback getSectionCallback(final List<DisplayListItem> list) {
        return new RecyclerSectionItemDecoration.SectionCallback() {
            @Override
            public boolean isSection(int position) {
                String dateOne = list.get(Math.min(position, list.size() - 1)).getPickUpDate();
                String dateTwo = list.get(Math.min(position + 1, list.size() - 1)).getPickUpDate();
                return (position == 0 || (dateOne != dateTwo) || position == list.size() - 1);
            }

            @Override
            public CharSequence getSectionHeader(int position) {
                return list.get(Math.min(position, list.size() - 1)).getPickUpDate();
            }
        };
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (toggle.onOptionsItemSelected(item))
            return true;
        drawerLayout.closeDrawer(GravityCompat.START);

        return true;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {

            case R.id.nav_driverRides:
                RideListHandler.getInstance().updateList(this, "/api/driver-requests?id=" + Driver.getInstance().get("id"), "Accepted");
                break;

            case R.id.nav_pendingRideRequests:
                RideListHandler.getInstance().updateList(this, "/api/serviceable-requests", "Pending");
                break;

            case R.id.nav_driverHistory:
                RideListHandler.getInstance().updateList(this, "/api/driver-history?id=" + Driver.getInstance().get("id"), "History");
                break;

            case R.id.nav_about:
                startActivity(new Intent(this, AboutActivity.class));
                break;

            case R.id.nav_logout:
                logout();
                break;
        }



        //The two lines below prevent a bug where the user can click on an item from the list
        // they are leaving after deciding to leave it. The bug is caused by the handler.postDelayed()
        // method we use right after to make sure we get a response from the server before displaying the new list.
        mAdapter = new MyAdapter();
        mRecyclerView.setAdapter(mAdapter);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                DataHolder.dismissProgressDialog();
                mAdapter = new MyAdapter();
                mRecyclerView.setAdapter(mAdapter);
                RecyclerSectionItemDecorationHelper();
            }
        }, 1500);


        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void parseJSON(final boolean shutdown) {
        DataHolder.showProgressDialog(this);
        mRequestQueue = Volley.newRequestQueue(this);
        String url = "https://apps.ericvillnow.com/rideshare/api/logout?email=" + mDriver.get("email");
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url,
                null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                DataHolder.dismissProgressDialog();
                try {
                    boolean logout = response.getBoolean("logout");
                    if (logout) {
                        toastMessage("Logout successful!");
                        if (!shutdown) {
                            Intent intent = new Intent(RecyclerViewActivity.this, SignInActivity.class);
                            startActivity(intent);
                        }
                    } else {
                        toastMessage("Couldn't log out.");
                    }
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                DataHolder.dismissProgressDialog();
                toastMessage("Error: no response from the server");
                error.printStackTrace();
            }
        });

        mRequestQueue.add(request);
    }

    public void toastMessage(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    public  void logout(){
        SharedPreferences sharedpreferences = getSharedPreferences(SignInActivity.MYPREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.clear();
        editor.commit();
        parseJSON(false);
    }
}
