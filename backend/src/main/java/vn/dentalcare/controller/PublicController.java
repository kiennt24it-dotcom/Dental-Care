package vn.dentalcare.controller;
import vn.dentalcare.dto.*;
import vn.dentalcare.dto.CatalogDtos.*;
import vn.dentalcare.service.CatalogService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/public")
public class PublicController {
    private final CatalogService catalog;
    public PublicController(CatalogService catalog) { this.catalog=catalog; }
    @GetMapping("/services") public ApiResponse<List<ServiceResponse>> services() { return ApiResponse.ok(catalog.services()); }
    @GetMapping("/doctors") public ApiResponse<List<DoctorResponse>> doctors() { return ApiResponse.ok(catalog.doctors()); }
    @GetMapping("/specialties") public ApiResponse<List<SpecialtyResponse>> specialties() { return ApiResponse.ok(catalog.specialties()); }
}
