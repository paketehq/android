package ph.pakete;

import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;

import java.util.ArrayList;
import java.util.List;

import io.fabric.sdk.android.Fabric;
import io.fabric.sdk.android.Kit;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.smooch.core.Smooch;
import ph.pakete.model.PaketeService;
import rx.Scheduler;
import rx.schedulers.Schedulers;

public class PaketeApplication extends Application {
    private PaketeService paketeService;
    private Scheduler defaultSubscribeScheduler;
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Realm
        RealmConfiguration config = new RealmConfiguration.Builder(this).build();
        Realm.setDefaultConfiguration(config);
        // Smooch
        Smooch.init(this, getResources().getString(R.string.smooch_app_token));
        // Fabric
        List<Kit> kits = new ArrayList<>();
        TwitterAuthConfig authConfig = new TwitterAuthConfig(getResources().getString(R.string.twitter_key), getResources().getString(R.string.twitter_secret));
        kits.add(new TwitterCore(authConfig));
        kits.add(new TweetComposer());
        if (BuildConfig.USE_CRASHLYTICS) {
            kits.add(new Crashlytics());
        }
        Fabric.with(this, kits.toArray(new Kit[kits.size()]));

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
