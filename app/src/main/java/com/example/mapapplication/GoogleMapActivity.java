package com.example.mapapplication;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

/**
 * Activité affichant une carte interactive utilisant la bibliothèque OSMDroid.
 * Récupère les points d'intérêt depuis un serveur distant et les affiche sous forme de marqueurs.
 */
public class GoogleMapActivity extends AppCompatActivity {

    // Composant d'affichage de la carte
    private MapView osmMapView;
    
    // File d'attente pour les requêtes réseau (Volley)
    private RequestQueue networkRequestQueue;
    
    // URL pointant vers le script PHP de récupération des données
    private final String API_FETCH_URL = "http://10.0.2.2/map_project/getPosition.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialisation de la configuration OSMDroid (indispensable au chargement des tuiles)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid_prefs", MODE_PRIVATE));

        setContentView(R.layout.activity_google_map);

        // Configuration initiale de l'interface de la carte
        osmMapView = findViewById(R.id.map);
        osmMapView.setTileSource(TileSourceFactory.MAPNIK); // Source de données OpenStreetMap
        osmMapView.setBuiltInZoomControls(true);           // Afficher boutons +/-
        osmMapView.setMultiTouchControls(true);            // Support du zoom tactile (pinch)

        // Définition du centre de la carte et du niveau de zoom par défaut
        osmMapView.getController().setZoom(14.5);
        osmMapView.getController().setCenter(new GeoPoint(33.5731, -7.5898)); // Coordonnées par défaut (ex: Casablanca)

        // Préparation de la file de requêtes Volley
        networkRequestQueue = Volley.newRequestQueue(this);

        // Lancement du chargement des données de position
        fetchMarkersFromServer();
    }

    /**
     * Effectue un appel API pour récupérer la liste des positions stockées en base de données.
     */
    private void fetchMarkersFromServer() {
        JsonObjectRequest jsonRequest = new JsonObjectRequest(
                Request.Method.POST,
                API_FETCH_URL,
                null,
                response -> {
                    try {
                        // On vérifie si le serveur a retourné un succès
                        if (response.getBoolean("success")) {
                            JSONArray locationsArray = response.getJSONArray("positions");
                            displayMarkersOnMap(locationsArray);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    Toast.makeText(this, "Erreur de connexion au serveur", Toast.LENGTH_SHORT).show();
                }
        );

        networkRequestQueue.add(jsonRequest);
    }

    /**
     * Parcourt les données JSON reçues et ajoute les marqueurs correspondants sur la carte.
     * 
     * @param markersData Tableau JSON contenant les latitudes et longitudes
     */
    private void displayMarkersOnMap(JSONArray markersData) {
        try {
            for (int i = 0; i < markersData.length(); i++) {
                JSONObject item = markersData.getJSONObject(i);
                double latitude = item.getDouble("latitude");
                double longitude = item.getDouble("longitude");

                // Création d'une instance de marqueur pour le point courant
                Marker mapMarker = new Marker(osmMapView);
                mapMarker.setPosition(new GeoPoint(latitude, longitude));
                mapMarker.setTitle("Position enregistrée " + (i + 1));

                // Personnalisation de l'icône du marqueur
                Drawable markerIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.marker, null);
                if (markerIcon != null) {
                    Bitmap rawBitmap = ((BitmapDrawable) markerIcon).getBitmap();
                    // Redimensionnement pour assurer une visibilité optimale sur la carte
                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(rawBitmap, 70, 70, false);
                    mapMarker.setIcon(new BitmapDrawable(getResources(), resizedBitmap));
                }

                // Point d'ancrage : centré horizontalement et posé sur le point en bas de l'image
                mapMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                
                // Ajout effectif du marqueur à la collection de la carte
                osmMapView.getOverlays().add(mapMarker);
            }
            
            // On force le rafraîchissement de la vue pour afficher les nouveaux éléments
            osmMapView.invalidate();
            
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        osmMapView.onResume(); // Nécessaire pour le cycle de vie de la carte
    }

    @Override
    protected void onPause() {
        super.onPause();
        osmMapView.onPause(); // Nécessaire pour libérer les ressources
    }
}
