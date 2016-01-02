package steer.clear.fragment;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import steer.clear.MainApp;
import steer.clear.R;
import steer.clear.activity.ActivityHome;
import steer.clear.adapter.AdapterAutoComplete;
import steer.clear.event.EventAnimateToMarker;
import steer.clear.event.EventPlacesChosen;
import steer.clear.util.Hue;
import steer.clear.util.LoadingDialog;
import steer.clear.util.ViewUtils;
import steer.clear.view.ViewAutoComplete;
import steer.clear.view.ViewFooter;
import steer.clear.view.ViewHeader;
import steer.clear.view.ViewMarkerSelectLayout;

public class FragmentMap extends Fragment
	implements View.OnClickListener, ViewAutoComplete.AutoCompleteListener,
            OnMapReadyCallback, GoogleMap.OnMarkerDragListener, GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapClickListener {

    @Bind(R.id.fragment_map_header) ViewHeader header;
	@Bind(R.id.fragment_map_pickup) ViewAutoComplete pickupText;
    @Bind(R.id.fragment_map_dropoff) ViewAutoComplete dropoffText;
	@Bind(R.id.fragment_map_view) MapView mapView;
    @Bind(R.id.fragment_map_post) ViewFooter confirm;
    @Bind(R.id.fragment_map_current_location) ImageButton myLocation;
    @Bind(R.id.fragment_map_marker_select_layout) ViewMarkerSelectLayout viewMarkerSelectLayout;

    @Inject EventBus bus;
    private LoadingDialog loadingDialog;

	private LatLng pickupLatLng;
	private CharSequence pickupName;
    private LatLng dropoffLatLng;
    private CharSequence dropoffName;

	private final static String PICKUP_TEXT = "pickupText";
    private final static String DROPOFF_TEXT = "dropoffText";
    private final static String USER_LATITUDE = "lat";
    private final static String USER_LONGITUDE = "lng";
    private final static String PICKUP_MARKER_TITLE = "Pick Up Location";
    private final static String DROPOFF_MARKER_TITLE = "Drop Off Location";

    private Geocoder geocoder;
    private Marker pickupMarker;
    private Marker dropoffMarker;
    private AdapterAutoComplete mAdapter;
    private static final LatLngBounds BOUNDS_WILLIAMSBURG = new LatLngBounds(
			new LatLng(37.244926, -76.747861), new LatLng(37.295667, -76.686084));

	public FragmentMap() {}

	public static FragmentMap newInstance(LatLng userLocation) {
		FragmentMap frag = new FragmentMap();
		Bundle args = new Bundle();
        args.putDouble(USER_LATITUDE, userLocation.latitude);
        args.putDouble(USER_LONGITUDE, userLocation.longitude);
		frag.setArguments(args);
		return frag;
	}
	
	@Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
        outState.putString(PICKUP_TEXT, pickupText.getText().toString());
        outState.putString(DROPOFF_TEXT, dropoffText.getText().toString());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

	@Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((MainApp) context.getApplicationContext()).getApplicationComponent().inject(this);

        geocoder = new Geocoder(context, Locale.US);

        loadingDialog = new LoadingDialog(context, R.style.ProgressDialogTheme);

        bus.register(this);
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_map, container, false);
		ButterKnife.bind(this, rootView);

        mAdapter = new AdapterAutoComplete(getActivity(), R.layout.adapter_view,
                getGoogleApiClient(), BOUNDS_WILLIAMSBURG);

		pickupText.setAdapter(mAdapter);
        pickupText.setAutoCompleteListener(this);
        pickupText.setOnItemClickListener(pickupAdapterViewClick);

        dropoffText.setAdapter(mAdapter);
        dropoffText.setAutoCompleteListener(this);
        dropoffText.setOnItemClickListener(dropoffAdapterViewClick);

		mapView.onCreate(savedInstanceState);

		return rootView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

	    if (savedInstanceState != null) {
			pickupText.setText(savedInstanceState.getString(PICKUP_TEXT), false);
            dropoffText.setText(savedInstanceState.getString(DROPOFF_TEXT), false);
	    }

        mapView.getMapAsync(this);
    }

    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        AnimatorSet animatorSet = new AnimatorSet();
        if (enter) {
            animatorSet.playTogether(
                    ObjectAnimator.ofFloat(getActivity(), ViewUtils.SCALE_X, .45f, 1f),
                    ObjectAnimator.ofFloat(getActivity(), ViewUtils.SCALE_X, .45f, 1f),
                    ObjectAnimator.ofFloat(getActivity(), ViewUtils.ALPHA, 0f, 1f));
        } else {
            animatorSet.playTogether(
                    ObjectAnimator.ofFloat(getActivity(), ViewUtils.SCALE_X, 1f, .45f),
                    ObjectAnimator.ofFloat(getActivity(), ViewUtils.SCALE_Y, 1f, .45f),
                    ObjectAnimator.ofFloat(getActivity(), ViewUtils.ALPHA, 1f, 0f));
        }

        animatorSet.setDuration(1000);
        animatorSet.setInterpolator(new FastOutSlowInInterpolator());
        return animatorSet;
    }

    @Override
	public void onMapReady(GoogleMap map) {
        UiSettings settings = map.getUiSettings();
        settings.setCompassEnabled(false);
        settings.setMyLocationButtonEnabled(false);
        settings.setIndoorLevelPickerEnabled(false);
        map.setMyLocationEnabled(true);
        map.setOnMapClickListener(this);
        map.setOnMarkerDragListener(this);
        map.setOnMarkerClickListener(this);

        LatLng userLocation = new LatLng(getArguments().getDouble(USER_LATITUDE),
                getArguments().getDouble(USER_LONGITUDE));
        if (pickupLatLng != null) {
            dropMarkerOnMap(pickupLatLng, PICKUP_MARKER_TITLE);
            animateCameraToLocation(pickupLatLng);
        } else {
            dropMarkerOnMap(userLocation, PICKUP_MARKER_TITLE);
            animateCameraToLocation(userLocation);
        }

        if (dropoffLatLng != null) {
            dropMarkerOnMap(dropoffLatLng, DROPOFF_MARKER_TITLE);
        }

        mapView.setVisibility(View.VISIBLE);
	}

    @Override
    public void onMapClick(LatLng latLng) {
        if (!BOUNDS_WILLIAMSBURG.contains(latLng)) {
            Toast.makeText(getActivity(), getResources().getString(R.string.fragment_map_no_service),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int idOfSelectedButton = viewMarkerSelectLayout.getIdOfSelectedButton();
        if (idOfSelectedButton == -1) {
            Toast.makeText(getActivity(),
                    getResources().getString(R.string.fragment_map_marker_drop_hint),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        switch (idOfSelectedButton) {
            case R.id.fragment_map_show_pickup_location:
                pickupLatLng = latLng;

                dropMarkerOnMap(pickupLatLng, PICKUP_MARKER_TITLE);

                reverseGeocodeLocation(pickupLatLng, PICKUP_MARKER_TITLE);

                animateCameraToLocation(pickupLatLng);
                break;
            case R.id.fragment_map_show_dropoff_location:
                dropoffLatLng = latLng;

                dropMarkerOnMap(dropoffLatLng, DROPOFF_MARKER_TITLE);

                reverseGeocodeLocation(dropoffLatLng, DROPOFF_MARKER_TITLE);

                animateCameraToLocation(dropoffLatLng);
                break;
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Toast.makeText(getActivity(), getResources().getString(R.string.fragment_map_marker_hint),
                Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        reverseGeocodeLocation(marker.getPosition(), marker.getTitle());
    }

    @Override
    @OnClick({R.id.fragment_map_post, R.id.fragment_map_current_location})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fragment_map_post:
                if (pickupLatLng == null) {
                    Snackbar.make(getView(), getResources().getString(R.string.fragment_map_pickup_snackbar_text), Snackbar.LENGTH_SHORT).show();
                } else if (dropoffLatLng == null) {
                    Snackbar.make(getView(), getResources().getString(R.string.fragment_map_dropoff_snackbar_text), Snackbar.LENGTH_SHORT).show();
                } else if (pickupLatLng.equals(dropoffLatLng)) {
                    Snackbar.make(getView(), getResources().getString(R.string.fragment_map_same_location), Snackbar.LENGTH_SHORT).show();
                } else {
                    bus.post(new EventPlacesChosen(pickupLatLng, pickupName, dropoffLatLng, dropoffName));
                }
                break;
            case R.id.fragment_map_current_location:
                animateCameraToLocation(new LatLng(getArguments().getDouble(USER_LATITUDE),
                        getArguments().getDouble(USER_LONGITUDE)));
                break;
        }
    }

    @Override
    public void clearClicked(View v) {
        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (v.getWindowToken() != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }

        switch (v.getId()) {
            case R.id.fragment_map_pickup:
                pickupLatLng = null;
                pickupName = null;
                if (pickupMarker != null) {
                    pickupMarker.remove();
                }
                break;
            case R.id.fragment_map_dropoff:
                dropoffLatLng = null;
                dropoffName = null;
                if (dropoffMarker != null) {
                    dropoffMarker.remove();
                }
                break;
        }
    }

    public void onEvent(EventAnimateToMarker eventAnimateToMarker) {
        switch (eventAnimateToMarker.buttonId) {
            case R.id.fragment_map_show_pickup_location:
                if (pickupMarker != null) {
                    animateCameraToLocation(pickupMarker.getPosition());
                }
                break;
            case  R.id.fragment_map_show_dropoff_location:
                if (dropoffMarker != null) {
                    animateCameraToLocation(dropoffMarker.getPosition());
                }
                break;
        }
    }

    private GoogleApiClient getGoogleApiClient() {
        return ((ActivityHome) getActivity()).getGoogleApiClient();
    }

    private final AdapterView.OnItemClickListener pickupAdapterViewClick
            = (parent, view, position, id) -> {

        closeKeyboard(pickupText);

        final AdapterAutoComplete.AdapterAutoCompleteItem item = mAdapter.getItem(position);
        final String placeId = String.valueOf(item.placeId);
        loadingDialog.show();

        PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(getGoogleApiClient(), placeId);
        placeResult.setResultCallback(places -> {
            if (!places.getStatus().isSuccess()) {
                places.release();
                return;
            }

            final Place place = places.get(0);

            if (!BOUNDS_WILLIAMSBURG.contains(place.getLatLng())) {
                places.release();
                Toast.makeText(getActivity(), getResources().getString(R.string.toast_too_far_for_steer_clear),
                        Toast.LENGTH_SHORT).show();
                pickupLatLng = null;
                pickupName = null;
                pickupText.setText("");
                loadingDialog.dismiss();
                return;
            }

            loadingDialog.dismiss();

            pickupLatLng = place.getLatLng();
            pickupName = place.getAddress();

            dropMarkerOnMap(pickupLatLng, PICKUP_MARKER_TITLE);

            animateCameraToLocation(pickupLatLng);

            places.release();
        });
    };

    private final AdapterView.OnItemClickListener dropoffAdapterViewClick
            = (parent, view, position, id) -> {

        closeKeyboard(dropoffText);

        final AdapterAutoComplete.AdapterAutoCompleteItem item = mAdapter.getItem(position);
        final String placeId = String.valueOf(item.placeId);
        loadingDialog.show();

        PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(getGoogleApiClient(), placeId);
        placeResult.setResultCallback(places -> {
            if (!places.getStatus().isSuccess()) {
                places.release();
                return;
            }

            final Place place = places.get(0);

            if (!BOUNDS_WILLIAMSBURG.contains(place.getLatLng())) {
                places.release();
                Toast.makeText(getActivity(), getResources().getString(R.string.fragment_map_no_service),
                        Toast.LENGTH_SHORT).show();
                dropoffLatLng = null;
                dropoffName = null;
                dropoffText.setText("");
                loadingDialog.dismiss();
                return;
            }

            loadingDialog.dismiss();

            dropoffLatLng = place.getLatLng();
            dropoffName = place.getAddress();

            dropMarkerOnMap(dropoffLatLng, DROPOFF_MARKER_TITLE);

            animateCameraToLocation(dropoffLatLng);

            places.release();
        });
    };

    private void closeKeyboard(View v) {
        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (v.getWindowToken() != null) {
            imm.hideSoftInputFromWindow(pickupText.getWindowToken(), 0);
        }
    }

    private void dropMarkerOnMap(LatLng latLng, String whichMarker) {
        loadingDialog.show();
        switch (whichMarker) {
            case PICKUP_MARKER_TITLE:

                MarkerOptions options = new MarkerOptions()
                        .position(latLng)
                        .title(PICKUP_MARKER_TITLE)
                        .icon(BitmapDescriptorFactory.defaultMarker(
                                Hue.getHue(getResources().getColor(R.color.accent))))
                        .draggable(true);

                if (pickupMarker != null) {
                    pickupMarker.remove();
                }

                pickupMarker = mapView.getMap().addMarker(options);
                break;
            case DROPOFF_MARKER_TITLE:

                MarkerOptions options1 = new MarkerOptions()
                        .position(latLng)
                        .title(DROPOFF_MARKER_TITLE)
                        .icon(BitmapDescriptorFactory.defaultMarker(
                                Hue.getHue(getResources().getColor(R.color.accent))))
                        .draggable(true);

                if (dropoffMarker != null) {
                    dropoffMarker.remove();
                }
                dropoffMarker = mapView.getMap().addMarker(options1);
                break;
        }
        loadingDialog.dismiss();
    }

    private void reverseGeocodeLocation(LatLng latLng, String whichMarker) {
        if (!BOUNDS_WILLIAMSBURG.contains(latLng)) {
            Toast.makeText(getActivity(), getResources().getString(R.string.fragment_map_no_service),
                    Toast.LENGTH_SHORT).show();

            switch (whichMarker) {
                case PICKUP_MARKER_TITLE:
                    if (pickupLatLng != null) {
                        pickupMarker.setPosition(pickupLatLng);
                    }
                    break;
                case DROPOFF_MARKER_TITLE:
                    if (dropoffLatLng != null) {
                        dropoffMarker.setPosition(dropoffLatLng);
                    }
                    break;
            }
            return;
        }

        loadingDialog.show();
        try {
            Observable.just(geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(addresses -> {
                        loadingDialog.dismiss();
                        Address address = addresses.get(0);
                        switch (whichMarker) {
                            case PICKUP_MARKER_TITLE:
                                pickupLatLng = new LatLng(address.getLatitude(), address.getLongitude());
                                pickupName = address.getAddressLine(0) + ", " +
                                        address.getAddressLine(1);
                                pickupText.setText(address.getAddressLine(0) + ", " +
                                        address.getAddressLine(1), false);
                                break;
                            case DROPOFF_MARKER_TITLE:
                                dropoffLatLng = new LatLng(address.getLatitude(), address.getLongitude());
                                dropoffName = address.getAddressLine(0) + ", " +
                                        address.getAddressLine(1);
                                dropoffText.setText(address.getAddressLine(0) + ", " +
                                        address.getAddressLine(1), false);
                                break;
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void animateCameraToLocation(LatLng latLng) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)
                .zoom(17)
                .bearing(45)
                .tilt(45)
                .build();
        mapView.getMap().animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }
}