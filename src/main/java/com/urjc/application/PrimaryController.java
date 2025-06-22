package com.urjc.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.urjc.application.analyzer.ASTAnalyzer;
import com.urjc.application.e3value.E3ValueMXGraphGenerator;
import com.urjc.application.model.Contract;
import com.urjc.application.model.Event;
import com.urjc.application.model.EventType;
import com.urjc.application.parser.CompilationException;
import com.urjc.application.parser.SolidityParser;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class PrimaryController {

    @FXML
    private Button btnLoadSolidity;

    @FXML
    public void initialize() {
        btnLoadSolidity.setOnAction(e -> loadSolidity());
    }

    private void loadSolidity() {
        try {
            FileChooser fc = new FileChooser();
            fc.setTitle("Seleccionar archivo Solidity (.sol)");
            File solFile = fc.showOpenDialog(App.getMainStage());
            if (solFile == null) {
                return;
            }

            String sourceCode = Files.readString(solFile.toPath());

            JsonNode ast = SolidityParser.compileToAst(sourceCode);

            List<String> names = ASTAnalyzer.listContractNames(ast);
            String chosen;
            if (names.size() <= 1) {
                chosen = names.isEmpty() ? null : names.get(0);
            } else {
                ChoiceDialog<String> dlg = new ChoiceDialog<>(names.get(0), names);
                dlg.setTitle("Seleccionar contrato");
                dlg.setHeaderText("Varios contratos encontrados:");
                dlg.setContentText("Contrato:");
                Optional<String> res = dlg.showAndWait();
                if (res.isEmpty()) {
                    return;
                }
                chosen = res.get();
            }

            Contract contract = ASTAnalyzer.analyzeAST(ast, chosen, sourceCode);

            List<Event> toAsk = contract.getEvents().stream()
                    .collect(Collectors.toList());
            Map<Event, EventType> map = EventTypeDialog.ask(toAsk);
            contract.setEventTypeMap(map);

            List<ValueObjectDialog.Question> qs = new ArrayList<>();
            contract.getFunctions().forEach(fn -> {
                String src = fn.getTriggeredByActor();
                fn.getDestinations().forEach(dst -> {
                    qs.add(new ValueObjectDialog.Question(src, dst, fn.getName()));
                });
            });
            Map<String, String> revMap = ValueObjectDialog.ask(qs);
            contract.getFunctions().forEach(fn -> {
                fn.getDestinations().forEach(dst -> {
                    String key = fn.getTriggeredByActor() + "→" + dst + "→" + fn.getName();
                    if (revMap.containsKey(key)) {
                        fn.setReverseObject(dst, revMap.get(key));
                    }
                });
            });

            String xml = E3ValueMXGraphGenerator.generate(contract);
            goToSecondary(xml);

        } catch (CompilationException | IOException ex) {
            ex.printStackTrace();
        }
    }

    private void goToSecondary(String xmlContent) {
        try {
            var loader = new javafx.fxml.FXMLLoader(getClass().getResource("secondary.fxml"));
            javafx.scene.Parent root = loader.load();
            SecondaryController sc = loader.getController();
            sc.setXmlContent(xmlContent);
            App.getMainStage().setScene(new javafx.scene.Scene(root, 600, 400));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
