package com.jadwalshalat.gt5.qibla;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.jadwalshalat.gt5.prayer.QiblaCalculator;

/**
 * Komponen kompas kustom yang menggambar dial arah mata angin dan jarum arah Kiblat.
 */
public class QiblaCompassView extends View {

    private float azimuthDegrees = 0f;
    private float qiblaBearingDegrees = 295f;
    private boolean aligned = false;

    private final Paint circleBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardinalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint northPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint qiblaArrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint kaabaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint kaabaBandPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint topMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path arrowPath = new Path();
    private final Path topMarkerPath = new Path();

    public QiblaCompassView(Context context) {
        super(context);
        init();
    }

    public QiblaCompassView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public QiblaCompassView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        float density = getResources().getDisplayMetrics().density;

        circleBgPaint.setColor(Color.WHITE);
        circleBgPaint.setStyle(Paint.Style.FILL);

        ringPaint.setColor(Color.parseColor("#CBD5E1"));
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(4f * density);

        tickPaint.setColor(Color.parseColor("#94A3B8"));
        tickPaint.setStrokeWidth(1.5f * density);

        cardinalPaint.setColor(Color.parseColor("#334155"));
        cardinalPaint.setTextSize(16f * density);
        cardinalPaint.setTextAlign(Paint.Align.CENTER);
        cardinalPaint.setFakeBoldText(true);

        northPaint.setColor(Color.parseColor("#DC2626"));
        northPaint.setTextSize(18f * density);
        northPaint.setTextAlign(Paint.Align.CENTER);
        northPaint.setFakeBoldText(true);

        qiblaArrowPaint.setColor(Color.parseColor("#0F766E"));
        qiblaArrowPaint.setStyle(Paint.Style.FILL);

        kaabaPaint.setColor(Color.parseColor("#111827"));
        kaabaPaint.setStyle(Paint.Style.FILL);

        kaabaBandPaint.setColor(Color.parseColor("#F59E0B"));
        kaabaBandPaint.setStyle(Paint.Style.FILL);

        topMarkerPaint.setColor(Color.parseColor("#D97706"));
        topMarkerPaint.setStyle(Paint.Style.FILL);

        centerDotPaint.setColor(Color.parseColor("#0B4F4A"));
        centerDotPaint.setStyle(Paint.Style.FILL);
    }

    public void updateAngles(float azimuthDegrees, float qiblaBearingDegrees) {
        this.azimuthDegrees = azimuthDegrees;
        this.qiblaBearingDegrees = qiblaBearingDegrees;
        double diff = Math.abs(QiblaCalculator.shortestAngleDiff(qiblaBearingDegrees, azimuthDegrees));
        this.aligned = diff <= 3.5;
        ringPaint.setColor(Color.parseColor(aligned ? "#10B981" : "#CBD5E1"));
        invalidate();
    }

    public boolean isAligned() {
        return aligned;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.min(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
        if (size == 0) {
            size = (int) (280 * getResources().getDisplayMetrics().density);
        }
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) - (16f * density);

        // Lingkaran kompas
        canvas.drawCircle(cx, cy, radius, circleBgPaint);
        canvas.drawCircle(cx, cy, radius, ringPaint);

        // Penanda tetap di bagian atas (arah hadap atas ponsel)
        topMarkerPath.reset();
        topMarkerPath.moveTo(cx, cy - radius - (10f * density));
        topMarkerPath.lineTo(cx - (8f * density), cy - radius + (2f * density));
        topMarkerPath.lineTo(cx + (8f * density), cy - radius + (2f * density));
        topMarkerPath.close();
        canvas.drawPath(topMarkerPath, topMarkerPaint);

        // Dial berputar berlawanan arah hadap ponsel agar Utara selalu mengarah ke Utara asli
        canvas.save();
        canvas.rotate(-azimuthDegrees, cx, cy);

        for (int deg = 0; deg < 360; deg += 15) {
            double rad = Math.toRadians(deg);
            boolean major = (deg % 45 == 0);
            float inner = radius - (major ? 14f * density : 8f * density);
            float outer = radius - (3f * density);
            float x1 = cx + (float) (Math.sin(rad) * inner);
            float y1 = cy - (float) (Math.cos(rad) * inner);
            float x2 = cx + (float) (Math.sin(rad) * outer);
            float y2 = cy - (float) (Math.cos(rad) * outer);
            canvas.drawLine(x1, y1, x2, y2, tickPaint);
        }

        float labelRadius = radius - (28f * density);
        canvas.drawText("U", cx, cy - labelRadius + (6f * density), northPaint);
        canvas.drawText("T", cx + labelRadius, cy + (6f * density), cardinalPaint);
        canvas.drawText("S", cx, cy + labelRadius + (6f * density), cardinalPaint);
        canvas.drawText("B", cx - labelRadius, cy + (6f * density), cardinalPaint);

        // Jarum arah Kiblat di atas dial
        canvas.save();
        canvas.rotate(qiblaBearingDegrees, cx, cy);

        arrowPath.reset();
        arrowPath.moveTo(cx, cy - radius + (36f * density));
        arrowPath.lineTo(cx - (12f * density), cy + (14f * density));
        arrowPath.lineTo(cx, cy + (2f * density));
        arrowPath.lineTo(cx + (12f * density), cy + (14f * density));
        arrowPath.close();
        canvas.drawPath(arrowPath, qiblaArrowPaint);

        // Ikon kotak Ka'bah kecil dekat ujung panah Kiblat
        float kaabaHalf = 9f * density;
        float kaabaCenterY = cy - radius + (22f * density);
        canvas.drawRect(
                cx - kaabaHalf,
                kaabaCenterY - kaabaHalf,
                cx + kaabaHalf,
                kaabaCenterY + kaabaHalf,
                kaabaPaint
        );
        canvas.drawRect(
                cx - kaabaHalf,
                kaabaCenterY - (4f * density),
                cx + kaabaHalf,
                kaabaCenterY - (1f * density),
                kaabaBandPaint
        );

        canvas.restore();
        canvas.restore();

        canvas.drawCircle(cx, cy, 7f * density, centerDotPaint);
    }
}
