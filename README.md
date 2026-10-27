# Pengingat Salat Mataram — Android TV starter

Proyek APK Android TV untuk pengingat salat dengan lokasi acuan **Kota Mataram, Nusa Tenggara Barat** dan zona waktu **WITA (`Asia/Makassar`)**.

Fitur starter saat ini: editor jadwal manual, alur izin popup, popup uji, pengaktifan alarm harian, dan workflow build APK melalui GitHub Actions.

## Penting sebelum digunakan

- Jadwal salat **belum diambil otomatis** dari Kemenag. Masukkan jadwal resmi yang berlaku untuk **Kota Mataram** secara manual di aplikasi. Jangan menggunakan jadwal dari kabupaten/kota lain.
- Proyek ini belum diuji pada Xiaomi MiTV-AXFU0; ini belum boleh dianggap sebagai aplikasi siap pakai tanpa pengujian.
- Android TV/Android 14 dapat membatasi popup dan alarm latar belakang. Izin popup perlu diaktifkan manual, lalu uji di TV.
- Alarm menggunakan `setAndAllowWhileIdle`, bukan izin alarm presisi; notifikasi bisa terlambat.
- Fitur menjeda video lalu melanjutkan di posisi yang sama **belum dibuat**.
- Tutup popup dengan remote perlu diuji di TV. Jika tombol tutup tidak bisa difokuskan dengan DPAD, perlu perbaikan sebelum dipakai rutin.

## Build

GitHub Actions membangun APK saat perubahan di-push ke `main`, atau saat dijalankan manual melalui `workflow_dispatch`.
Artifact: `pengingat-salat-mataram-debug-apk`.

## Instalasi setelah build berhasil

Periksa endpoint ADB yang aktif terlebih dahulu dengan `adb devices -l`, karena port bisa berubah. Contoh:

```powershell
adb -s 192.168.1.10:35201 install -r app-debug.apk
```

## Izin popup

Buka aplikasi di TV, pilih **Atur izin popup**, aktifkan izin "Display over other apps" di Android Settings, kembali ke aplikasi, lalu pilih **Uji popup sekarang**.

## Batasan keamanan

Proyek ini tidak menonaktifkan launcher bawaan, tidak mengubah aplikasi yang dilindungi, dan tidak memodifikasi firmware.
