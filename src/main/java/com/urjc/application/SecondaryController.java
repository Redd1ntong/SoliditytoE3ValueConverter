package com.urjc.application;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SecondaryController {

    @FXML
    private TextArea txtXml;

    @FXML
    private Button btnExport;

    @FXML
    private Button btnBack;

    public void setXmlContent(String content) {
        txtXml.setText(content);
    }

    @FXML
    public void initialize() {
        btnExport.setOnAction(e -> exportXml());
        btnBack.setOnAction(e -> backToPrimary());
    }

    private void exportXml() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar diagrama e3value");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo XML", "*.xml"));
        File file = fileChooser.showSaveDialog(App.getMainStage());
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(txtXml.getText());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void backToPrimary() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("primary.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 600, 400);
            App.getMainStage().setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
