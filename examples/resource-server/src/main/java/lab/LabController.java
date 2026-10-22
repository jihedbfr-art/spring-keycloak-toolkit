package lab;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class LabController {
    @GetMapping("/api/public") Map<String, Object> publicRoute() { return Map.of("status", "ok"); }
    @GetMapping({"/api/reader", "/api/admin"}) Map<String, Object> protectedRoute(Authentication authentication) {
        return Map.of("subject", authentication.getName(), "authorities",
                authentication.getAuthorities().stream().map(Object::toString).sorted().toList());
    }
}
