package com.jadwalshalat.gt5.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.content.ContextCompat;

import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

/**
 * Pembantu lokasi berbasis LocationManager bawaan Android (tanpa Google Play Services
 * sehingga kompatibel dengan ponsel Huawei HMS maupun Android lainnya).
 */
public final class LocationHelper {

    public static final class DetectedLocation {
        private final String cityName;
        private final double latitude;
        private final double longitude;
        private final String zoneId;

        public DetectedLocation(String cityName, double latitude, double longitude, String zoneId) {
            this.cityName = cityName;
            this.latitude = latitude;
            this.longitude = longitude;
            this.zoneId = zoneId;
        }

        public String getCityName() {
            return cityName;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public String getZoneId() {
            return zoneId;
        }
    }

    private LocationHelper() {
    }

    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static DetectedLocation getBestLastKnownLocation(Context context) {
        if (!hasLocationPermission(context)) {
            return null;
        }

        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (manager == null) {
            return null;
        }

        Location best = null;
        List<String> providers = manager.getProviders(true);
        for (String provider : providers) {
            try {
                Location candidate = manager.getLastKnownLocation(provider);
                if (candidate != null && (best == null || candidate.getAccuracy() < best.getAccuracy())) {
                    best = candidate;
                }
            } catch (SecurityException ignored) {
            }
        }

        if (best == null) {
            return null;
        }

        String cityName = resolveCityName(context, best.getLatitude(), best.getLongitude());
        return new DetectedLocation(
                cityName,
                best.getLatitude(),
                best.getLongitude(),
                ZoneId.systemDefault().getId()
        );
    }

    @SuppressWarnings("deprecation")
    private static String resolveCityName(Context context, double lat, double lon) {
        try {
            Geocoder geocoder = new Geocoder(context, new Locale("id", "ID"));
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                if (address.getSubAdminArea() != null) {
                    return address.getSubAdminArea();
                }
                if (address.getLocality() != null) {
                    return address.getLocality();
                }
                if (address.getAdminArea() != null) {
                    return address.getAdminArea();
                }
            }
        } catch (Exception ignored) {
        }
        return String.format(Locale.US, "GPS (%.3f, %.3f)", lat, lon);
    }
}
