package ph.pakete.viewmodel;

import android.content.Context;
import android.databinding.BaseObservable;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.content.ContextCompat;

import ph.pakete.R;
import ph.pakete.model.PackageTrackHistory;

public class ItemPackageTrackHistoryViewModel extends BaseObservable implements ViewModel {
    private PackageTrackHistory trackHistory;
    private Context context;

    public ItemPackageTrackHistoryViewModel(PackageTrackHistory trackHistory, Context context) {
        this.trackHistory = trackHistory;
        this.context = context;
    }

    public String getStatus() {
        return trackHistory.getStatus();
    }

    public String getDate() {
        return trackHistory.formattedDate();
    }

    public GradientDrawable getStatusIconBackgroundColor() {
        GradientDrawable circleShape = (GradientDrawable) ContextCompat.getDrawable(context, R.drawable.shape_circle);
        circleShape.setColor(ContextCompat.getColor(context, R.color.medium_sea_green));
        return circleShape;
    }

    // Allows recycling ItemPackageTrackHistoryViewModel within the recyclerview adapter
    public void setPackageTrackHistory(PackageTrackHistory trackHistory) {
        this.trackHistory = trackHistory;
        notifyChange();
    }

    @Override
    public void destroy() {
        //In this case destroy doesn't need to do anything because there is not async calls
    }
}
