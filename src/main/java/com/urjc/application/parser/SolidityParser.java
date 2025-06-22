package com.urjc.application.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SolidityParser {

    private static final String SOLC_PATH = System.getProperty("os.name").toLowerCase().startsWith("windows")
                    ? "solc.exe"
                    : "solc"; 

    public static JsonNode compileToAst(String sourceCode) throws CompilationException {
        try {
            Path root = Paths.get("").toAbsolutePath();
            Path nodeModules = root.resolve("node_modules");

            File tempSol = File.createTempFile("contract_", ".sol");
            try (FileWriter fw = new FileWriter(tempSol)) {
                fw.write(sourceCode);
            }

            ProcessBuilder pb = new ProcessBuilder(
                    SOLC_PATH,
                    tempSol.getAbsolutePath(),
                    "--base-path", root.toString(),
                    "--include-path", nodeModules.toString(),
                    "--allow-paths", root + "," + nodeModules,
                    "--combined-json", "ast",
                    "--pretty-json"
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (InputStream is = process.getInputStream()) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
            }

            int exitCode = process.waitFor();
            String output = baos.toString(StandardCharsets.UTF_8);

            if (exitCode != 0) {
                throw new CompilationException("Error compiling with solc:\n" + output);
            }

            int start = output.indexOf('{');
            if (start < 0) {
                throw new CompilationException(" '{' not found:\n" + output);
            }
            int end = output.lastIndexOf('}');
            if (end < 0) {
                throw new CompilationException(" '}' not found:\n" + output);
            }
            String jsonPart = output.substring(start, end + 1);

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(jsonPart);

        } catch (IOException | InterruptedException e) {
            throw new CompilationException(e.getMessage());
        }
    }

}
