package vn.dentalcare.dto;
import java.math.BigDecimal;
import java.util.List;
public final class StatisticsDtos {
    private StatisticsDtos() {}
    public record DayMetric(String label,long appointments) {}
    public record ServiceMetric(String name,long appointments) {}
    public record Overview(long patients,long doctors,long appointmentsToday,long completed,long cancelled,long noShow,
                           double noShowRate,BigDecimal revenue,List<DayMetric> appointmentsByDay,List<ServiceMetric> popularServices) {}
}
