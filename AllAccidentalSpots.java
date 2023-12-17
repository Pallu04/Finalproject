package com.accidentalspot.spot;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.widget.Toast;

import com.accidentalspot.spot.Adapters.AllSpotsAdapter;
import com.accidentalspot.spot.Models.LocationObject;
import com.accidentalspot.spot.Models.LocationObjects;
import com.accidentalspot.spot.api.APIService;
import com.accidentalspot.spot.api.APIUrl;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AllAccidentalSpots extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private List<LocationObject> contactList;
    private AllSpotsAdapter mAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.all_accidental_spots);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);

        contactList = new ArrayList<>();
        mAdapter = new AllSpotsAdapter(this, contactList);

        // white background notification bar
        //whiteNotificationBar(recyclerView);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.HORIZONTAL));
        recyclerView.setAdapter(mAdapter);

        // show loader and fetch messages
        swipeRefreshLayout.post(
                new Runnable() {
                    @Override
                    public void run() {
                        getAllCriminalSpot();
                    }
                }
        );

    }

    private void getAllCriminalSpot() {
        swipeRefreshLayout.setRefreshing(true);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(APIUrl.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()) //Here we are using the GsonConverterFactory to directly convert json data to object
                .build();

        APIService api = retrofit.create(APIService.class);

        Call<LocationObjects> call = api.getData();

        call.enqueue(new Callback<LocationObjects>() {
            @Override
            public void onResponse(Call<LocationObjects> call, Response<LocationObjects> response) {
                if (response.isSuccessful()) {
                    List<LocationObject> categoryItems = new ArrayList<>();
                    categoryItems = response.body().getLocationObject();

                    if (categoryItems == null) {
                        Toast.makeText(AllAccidentalSpots.this, "No Items Found..!", Toast.LENGTH_SHORT).show();

                    }else{
                        // adding contacts to contacts list
                        contactList.clear();
                        contactList.addAll(categoryItems);

                        // refreshing recycler view
                        mAdapter.notifyDataSetChanged();
                    }

                    swipeRefreshLayout.setRefreshing(false);

                } else {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(getApplicationContext(), "Failed to Retrive Data ", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<LocationObjects> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });

    }

    @Override
    public void onRefresh() {
        getAllCriminalSpot();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
            super.onBackPressed();
    }
}
