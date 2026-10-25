package ec.edu.unl.sec.archive.api;

import ec.edu.unl.sec.archive.application.ArchiveService;
import ec.edu.unl.sec.archive.application.IntegralClosureService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/archive")
public class ArchiveController {
    private final IntegralClosureService closure;
    private final ArchiveService archive;

    public ArchiveController(IntegralClosureService closure, ArchiveService archive) {
        this.closure = closure;
        this.archive = archive;
    }

    @GetMapping("/closure/preview")
    public IntegralClosureService.Preview preview(@RequestParam String processId) {
        return closure.preview(processId);
    }

    @PostMapping("/closure")
    public void close(@RequestParam String processId) {
        closure.close(processId);
    }

    @PostMapping("/processes/{processId}")
    public void archive(@PathVariable String processId) {
        archive.archive(processId);
    }
}
