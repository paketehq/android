package ph.pakete.viewmodel;

import android.content.Context;
import android.databinding.BaseObservable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.content.ContextCompat;
import android.view.View;

import io.realm.RealmList;
import ph.pakete.R;
import ph.pakete.model.Package;
import ph.pakete.model.PackageTrackHistory;
import rx.subjects.ReplaySubject;

public class ItemPackageViewModel extends BaseObservable implements ViewModel {
    private ReplaySubject<Package> aPackage;
    private Context context;

    public ItemPackageViewModel(ReplaySubject<Package> aPackage, Context context) {
        this.context = context;
        setPackage(aPackage);
    }

    public ReplaySubject<Package> getPackage() {
        return aPackage;
    }

    public String getName() {
        String name = aPackage.getValue().getName().toLowerCase();
        return name.substring(0,1).toUpperCase() + name.substring(1).toLowerCase();
    }

    public String getTrackingNumber() {
        return aPackage.getValue().getTrackingNumber();
    }

    public String getCourierName() {
        return aPackage.getValue().getCourier().getName();
    }

    public RealmList<PackageTrackHistory> getTrackHistory() {
        return aPackage.getValue().getTrackHistory();
    }

    public String getStatus() {
        PackageTrackHistory lastTrackHistory = aPackage.getValue().latestTrackHistory();
        if (lastTrackHistory != null) {
            return lastTrackHistory.getStatus();
        }
        return "No information available yet";
    }

    public int getUpdating() {
        if (aPackage == null) return View.INVISIBLE;
        return aPackage.getValue().getUpdating() ? View.VISIBLE : View.GONE;
    }

    public int getUpdated() {
        if (aPackage == null) return View.INVISIBLE;
        return aPackage.getValue().getUpdating() ? View.GONE : View.VISIBLE;
    }

    public Drawable getStatusIcon() {
        if (aPackage.getValue().getCompleted()) {
            return ContextCompat.getDrawable(context, R.drawable.ic_delivered);
        } else {
            return ContextCompat.getDrawable(context, R.drawable.ic_intransit);
        }
    }

    public GradientDrawable getStatusIconBackgroundColor() {
        GradientDrawable circleShape = (GradientDrawable) ContextCompat.getDrawable(context, R.drawable.shape_circle);
        if (aPackage.getValue().getCompleted()) {
            circleShape.setColor(ContextCompat.getColor(context, R.color.medium_sea_green));
        } else {
            circleShape.setColor(ContextCompat.getColor(context, R.color.raven));
        }

        return circleShape;
    }

    // Allows recycling ItemPackageViewModel within the recyclerview adapter
    public void setPackage(ReplaySubject<Package> aPackage) {
        this.aPackage = aPackage;
        this.aPackage.subscribe(aPackage1 -> notifyChange());
    }

    @Override
    public void destroy() {
        //In this case destroy doesn't need to do anything because there is not async calls
    }
}
