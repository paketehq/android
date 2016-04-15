package ph.pakete.model;

import org.parceler.Parcel;

import io.realm.CourierRealmProxy;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

@Parcel(implementations = { CourierRealmProxy.class },
        value = Parcel.Serialization.BEAN,
        analyze = { Courier.class })

public class Courier extends RealmObject {
    @PrimaryKey
    private String code;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
