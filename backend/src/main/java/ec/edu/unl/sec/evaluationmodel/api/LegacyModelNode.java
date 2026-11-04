package ec.edu.unl.sec.evaluationmodel.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import ec.edu.unl.sec.evaluationmodel.domain.ModelNode;
import java.util.List;
import java.util.UUID;

/** The v1 transport shape stays independent of the typed hierarchy planned for v2. */
public record LegacyModelNode(UUID id, String code, String name, String description, int position,
        List<LegacyModelNode> children) {
    @JsonAnySetter
    public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("unknown v1 model node field");
    }

    public ModelNode toDomain() {
        return new ModelNode(id, code, name, description, position,
                children == null ? List.of() : children.stream().map(LegacyModelNode::toDomain).toList());
    }

    static LegacyModelNode from(ModelNode node) {
        return new LegacyModelNode(node.id(), node.code(), node.name(), node.description(), node.position(),
                node.children().stream().map(LegacyModelNode::from).toList());
    }
}
