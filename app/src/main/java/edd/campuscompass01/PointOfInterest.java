package edd.campuscompass01;

public class PointOfInterest {

    private String descr;
    private String key;
    private String lat;
    private String lon;
    private String name;
    private String phone;
    private String tim;

    public PointOfInterest() {

    }

    public PointOfInterest(String descr, String key, String lat, String lon, String name, String phone, String tim) {
        this.descr = descr;
        this.key = key;
        this.lat = lat;
        this.lon = lon;
        this.name = name;
        this.phone = phone;
        this.tim = tim;
    }

    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLon() {
        return lon;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTim() {
        return tim;
    }

    public void setTim(String tim) {
        this.tim = tim;
    }
}
