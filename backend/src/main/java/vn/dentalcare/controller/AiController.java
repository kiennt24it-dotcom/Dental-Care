package vn.dentalcare.controller;
import vn.dentalcare.dto.ApiResponse;
import vn.dentalcare.service.AiAssistantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController
@RequestMapping("/api/ai")
public class AiController {
    public record SymptomRequest(
        @NotBlank @Size(max=4000) String symptoms,
        @AssertTrue(message="Bạn cần đồng ý chia sẻ triệu chứng với nhà cung cấp AI bên ngoài")
        boolean consentToExternalProcessing
    ) {}
    private final AiAssistantService ai;
    public AiController(AiAssistantService ai) { this.ai=ai; }
    @PostMapping("/symptoms") @PreAuthorize("hasRole('PATIENT')")
    public ApiResponse<Map<?,?>> analyze(@Valid @RequestBody SymptomRequest request) { return ApiResponse.ok(ai.analyze(request.symptoms())); }
}
