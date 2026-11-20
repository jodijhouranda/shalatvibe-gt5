# ShalatVibe GT5 v2.0 — Pengingat Shalat Modern untuk Huawei Watch GT 5

Aplikasi Android pendamping (**Companion App**) berbasis **Kotlin + Jetpack Compose + Material 3** yang menghitung jadwal shalat secara **100% offline tanpa Google Play Services (GMS)** dan mengirim notifikasi prioritas tinggi sebelum & saat waktu shalat tiba, sehingga **Huawei Health** otomatis meneruskannya menjadi **getaran di Huawei Watch GT 5** Anda.

## Mengapa Pakai Aplikasi Android Pendamping?
Huawei Watch GT 5 menjalankan sistem operasi **HarmonyOS LiteOS (Lite Wearable)** yang membatasi aplikasi pihak ketiga berjalan terus-menerus di *background* demi baterai tahan hingga 14 hari. Dengan memanfaatkan *Notification Mirroring* resmi dari **Huawei Health**, jam GT 5 Anda bisa bergetar 24/7 sebelum waktu shalat tanpa menguras baterai jam dan tanpa perlu akun developer Huawei.

## Fitur Utama v2.1 (*Celestial Watchcraft*)
- **Desain Modern 4 Tab (Jetpack Compose & Material 3)**:
  - **Jadwal**: *Dynamic Atmosphere Hero Card* (gradien berubah mengikuti fase Subuh, Dzuhur, Ashar, Maghrib, Isya), busur matahari (*Solar Arc*), *live countdown* presisi tiap detik, tanggal Masehi & **Kalender Hijriah otomatis offline**, navigasi jadwal kemarin/besok, serta kartu jadwal dengan ikon celestial.
  - **Belajar (Bahasa Arab TAFL)**: Ruang belajar terpadu (*Split-View*) yang memutar **video YouTube otomatis mulai dari menit yang ditentukan** (lengkap dengan *chip* sub-segmen, kecepatan `0.75×/1.0×/1.25×`, *auto-loop* segmen, dan *Mode Fokus PDF* tanpa memutus audio) berdampingan dengan **PDF Viewer native** (`Asli` / `Sepia` / `Gelap` + zoom). Mendukung **Opsi 1 (Hybrid Offline + Sync Manifest)** melalui `lessons/catalog.json` atau link publik Google Drive.
  - **Kiblat**: Kompas presisi bergaya *bezel* Huawei Watch GT 5 dengan koreksi deklinasi magnetik, halo penjajaran emerald + *haptic feedback*, dan panduan kalibrasi angka 8.
  - **GT 5 Hub**: Pratinjau langsung tampilan notifikasi di layar bulat GT 5, **Status Kesiapan Sistem 4-Poin** (Notifikasi, Alarm Presisi, Baterai Bebas, Anti-Tidur), uji getar dengan *countdown visual* 5 detik, dan checklist interaktif panduan Huawei Health.
- **AMOLED Dark Mode, Serene Light Mode & Adaptive Launcher Icon**: Ikuti sistem atau kunci tema gelap pekat yang selaras dengan layar AMOLED Huawei Watch GT 5.
- **Perhitungan Jadwal Shalat Offline & Live Preview**: Mendukung metode **Kemenag RI**, MABIMS/JAKIM/MUIS, MWL, Umm Al-Qura, ISNA, serta opsi Ihtiyat +2 menit — lengkap dengan pratinjau langsung jam Subuh & Isya sebelum memilih metode.
- **52 Kota Indonesia + Pencarian & GPS Tanpa GMS**: Cari kota/provinsi (WIB/WITA/WIT) secara instan atau deteksi GPS otomatis memakai `LocationManager` bawaan Android.
- **Kustomisasi Jeda Getar Per Waktu Shalat**: Atur jeda global (5/10/15/20/30 menit) sekaligus *override* khusus per shalat (misal Subuh 20 menit sebelum, lainnya 10 menit sebelum), plus opsi **Format Ringkas Layar GT 5** & **Getar Ganda**.
- **Home Screen Widget**: Pantau hitung mundur shalat berikutnya, jadwal getar GT 5, tanggal Hijriah, dan ringkasan 5 waktu shalat langsung dari layar utama HP.

## Cara Build APK

### Opsi A — Build Otomatis di GitHub (Tanpa Install Android Studio)
Proyek ini dilengkapi workflow GitHub Actions di `.github/workflows/build-apk.yml`.
1. Push repository ini ke GitHub (branch `main`).
2. Buka tab **Actions** di repository GitHub Anda, tunggu workflow **Build ShalatVibe GT5 APK** selesai.
3. Unduh artifact **ShalatVibeGT5-debug-apk** (berisi `app-debug.apk`), lalu install di HP Android Anda.

### Opsi B — Build Lokal dengan Android Studio
1. Buka folder proyek menggunakan **Android Studio** (Ladybug / Meerkat atau lebih baru).
2. Tunggu Gradle selesai *sync* (Gradle 8.9, Android Gradle Plugin 8.7.3, Kotlin 2.0.21, Jetpack Compose, `compileSdk 35`, `minSdk 26`).
3. Pilih **Build → Build Bundle(s) / APK(s) → Build APK(s)**, atau jalankan `gradle testDebugUnitTest assembleDebug`.
4. Install APK hasil build ke HP Android Anda.

## Pengaturan Wajib di Huawei Health
1. Buka **Huawei Health** → **Perangkat** → **HUAWEI WATCH GT 5** → **Notifikasi**.
2. Aktifkan **Notifikasi**, lalu aktifkan toggle untuk aplikasi **ShalatVibe GT5**.
3. Pastikan **Jangan Ganggu (DND)** di jam tidak aktif.
4. Matikan opsi **"Bisukan notifikasi saat memakai ponsel"** jika Anda ingin jam tetap bergetar meskipun layar HP sedang menyala.
5. Di tab **GT 5 Hub**, pastikan keempat indikator kesiapan berwarna hijau, lalu tekan **Tes Getar 5 Detik (Kunci Layar)** untuk memverifikasi getaran di jam.
