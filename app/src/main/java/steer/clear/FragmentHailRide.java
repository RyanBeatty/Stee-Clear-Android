package steer.clear;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class FragmentHailRide extends Fragment implements OnClickListener, OnTouchListener {
	
	// Global views
	private TextView pickup;
	private ImageButton changePickup;
	private TextView dropoff;
	private ImageButton changeDropoff;
	private ImageButton postRide;
	private TextView numPassengers;
	
	// static int used for, you guessed it, storing the current passenger count
	private static int passengers = 0;

	// Final static strings used as keys for getArguments()
	private final static String PICKUP = "pickup";
	private final static String DROPOFF = "dropoff";

	// Needed for life
	private final static String POST = "post";
	
	private ListenerForFragments listener;
	public FragmentHailRide(){}
	
	/**
	 * Instantiates newInstance of this fragment with two variables: the name of the pickupLocation and the dropoffLocation
	 * @param pickupLocationName
	 * @param dropoffLocationName
	 * @return
	 */
	public static FragmentHailRide newInstance(CharSequence pickupLocationName,
											   CharSequence dropoffLocationName) {
		FragmentHailRide frag = new FragmentHailRide();
		Bundle args = new Bundle();
		args.putCharSequence(PICKUP, pickupLocationName);
		args.putCharSequence(DROPOFF, dropoffLocationName);
		frag.setArguments(args);
		return frag;
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (ListenerForFragments) activity;
        } catch (ClassCastException e) {
        	e.printStackTrace();
        }
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_hail_ride, container, false);
		
		Bundle args = getArguments();
		
		pickup = (TextView) rootView.findViewById(R.id.fragment_hail_ride_pickup_location);
		pickup.setText("PICKUP LOCATION: \n" + args.getCharSequence(PICKUP));

		changePickup = (ImageButton) rootView.findViewById(R.id.fragment_hail_ride_change_pickup);
		changePickup.setOnClickListener(this);
		
		dropoff = (TextView) rootView.findViewById(R.id.fragment_hail_ride_dropoff_location);
		dropoff.setText("DROPOFF LOCATION: \n" + args.getCharSequence(DROPOFF));

		changeDropoff = (ImageButton) rootView.findViewById(R.id.fragment_hail_ride_change_dropoff);
		changeDropoff.setOnClickListener(this);
		
		numPassengers = (TextView) rootView.findViewById(R.id.fragment_hail_ride_passenger_select);
		numPassengers.setOnTouchListener(this);
		
		postRide = (ImageButton) rootView.findViewById(R.id.fragment_hail_ride_post);
		postRide.setOnClickListener(this);
		return rootView;
	}

	@Override
	public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
		Animator animator;
		if (enter) {
			animator = ObjectAnimator.ofFloat(getActivity(), "alpha", 0, 1);
		} else {
			animator = ObjectAnimator.ofFloat(getActivity(), "alpha", 1, 0);
		}

		animator.setDuration(750);
		animator.setInterpolator(new AccelerateDecelerateInterpolator());
		return animator;
	}

	public void onLocationChanged(String whichChanged, CharSequence newLocationName) {
		switch (whichChanged) {
			case PICKUP:
				pickup.setText("PICKUP LOCATION: \n" + newLocationName);
				break;

			case DROPOFF:
				dropoff.setText("DROPOFF LOCATION: \n" + newLocationName);
				break;
		}
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.fragment_hail_ride_post:
				if (passengers != 0) {
					listener.makeHttpPostRequest(passengers);
				} else {
					Toast.makeText(getActivity(), "Choose number of passengers", Toast.LENGTH_SHORT).show();
				}
				break;

			case R.id.fragment_hail_ride_change_pickup:
				listener.changePickup();
				break;

			case R.id.fragment_hail_ride_change_dropoff:
				listener.changeDropoff();
				break;
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		final TextView view = (TextView) v;
		v.performClick();
		final int DRAWABLE_LEFT = 0;
        //final int DRAWABLE_TOP = 1;
        final int DRAWABLE_RIGHT = 2;
        //final int DRAWABLE_BOTTOM = 3;
        
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
			if (event.getRawX() >= (view.getRight() - view.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
				if (passengers < 8) {
					passengers = passengers + 1;
					view.setText(String.valueOf(passengers));
				}
				return true;
			}

			if (event.getRawX() <= (view.getLeft() + view.getCompoundDrawables()[DRAWABLE_LEFT].getBounds().width())) {
				if (passengers > 1) {
					passengers = passengers - 1;
					view.setText(String.valueOf(passengers));
				}
				return true;
			}
		}
		return true;
	}
}
