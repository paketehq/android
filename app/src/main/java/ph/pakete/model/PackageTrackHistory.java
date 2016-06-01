package ph.pakete.model;

import android.text.format.DateUtils;

import java.util.Date;

import io.realm.RealmObject;

public class PackageTrackHistory extends RealmObject {
    private String status;
    private Date date;

    public String getStatus() {
        return status;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
