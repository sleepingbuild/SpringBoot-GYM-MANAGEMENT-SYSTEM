package com.gym.management.service.impl;

import com.gym.management.dto.response.CheckinTrafficResponse;
import com.gym.management.dto.response.RenewalRateResponse;
import com.gym.management.dto.response.RevenueReportResponse;
import com.gym.management.dto.response.TopPackagesResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ErrorCode;
import com.gym.management.repository.ReportRepository;
import com.gym.management.repository.ReportRepository.RenewalCounts;
import com.gym.management.repository.ReportRepository.RevenueRow;
import com.gym.management.repository.ReportRepository.TopPackageRow;
import com.gym.management.repository.ReportRepository.TrafficRow;
import com.gym.management.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 6 - Báo cáo chỉ đọc. Ngày "hôm nay" và khoảng mặc định tính theo Asia/Ho_Chi_Minh.
 * Khoảng ngày [from, to] gồm cả hai đầu, tối đa 366 ngày, mặc định từ ngày 1 của tháng đến hôm nay.
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MAX_RANGE_DAYS = 366;

    private final ReportRepository reportRepository;

    /** Không phải final để test cố định "hôm nay"; không nằm trong constructor. */
    Clock clock = Clock.system(VN_ZONE);

    private record Range(LocalDate from, LocalDate to) {
    }

    // ---------- revenue ----------

    @Override
    @Transactional(readOnly = true)
    public RevenueReportResponse revenue(String fromRaw, String toRaw, String groupByRaw) {
        Range range = resolveRange(fromRaw, toRaw);
        boolean monthly = parseMonthly(groupByRaw);

        List<RevenueRow> rows = reportRepository.revenueByDay(
                range.from().atStartOfDay(), range.to().plusDays(1).atStartOfDay());

        // bucket: [0] = MEMBERSHIP, [1] = POS; điền sẵn số 0 cho kỳ không có dữ liệu
        Map<String, BigDecimal[]> buckets = new LinkedHashMap<>();
        if (monthly) {
            YearMonth last = YearMonth.from(range.to());
            for (YearMonth ym = YearMonth.from(range.from()); !ym.isAfter(last); ym = ym.plusMonths(1)) {
                buckets.put(ym.toString(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            }
        } else {
            for (LocalDate d = range.from(); !d.isAfter(range.to()); d = d.plusDays(1)) {
                buckets.put(d.toString(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            }
        }

        for (RevenueRow row : rows) {
            String key = monthly ? YearMonth.from(row.day()).toString() : row.day().toString();
            BigDecimal[] bucket = buckets.computeIfAbsent(key,
                    k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal amount = row.total() == null ? BigDecimal.ZERO : row.total();
            if ("MEMBERSHIP".equals(row.relatedType())) {
                bucket[0] = bucket[0].add(amount);
            } else if ("POS".equals(row.relatedType())) {
                bucket[1] = bucket[1].add(amount);
            }
        }

        List<RevenueReportResponse.Item> items = new ArrayList<>();
        BigDecimal membershipTotal = BigDecimal.ZERO;
        BigDecimal posTotal = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal[]> e : buckets.entrySet()) {
            BigDecimal membership = e.getValue()[0];
            BigDecimal pos = e.getValue()[1];
            items.add(new RevenueReportResponse.Item(e.getKey(), membership, pos, membership.add(pos)));
            membershipTotal = membershipTotal.add(membership);
            posTotal = posTotal.add(pos);
        }
        return new RevenueReportResponse(range.from(), range.to(), monthly ? "MONTH" : "DAY",
                membershipTotal.add(posTotal), membershipTotal, posTotal, items);
    }

    // ---------- renewal-rate ----------

    @Override
    @Transactional(readOnly = true)
    public RenewalRateResponse renewalRate(String fromRaw, String toRaw, String windowRaw) {
        Range range = resolveRange(fromRaw, toRaw);
        int window = parseBoundedInt(windowRaw, 30, 1, 365, "windowDays");
        LocalDate today = LocalDate.now(clock);

        RenewalCounts counts = reportRepository.renewalCounts(range.from(), range.to(), today, window);
        BigDecimal rate = counts.expired() == 0 ? null
                : BigDecimal.valueOf(counts.renewed())
                        .divide(BigDecimal.valueOf(counts.expired()), 4, RoundingMode.HALF_UP);
        return new RenewalRateResponse(range.from(), range.to(), window,
                counts.expired(), counts.renewed(), rate);
    }

    // ---------- checkin-traffic ----------

    @Override
    @Transactional(readOnly = true)
    public CheckinTrafficResponse checkinTraffic(String fromRaw, String toRaw) {
        Range range = resolveRange(fromRaw, toRaw);
        List<TrafficRow> rows = reportRepository.checkinTraffic(
                range.from().atStartOfDay(), range.to().plusDays(1).atStartOfDay());

        Map<LocalDate, Long> byDay = new LinkedHashMap<>();
        for (LocalDate d = range.from(); !d.isAfter(range.to()); d = d.plusDays(1)) {
            byDay.put(d, 0L);
        }
        long[] byHour = new long[24];
        long total = 0;
        for (TrafficRow row : rows) {
            byDay.merge(row.day(), row.count(), Long::sum);
            if (row.hour() >= 0 && row.hour() < 24) {
                byHour[row.hour()] += row.count();
            }
            total += row.count();
        }

        List<CheckinTrafficResponse.DayCount> dayItems = new ArrayList<>();
        byDay.forEach((date, count) -> dayItems.add(new CheckinTrafficResponse.DayCount(date, count)));
        List<CheckinTrafficResponse.HourCount> hourItems = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            hourItems.add(new CheckinTrafficResponse.HourCount(h, byHour[h]));
        }
        return new CheckinTrafficResponse(range.from(), range.to(), total, dayItems, hourItems);
    }

    // ---------- top-packages ----------

    @Override
    @Transactional(readOnly = true)
    public TopPackagesResponse topPackages(String fromRaw, String toRaw, String limitRaw) {
        Range range = resolveRange(fromRaw, toRaw);
        int limit = parseBoundedInt(limitRaw, 10, 1, 50, "limit");

        List<TopPackageRow> rows = reportRepository.topPackages(
                range.from().atStartOfDay(), range.to().plusDays(1).atStartOfDay(), limit);
        List<TopPackagesResponse.Item> items = rows.stream()
                .map(r -> new TopPackagesResponse.Item(r.packageId(), r.packageName(), r.registrations(),
                        r.revenue() == null ? BigDecimal.ZERO : r.revenue()))
                .toList();
        return new TopPackagesResponse(range.from(), range.to(), items);
    }

    // ---------- helpers ----------

    private Range resolveRange(String fromRaw, String toRaw) {
        LocalDate today = LocalDate.now(clock);
        LocalDate to = parseDate(toRaw, "to");
        if (to == null) {
            to = today;
        }
        LocalDate from = parseDate(fromRaw, "from");
        if (from == null) {
            from = to.withDayOfMonth(1);
        }
        if (from.isAfter(to)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "from phải nhỏ hơn hoặc bằng to");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Khoảng thời gian tối đa " + MAX_RANGE_DAYS + " ngày");
        }
        return new Range(from, to);
    }

    private LocalDate parseDate(String raw, String name) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Tham số " + name + " phải có dạng yyyy-MM-dd");
        }
    }

    private boolean parseMonthly(String raw) {
        if (raw == null || raw.isBlank() || "DAY".equalsIgnoreCase(raw.trim())) {
            return false;
        }
        if ("MONTH".equalsIgnoreCase(raw.trim())) {
            return true;
        }
        throw new BusinessException(ErrorCode.VALIDATION_ERROR, "groupBy chỉ nhận DAY hoặc MONTH");
    }

    private int parseBoundedInt(String raw, int defaultValue, int min, int max, String name) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        int value;
        try {
            value = Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tham số " + name + " phải là số nguyên");
        }
        if (value < min || value > max) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Tham số " + name + " phải trong khoảng " + min + " đến " + max);
        }
        return value;
    }
}
