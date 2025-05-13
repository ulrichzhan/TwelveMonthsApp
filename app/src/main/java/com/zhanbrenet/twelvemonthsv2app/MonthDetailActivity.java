package com.zhanbrenet.twelvemonthsv2app;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.zhanbrenet.twelvemonthsv2app.utils.MonthUtils;
import com.zhanbrenet.twelvemonthsv2app.utils.PhotoDatabaseHelper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.Manifest;

public class MonthDetailActivity extends AppCompatActivity implements OnMapReadyCallback {
    private String month;
    private String monthFileName;
    private Uri photoUri;

    private ImageView monthPhoto;

    private File storageDir;

    private TextView locationText;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_month_detail);

        storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        TextView titleView = findViewById(R.id.monthTitle);
        String monthMetaData = getIntent().getStringExtra("month");
        int monthLabelId = MonthUtils.getMonthStringId(monthMetaData); // pour récupérer le bon mois traduite derrière
        month = getString(monthLabelId);
        System.out.println(month);
        titleView.setText(getString(R.string.month_selected, month));

        // demande de permission pour la caméra
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        }

        monthPhoto = findViewById(R.id.monthPhoto);
        Button captureButton = findViewById(R.id.captureButton);
        captureButton.setOnClickListener(v -> openCamera());

        monthFileName = "photo_" + monthMetaData;
        System.out.println(monthFileName);
        ChargePicture();

        // Partie Geolicalisation
        locationText = findViewById(R.id.addressFound);
        loadAddressFromDb();

        // Partie Google maps
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if(mapFragment != null)
            mapFragment.getMapAsync(this);

    }

    @SuppressWarnings("deprecation")
    public void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = createImageFile();

        if(photoFile != null) {
            photoUri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".provider",
                    photoFile
            );
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(intent, 102);
        }
    }

    private File createImageFile() {
        File imageFile = new File(storageDir, monthFileName + ".jpg");
        try {
            if (!imageFile.exists()) {
                imageFile.createNewFile(); // créer s'il n'existe pas
            }
            return imageFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void ChargePicture() {
        // Charge la photo existante
        File savedPhoto = new File(storageDir, monthFileName + ".jpg");
        if (savedPhoto.exists()) {
            monthPhoto.setImageURI(Uri.fromFile(savedPhoto));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 102 && resultCode == RESULT_OK) {
            monthPhoto.setImageURI(photoUri);
            getLocationAndSave();
            loadAddressFromDb();
        }
    }

//----------- GESTION DE LA SQL LITE -----------------------------------
    @SuppressWarnings("MissingPermission")
    private void getLocationAndSave() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 999);
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.requestSingleUpdate(
                LocationManager.GPS_PROVIDER,
                location -> {
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();

                    // 👉 On lance un Thread séparé pour le Geocoder et la DB
                    new Thread(() -> {
                        getAddressFromLocation(lat, lng);
                    }).start();
                },
                null
        );
    }

    private void getAddressFromLocation(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String address = addresses.get(0).getAddressLine(0);

                // 👉 On lance un Thread pour la sauvegarde en DB
                new Thread(() -> {
                    savePhotoInfoToDb(address, lat, lng);

                    // ⚠️ On utilise le Handler pour mettre à jour l'interface
                    mainHandler.post(() -> {
                        locationText.setText(getString(R.string.address_found, address));
                    });

                }).start();

                // MIS A JOUR DE GOOGLE MAP
                LatLng defaultLocation = new LatLng(lat, lng);
                mainHandler.post(()-> {
                    map.clear(); // on nettoie les anciens marqueurs
                    map.addMarker(new MarkerOptions().position(defaultLocation).title("Default Location"));
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));
                });

            } else {
                // ⚠️ Mise à jour de l'interface avec le Handler si pas d'adresse
                mainHandler.post(() -> locationText.setText(getString(R.string.address_not_found)));
            }
        } catch (IOException e) {
            mainHandler.post(() -> locationText.setText(getString(R.string.geocoder_error)));
            e.printStackTrace();
        }
    }

    private void savePhotoInfoToDb(String address, double lat, double lng) {
        PhotoDatabaseHelper dbHelper = new PhotoDatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String filename = monthFileName + ".jpg";
        String date = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());

        ContentValues values = new ContentValues();
        values.put(PhotoDatabaseHelper.COL_FILENAME, filename);
        values.put(PhotoDatabaseHelper.COL_DATE, date);
        values.put(PhotoDatabaseHelper.COL_ADDRESS, address);
        values.put(PhotoDatabaseHelper.COL_LATITUDE, lat);
        values.put(PhotoDatabaseHelper.COL_LONGITUDE, lng);

        db.insert(PhotoDatabaseHelper.TABLE_NAME, null, values);
        db.close();
    }

    private void loadAddressFromDb() {
        // 👉 Lancement dans un thread séparé
        new Thread(() -> {
            // On ouvre la base en lecture
            PhotoDatabaseHelper dbHelper = new PhotoDatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            // On prépare la requête SQL pour chercher l'adresse
            String[] projection = {
                    PhotoDatabaseHelper.COL_ADDRESS,
                    PhotoDatabaseHelper.COL_LATITUDE,
                    PhotoDatabaseHelper.COL_LONGITUDE
            }; // Colonnes à récupérer
            String selection = PhotoDatabaseHelper.COL_FILENAME + " = ?";
            String[] selectionArgs = {monthFileName + ".jpg"};

            // On lance la requête
            Cursor cursor = db.query(
                    PhotoDatabaseHelper.TABLE_NAME,  // Table
                    projection,                      // Colonnes à récupérer
                    selection,                       // Clause WHERE
                    selectionArgs,                   // Valeurs pour le WHERE
                    null,                            // GroupBy
                    null,                            // Having
                    null                             // OrderBy
            );

            // On récupère le résultat
            String address = null;
            double lat = 0;
            double lng = 0;
            if (cursor.moveToFirst()) {
                address = cursor.getString(cursor.getColumnIndexOrThrow(PhotoDatabaseHelper.COL_ADDRESS));
                lat = cursor.getDouble(cursor.getColumnIndexOrThrow(PhotoDatabaseHelper.COL_LATITUDE));
                lng = cursor.getDouble(cursor.getColumnIndexOrThrow(PhotoDatabaseHelper.COL_LONGITUDE));
            }

            // On ferme le curseur et la DB
            cursor.close();
            db.close();

            // 👉 Mise à jour de l'interface avec un Handler (UI Thread)
            double finalLat = lat;
            double finalLng = lng;
            String finalAddress = address;
            mainHandler.post(() -> {
                if (finalAddress != null) {
                    locationText.setText(getString(R.string.address_found, finalAddress));

                    // 🔄 Mise à jour de la carte
                    if (map != null) {
                        LatLng position = new LatLng(finalLat, finalLng);
                        map.clear();
                        map.addMarker(new MarkerOptions().position(position).title(finalAddress));
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
                    }
                } else {
                    locationText.setText(getString(R.string.address_not_found));
                }
            });

        }).start();
    }

//-------------GOOGLE MAPS-----------------------------------

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        loadAddressFromDb();
    }

}
