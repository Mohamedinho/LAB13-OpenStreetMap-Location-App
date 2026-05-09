<?php
// Configuration de la base de données
$serveur = "localhost";
$base_donnees = "map_project";
$utilisateur = "root";
$mot_de_passe = ""; 

try {
    // Connexion sécurisée via PDO
    $pdo = new PDO("mysql:host=$serveur;dbname=$base_donnees;charset=utf8", $utilisateur, $mot_de_passe);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Récupération sécurisée des données POST
    $lat  = $_POST['latitude']  ?? null;
    $lon  = $_POST['longitude'] ?? null;
    $date = $_POST['date']      ?? null;
    $uid  = $_POST['imei']      ?? null; // ID unique envoyé par l'app

    if ($lat && $lon && $date && $uid) {
        $sql = "INSERT INTO positions (latitude, longitude, date, imei) VALUES (?, ?, ?, ?)";
        $stmt = $pdo->prepare($sql);
        $stmt->execute([$lat, $lon, $date, $uid]);
        
        echo json_encode(["success" => true, "message" => "Position sauvegardée"]);
    } else {
        echo json_encode(["success" => false, "message" => "Données incomplètes"]);
    }
} catch (PDOException $e) {
    echo json_encode(["success" => false, "message" => "Erreur : " . $e->getMessage()]);
}
?>