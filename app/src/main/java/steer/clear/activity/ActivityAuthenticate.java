package steer.clear.activity;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;
import steer.clear.event.EventAuthenticate;
import steer.clear.util.Datastore;
import steer.clear.MainApp;
import steer.clear.R;
import steer.clear.fragment.FragmentAuthenticate;
import steer.clear.retrofit.Client;
import steer.clear.util.ErrorDialog;
import steer.clear.util.Logger;


public class ActivityAuthenticate extends AppCompatActivity {

    @Inject Client helper;
    @Inject Datastore store;
    @Inject EventBus bus;

    private static final String AUTHENTICATE_TAG = "authenticate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ((MainApp) getApplication()).getApplicationComponent().inject(this);

        bus.register(this);

        addFragmentAuthenticate();
    }

    private void addFragmentAuthenticate() {
        FragmentManager manager = getFragmentManager();
        FragmentAuthenticate login = (FragmentAuthenticate) manager.findFragmentByTag(AUTHENTICATE_TAG);
        if (login != null) {
            manager.beginTransaction().show(login).commit();
        } else {
            manager.beginTransaction()
                    .add(R.id.activity_home_fragment_frame,
                            FragmentAuthenticate.newInstance(store.checkRegistered()), AUTHENTICATE_TAG)
                    .commit();
        }
    }

    public void onEvent(EventAuthenticate eventAuthenticate) {
        if (store.checkRegistered()) {
            helper.login(new WeakReference<>(this),
                    eventAuthenticate.username, eventAuthenticate.password);
        } else {
            helper.register(new WeakReference<>(this),
                    eventAuthenticate.username, eventAuthenticate.password, eventAuthenticate.phone);
        }
    }

    public void onRegisterSuccess() {
        store.userHasRegistered();
        Logger.log("ON REGISTER SUCESSS");
    }

    public void onRegisterError(int errorCode) {
        FragmentAuthenticate fragmentAuthenticate = (FragmentAuthenticate)
                getFragmentManager().findFragmentByTag(AUTHENTICATE_TAG);
        if (fragmentAuthenticate != null) {
            fragmentAuthenticate.stopTheRipple();
        }
        Logger.log("ON REGISTER ERROR: " + errorCode);
        ErrorDialog.createFromErrorCode(this, errorCode).show();
    }

    public void onLoginSuccess() {
        Logger.log("ON LOGIN SUCCESS");
    }

    public void onLoginError(int errorCode) {
        FragmentAuthenticate fragmentAuthenticate = (FragmentAuthenticate)
                getFragmentManager().findFragmentByTag(AUTHENTICATE_TAG);
        if (fragmentAuthenticate != null) {
            fragmentAuthenticate.stopTheRipple();
        }
        Logger.log("ON REGISTER ERROR: " + errorCode);
        ErrorDialog.createFromErrorCode(this, errorCode).show();
    }

}
