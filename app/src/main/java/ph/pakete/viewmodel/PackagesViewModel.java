package ph.pakete.viewmodel;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;
import ph.pakete.Constants;
import ph.pakete.PaketeApplication;
import ph.pakete.model.Courier;
import ph.pakete.model.Package;
import ph.pakete.PackagesSortBy;
import ph.pakete.model.PackageTrackHistory;
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
        refreshPackages();
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
        Package packageToUpdate = realm.where(Package.class).equalTo("trackingNumber", aPackage.getValue().getTrackingNumber()).findFirst();
        packageToUpdate.setName(name);
        realm.commitTransaction();
        // trigger update
        aPackage.onNext(packageToUpdate);
    }

    public void addPackage(ReplaySubject<Package> aPackage) {
        packages.getValue().add(0, aPackage); // add at the top
        packages.onNext(packages.getValue());
    }

    public void archivePackage(ReplaySubject<Package> aPackage) {
        packages.getValue().remove(aPackage);
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        Package packageToArchive = realm.where(Package.class).equalTo("trackingNumber", aPackage.getValue().getTrackingNumber()).findFirst();
        packageToArchive.setArchived(true);
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

    // Settings
    public PackagesSortBy packagesSortBy() {
        SharedPreferences preferences = PaketeApplication.getAppContext().getSharedPreferences(Constants.SharedPreferences.NAME, Context.MODE_PRIVATE);
        int packagesSortBy = preferences.getInt(Constants.SharedPreferences.SORT_BY_KEY, PackagesSortBy.LastUpdated.ordinal());
        return PackagesSortBy.fromInteger(packagesSortBy);
    }

    public boolean packagesGroupByDelivered() {
        SharedPreferences preferences = PaketeApplication.getAppContext().getSharedPreferences(Constants.SharedPreferences.NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(Constants.SharedPreferences.GROUP_BY_DELIVERED_KEY, true);
    }

    public void sortBy(PackagesSortBy sortBy) {
        SharedPreferences preferences = PaketeApplication.getAppContext().getSharedPreferences(Constants.SharedPreferences.NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(Constants.SharedPreferences.SORT_BY_KEY, sortBy.ordinal());
        editor.apply();
        // reload packages
        loadPackages();
    }

    public void groupByDelivered(boolean groupBy) {
        SharedPreferences preferences = PaketeApplication.getAppContext().getSharedPreferences(Constants.SharedPreferences.NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(Constants.SharedPreferences.GROUP_BY_DELIVERED_KEY, groupBy);
        editor.apply();
        // reload packages
        loadPackages();
    }

    private void loadCouriers() {
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<Courier> query = realm.where(Courier.class);
        RealmResults<Courier> results = query.findAllSorted("name");
        couriers.onNext(results);
    }

    private void loadPackages() {
        List<Package> sortedPackages = new ArrayList<>();
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<Package> query = realm.where(Package.class).equalTo("archived", false);
        sortedPackages.addAll(query.findAll());

        // sort packages
        switch (packagesSortBy()) {
            case LastUpdated:
                Collections.sort(sortedPackages, (lhs, rhs) -> {
                    PackageTrackHistory lhsTrackHistory = lhs.latestTrackHistory();
                    PackageTrackHistory rhsTrackHistory = rhs.latestTrackHistory();
                    if (lhsTrackHistory != null && rhsTrackHistory != null) {
                        return rhsTrackHistory.getDate().compareTo(lhsTrackHistory.getDate());
                    }
                    return 0;
                });
                break;
            case DateAdded:
                Collections.sort(sortedPackages, (lhs, rhs) -> rhs.getCreatedAt().compareTo(lhs.getCreatedAt()));
                break;
            case Name:
                Collections.sort(sortedPackages, (lhs, rhs) -> lhs.getName().compareTo(rhs.getName()));
                break;
        }

        // check if grouped by delivered
        if (packagesGroupByDelivered()) {
            // group packages by in transit and completed
            List<Package> inTransitPackages = new ArrayList<>();
            List<Package> completedPackages = new ArrayList<>();
            for (Package aPackage :sortedPackages) {
                if (aPackage.getCompleted()) {
                    completedPackages.add(aPackage);
                } else {
                    inTransitPackages.add(aPackage);
                }
            }
            sortedPackages.clear();
            sortedPackages.addAll(inTransitPackages);
            sortedPackages.addAll(completedPackages);
        }

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
