package ph.pakete.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    public String formattedDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return dateFormat.format(this.date);
    }
}
