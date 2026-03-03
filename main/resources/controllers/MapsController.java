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
    @FXML private Label  lblAddress;
    @FXML private Label  lblEventTitle;
    @FXML private Label  lblLat;
    @FXML private Label  lblLon;

    @FXML private Button btnBack;
    @FXML private Button btnOpenBrowser;
    @FXML private Button btnZoomIn;
    @FXML private Button btnZoomOut;
    @FXML private Button btnCenter;

    @FXML private Circle orb1, orb2, orb3;

    private String  localisation;
    private String  eventTitre;
    private String  encodedAddress;

    // ✅ Flag : true uniquement quand Leaflet + carte sont prets
    private boolean mapReady = false;

    @FXML
    public void initialize() {
        animateOrbs();
        animateLoadingBar();
        wireButtons();
        if (mapView != null) {
            mapView.getEngine().setJavaScriptEnabled(true);
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

        setStatus("Geocodage en cours...", C_CYAN);

        Thread geo = new Thread(() -> {
            double[] coords = geocodeWithJava(localisation);
            Platform.runLater(() -> {
                double lat, lon;
                if (coords != null) {
                    lat = coords[0]; lon = coords[1];
                    if (lblLat != null) lblLat.setText(String.format("%.6f", lat));
                    if (lblLon != null) lblLon.setText(String.format("%.6f", lon));
                    setStatus("Localisation trouvee", C_GREEN);
                } else {
                    lat = 36.8065; lon = 10.1815;
                    setStatus("Tunis par defaut", "#FFB347");
                }
                loadMapWithCoords(lat, lon);
            });
        }, "GeoCode-Thread");
        geo.setDaemon(true);
        geo.start();
    }

    // ─── Geocodage Java ──────────────────────────────────────
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

    // ─── Chargement carte ─────────────────────────────────────
    private void loadMapWithCoords(double lat, double lon) {
        if (mapView == null) return;

        mapReady = false;

        String safeTitle = eventTitre   != null ? eventTitre.replace("'", "\\'").replace("\"", "") : "";
        String safeAddr  = localisation != null ? localisation.replace("'", "\\'").replace("\"", "") : "";

        // ✅ Les fonctions JS sont definies AVANT map.whenReady()
        // ✅ On utilise une variable globale _mapObj au lieu de 'map' pour eviter les conflits
        String html =
                "<!DOCTYPE html><html><head>" +
                        "<meta charset='utf-8'/>" +
                        "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
                        "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
                        "<style>" +
                        "* { margin:0; padding:0; box-sizing:border-box; }" +
                        "html,body,#map { width:100%; height:100%; background:#050C07; }" +
                        ".leaflet-tile-pane { filter:invert(1) hue-rotate(180deg) brightness(0.80) saturate(1.3); }" +
                        ".leaflet-control-zoom a { background:rgba(13,31,18,0.95)!important; color:#00D9FF!important; border-color:rgba(0,217,255,0.3)!important; }" +
                        ".leaflet-control-attribution { background:rgba(5,12,7,0.80)!important; color:rgba(200,255,220,0.25)!important; font-size:9px!important; }" +
                        ".neo-popup .leaflet-popup-content-wrapper { background:rgba(13,31,18,0.97); border:1.5px solid rgba(0,217,255,0.45); border-radius:16px; color:#E8FFF0; box-shadow:0 0 30px rgba(0,217,255,0.25); }" +
                        ".neo-popup .leaflet-popup-tip { background:rgba(13,31,18,0.97); }" +
                        ".neo-popup .leaflet-popup-close-button { color:rgba(0,217,255,0.60)!important; }" +
                        ".pop-title { color:#00D9FF; font-weight:bold; font-size:14px; margin-bottom:6px; font-family:monospace; }" +
                        ".pop-addr  { color:rgba(200,255,220,0.75); font-size:12px; line-height:1.6; font-family:monospace; }" +
                        ".pop-badge { display:inline-block; background:rgba(0,255,136,0.12); border:1px solid rgba(0,255,136,0.35); border-radius:20px; padding:3px 12px; color:#00FF88; font-size:10px; margin-top:8px; }" +
                        "@keyframes pulse { 0%{transform:translate(-50%,-50%) scale(0.8);opacity:1} 100%{transform:translate(-50%,-50%) scale(2.5);opacity:0} }" +
                        ".pr { width:60px;height:60px;border-radius:50%;position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);border:2px solid rgba(0,217,255,0.7);animation:pulse 1.8s ease-out infinite; }" +
                        ".pr2 { animation-delay:0.9s; }" +
                        "</style></head><body>" +
                        "<div id='map'></div>" +
                        "<script>" +

                        // Variables globales accessibles depuis Java
                        "var _mapObj    = null;" +
                        "var _markerObj = null;" +
                        "var _LAT = " + lat + ";" +
                        "var _LON = " + lon + ";" +
                        "var _ready = false;" +

                        // ✅ Fonctions definies IMMEDIATEMENT (pas dans un callback)
                        // Elles verifient _ready avant d'agir
                        "function zoomIn()    { if(_mapObj) _mapObj.zoomIn();  }" +
                        "function zoomOut()   { if(_mapObj) _mapObj.zoomOut(); }" +
                        "function centerMap() { if(_mapObj && _markerObj) { _mapObj.setView([_LAT,_LON],15); _markerObj.openPopup(); } }" +

                        // Exposer sur window explicitement
                        "window.zoomIn    = zoomIn;" +
                        "window.zoomOut   = zoomOut;" +
                        "window.centerMap = centerMap;" +

                        // Init Leaflet apres que le DOM soit pret
                        "document.addEventListener('DOMContentLoaded', function() {" +
                        "  _mapObj = L.map('map', {zoomControl:true, attributionControl:true});" +
                        "  _mapObj.zoomControl.setPosition('bottomright');" +
                        "  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {" +
                        "    attribution:'OpenStreetMap contributors', maxZoom:19" +
                        "  }).addTo(_mapObj);" +

                        "  var mHtml = '<div style=\"position:relative;width:44px;height:54px;\">" +
                        "    <div style=\"position:absolute;bottom:0;left:50%;width:40px;height:40px;" +
                        "    background:linear-gradient(135deg,#00D9FF,#00FF88);" +
                        "    border-radius:50% 50% 50% 0;transform:translateX(-50%) rotate(-45deg);" +
                        "    border:2px solid rgba(255,255,255,0.6);" +
                        "    box-shadow:0 0 20px rgba(0,217,255,0.9),0 0 40px rgba(0,217,255,0.4);\"></div>" +
                        "    <div class=\\\"pr\\\"></div><div class=\\\"pr pr2\\\"></div>" +
                        "  </div>';" +

                        "  var icon = L.divIcon({className:'',html:mHtml,iconSize:[44,54],iconAnchor:[22,54],popupAnchor:[0,-58]});" +
                        "  _mapObj.setView([_LAT, _LON], 15);" +
                        "  _markerObj = L.marker([_LAT, _LON], {icon:icon}).addTo(_mapObj);" +
                        "  _markerObj.bindPopup(" +
                        "    '<div class=\"pop-title\">&#128205; " + safeTitle + "</div>" +
                        "     <div class=\"pop-addr\">&#128215; " + safeAddr + "</div>" +
                        "     <div class=\"pop-badge\">&#9899; Localise avec succes</div>'," +
                        "    {className:'neo-popup', maxWidth:300, minWidth:200}" +
                        "  ).openPopup();" +

                        "  _ready = true;" +
                        "  document.title = 'MAP_READY';" +
                        "});" +

                        "</script></body></html>";

        WebEngine engine = mapView.getEngine();

        // ✅ Ecouter Worker.State.SUCCEEDED = HTML charge, Leaflet en cours
        engine.getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                // Attendre 1.5s pour laisser Leaflet + tuiles se charger
                PauseTransition pause = new PauseTransition(Duration.millis(1500));
                pause.setOnFinished(e -> {
                    mapReady = true;
                    hideLoading();
                    animateMapReveal();
                    setStatus("Carte chargee", C_GREEN);
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
                    setStatus("Carte prete", C_GREEN);
                });
            }
        });

        engine.loadContent(html, "text/html");
    }

    // ─── execScript avec garde mapReady ──────────────────────
    private void execScript(String script) {
        if (mapView == null) return;
        if (!mapReady) {
            setStatus("Carte en cours de chargement...", C_CYAN);
            return;
        }
        try {
            mapView.getEngine().executeScript(script);
        } catch (Exception ex) {
            // Silencieux - la carte n'est pas encore prete
            setStatus("Patientez...", C_CYAN);
        }
    }

    // ─── Buttons ──────────────────────────────────────────────
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

        // ✅ execScript verifie mapReady avant d'appeler le JS
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
