package com.urjc.application.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.urjc.application.model.Actor;
import com.urjc.application.model.Contract;
import com.urjc.application.model.Event;
import com.urjc.application.model.Function;
import com.urjc.application.model.RoleRegistry;
import com.urjc.application.model.Variable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ASTAnalyzer {

    private static final Set<String> addressIds = new LinkedHashSet<>();
    private static final RoleRegistry roles = new RoleRegistry();

    public static List<String> listContractNames(JsonNode combinedJson) {
        List<String> names = new ArrayList<>();
        if (combinedJson == null || combinedJson.isMissingNode()) {
            return names;
        }

        combinedJson.path("sources").fields().forEachRemaining(entry -> {
            JsonNode ast = entry.getValue().path("AST");
            ast.path("nodes").forEach(node -> {
                if ("ContractDefinition".equals(node.path("nodeType").asText())) {
                    names.add(node.path("name").asText("Unnamed"));
                }
            });
        });
        return names;
    }

    public static Contract analyzeAST(JsonNode combinedJson, String targetContract,
            String sourceCode) {

        resetState();
        parseNatSpecActors(sourceCode);

        Contract contract = new Contract(targetContract == null ? "DefaultName" : targetContract);

        if (combinedJson == null || combinedJson.isMissingNode()) {
            return contract;
        }

        combinedJson.path("sources").fields().forEachRemaining(entry -> {
            JsonNode astRoot = entry.getValue().path("AST");
            if (astRoot.isMissingNode()) {
                return;
            }
            for (JsonNode node : astRoot.path("nodes")) {
                if (!"ContractDefinition".equals(node.path("nodeType").asText())) {
                    continue;
                }
                String cName = node.path("name").asText("");
                if (targetContract != null && !targetContract.equals(cName)) {
                    continue;
                }
                contract.setName(cName.isEmpty() ? "MyContract" : cName);

                for (JsonNode member : node.path("nodes")) {
                    switch (member.path("nodeType").asText("")) {
                        case "VariableDeclaration":
                            processVariable(member, contract);
                            break;
                        case "EventDefinition":
                            processEvent(member, contract);
                            break;
                        case "FunctionDefinition":
                            processFunction(member, contract);
                            break;
                    }
                }
            }
        });

        contract.getFunctions().stream().filter(fn -> !(fn.getTriggeredByActor() == null))
                .forEachOrdered(fn -> {
                    fn.getTriggeredEvents().stream()
                            .map(evName -> contract.getEventByName(evName))
                            .filter(ev -> (ev != null)).forEachOrdered(ev -> {
                        ev.setTriggeredByActor(fn.getTriggeredByActor());
                    });
                });

        addressIds.forEach(v -> contract.addActor(new Actor(v)));
        roles.allActors().forEach(a -> contract.addActor(new Actor(a)));

        System.out.println("=== RESUMEN CONTRATO ===================================");
        contract.getFunctions().forEach(f -> {
            System.out.println("Función: " + f.getName()
                    + " | actor=" + f.getTriggeredByActor()
                    + " | destinos=" + f.getDestinations()
                    + " | objetos=" + f.getValueObjects());
        });
        System.out.println("========================================================");

        return contract;
    }

    private static void processVariable(JsonNode v, Contract c) {
        String name = v.path("name").asText("");
        String type = detectType(v.path("typeName"));
        c.addVariable(new Variable(name, type));

        if ("bytes32".equalsIgnoreCase(type) && name.endsWith("_ROLE")) {
            roles.register(name, prettifyRoleName(name));
        }
    }

    private static void processEvent(JsonNode eNode, Contract c) {
        String name = eNode.path("name").asText("");
        Event ev = new Event(name);
        eNode.path("parameters").path("parameters").forEach(p -> {
            if (isAddressParam(p)) {
                String param = p.path("name").asText("");
                ev.addActorParameter(param);
                if (ev.getIndexedActorParameter() == null) {
                    ev.setIndexedActorParameter(param);
                }
                addressIds.add(param);
            }
        });
        c.addEvent(ev);
    }

    private static void processFunction(JsonNode fnNode, Contract contract) {
        if (Set.of("constructor", "fallback", "receive")
                .contains(fnNode.path("kind").asText(""))) {
            return;
        }

        String fnName = fnNode.path("name").asText("");
        Function fn = new Function(fnName.isEmpty() ? "anonymous" : fnName);

        fnNode.path("parameters").path("parameters").forEach(p -> {
            if (isAddressParam(p)) {
                addressIds.add(p.path("name").asText());
            }
        });

        fnNode.path("modifiers").forEach(mod -> {
            if ("ModifierInvocation".equals(mod.path("nodeType").asText())
                    && "onlyRole".equals(mod.path("modifierName").path("name").asText())) {
                String roleId = extractIdentifierName(mod.path("arguments").get(0));
                String actor = roles.actorForRole(roleId);
                if (actor != null) {
                    fn.setTriggeredByActor(actor);
                }
            }
        });

        analyzeFunctionBody(fnNode.path("body"), fn, contract);
        contract.addFunction(fn);
    }

    private static void analyzeFunctionBody(JsonNode n, Function fn, Contract c) {
        if (n == null || n.isMissingNode()) {
            return;
        }

        if (n.isObject()) {
            String nodeType = n.path("nodeType").asText();

            if ("EmitStatement".equals(nodeType)) {
                String evName = extractIdentifierName(n.path("eventCall"));
                if (!evName.isEmpty()) {
                    fn.addTriggeredEvent(evName);
                    Event evObj = c.getEventByName(evName);
                    if (evObj != null) {
                        evObj.setOriginFunction(fn.getName());
                    }
                    System.out.println("DEBUG emit encontrado: " + fn.getName() + " → " + evName);
                }
            }

            if ("FunctionCall".equals(nodeType)) {
                String fName = getFunctionCallName(n.path("expression"));
                JsonNode args = n.path("arguments");

                if (Set.of("transfer", "_transfer", "transferFrom").contains(fName) && args.size() >= 2) {
                    String src = extractIdentifierName(args.get(0));
                    String dst = extractIdentifierName(args.get(1));
                    System.out.println("DEBUG transfer " + src + " → " + dst);

                    if (fn.getTriggeredByActor() == null && addressIds.contains(src)) {
                        fn.setTriggeredByActor(src);
                    }
                    if (addressIds.contains(dst) && !dst.equals(fn.getTriggeredByActor())) {
                        fn.addDestination(dst);
                        fn.addValueObject(c.getName());
                    }
                } else if (Set.of("mint", "_mint").contains(fName) && args.size() >= 2) {
                    String dst = extractIdentifierName(args.get(0));
                    System.out.println("DEBUG mint RSU → " + dst);
                    if (addressIds.contains(dst)) {
                        fn.addDestination(dst);
                        fn.addValueObject(c.getName());
                    }
                }
            }
            n.fieldNames().forEachRemaining(f -> analyzeFunctionBody(n.get(f), fn, c));
        } else if (n.isArray()) {
            n.forEach(child -> analyzeFunctionBody(child, fn, c));
        }
    }

    private static boolean isAddressParam(JsonNode p) {
        JsonNode t = p.path("typeName");
        return "VariableDeclaration".equals(p.path("nodeType").asText())
                && "ElementaryTypeName".equals(t.path("nodeType").asText())
                && "address".equalsIgnoreCase(t.path("name").asText());
    }

    private static String detectType(JsonNode tn) {
        if ("ElementaryTypeName".equals(tn.path("nodeType").asText())) {
            return tn.path("name").asText("");
        }
        if ("Mapping".equals(tn.path("nodeType").asText())) {
            return "mapping";
        }
        return "unknown";
    }

    private static String extractIdentifierName(JsonNode n) {
        if (n == null || n.isMissingNode()) {
            return "";
        }
        String nt = n.path("nodeType").asText("");
        if ("Identifier".equals(nt)) {
            return n.path("name").asText("");
        }
        if ("MemberAccess".equals(nt)) {
            return n.path("memberName").asText("");
        }
        if ("FunctionCall".equals(nt)) {
            return getFunctionCallName(n.path("expression"));
        }
        return n.asText();
    }

    private static String getFunctionCallName(JsonNode expr) {
        String name = expr.path("name").asText("");
        if (name.isEmpty() && "MemberAccess".equals(expr.path("nodeType").asText(""))) {
            name = expr.path("memberName").asText("");
        }
        return name;
    }

    private static void parseNatSpecActors(String src) {
        if (src == null) {
            return;
        }
        Matcher m = Pattern.compile("@custom:e3-actor\\s+(\\S+)\\s+(\\S+)", Pattern.CASE_INSENSITIVE).matcher(src);
        while (m.find()) {
            roles.register(m.group(1).trim(), m.group(2).trim());
        }
    }

    private static String prettifyRoleName(String r) {
        return r.replace("_ROLE", "").toLowerCase();
    }

    private static void resetState() {
        addressIds.clear();
        try {
            var f = roles.getClass().getDeclaredField("roleToActor");
            f.setAccessible(true);
            ((Map<?, ?>) f.get(roles)).clear();
        } catch (Exception ignored) {
        }
    }
}
