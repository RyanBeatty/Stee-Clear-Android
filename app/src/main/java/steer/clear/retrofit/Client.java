package steer.clear.retrofit;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;

import com.squareup.okhttp.OkHttpClient;

import java.lang.ref.WeakReference;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import steer.clear.MainApp;
import steer.clear.R;
import steer.clear.activity.ActivityAuthenticate;
import steer.clear.pojo.LoginPost;
import steer.clear.pojo.RegisterPost;
import steer.clear.pojo.RideObject;
import steer.clear.pojo.RidePost;
import steer.clear.util.Datastore;
import steer.clear.util.ErrorDialog;

public class Client {

    private Api apiInterface;
    private Authenticate authenticateInterface;
    private Application application;

    @Inject Datastore store;

	public Client(Application application) {
        this.application = application;
        ((MainApp) application).getApplicationComponent().inject(this);

        Interceptor interceptor = new Interceptor(store);

        OkHttpClient okHttpClient = new OkHttpClient();
        CookieManager cookieHandler = new CookieManager();
        cookieHandler.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        okHttpClient.setFollowRedirects(true);
        okHttpClient.setCookieHandler(cookieHandler);
        okHttpClient.setConnectTimeout(10, TimeUnit.SECONDS);
        OkClient okClient = new OkClient(okHttpClient);

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(application.getResources().getString(R.string.url_base))
                .setClient(okClient)
                .setRequestInterceptor(interceptor)
                .build();
        apiInterface = restAdapter.create(Api.class);

        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint(application.getResources().getString(R.string.url_authenticate))
                .setClient(okClient)
                .setRequestInterceptor(interceptor)
                .build();
        authenticateInterface = adapter.create(Authenticate.class);
	}

    public Observable<Response> checkCookie() {
        return apiInterface.checkCookie();
    }

    public Observable<Response> login(String username, String password) {
        return authenticateInterface.login(new LoginPost(username, password));
    }

    public Observable<Response> register(String username, String password, String phone) {
        return authenticateInterface.register(new RegisterPost(username, password, phone));
    }

	public Observable<RideObject> addRide(final Integer numPassengers,
                        final Double startLag, final Double startLong,
			            final Double endLat, final Double endLong) {
        return apiInterface.addRide(new RidePost(numPassengers, startLag, startLong, endLat, endLong));
	}

	public Observable<Response> cancelRide(int cancelId) {
        return apiInterface.deleteRide(cancelId);
	}

    public Observable<Response> checkRideStatus(int cancelId) { return apiInterface.checkRideStatus(cancelId); }

    public Observable<Response> logout() { return authenticateInterface.logout(); }
}
