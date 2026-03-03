package controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MapsController {

    private static final String C_CYAN   = "#00D9FF";
    private static final String C_GREEN  = "#00FF88";
    private static final String C_DANGER = "#FF4D6D";

    @FXML private StackPane   rootPane;
    @FXML private WebView     mapView;
    @FXML private VBox        loadingOverlay;
    @FXML private ProgressBar loadingBar;

    @FXML private Circle statusDot;
    @FXML private Label  lblStatus;
    @FXML private TextArea  lblAddress;
    @FXML private TextField  lblEventTitle;
    @FXML private TextField  lblLat;
    @FXML private TextField  lblLon;

    @FXML private Button btnBack;
    @FXML private Button btnOpenBrowser;
    @FXML private Button btnZoomIn;
    @FXML private Button btnZoomOut;
    @FXML private Button btnCenter;

    @FXML private Circle orb1, orb2, orb3;

    private String  localisation;
    private String  eventTitre;
    private String  encodedAddress;

    private boolean mapReady = false;

    @FXML
    public void initialize() {
        animateOrbs();
        animateLoadingBar();
        wireButtons();
        if (mapView != null) {
            mapView.getEngine().setJavaScriptEnabled(true);
            // Désactiver le cache pour forcer le rechargement des tuiles
            mapView.getEngine().setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        }
    }

    public void initMap(String localisation, String eventTitre) {
        this.localisation = localisation;
        this.eventTitre   = eventTitre;
        this.mapReady     = false;

        if (lblEventTitle != null) lblEventTitle.setText(eventTitre);
        if (lblAddress    != null) lblAddress.setText(localisation);

        try {
            encodedAddress = URLEncoder.encode(localisation, StandardCharsets.UTF_8);
        } catch (Exception e) {
            encodedAddress = localisation.replace(" ", "+");
        }

        setStatus("Géocodage en cours...", C_CYAN);

        Thread geo = new Thread(() -> {
            double[] coords = geocodeWithJava(localisation);
            Platform.runLater(() -> {
                double lat, lon;
                if (coords != null) {
                    lat = coords[0]; lon = coords[1];
                    if (lblLat != null) lblLat.setText(String.format("%.6f", lat));
                    if (lblLon != null) lblLon.setText(String.format("%.6f", lon));
                    setStatus("Localisation trouvée", C_GREEN);
                } else {
                    lat = 36.8065; lon = 10.1815;
                    setStatus("Tunis par défaut", "#FFB347");
                }
                loadMapWithCoords(lat, lon);
            });
        }, "GeoCode-Thread");
        geo.setDaemon(true);
        geo.start();
    }

    private double[] geocodeWithJava(String address) {
        try {
            String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
            URL url = new URL("https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" + encoded);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("User-Agent", "NeuroWell-JavaFX/1.0");

            if (conn.getResponseCode() != 200) return null;

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            String json = sb.toString().trim();
            if (json.equals("[]") || json.isEmpty()) return null;

            double lat = extractJsonDouble(json, "lat");
            double lon = extractJsonDouble(json, "lon");
            return (lat == 0 && lon == 0) ? null : new double[]{lat, lon};

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private double extractJsonDouble(String json, String key) {
        try {
            String search = "\"" + key + "\":\"";
            int idx = json.indexOf(search);
            if (idx == -1) {
                search = "\"" + key + "\":";
                idx = json.indexOf(search);
                if (idx == -1) return 0;
                idx += search.length();
                int end = json.indexOf(",", idx);
                if (end == -1) end = json.indexOf("}", idx);
                return Double.parseDouble(json.substring(idx, end).trim());
            }
            idx += search.length();
            int end = json.indexOf("\"", idx);
            return Double.parseDouble(json.substring(idx, end).trim());
        } catch (Exception e) { return 0; }
    }

    // ✅ SOLUTION FINALE : Utiliser plusieurs serveurs de tuiles en fallback
    private void loadMapWithCoords(double lat, double lon) {
        if (mapView == null) return;

        mapReady = false;

        String safeTitle = eventTitre   != null ? eventTitre.replace("'", "\\'").replace("\"", "") : "";
        String safeAddr  = localisation != null ? localisation.replace("'", "\\'").replace("\"", "") : "";

        String html =
                "<!DOCTYPE html><html><head>" +
                        "<meta charset='utf-8'/>" +
                        "<meta name='viewport' content='width=device-width, initial-scale=1.0'/>" +
                        "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
                        "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
                        "<style>" +
                        "* { margin: 0; padding: 0; box-sizing: border-box; }" +
                        "html, body { width: 100%; height: 100%; overflow: hidden; }" +
                        "#map { width: 100%; height: 100%; background: #B8E6D5; }" +

                        // Loader pour les tuiles
                        ".tile-loader {" +
                        "  position: fixed;" +
                        "  top: 50%;" +
                        "  left: 50%;" +
                        "  transform: translate(-50%, -50%);" +
                        "  font-size: 18px;" +
                        "  color: #00D9FF;" +
                        "  font-weight: bold;" +
                        "  z-index: 9999;" +
                        "  display: none;" +
                        "}" +

                        ".leaflet-container { background: #B8E6D5 !important; }" +

                        ".leaflet-control-zoom a {" +
                        "  background: white !important;" +
                        "  color: #00D9FF !important;" +
                        "  border: 2px solid #00D9FF !important;" +
                        "  border-radius: 8px !important;" +
                        "  font-size: 20px !important;" +
                        "  font-weight: bold !important;" +
                        "  width: 36px !important;" +
                        "  height: 36px !important;" +
                        "  line-height: 32px !important;" +
                        "  margin: 4px !important;" +
                        "}" +

                        ".leaflet-control-zoom a:hover {" +
                        "  background: #00D9FF !important;" +
                        "  color: white !important;" +
                        "}" +

                        ".leaflet-control-attribution {" +
                        "  background: rgba(255, 255, 255, 0.9) !important;" +
                        "  color: #666 !important;" +
                        "  font-size: 10px !important;" +
                        "}" +

                        ".custom-popup .leaflet-popup-content-wrapper {" +
                        "  background: white;" +
                        "  border: 3px solid #00D9FF;" +
                        "  border-radius: 20px;" +
                        "  padding: 20px;" +
                        "  box-shadow: 0 10px 40px rgba(0, 217, 255, 0.4);" +
                        "}" +

                        ".custom-popup .leaflet-popup-tip {" +
                        "  background: white;" +
                        "  border: 3px solid #00D9FF;" +
                        "}" +

                        ".custom-popup .leaflet-popup-close-button {" +
                        "  color: #00D9FF !important;" +
                        "  font-size: 24px !important;" +
                        "  font-weight: bold !important;" +
                        "}" +

                        ".popup-title {" +
                        "  color: #00D9FF;" +
                        "  font-weight: bold;" +
                        "  font-size: 18px;" +
                        "  margin-bottom: 10px;" +
                        "}" +

                        ".popup-address {" +
                        "  color: #555;" +
                        "  font-size: 14px;" +
                        "  line-height: 1.6;" +
                        "  margin: 8px 0;" +
                        "}" +

                        ".popup-badge {" +
                        "  display: inline-block;" +
                        "  background: linear-gradient(135deg, rgba(0, 255, 136, 0.2), rgba(0, 217, 255, 0.2));" +
                        "  border: 2px solid #00FF88;" +
                        "  border-radius: 25px;" +
                        "  padding: 6px 16px;" +
                        "  color: #00AA55;" +
                        "  font-size: 12px;" +
                        "  font-weight: 700;" +
                        "  margin-top: 12px;" +
                        "}" +

                        "@keyframes pulse {" +
                        "  0% { transform: translate(-50%, -50%) scale(0.8); opacity: 0.8; }" +
                        "  50% { transform: translate(-50%, -50%) scale(1.5); opacity: 0.4; }" +
                        "  100% { transform: translate(-50%, -50%) scale(2.2); opacity: 0; }" +
                        "}" +

                        ".pulse-ring {" +
                        "  width: 70px; height: 70px; border-radius: 50%;" +
                        "  position: absolute; top: 50%; left: 50%;" +
                        "  transform: translate(-50%, -50%);" +
                        "  border: 3px solid #00D9FF;" +
                        "  animation: pulse 2s ease-out infinite;" +
                        "}" +

                        ".pulse-ring-2 { animation-delay: 1s; }" +

                        "</style></head><body>" +
                        "<div id='loader' class='tile-loader'>⏳ Chargement de la carte...</div>" +
                        "<div id='map'></div>" +
                        "<script>" +

                        "var _mapObj = null;" +
                        "var _markerObj = null;" +
                        "var _LAT = " + lat + ";" +
                        "var _LON = " + lon + ";" +
                        "var _ready = false;" +
                        "var _tilesLoaded = 0;" +
                        "var _tilesLoading = 0;" +

                        "function zoomIn() { if(_mapObj) _mapObj.zoomIn(); }" +
                        "function zoomOut() { if(_mapObj) _mapObj.zoomOut(); }" +
                        "function centerMap() { if(_mapObj && _markerObj) { _mapObj.setView([_LAT, _LON], 15); _markerObj.openPopup(); } }" +

                        "window.zoomIn = zoomIn;" +
                        "window.zoomOut = zoomOut;" +
                        "window.centerMap = centerMap;" +

                        "document.addEventListener('DOMContentLoaded', function() {" +
                        "  var loader = document.getElementById('loader');" +
                        "  loader.style.display = 'block';" +

                        "  _mapObj = L.map('map', {" +
                        "    center: [_LAT, _LON]," +
                        "    zoom: 15," +
                        "    zoomControl: true," +
                        "    preferCanvas: false," +
                        "    renderer: L.canvas()," +
                        "    fadeAnimation: true," +
                        "    zoomAnimation: true," +
                        "    markerZoomAnimation: true" +
                        "  });" +

                        "  _mapObj.zoomControl.setPosition('bottomright');" +

                        // ✅ SOLUTION : Essayer plusieurs serveurs de tuiles
                        "  var tileUrls = [" +
                        // Option 1: OpenStreetMap standard
                        "    'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'," +
                        // Option 2: OpenStreetMap France (plus rapide en Europe)
                        "    'https://{s}.tile.openstreetmap.fr/osmfr/{z}/{x}/{y}.png'," +
                        // Option 3: OpenStreetMap DE (serveurs allemands)
                        "    'https://{s}.tile.openstreetmap.de/{z}/{x}/{y}.png'" +
                        "  ];" +

                        "  var currentTileUrl = tileUrls[0];" +

                        "  var tileLayer = L.tileLayer(currentTileUrl, {" +
                        "    attribution: '© <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a>'," +
                        "    maxZoom: 19," +
                        "    minZoom: 3," +
                        "    tileSize: 256," +
                        "    updateWhenIdle: false," +
                        "    updateWhenZooming: true," +
                        "    keepBuffer: 2," +
                        "    maxNativeZoom: 19," +
                        "    errorTileUrl: ''," +
                        "    crossOrigin: true" +
                        "  });" +

                        // Compteur de tuiles
                        "  tileLayer.on('loading', function() {" +
                        "    _tilesLoading++;" +
                        "    console.log('Chargement tuile:', _tilesLoading);" +
                        "  });" +

                        "  tileLayer.on('load', function() {" +
                        "    _tilesLoaded++;" +
                        "    console.log('Tuile chargée:', _tilesLoaded);" +
                        "    if (_tilesLoaded >= 8) {" + // Au moins 8 tuiles chargées
                        "      loader.style.display = 'none';" +
                        "      _ready = true;" +
                        "      document.title = 'MAP_READY';" +
                        "    }" +
                        "  });" +

                        "  tileLayer.on('tileerror', function(error) {" +
                        "    console.error('Erreur tuile:', error);" +
                        "  });" +

                        "  tileLayer.addTo(_mapObj);" +

                        // Forcer le rechargement après 3 secondes si pas de tuiles
                        "  setTimeout(function() {" +
                        "    if (_tilesLoaded < 5) {" +
                        "      console.log('Rechargement forcé des tuiles...');" +
                        "      _mapObj.invalidateSize();" +
                        "      _mapObj.setView([_LAT, _LON], 15);" +
                        "    }" +
                        "  }, 3000);" +

                        // Marqueur
                        "  var markerHtml = " +
                        "    '<div style=\"position:relative;width:50px;height:60px;\">' +" +
                        "      '<div style=\"position:absolute;bottom:0;left:50%;width:46px;height:46px;' +" +
                        "        'background:linear-gradient(135deg, #00D9FF 0%, #00FF88 100%);' +" +
                        "        'border-radius:50% 50% 50% 0;' +" +
                        "        'transform:translateX(-50%) rotate(-45deg);' +" +
                        "        'border:4px solid white;' +" +
                        "        'box-shadow:0 5px 25px rgba(0, 0, 0, 0.4), 0 0 40px rgba(0, 217, 255, 0.7);' +" +
                        "      '\"></div>' +" +
                        "      '<div class=\"pulse-ring\"></div>' +" +
                        "      '<div class=\"pulse-ring pulse-ring-2\"></div>' +" +
                        "    '</div>';" +

                        "  var markerIcon = L.divIcon({" +
                        "    className: ''," +
                        "    html: markerHtml," +
                        "    iconSize: [50, 60]," +
                        "    iconAnchor: [25, 60]," +
                        "    popupAnchor: [0, -65]" +
                        "  });" +

                        "  _markerObj = L.marker([_LAT, _LON], {icon: markerIcon}).addTo(_mapObj);" +

                        "  var popupContent = " +
                        "    '<div class=\"popup-title\">📍 " + safeTitle + "</div>' +" +
                        "    '<div class=\"popup-address\">📌 " + safeAddr + "</div>' +" +
                        "    '<div class=\"popup-badge\">✓ Localisé avec succès</div>';" +

                        "  _markerObj.bindPopup(popupContent, {" +
                        "    className: 'custom-popup'," +
                        "    maxWidth: 320," +
                        "    minWidth: 250" +
                        "  }).openPopup();" +

                        "  console.log('Carte initialisée:', _LAT, _LON);" +

                        // Cacher le loader après 5 secondes de toute façon
                        "  setTimeout(function() {" +
                        "    loader.style.display = 'none';" +
                        "    if (!_ready) {" +
                        "      _ready = true;" +
                        "      document.title = 'MAP_READY';" +
                        "    }" +
                        "  }, 5000);" +
                        "});" +

                        "</script></body></html>";

        WebEngine engine = mapView.getEngine();

        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                PauseTransition pause = new PauseTransition(Duration.millis(2500));
                pause.setOnFinished(e -> {
                    mapReady = true;
                    hideLoading();
                    animateMapReveal();
                    setStatus("Carte chargée", C_GREEN);
                });
                pause.play();
            } else if (state == Worker.State.FAILED) {
                mapReady = false;
                hideLoading();
                setStatus("Erreur chargement", C_DANGER);
            }
        });

        engine.titleProperty().addListener((obs, oldT, newT) -> {
            if ("MAP_READY".equals(newT)) {
                Platform.runLater(() -> {
                    mapReady = true;
                    hideLoading();
                    animateMapReveal();
                    setStatus("Carte prête", C_GREEN);
                });
            }
        });

        engine.loadContent(html, "text/html");
    }

    private void execScript(String script) {
        if (mapView == null) return;
        if (!mapReady) {
            setStatus("Carte en cours de chargement...", C_CYAN);
            return;
        }
        try {
            mapView.getEngine().executeScript(script);
        } catch (Exception ex) {
            setStatus("Patientez...", C_CYAN);
        }
    }

    private void wireButtons() {
        if (btnBack != null) {
            String base  = "-fx-background-color: transparent; -fx-border-color: rgba(0,217,255,0.30); " +
                    "-fx-border-width: 1; -fx-border-radius: 14; -fx-background-radius: 14; " +
                    "-fx-text-fill: rgba(0,217,255,0.70); -fx-font-size: 12px; -fx-padding: 11 0; -fx-cursor: hand;";
            String hover = "-fx-background-color: rgba(0,217,255,0.10); -fx-border-color: #00D9FF; " +
                    "-fx-border-width: 1; -fx-border-radius: 14; -fx-background-radius: 14; " +
                    "-fx-text-fill: #00D9FF; -fx-font-size: 12px; -fx-padding: 11 0; -fx-cursor: hand;";
            btnBack.setStyle(base);
            btnBack.setOnMouseEntered(e -> btnBack.setStyle(hover));
            btnBack.setOnMouseExited(e  -> btnBack.setStyle(base));
            btnBack.setOnAction(e -> goBack());
        }

        if (btnOpenBrowser != null) {
            String base  = "-fx-background-color: linear-gradient(to right, #00D9FF, #00FF88); " +
                    "-fx-text-fill: #050C07; -fx-font-size: 13px; -fx-font-weight: bold; " +
                    "-fx-padding: 13 0; -fx-background-radius: 14; -fx-cursor: hand;";
            String hover = "-fx-background-color: linear-gradient(to right, #00FF88, #00D9FF); " +
                    "-fx-text-fill: #050C07; -fx-font-size: 13px; -fx-font-weight: bold; " +
                    "-fx-padding: 13 0; -fx-background-radius: 14; -fx-cursor: hand;";
            btnOpenBrowser.setStyle(base);
            btnOpenBrowser.setOnMouseEntered(e -> btnOpenBrowser.setStyle(hover));
            btnOpenBrowser.setOnMouseExited(e  -> btnOpenBrowser.setStyle(base));
            btnOpenBrowser.setOnAction(e -> openInBrowser());
        }

        if (btnZoomIn  != null) btnZoomIn.setOnAction(e  -> execScript("zoomIn()"));
        if (btnZoomOut != null) btnZoomOut.setOnAction(e -> execScript("zoomOut()"));
        if (btnCenter  != null) btnCenter.setOnAction(e  -> execScript("centerMap()"));
    }

    private void goBack() {
        try {
            Parent root  = FXMLLoader.load(getClass().getResource("/views/showEvent.fxml"));
            Stage  stage = (Stage) rootPane.getScene().getWindow();
            FadeTransition fo = new FadeTransition(Duration.millis(250), rootPane.getScene().getRoot());
            fo.setFromValue(1); fo.setToValue(0);
            fo.setOnFinished(ev -> {
                stage.setScene(new Scene(root, stage.getWidth(), stage.getHeight()));
                FadeTransition fi = new FadeTransition(Duration.millis(250), root);
                fi.setFromValue(0);
                fi.setToValue(1);
                fi.play();
            });
            fo.play();
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void openInBrowser() {
        if (localisation == null) return;
        try {
            URI uri = new URI("https://www.google.com/maps/search/?api=1&query=" + encodedAddress);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                Desktop.getDesktop().browse(uri);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void hideLoading() {
        if (loadingOverlay == null) return;
        FadeTransition ft = new FadeTransition(Duration.millis(400), loadingOverlay);
        ft.setToValue(0);
        ft.setOnFinished(e -> loadingOverlay.setVisible(false));
        ft.play();
    }

    private void animateMapReveal() {
        if (mapView == null) return;
        mapView.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(700), mapView);
        ft.setToValue(1);
        ft.play();
    }

    private void animateLoadingBar() {
        if (loadingBar == null) return;
        new Timeline(
                new KeyFrame(Duration.ZERO,         new KeyValue(loadingBar.progressProperty(), 0.0)),
                new KeyFrame(Duration.seconds(3.0), new KeyValue(loadingBar.progressProperty(), 0.90))
        ).play();
    }

    private void setStatus(String msg, String color) {
        if (lblStatus != null) {
            lblStatus.setText(msg);
            lblStatus.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: 600;");
        }
        if (statusDot != null) statusDot.setFill(Color.web(color));
    }

    private void animateOrbs() {
        animateOrb(orb1,  40, -25, 14);
        animateOrb(orb2, -50,  35, 18);
        animateOrb(orb3,  25, -40, 20);
    }

    private void animateOrb(Circle orb, double dx, double dy, double secs) {
        if (orb == null) return;
        TranslateTransition tt = new TranslateTransition(Duration.seconds(secs), orb);
        tt.setByX(dx); tt.setByY(dy);
        tt.setCycleCount(Animation.INDEFINITE);
        tt.setAutoReverse(true);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.play();
    }
}