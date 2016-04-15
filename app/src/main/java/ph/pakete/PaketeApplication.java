package ph.pakete;

import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.smooch.core.Smooch;
import ph.pakete.model.PaketeService;
import rx.Scheduler;
import rx.schedulers.Schedulers;
import io.fabric.sdk.android.Fabric;

public class PaketeApplication extends Application {
    private PaketeService paketeService;
    private Scheduler defaultSubscribeScheduler;
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        // Initialize Realm
        RealmConfiguration config = new RealmConfiguration.Builder(this).build();
        Realm.setDefaultConfiguration(config);
        // Smooch
        Smooch.init(this, getResources().getString(R.string.smooch_app_token));
        // Fabric
        // check if fabric token is empty
        if (!getResources().getString(R.string.fabric_api_key).isEmpty()) {
            Fabric.with(this, new Crashlytics());
        }

        context = getApplicationContext();
    }

    public static PaketeApplication get(Context context) {
        return (PaketeApplication) context.getApplicationContext();
    }

    public PaketeService getPaketeService() {
        if (paketeService == null) {
            paketeService = PaketeService.Factory.create();
        }
        return paketeService;
    }

    public static Context getAppContext() {
        return context;
    }

    //For setting mocks during testing
    public void setPaketeService(PaketeService paketeService) {
        this.paketeService = paketeService;
    }

    public Scheduler defaultSubscribeScheduler() {
        if (defaultSubscribeScheduler == null) {
            defaultSubscribeScheduler = Schedulers.io();
        }
        return defaultSubscribeScheduler;
    }

    //User to change scheduler from tests
    public void setDefaultSubscribeScheduler(Scheduler scheduler) {
        this.defaultSubscribeScheduler = scheduler;
    }
}
