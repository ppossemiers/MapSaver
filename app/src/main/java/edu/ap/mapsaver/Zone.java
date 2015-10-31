package edu.ap.mapsaver;

public class Zone {
    private String tariefkleur;
    private String tariefzone;
    private String geometry;

    public Zone(String tariefkleur, String tariefzone, String geometry) {
        this.tariefkleur = tariefkleur;
        this.tariefzone = tariefzone;
        this.geometry = geometry;
    }

    public String getTariefkleur() {
        return tariefkleur;
    }

    public String getTariefzone() {
        return tariefzone;
    }

    public String getGeometry() {
        return geometry;
    }
}
