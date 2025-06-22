package com.urjc.application;

import com.urjc.application.model.Event;
import com.urjc.application.model.EventType;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.*;

public class EventTypeDialog {

    public static Map<Event, EventType> ask(List<Event> events) {
        Dialog<Map<Event, EventType>> dialog = new Dialog<>();
        dialog.setTitle("Tipo de eventos");
        dialog.setHeaderText("Marca cada evento como INICIAL o FINAL");

        ButtonType btnOk = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnOk, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(20, 150, 10, 10));

        Map<Event, ToggleGroup> toggles = new LinkedHashMap<>();
        for (int i = 0; i < events.size(); i++) {
            Event ev = events.get(i);
            Label lbl = new Label(ev.getName());
            RadioButton rbInit = new RadioButton("Inicial");
            RadioButton rbFinal = new RadioButton("Final");

            ToggleGroup group = new ToggleGroup();
            rbInit.setToggleGroup(group);
            rbFinal.setToggleGroup(group);
            rbInit.setSelected(true);

            grid.add(lbl, 0, i);
            grid.add(rbInit, 1, i);
            grid.add(rbFinal, 3, i);

            toggles.put(ev, group);
        }

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(bt -> {
            if (bt == btnOk) {
                Map<Event, EventType> result = new LinkedHashMap<>();
                for (Map.Entry<Event, ToggleGroup> e : toggles.entrySet()) {
                    RadioButton sel = (RadioButton) e.getValue().getSelectedToggle();
                    EventType type;
                    switch (sel.getText()) {
                        case "Final":
                            type = EventType.FINAL;
                            break;
                        default:
                            type = EventType.INITIAL;
                            break;
                    }
                    result.put(e.getKey(), type);
                }
                return result;
            }
            return null;
        });

        Optional<Map<Event, EventType>> opt = dialog.showAndWait();
        return opt.orElse(Collections.emptyMap());
    }

}
