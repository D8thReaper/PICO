package us.pico.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.search.DiscoveryResult;
import com.here.android.mpa.search.DiscoveryResultPage;
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.PlaceLink;
import com.here.android.mpa.search.ResultListener;
import com.here.android.mpa.search.SearchRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import us.pico.R;
import us.pico.activity.ResultListActivity;
import us.pico.activity.Search;

/**
 * Created by ayush on 11/10/17.
 */

public class MapSearchFragment {

    private static final String TAG = "MapSearchFragment";
    public static List<DiscoveryResult> s_ResultList;
    private MapFragment m_mapFragment;
    private Search m_activity;
    private Map m_map;
    private Button m_placeDetailButton;
    private List<MapObject> m_mapObjectList = new ArrayList<>();
    public double startLong, startLat;
    private ProgressDialog dialog;

    public MapSearchFragment(Activity activity) {
        m_activity = (Search) activity;
        /*
         * The map fragment is not required for executing search requests. However in this example,
         * we will put some markers on the map to visualize the location of the search results.
         */

        dialog = new ProgressDialog(m_activity);
        dialog.setMessage("Initializing");
        dialog.show();

        initMapFragment();


        initSearchControlButtons();
        /* We use a list view to present the search results */
        initResultListButton();
    }

    private void initMapFragment() {
        /* Locate the mapFragment UI element */
        m_mapFragment = (MapFragment) m_activity.getFragmentManager()
                .findFragmentById(R.id.mapfragment);

        if (m_mapFragment != null) {
            /* Initialize the MapFragment, results will be given via the called back. */
            m_mapFragment.init(new OnEngineInitListener() {
                @Override
                public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                    if (error == Error.NONE) {



                        final PositioningManager posManager = PositioningManager.getInstance();

                        if (posManager!=null) {
                            posManager.start(PositioningManager.LocationMethod.GPS_NETWORK);

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    GeoCoordinate pos = posManager.getPosition().getCoordinate();

                                    startLat = pos.getLatitude();
                                    startLong = pos.getLongitude();

                                    Log.d(TAG, "onCreate: Got cords");
                                    Log.d(TAG, "onCreate: " + startLat);
                                    Log.d(TAG, "onCreate: " + startLong);

                                    posManager.stop();

                                    m_map = m_mapFragment.getMap();
                                    m_map.setCenter(new GeoCoordinate(startLat, startLong),
                                            Map.Animation.NONE);
                                    m_map.setZoomLevel(13.2);

                                    dialog.dismiss();
                                }
                            },3000);


                        } else {
                            Log.d(TAG, "MapSearchFragment: Not located");
                        }
                    } else {
                        Toast.makeText(m_activity,
                                "ERROR: Cannot initialize Map with error " + error,
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

    }

    private void initResultListButton() {
        m_placeDetailButton = (Button) m_activity.findViewById(R.id.resultListBtn);
        m_placeDetailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Open the ResultListActivity */
                Intent intent = new Intent(m_activity, ResultListActivity.class);
                intent.putExtra("startLat",startLat);
                intent.putExtra("startLong",startLong);
                m_activity.startActivity(intent);
            }
        });
    }

    private void initSearchControlButtons() {

        final Button searchRequestButton = (Button) m_activity.findViewById(R.id.searchRequestBtn);
        final EditText inputSearch = (EditText) m_activity.findViewById(R.id.input_search);

        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                searchRequestButton.setEnabled(s.length() != 0);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        searchRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Trigger a SearchRequest based on the current map center and search query
                 * "Hotel".Please refer to HERE Android SDK API doc for other supported location
                 * parameters and categories.
                 */
                cleanMap();
                SearchRequest searchRequest = new SearchRequest(inputSearch.getText().toString());
                searchRequest.setSearchCenter(m_map.getCenter());
                searchRequest.execute(discoveryResultPageListener);
            }
        });
    }

    private ResultListener<DiscoveryResultPage> discoveryResultPageListener = new ResultListener<DiscoveryResultPage>() {
        @Override
        public void onCompleted(DiscoveryResultPage discoveryResultPage, ErrorCode errorCode) {
            if (errorCode == ErrorCode.NONE) {
                /* No error returned,let's handle the results */
                m_placeDetailButton.setVisibility(View.VISIBLE);

                /*
                 * The result is a DiscoveryResultPage object which represents a paginated
                 * collection of items.The items can be either a PlaceLink or DiscoveryLink.The
                 * PlaceLink can be used to retrieve place details by firing another
                 * PlaceRequest,while the DiscoveryLink is designed to be used to fire another
                 * DiscoveryRequest to obtain more refined results.
                 */
                s_ResultList = discoveryResultPage.getItems();
                for (DiscoveryResult item : s_ResultList) {
                    /*
                     * Add a marker for each result of PlaceLink type.For best usability, map can be
                     * also adjusted to display all markers.This can be done by merging the bounding
                     * box of each result and then zoom the map to the merged one.
                     */
                    if (item.getResultType() == DiscoveryResult.ResultType.PLACE) {
                        PlaceLink placeLink = (PlaceLink) item;
                        addMarkerAtPlace(placeLink);
                    }
                }
            } else {
                Toast.makeText(m_activity,
                        "ERROR:Discovery search request returned return error code+ " + errorCode,
                        Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void addMarkerAtPlace(PlaceLink placeLink) {
        Image img = new Image();
        try {
            img.setImageResource(R.drawable.marker);
        } catch (IOException e) {
            e.printStackTrace();
        }

        MapMarker mapMarker = new MapMarker();
        mapMarker.setIcon(img);
        mapMarker.setCoordinate(new GeoCoordinate(placeLink.getPosition()));
        m_map.addMapObject(mapMarker);
        m_mapObjectList.add(mapMarker);
    }

    private void cleanMap() {
        if (!m_mapObjectList.isEmpty()) {
            m_map.removeMapObjects(m_mapObjectList);
            m_mapObjectList.clear();
        }
        m_placeDetailButton.setVisibility(View.GONE);
    }
}