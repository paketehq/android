package ph.pakete;

public enum PackagesSortBy {
    LastUpdated,
    DateAdded,
    Name;

    public static PackagesSortBy fromInteger(int x) {
        switch(x) {
            case 0:
                return LastUpdated;
            case 1:
                return DateAdded;
            case 2:
                return Name;
        }
        return null;
    }

    @Override
    public String toString() {
        switch (ordinal()) {
            case 0:
                return  "Last Updated";
            case 1:
                return  "Date Added";
            case 2:
                return  "Name";
        }
        return "";
    }

}