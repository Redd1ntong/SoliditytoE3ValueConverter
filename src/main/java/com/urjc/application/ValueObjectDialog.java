package com.urjc.application;

import javafx.scene.control.TextInputDialog;
import java.util.*;

public class ValueObjectDialog {

    public static Map<String, String> ask(List<Question> questions) {

        Map<String, String> result = new HashMap<>();

        questions.forEach(q -> {
            TextInputDialog dlg = new TextInputDialog();
            dlg.setTitle("Objeto de valor de vuelta");
            dlg.setHeaderText("Actividad: " + q.activity);
            dlg.setContentText(
                    "¿Qué entrega " + q.dst + " a " + q.src + "?\n"
                            + "(deja vacío si no hay nada)");
            
            dlg.showAndWait().ifPresent(txt -> {
                if (!txt.isBlank()) {
                    result.put(q.key(), txt.trim());
                }
            });
        });
        return result;
    }

    public static class Question {

        public final String src, dst, activity;

        public Question(String s, String d, String a) {
            src = s;
            dst = d;
            activity = a;
        }

        String key() {
            return src + "→" + dst + "→" + activity;
        }
    }
}
