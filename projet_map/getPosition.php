<?php
$serveur = "localhost";
$base_donnees = "map_project";$utilisateur = "root";
$mot_de_passe = "";

try {
    $pdo = new PDO("mysql:host=$serveur;dbname=$base_donnees;charset=utf8", $utilisateur, $mot_de_passe);
    
    // On récupère toutes les positions
    $stmt = $pdo->query("SELECT latitude, longitude FROM positions");
    $positions = $stmt->fetchAll(PDO::FETCH_ASSOC);

    echo json_encode([
        "success" => true, 
        "positions" => $positions
    ]);
} catch (PDOException $e) {
    echo json_encode(["success" => false, "message" => $e->getMessage()]);
}
?>