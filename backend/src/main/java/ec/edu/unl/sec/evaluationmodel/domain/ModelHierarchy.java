package ec.edu.unl.sec.evaluationmodel.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Resolves legacy levels and explicit node types without changing stored identities. */
public final class ModelHierarchy {
    private ModelHierarchy() {}

    public static Result inspect(List<ModelNode> roots) {
        List<Entry> entries = new ArrayList<>();
        List<String> issues = new ArrayList<>();
        walk(roots, null, 0, new HashSet<>(), new HashSet<>(), new HashMap<>(), entries, issues);
        return new Result(List.copyOf(entries), List.copyOf(issues));
    }

    private static void walk(List<ModelNode> nodes, UUID parentId, int depth, Set<UUID> path, Set<UUID> ids,
            Map<String, Set<String>> codes, List<Entry> entries, List<String> issues) {
        Set<Integer> positions = new HashSet<>();
        for (ModelNode node : nodes) {
            String type = node.nodeType() == null ? switch (depth) {
                case 0 -> "CRITERION";
                case 1 -> "STANDARD";
                default -> "INDICATOR";
            } : node.nodeType();
            if (path.contains(node.id())) {
                issues.add("NODE_CYCLE:" + node.code());
                continue;
            }
            if (!ids.add(node.id())) issues.add("NODE_ID_DUPLICATE:" + node.id());
            if (!positions.add(node.position())) issues.add("NODE_POSITION_DUPLICATE:" + node.code());
            if (!codes.computeIfAbsent(type, ignored -> new HashSet<>()).add(node.code())) {
                issues.add(type + "_CODE_DUPLICATE:" + node.code());
            }
            entries.add(new Entry(node, type, parentId));
            if (type.equals("INDICATOR")) {
                if (!node.children().isEmpty()) issues.add("INDICATOR_CHILDREN_FORBIDDEN:" + node.code());
            } else if (node.children().isEmpty()) {
                issues.add((node.nodeType() == null && depth == 0 ? "STANDARD_REQUIRED:" : "INDICATOR_REQUIRED:") + node.code());
            }
            path.add(node.id());
            walk(node.children(), node.id(), depth + 1, path, ids, codes, entries, issues);
            path.remove(node.id());
        }
    }

    public record Entry(ModelNode node, String nodeType, UUID parentId) {}
    public record Result(List<Entry> entries, List<String> issues) {
        public List<ModelNode> indicators() {
            return entries.stream().filter(e -> e.nodeType().equals("INDICATOR")).map(Entry::node).toList();
        }
    }
}
