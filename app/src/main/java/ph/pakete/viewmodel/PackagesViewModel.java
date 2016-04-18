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

    public Observable<ReplaySubject<Package>> trackPackage(ReplaySubject<Package> aPackage) {
        return Observable.create((Observable.OnSubscribe<ReplaySubject<Package>>) subscriber -> {
            PaketeApplication application = PaketeApplication.get(context);
            PaketeService paketeService = application.getPaketeService();

            Map<String, String> query = new HashMap<>();
            query.put("courier", aPackage.getValue().getCourier().getCode());
            query.put("tracking_number", aPackage.getValue().getTrackingNumber());
            paketeService.track(query)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<Package>() {
                        @Override
                        public void onCompleted() {
                            subscriber.onCompleted();
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e(TAG, "Error tracking package ", e);
                            subscriber.onError(e);
                        }

                        @Override
                        public void onNext(Package newPackage) {
                            newPackage.setName(aPackage.getValue().getName());
                            newPackage.setCourier(aPackage.getValue().getCourier());
                            savePackage(newPackage);
                            ReplaySubject<Package> packageReplaySubject = ReplaySubject.create();
                            packageReplaySubject.onNext(newPackage);
                            subscriber.onNext(packageReplaySubject);
                        }
                    });
        });
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

    public void addPackage(ReplaySubject<Package> aPackage) {
        packages.getValue().add(0, aPackage); // add at the top
        packages.onNext(packages.getValue());
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
            trackPackage(aPackage)
                    .subscribe(new Subscriber<ReplaySubject<Package>>() {
                        @Override
                        public void onCompleted() { }

                        @Override
                        public void onError(Throwable e) {
                            aPackage.getValue().setUpdating(false);
                            aPackage.onNext(aPackage.getValue());
                        }

                        @Override
                        public void onNext(ReplaySubject<Package> packageReplaySubject) {
                            aPackage.onNext(packageReplaySubject.getValue());
                        }
                    });
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

    private void savePackage(Package aPackage) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(aPackage);
        realm.commitTransaction();
    }

}
