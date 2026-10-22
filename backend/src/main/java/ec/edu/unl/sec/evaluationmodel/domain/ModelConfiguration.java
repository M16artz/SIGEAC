package ec.edu.unl.sec.evaluationmodel.domain;

import java.util.List;

public record ModelConfiguration(List<IndicatorDefinition> indicators, List<RatingScale> scales, List<ValuationRule> rules) {
    public ModelConfiguration {
        indicators = indicators == null ? List.of() : List.copyOf(indicators);
        scales = scales == null ? List.of() : List.copyOf(scales);
        rules = rules == null ? List.of() : List.copyOf(rules);
    }
}
