package ec.edu.unl.sec.evaluationmodel.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RatingScale(UUID id, String code, String name, List<Option> options) {
    public RatingScale {
        id = id == null ? UUID.randomUUID() : id;
        code = ModelNode.normalize(code);
        if (name == null || name.isBlank() || name.strip().length() > 300) throw new IllegalArgumentException("scale name invalid");
        name = name.strip();
        options = options == null ? List.of() : List.copyOf(options);
    }

    public record Option(UUID id, String code, String label, int position, BigDecimal declaredValue) {
        public Option {
            id = id == null ? UUID.randomUUID() : id;
            code = ModelNode.normalize(code);
            if (label == null || label.isBlank() || label.strip().length() > 200) throw new IllegalArgumentException("option label invalid");
            label = label.strip();
            if (position < 1) throw new IllegalArgumentException("position must be positive");
        }
    }
}
