package us.pico.fragment;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.Maneuver;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.Router;
import com.here.android.mpa.routing.RoutingError;

import java.lang.ref.WeakReference;
import java.util.List;

import us.pico.R;
import us.pico.activity.Navigation;

import static com.here.services.playback.TestTrackSimulationApi.TAG;

/**
 * Created by ayush on 11/10/17.
 */

public class MapFragmentView {
    private MapFragment m_mapFragment;
    private Navigation m_activity;
    private Button m_naviControlButton;
    private Map m_map;
    private NavigationManager m_navigationManager;
    private GeoBoundingBox m_geoBoundingBox;
    private Route m_route;

    public MapFragmentView(Activity activity) {
        m_activity = (Navigation) activity;
        initMapFragment(m_activity.startLat,m_activity.startLong);
        initNaviControlButton();
    }

    private void initMapFragment(final double startLat, final double startLong) {
        /* Locate the mapFragment UI element */
        m_mapFragment = (MapFragment) m_activity.getFragmentManager()
                .findFragmentById(R.id.mapfragment);

        if (m_mapFragment != null) {
            /* Initialize the MapFragment, results will be given via the called back. */
            m_mapFragment.init(new OnEngineInitListener() {
                @Override
                public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {

                    if (error == Error.NONE) {
                        m_map = m_mapFragment.getMap();
                        m_map.setCenter(new GeoCoordinate(startLat, startLong),
                                Map.Animation.LINEAR);
                        m_map.setZoomLevel(13.2);
                        /*
                         * Get the NavigationManager instance.It is responsible for providing voice
                         * and visual instructions while driving and walking
                         */
                        m_navigationManager = NavigationManager.getInstance();

                        createRoute(m_activity.startLat,m_activity.startLong, m_activity.destLat,m_activity.destLong);
                    } else {
                        Toast.makeText(m_activity,
                                "ERROR: Cannot initialize Map with error " + error,
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

    }

    private void createRoute(double startLat, double startLong, double destLat, double destLong) {
        /* Initialize a CoreRouter */
        CoreRouter coreRouter = new CoreRouter();

        /* Initialize a RoutePlan */
        RoutePlan routePlan = new RoutePlan();

        /*
         * Initialize a RouteOption.HERE SDK allow users to define their own parameters for the
         * route calculation,including transport modes,route types and route restrictions etc.Please
         * refer to API doc for full list of APIs
         */
        RouteOptions routeOptions = new RouteOptions();
        /* Other transport modes are also available e.g Pedestrian */
        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
        /* Disable highway in this route. */
        routeOptions.setHighwaysAllowed(false);
        /* Calculate the shortest route available. */
        routeOptions.setRouteType(RouteOptions.Type.SHORTEST);
        /* Calculate 1 route. */
        routeOptions.setRouteCount(1);
        /* Finally set the route option */
        routePlan.setRouteOptions(routeOptions);

        RouteWaypoint startPoint = new RouteWaypoint(new GeoCoordinate(startLat, startLong));
        RouteWaypoint destination = new RouteWaypoint(new GeoCoordinate(destLat, destLong));

        /* Add both waypoints to the route plan */
        routePlan.addWaypoint(startPoint);
        routePlan.addWaypoint(destination);

        /* Trigger the route calculation,results will be called back via the listener */
        coreRouter.calculateRoute(routePlan,
                new Router.Listener<List<RouteResult>, RoutingError>() {

                    @Override
                    public void onProgress(int i) {
                        /* The calculation progress can be retrieved in this callback. */
                    }

                    @Override
                    public void onCalculateRouteFinished(List<RouteResult> routeResults,
                                                         RoutingError routingError) {
                        /* Calculation is done.Let's handle the result */
                        if (routingError == RoutingError.NONE) {
                            if (routeResults.get(0).getRoute() != null) {

                                m_route = routeResults.get(0).getRoute();
                                /* Create a MapRoute so that it can be placed on the map */
                                MapRoute mapRoute = new MapRoute(routeResults.get(0).getRoute());

                                /* Show the maneuver number on top of the route */
                                mapRoute.setManeuverNumberVisible(true);

                                /* Add the MapRoute to the map */
                                m_map.addMapObject(mapRoute);

                                /*
                                 * We may also want to make sure the map view is orientated properly
                                 * so the entire route can be easily seen.
                                 */
                                m_geoBoundingBox = routeResults.get(0).getRoute().getBoundingBox();
                                m_map.zoomTo(m_geoBoundingBox, Map.Animation.NONE,
                                        Map.MOVE_PRESERVE_ORIENTATION);

                                startNavigation(m_route);
                            } else {
                                Toast.makeText(m_activity,
                                        "Error:route results returned is not valid",
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(m_activity,
                                    "Error:route calculation returned error code: " + routingError,
                                    Toast.LENGTH_LONG).show();

                        }
                    }
                });
    }

    private void initNaviControlButton() {
        m_naviControlButton = (Button) m_activity.findViewById(R.id.naviCtrlButton);
        m_naviControlButton.setText(R.string.start_navi);
        m_naviControlButton.setOnClickListener(new View.OnClickListener() {
            @Override

            public void onClick(View v) {
                /*
                 * To start a turn-by-turn navigation, a concrete route object is required.We use
                 * the same steps from Routing sample app to create a route from 4350 Still Creek Dr
                 * to Langley BC without going on HWY.
                 *
                 * The route calculation requires local map data.Unless there is pre-downloaded map
                 * data on device by utilizing MapLoader APIs,it's not recommended to trigger the
                 * route calculation immediately after the MapEngine is initialized.The
                 * INSUFFICIENT_MAP_DATA error code may be returned by CoreRouter in this case.
                 *
                 */

                if (m_route == null){

                    createRoute(m_activity.startLat,m_activity.startLong, m_activity.destLat,m_activity.destLong);
                } else {

                    m_navigationManager.stop();
                    /*
                     * Restore the map orientation to show entire route on screen
                     */
                    m_map.zoomTo(m_geoBoundingBox, Map.Animation.NONE, 0f);
                    m_naviControlButton.setText(R.string.start_navi);
                    m_route = null;
                }
            }
        });
    }

    private void startNavigation(Route route) {
        String packet = "Navigation,Start,Start,0,Start";
        m_activity.sendNavData(packet);
        m_naviControlButton.setText(R.string.stop_navi);
        /* Display the position indicator on map */
        m_map.getPositionIndicator().setVisible(true);
        /* Configure Navigation manager to launch navigation on current map */
        m_navigationManager.setMap(m_map);
        /*
         * Start the turn-by-turn navigation.Please note if the transport mode of the passed-in
         * route is pedestrian, the NavigationManager automatically triggers the guidance which is
         * suitable for walking. Simulation and tracking modes can also be launched at this moment
         * by calling either simulate() or startTracking()
         */
        m_navigationManager.startNavigation(route);
        /*
         * Set the map update mode to ROADVIEW.This will enable the automatic map movement based on
         * the current location.If user gestures are expected during the navigation, it's
         * recommended to set the map update mode to NONE first. Other supported update mode can be
         * found in HERE Android SDK API doc
         */
        m_navigationManager.setMapUpdateMode(NavigationManager.MapUpdateMode.ROADVIEW);

        /*
         * NavigationManager contains a number of listeners which we can use to monitor the
         * navigation status and getting relevant instructions.In this example, we will add 2
         * listeners for demo purpose,please refer to HERE Android SDK API documentation for details
         */
        addNavigationListeners();
    }

    private void addNavigationListeners() {

        /*
         * Register a NavigationManagerEventListener to monitor the status change on
         * NavigationManager
         */
        m_navigationManager.addNavigationManagerEventListener(
                new WeakReference<NavigationManager.NavigationManagerEventListener>(
                        m_navigationManagerEventListener));

        /* Register a PositionListener to monitor the position updates */
        m_navigationManager.addPositionListener(
                new WeakReference<NavigationManager.PositionListener>(m_positionListener));

        m_navigationManager.addNewInstructionEventListener(new WeakReference<NavigationManager.NewInstructionEventListener>(m_instructionlistener));
    }

    private NavigationManager.NewInstructionEventListener m_instructionlistener = new NavigationManager.NewInstructionEventListener() {
        @Override
        public void onNewInstructionEvent() {
            Maneuver maneuver = m_navigationManager.getNextManeuver();
            if (maneuver != null) {
                String packet;
                if (maneuver.getAction() == Maneuver.Action.END) {
                    //notify the user that the route is complete
                    packet = "Navigation,Completed,End,0,End";
                }

                packet = "Navigation,Ongoing,"
                        + maneuver.getTurn().toString() + ","
                        + maneuver.getDistanceToNextManeuver() + ","
                        + maneuver.getAction().toString();
                Log.d(TAG, "onNewInstructionEvent: " + maneuver.getTurn().toString());
                Log.d(TAG, "onNewInstructionEvent: " + maneuver.getDistanceToNextManeuver());
                Log.d(TAG, "onNewInstructionEvent: " + maneuver.getAction().toString());

                m_activity.sendNavData(packet);
            }
        }
    };

    private NavigationManager.PositionListener m_positionListener = new NavigationManager.PositionListener() {
        @Override
        public void onPositionUpdated(GeoPosition geoPosition) {
            /* Current position information can be retrieved in this callback */

        }
    };

    private NavigationManager.NavigationManagerEventListener m_navigationManagerEventListener = new NavigationManager.NavigationManagerEventListener() {
        @Override
        public void onRunningStateChanged() {
//            Toast.makeText(m_activity, "Running state changed", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onNavigationModeChanged() {
//            Toast.makeText(m_activity, "Navigation mode changed", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onEnded(NavigationManager.NavigationMode navigationMode) {
//            Toast.makeText(m_activity, navigationMode + " was ended", Toast.LENGTH_SHORT).show();
            String packet = "Navigation,Stopped,End,0,End";
            m_activity.sendNavData(packet);
        }

        @Override
        public void onMapUpdateModeChanged(NavigationManager.MapUpdateMode mapUpdateMode) {
//            Toast.makeText(m_activity, "Map update mode is changed to " + mapUpdateMode,
//                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onRouteUpdated(Route route) {
//            Toast.makeText(m_activity, "Route updated", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCountryInfo(String s, String s1) {
//            Toast.makeText(m_activity, "Country info updated from " + s + " to " + s1,
//                    Toast.LENGTH_SHORT).show();
        }
    };

    public void onDestroy() {
        /* Stop the navigation when app is destroyed */
        if (m_navigationManager != null) {
            m_navigationManager.stop();
        }
    }
}