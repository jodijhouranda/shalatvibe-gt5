package com.jadwalshalat.gt5.qibla;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.jadwalshalat.gt5.data.PreferencesManager;
import com.jadwalshalat.gt5.databinding.ActivityQiblaBinding;
import com.jadwalshalat.gt5.prayer.QiblaCalculator;

import java.util.Locale;

/**
 * Layar Kompas Kiblat berbasis sensor magnetik & orientasi HP dengan koreksi deklinasi magnetik.
 */
public class QiblaActivity extends AppCompatActivity implements SensorEventListener {

    private static final float SMOOTHING_ALPHA = 0.15f;

    private ActivityQiblaBinding binding;
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor accelSensor;
    private Sensor magnetSensor;

    private final float[] gravity = new float[3];
    private final float[] geomagnetic = new float[3];
    private boolean hasGravity = false;
    private boolean hasMagnet = false;

    private float smoothedAzimuth = 0f;
    private boolean hasFirstHeading = false;
    private boolean wasAligned = false;

    private double qiblaBearing = 295.0;
    private float magneticDeclination = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQiblaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        PreferencesManager prefs = new PreferencesManager(this);
        double lat = prefs.getLatitude();
        double lon = prefs.getLongitude();

        qiblaBearing = QiblaCalculator.calculateBearingDegrees(lat, lon);
        double distanceKm = QiblaCalculator.calculateDistanceKm(lat, lon);

        GeomagneticField geoField = new GeomagneticField(
                (float) lat,
                (float) lon,
                0f,
                System.currentTimeMillis()
        );
        magneticDeclination = geoField.getDeclination();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.tvQiblaCity.setText(String.format(
                Locale.US, "%s (%.2f, %.2f)", prefs.getCityName(), lat, lon
        ));
        binding.tvQiblaDegree.setText(String.format(
                Locale.getDefault(),
                "%.1f° (%s)",
                qiblaBearing,
                QiblaCalculator.formatCardinalDirection(qiblaBearing)
        ));
        binding.tvQiblaDistance.setText(String.format(
                Locale.getDefault(),
                "Jarak ke Ka'bah: %,.0f km",
                distanceKm
        ));

        binding.compassView.updateAngles(0f, (float) qiblaBearing);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        if (rotationSensor == null && (accelSensor == null || magnetSensor == null)) {
            binding.tvAlignmentStatus.setText(
                    "Sensor kompas tidak tersedia di perangkat ini. Gunakan sudut "
                            + String.format(Locale.getDefault(), "%.1f°", qiblaBearing)
                            + " dari Utara."
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager == null) {
            return;
        }
        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
        } else if (accelSensor != null && magnetSensor != null) {
            sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this, magnetSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float rawMagneticAzimuth;

        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotationMatrix = new float[9];
            float[] orientation = new float[3];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientation);
            rawMagneticAzimuth = (float) Math.toDegrees(orientation[0]);
        } else {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, gravity, 0, 3);
                hasGravity = true;
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, geomagnetic, 0, 3);
                hasMagnet = true;
            }
            if (!hasGravity || !hasMagnet) {
                return;
            }
            float[] r = new float[9];
            float[] i = new float[9];
            if (!SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                return;
            }
            float[] orientation = new float[3];
            SensorManager.getOrientation(r, orientation);
            rawMagneticAzimuth = (float) Math.toDegrees(orientation[0]);
        }

        float trueAzimuth = ((rawMagneticAzimuth + magneticDeclination) % 360f + 360f) % 360f;
        if (!hasFirstHeading) {
            smoothedAzimuth = trueAzimuth;
            hasFirstHeading = true;
        } else {
            double delta = QiblaCalculator.shortestAngleDiff(trueAzimuth, smoothedAzimuth);
            smoothedAzimuth = (float) (((smoothedAzimuth + SMOOTHING_ALPHA * delta) % 360.0 + 360.0) % 360.0);
        }

        binding.compassView.updateAngles(smoothedAzimuth, (float) qiblaBearing);
        updateAlignmentFeedback();
    }

    private void updateAlignmentFeedback() {
        double diff = QiblaCalculator.shortestAngleDiff(qiblaBearing, smoothedAzimuth);
        boolean aligned = binding.compassView.isAligned();

        if (aligned) {
            binding.tvAlignmentStatus.setText("Menghadap Kiblat ✓ ("
                    + String.format(Locale.getDefault(), "%.0f°", smoothedAzimuth) + ")");
            if (!wasAligned) {
                binding.compassView.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            }
        } else {
            String turnDir = diff > 0 ? "Putar ke kanan" : "Putar ke kiri";
            binding.tvAlignmentStatus.setText(String.format(
                    Locale.getDefault(),
                    "%s %.0f° (Arah HP: %.0f°)",
                    turnDir,
                    Math.abs(diff),
                    smoothedAzimuth
            ));
        }
        wasAligned = aligned;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            Toast.makeText(this,
                    "Akurasi kompas rendah. Gerakkan HP membentuk angka 8.",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
