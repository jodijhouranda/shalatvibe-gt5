package com.jadwalshalat.gt5.model;

/**
 * Metode perhitungan sudut matahari untuk jadwal shalat.
 */
public enum CalculationMethod {
    KEMENAG_RI("Kemenag RI (Indonesia)", 20.0, 18.0, 0, 2),
    JAKIM_MUIS("MABIMS / JAKIM / MUIS", 20.0, 18.0, 0, 2),
    MWL("Muslim World League (MWL)", 18.0, 17.0, 0, 0),
    UMM_AL_QURA("Umm Al-Qura (Makkah)", 18.5, 0.0, 90, 0),
    ISNA("ISNA (Amerika Utara)", 15.0, 15.0, 0, 0);

    private final String label;
    private final double fajrAngle;
    private final double ishaAngle;
    private final int ishaMinutesAfterMaghrib;
    private final int defaultIhtiyatMinutes;

    CalculationMethod(String label, double fajrAngle, double ishaAngle,
                      int ishaMinutesAfterMaghrib, int defaultIhtiyatMinutes) {
        this.label = label;
        this.fajrAngle = fajrAngle;
        this.ishaAngle = ishaAngle;
        this.ishaMinutesAfterMaghrib = ishaMinutesAfterMaghrib;
        this.defaultIhtiyatMinutes = defaultIhtiyatMinutes;
    }

    public String getLabel() {
        return label;
    }

    public double getFajrAngle() {
        return fajrAngle;
    }

    public double getIshaAngle() {
        return ishaAngle;
    }

    public int getIshaMinutesAfterMaghrib() {
        return ishaMinutesAfterMaghrib;
    }

    public int getDefaultIhtiyatMinutes() {
        return defaultIhtiyatMinutes;
    }
}
