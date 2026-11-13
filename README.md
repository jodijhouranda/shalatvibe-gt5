# ShalatVibe GT5 — Pengingat Shalat untuk Huawei Watch GT 5

Aplikasi Android pendamping (**Companion App**) yang menghitung jadwal shalat secara offline dan mengirim notifikasi prioritas tinggi sebelum & saat waktu shalat tiba, sehingga **Huawei Health** otomatis meneruskannya menjadi **getaran di Huawei Watch GT 5** Anda.

## Mengapa Pakai Aplikasi Android Pendamping?
Huawei Watch GT 5 menjalankan sistem operasi **HarmonyOS LiteOS (Lite Wearable)** yang membatasi aplikasi pihak ketiga berjalan terus-menerus di *background* demi baterai tahan hingga 14 hari. Dengan memanfaatkan *Notification Mirroring* resmi dari **Huawei Health**, jam GT 5 Anda bisa bergetar 24/7 sebelum waktu shalat tanpa menguras baterai jam dan tanpa perlu akun developer Huawei.

## Fitur Utama
- **Perhitungan Jadwal Shalat Offline**: Menggunakan algoritma astronomis matahari (mendukung metode **Kemenag RI**, MABIMS/JAKIM/MUIS, MWL, Umm Al-Qura, ISNA, serta opsi Ihtiyat +2 menit).
- **Getar Sebelum Waktu Shalat**: Pilih pengingat 5, 10, 15, 20, atau 30 menit sebelum adzan.
- **Kontrol Per Waktu Shalat**: Aktif/nonaktifkan pengingat untuk Subuh, Terbit, Dzuhur, Ashar, Maghrib, dan Isya secara terpisah.
- **Opsi Getar Ganda**: Kirim 2 notifikasi selang 30 detik agar tidak terlewat.
- **Tanpa Google Play Services (GMS)**: Deteksi lokasi GPS memakai `LocationManager` bawaan Android, ditambah 17 kota preset Indonesia.
- **Tombol Tes Getar ke GT 5**: Uji langsung atau dengan jeda 5 detik (sambil mengunci layar HP).

## Cara Build APK

### Opsi A — Build Otomatis di GitHub (Tanpa Install Android Studio)
Proyek ini sudah dilengkapi workflow GitHub Actions di `.github/workflows/build-apk.yml`.
1. Push repository ini ke GitHub (branch `main`).
2. Buka tab **Actions** di repository GitHub Anda, tunggu workflow **Build ShalatVibe GT5 APK** selesai.
3. Unduh artifact **ShalatVibeGT5-debug-apk** (berisi `app-debug.apk`), lalu install di HP Android Anda.

### Opsi B — Build Lokal dengan Android Studio
1. Buka folder `d:\coba\huawei` menggunakan **Android Studio** (Ladybug / Meerkat atau lebih baru).
2. Tunggu Gradle selesai *sync* (menggunakan Gradle 8.9 & Android Gradle Plugin 8.7.3, `compileSdk 35`, `minSdk 26`).
3. Pilih **Build → Build Bundle(s) / APK(s) → Build APK(s)**, atau jalankan `gradle assembleDebug`.
4. Install APK hasil build ke HP Android Anda.

## Pengaturan Wajib di Huawei Health
1. Buka **Huawei Health** → **Perangkat** → **HUAWEI WATCH GT 5** → **Notifikasi**.
2. Aktifkan **Notifikasi**, lalu aktifkan toggle untuk aplikasi **ShalatVibe GT5**.
3. Pastikan **Jangan Ganggu (DND)** di jam tidak aktif.
4. Matikan opsi **"Bisukan notifikasi saat memakai ponsel"** jika Anda ingin jam tetap bergetar meskipun layar HP sedang menyala.
5. Di aplikasi **ShalatVibe GT5**, pastikan izin **Notifikasi** dan **Alarm Tepat Waktu** aktif, lalu tekan **Tes 5 Detik (Kunci Layar)** untuk memverifikasi getaran di jam.
