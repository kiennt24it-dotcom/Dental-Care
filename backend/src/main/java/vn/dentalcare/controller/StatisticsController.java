package vn.dentalcare.controller;
import vn.dentalcare.dto.*;
import vn.dentalcare.dto.StatisticsDtos.Overview;
import vn.dentalcare.service.StatisticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {
    private final StatisticsService statistics;
    public StatisticsController(StatisticsService statistics){this.statistics=statistics;}
    @GetMapping("/overview") @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ApiResponse<Overview> overview(){return ApiResponse.ok(statistics.overview());}
}
