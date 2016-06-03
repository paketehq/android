package ph.pakete.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Ignore;
import io.realm.annotations.PrimaryKey;

public class Package extends RealmObject {
    @PrimaryKey
    @SerializedName("tracking_number")
    private String trackingNumber;
    private String name;
    @SerializedName("track_history")
    private RealmList<PackageTrackHistory> trackHistory = new RealmList<>();
    private Courier courier;
    private Boolean completed = false;
    private Boolean archived = false;
    private Date createdAt = new Date();
    @Ignore
    private Boolean updating = false;

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RealmList<PackageTrackHistory> getTrackHistory() {
        return trackHistory;
    }

    public Courier getCourier() {
        return courier;
    }

    public void setCourier(Courier courier) {
        this.courier = courier;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public Boolean getUpdating() {
        return updating;
    }

    public void setUpdating(Boolean updating) {
        this.updating = updating;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public PackageTrackHistory latestTrackHistory() {
        if (getTrackHistory().size() > 0) {
            return getTrackHistory().first();
        } else {
            return  null;
        }
    }
}
