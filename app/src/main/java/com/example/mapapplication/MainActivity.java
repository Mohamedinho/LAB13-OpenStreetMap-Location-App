package com.example.mapapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Activité principale de l'application MapApplication. préparée par Mohamed DOUASSI
 * Gère la récupération de la position GPS et l'envoi des données au serveur.
 */
public class MainActivity extends AppCompatActivity {

    // Éléments de l'interface utilisateur
    private Button btnShowMap;
    
    // Variables pour stocker les informations de géolocalisation
    private double currentLatitude;
    private double currentLongitude;
    private double currentAltitude;
    private float locationAccuracy;
    
    // Gestionnaire de requêtes réseau Volley
    private RequestQueue networkQueue;
    
    // URL du script PHP pour l'insertion (10.0.2.2 pointe vers localhost de la machine hôte dans l'émulateur)
    private final String API_INSERT_URL = "http://10.0.2.2/map_project/createPosition.php";
    
    // Gestionnaire de localisation du système Android
    private LocationManager systemLocationManager;

    // Code de requête pour les permissions (identifiant arbitraire)
    private static final int LOCATION_PERMISSION_ID = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation de la file d'attente pour les appels API
        networkQueue = Volley.newRequestQueue(this);
        
        // Récupération du service de localisation
        systemLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Liaison du bouton et configuration de l'action de clic
        btnShowMap = findViewById(R.id.btnMap);
        btnShowMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Redirection vers l'activité affichant la carte
                Intent intent = new Intent(MainActivity.this, GoogleMapActivity.class);
                startActivity(intent);
            }
        });
        
        // Vérification et demande des permissions nécessaires au démarrage
        requestAppPermissions();
    }

    /**
     * Vérifie si les permissions de localisation sont accordées.
     */
    private void requestAppPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }, LOCATION_PERMISSION_ID);
        } else {
            // Permissions déjà présentes, on peut lancer le suivi
            initLocationTracking();
        }
    }

    /**
     * Initialise l'écouteur GPS pour recevoir les mises à jour de position.
     */
    private void initLocationTracking() {
        // Vérification de sécurité pour l'IDE
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Demande des mises à jour toutes les 60 secondes ou tous les 150 mètres
        systemLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 150, new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                // Mise à jour des variables locales avec les données reçues
                currentLatitude = location.getLatitude();
                currentLongitude = location.getLongitude();
                currentAltitude = location.getAltitude();
                locationAccuracy = location.getAccuracy();
                
                // Préparation du message d'affichage personnalisé
                String statusMessage = String.format(Locale.getDefault(),
                        getString(R.string.new_location), 
                        currentLatitude, currentLongitude, currentAltitude, locationAccuracy);
                
                // Transmission des coordonnées vers la base de données distante
                uploadPositionToServer(currentLatitude, currentLongitude);
                
                // Notification visuelle pour l'utilisateur
                Toast.makeText(MainActivity.this, statusMessage, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {}

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                Toast.makeText(MainActivity.this, "Veuillez activer le GPS", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // L'utilisateur a accepté, on démarre le service
                initLocationTracking();
            } else {
                // L'utilisateur a refusé
                Toast.makeText(this, "L'accès à la localisation est requis.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Envoie les données de géolocalisation au serveur web via une requête POST.
     */
    private void uploadPositionToServer(final double lat, final double lon) {
        StringRequest postRequest = new StringRequest(Request.Method.POST,
                API_INSERT_URL, 
                response -> {
                    // Succès : Affichage de la réponse du serveur (utile pour le debug)
                    Toast.makeText(MainActivity.this, "Serveur : " + response, Toast.LENGTH_SHORT).show();
                }, 
                error -> {
                    // Échec : Affichage de l'erreur
                    String message = "Erreur de connexion";
                    if (error.networkResponse != null) {
                        message += " (Code : " + error.networkResponse.statusCode + ")";
                    } else if (error.getMessage() != null) {
                        message += " : " + error.getMessage();
                    }
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    error.printStackTrace();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> dataParameters = new HashMap<>();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                // On remplit la map avec les données à envoyer
                dataParameters.put("latitude", String.valueOf(lat));
                dataParameters.put("longitude", String.valueOf(lon));
                dataParameters.put("date", dateFormat.format(new Date()));

                // Identification de l'appareil via l'ANDROID_ID
                String deviceUniqueId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                dataParameters.put("imei", deviceUniqueId);

                return dataParameters;
            }
        };
        
        // Ajout de la requête à la file d'attente Volley
        networkQueue.add(postRequest);
    }
}
