package ec.edu.unl.sec.evaluationmodel.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PublicationValidator {
    private PublicationValidator() {}

    public static List<String> validate(ModelVersion version, UUID approver, String formulaSchema) {
        List<String> issues = new ArrayList<>();
        if (version.source().isBlank()) issues.add("SOURCE_REQUIRED");
        if (approver == null) issues.add("APPROVER_REQUIRED");
        else if (approver.equals(version.authorId())) issues.add("APPROVER_MUST_DIFFER_FROM_AUTHOR");
        if (version.criteria().isEmpty()) issues.add("CRITERION_REQUIRED");
        Set<String> criteria = new HashSet<>(), standards = new HashSet<>(), indicators = new HashSet<>();
        for (ModelNode criterion : version.criteria()) {
            duplicate(criteria, criterion.code(), "CRITERION_CODE_DUPLICATE", issues);
            if (criterion.children().isEmpty()) issues.add("STANDARD_REQUIRED:" + criterion.code());
            for (ModelNode standard : criterion.children()) {
                duplicate(standards, standard.code(), "STANDARD_CODE_DUPLICATE", issues);
                if (standard.children().isEmpty()) issues.add("INDICATOR_REQUIRED:" + standard.code());
                for (ModelNode indicator : standard.children()) duplicate(indicators, indicator.code(), "INDICATOR_CODE_DUPLICATE", issues);
            }
        }
        if (version.scale() == null) issues.add("SCALE_REQUIRED");
        else {
            if (version.scale().options().isEmpty()) issues.add("SCALE_OPTION_REQUIRED");
            Set<String> options = new HashSet<>();
            version.scale().options().forEach(o -> duplicate(options, o.code(), "SCALE_OPTION_CODE_DUPLICATE", issues));
        }
        issues.addAll(FormulaValidator.validate(version.formula(), formulaSchema, indicators));
        return List.copyOf(issues);
    }

    private static void duplicate(Set<String> seen, String code, String issue, List<String> issues) {
        if (!seen.add(code)) issues.add(issue + ":" + code);
    }
}
