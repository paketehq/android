package ph.pakete.viewmodel;

import android.databinding.BaseObservable;

import ph.pakete.PackagesSortBy;

public class ItemSortByViewModel extends BaseObservable implements ViewModel {
    private PackagesSortBy sortBy;

    public ItemSortByViewModel(PackagesSortBy sortBy) {
        this.sortBy = sortBy;
    }

    public String getName() {
        return sortBy.toString();
    }

    // Allows recycling ItemSortByViewModel within the recyclerview adapter
    public void setPackagesSortBy(PackagesSortBy sortBy) {
        this.sortBy = sortBy;
        notifyChange();
    }

    @Override
    public void destroy() {
        //In this case destroy doesn't need to do anything because there is not async calls
    }
}