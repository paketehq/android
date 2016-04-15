package ph.pakete.viewmodel;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;
import ph.pakete.PaketeApplication;
import ph.pakete.model.Courier;
import ph.pakete.model.Package;
import ph.pakete.model.PaketeService;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.ReplaySubject;

public class PackagesViewModel implements ViewModel {

    private static final String TAG = "PackagesViewModel";

    public ReplaySubject<List<ReplaySubject<Package>>> packages = ReplaySubject.create();
    public ReplaySubject<List<Courier>> couriers = ReplaySubject.create();

    private Context context;

    public PackagesViewModel(Context context) {
        this.context = context;
        loadCouriers();
        loadPackages();
        // nuke track history
//        Realm realm = Realm.getDefaultInstance();
//        RealmQuery<Package> query = realm.where(Package.class).equalTo("archived", false);
//        RealmResults<Package> results = query.findAll();
//        realm.beginTransaction();
//        for (int i = 0; i < results.size(); i++) {
//            Package aPackage = results.get(i);
//            aPackage.getTrackHistory().deleteAllFromRealm();
//        }
//        realm.commitTransaction();
        refreshPackages();
        getCouriers();
    }

    @Override
    public void destroy() {
        //In this case destroy doesn't need to do anything because there is not async calls
    }

    public Observable<Void> getCouriers() {
        return  Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                PaketeApplication application = PaketeApplication.get(context);
                PaketeService paketeService = application.getPaketeService();
                paketeService.couriers()
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<List<Courier>>() {
                            @Override
                            public void onCompleted() {
                                subscriber.onCompleted();
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.e(TAG, "Error couriers " + e);
                                subscriber.onError(e);
                            }

                            @Override
                            public void onNext(List<Courier> couriers) {
                                Log.i(TAG, "Couriers loaded " + couriers);
                                saveCouriers(couriers);
                                loadCouriers();
                            }
                        });
            }
        });
    }

    public Observable<ReplaySubject<Package>> addPackage(String trackingNumber, String name, Courier courier) {
        return Observable.create(new Observable.OnSubscribe<ReplaySubject<Package>>() {
            @Override
            public void call(Subscriber<? super ReplaySubject<Package>> subscriber) {
                PaketeApplication application = PaketeApplication.get(context);
                PaketeService paketeService = application.getPaketeService();

                Map<String, String> query = new HashMap<>();
                query.put("courier", courier.getCode());
                query.put("tracking_number", trackingNumber);
                paketeService.track(query)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Subscriber<Package>() {
                            @Override
                            public void onCompleted() {
                                subscriber.onCompleted();
                            }

                            @Override
                            public void onError(Throwable error) {
                                subscriber.onError(error);
                            }

                            @Override
                            public void onNext(Package aPackage) {
                                Package savedPackage = savePackage(aPackage, name, courier);
                                // update packages list
                                ReplaySubject<Package> subject = ReplaySubject.create();
                                subject.onNext(savedPackage);
                                packages.getValue().add(0, subject); // add at the top
                                packages.onNext(packages.getValue());
                                subscriber.onNext(subject);
                            }
                        });
            }
        });
    }

    public void trackPackage(ReplaySubject<Package> aPackage) {
        PaketeApplication application = PaketeApplication.get(context);
        PaketeService paketeService = application.getPaketeService();

        Map<String, String> query = new HashMap<>();
        query.put("courier", aPackage.getValue().getCourier().getCode());
        query.put("tracking_number", aPackage.getValue().getTrackingNumber());
        paketeService.track(query)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(aPackage1 -> aPackage.onNext(updatePackage(aPackage1)))
                .doOnError(throwable -> {
                    Log.e(TAG, "Error tracking package ", throwable);
                    aPackage.getValue().setUpdating(false);
                    aPackage.onNext(aPackage.getValue());
                })
                .onErrorReturn(throwable1 -> aPackage.getValue())
                .subscribe();
    }

    public Package packageForTrackingNumber(String trackingNumber, Courier courier) {
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<Package> query = realm.where(Package.class)
                .equalTo("trackingNumber", trackingNumber)
                .equalTo("courier.code", courier.getCode());
        return query.findFirst();
    }

    public void updatePackageName(String name, ReplaySubject<Package> aPackage) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        aPackage.getValue().setName(name);
        realm.commitTransaction();
        // trigger update
        aPackage.onNext(aPackage.getValue());
    }

    public void archivePackage(ReplaySubject<Package> aPackage) {
        packages.getValue().remove(aPackage);
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        aPackage.getValue().setArchived(true);
        realm.commitTransaction();
        // trigger to update
        packages.onNext(packages.getValue());
    }

    public void unarchivePackage(Package aPackage) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        aPackage.setArchived(false);
        realm.commitTransaction();
        // update packages list
        ReplaySubject<Package> subject = ReplaySubject.create();
        subject.onNext(aPackage);
        packages.getValue().add(0, subject);
        packages.onNext(packages.getValue());
    }

    public void refreshPackages() {
        for (ReplaySubject<Package> aPackage: packages.getValue()) {
            if (aPackage.getValue().getCompleted() || aPackage.getValue().getUpdating()) { continue; }
            aPackage.getValue().setUpdating(true);
            aPackage.onNext(aPackage.getValue()); // trigger to refresh object
            trackPackage(aPackage);
        }
    }

    private void loadCouriers() {
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<Courier> query = realm.where(Courier.class);
        RealmResults<Courier> results = query.findAll();
        results.sort("name");
        couriers.onNext(results);
    }

    private void loadPackages() {
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<Package> query = realm.where(Package.class).equalTo("archived", false);
        RealmResults<Package> results = query.findAllSorted("createdAt", Sort.DESCENDING);
        // we would like to sort packages by grouping intransit at the top and completed at the bottom
        RealmResults<Package> inTransitPackages = results.where().equalTo("completed", false).findAll();
        RealmResults<Package> completedPackages = results.where().equalTo("completed", true).findAll();
        List<Package> sortedPackages = new ArrayList<>(inTransitPackages);
        sortedPackages.addAll(completedPackages);

        // convert to replay subjects
        List<ReplaySubject<Package>> aPackages = new ArrayList<>();
        for (Package aPackage :sortedPackages) {
            ReplaySubject<Package>replayPackage = ReplaySubject.create();
            replayPackage.onNext(aPackage);
            aPackages.add(replayPackage);
        }
        packages.onNext(aPackages);
    }

    private void saveCouriers(List<Courier> couriers) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(couriers);
        realm.commitTransaction();
    }

    private Package savePackage(Package aPackage, String name, Courier courier) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        aPackage.setCourier(courier);
        aPackage.setName(name);
        aPackage.setCourier(courier);
        realm.copyToRealmOrUpdate(aPackage);
        realm.commitTransaction();

        return aPackage;
    }

    private Package updatePackage(Package aPackage) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        Package existingPackage = realm.where(Package.class)
                .equalTo("trackingNumber", aPackage.getTrackingNumber())
                .findFirst();
        existingPackage.getTrackHistory().deleteAllFromRealm();
        existingPackage.getTrackHistory().addAll(aPackage.getTrackHistory());
        existingPackage.setCompleted(aPackage.getCompleted());
        existingPackage.setUpdating(false);
        realm.commitTransaction();
        return existingPackage;
    }
}
