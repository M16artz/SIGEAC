package ec.edu.unl.sec.improvement.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/improvement")
public class ImprovementController {
    @GetMapping("/plans")
    public String plans() { return "[]"; }
}
