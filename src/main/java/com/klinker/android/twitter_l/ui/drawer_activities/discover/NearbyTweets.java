package com.klinker.android.twitter_l.ui.drawer_activities.discover;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.adapters.ArrayListLoader;
import com.klinker.android.twitter_l.adapters.TimelineArrayAdapter;
import com.klinker.android.twitter_l.data.App;
import com.klinker.android.twitter_l.settings.AppSettings;
import com.klinker.android.twitter_l.ui.drawer_activities.DrawerActivity;
import com.klinker.android.twitter_l.utils.Utils;

import org.lucasr.smoothie.AsyncListView;
import org.lucasr.smoothie.ItemManager;

import java.util.ArrayList;

import twitter4j.GeoLocation;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Trend;
import twitter4j.Twitter;
import uk.co.senab.bitmapcache.BitmapLruCache;

/**
 * Created by luke on 2/23/14.
 */
public class NearbyTweets extends Fragment implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private LocationClient mLocationClient;
    private boolean connected = false;

    private Context context;
    private AppSettings settings;

    private AsyncListView listView;
    private View layout;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        settings = AppSettings.getInstance(context);

        layout = inflater.inflate(R.layout.profiles_list, null);

        listView = (AsyncListView) layout.findViewById(R.id.listView);

        BitmapLruCache cache = App.getInstance(context).getBitmapCache();
        ArrayListLoader loader = new ArrayListLoader(cache, context);

        ItemManager.Builder builder = new ItemManager.Builder(loader);
        builder.setPreloadItemsEnabled(true).setPreloadItemsCount(10);
        builder.setThreadPoolSize(2);

        listView.setItemManager(builder.build());

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                final int lastItem = firstVisibleItem + visibleItemCount;

                if(lastItem == totalItemCount && canRefresh) {
                    getMore();
                }
            }
        });

        if (Utils.hasNavBar(context) && (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) || getResources().getBoolean(R.bool.isTablet)) {
            View footer = new View(context);
            footer.setOnClickListener(null);
            footer.setOnLongClickListener(null);
            ListView.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, Utils.getNavBarHeight(context) +
                    (DrawerActivity.hasToolbar ? Utils.getStatusBarHeight(context) : 0));
            footer.setLayoutParams(params);
            listView.addFooterView(footer);
            listView.setFooterDividersEnabled(false);
        }

        mLocationClient = new LocationClient(context, this, this);

        getTweets();

        return layout;
    }

    @Override
    public void onConnected(Bundle bundle) {
        connected = true;
    }

    @Override
    public void onDisconnected() {
        connected = false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(context, getResources().getString(R.string.error), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        mLocationClient.connect();
    }

    @Override
    public void onStop() {
        mLocationClient.disconnect();
        super.onStop();
    }

    public Query query;
    public boolean hasMore = true;
    public ArrayList<Status> statuses = new ArrayList<Status>();
    public TimelineArrayAdapter adapter;

    public void getTweets() {

        canRefresh = false;

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Twitter twitter =  Utils.getTwitter(context, DrawerActivity.settings);

                    int i = 0;
                    while (!connected && i < 5) {
                        try {
                            Thread.sleep(1500);
                        } catch (Exception e) {

                        }

                        i++;
                    }

                    Location location = mLocationClient.getLastLocation();

                    query = new Query();
                    query.setGeoCode(new GeoLocation(location.getLatitude(),location.getLongitude()), 10, Query.MILES);

                    QueryResult result = twitter.search(query);

                    if (result.hasNext()) {
                        hasMore = true;
                        query = result.nextQuery();
                    } else {
                        hasMore = false;
                    }

                    for(Status s : result.getTweets()){
                        statuses.add(s);
                    }

                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter = new TimelineArrayAdapter(context, statuses);
                            listView.setAdapter(adapter);
                            listView.setVisibility(View.VISIBLE);

                            LinearLayout spinner = (LinearLayout) layout.findViewById(R.id.list_progress);
                            spinner.setVisibility(View.GONE);
                        }
                    });
                } catch (Throwable e) {
                    e.printStackTrace();
                    ((Activity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Toast.makeText(context, getString(R.id.error), Toast.LENGTH_SHORT).show();
                            } catch (IllegalStateException e) {
                                // not attached to activity
                            }
                        }
                    });
                }

                canRefresh = true;
            }
        }).start();
    }

    public boolean canRefresh = true;

    public void getMore() {
        if (hasMore) {
            canRefresh = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Twitter twitter = Utils.getTwitter(context, settings);
                        QueryResult result = twitter.search(query);

                        for (twitter4j.Status status : result.getTweets()) {
                            statuses.add(status);
                        }

                        if (result.hasNext()) {
                            query = result.nextQuery();
                            hasMore = true;
                        } else {
                            hasMore = false;
                        }

                        ((Activity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();
                                canRefresh = true;
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        ((Activity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                canRefresh = false;
                            }
                        });
                    }
                }
            }).start();
        }
    }

    public int toDP(int px) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, getResources().getDisplayMetrics());
    }

}