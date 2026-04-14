package com.dishes.service.admin;

import com.dishes.api.BusinessErrorCode;
import com.dishes.api.BusinessException;
import com.dishes.domain.DishesStore;
import com.dishes.service.dto.ApiPayloads;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminOrderService {

    private final DishesStore store;

    public AdminOrderService(DishesStore store) {
        this.store = store;
    }

    public Map<String, Object> listOrders(String from, String to, String period, int page, int limit) {
        var toDate = (to == null || to.isBlank()) ? LocalDate.now() : LocalDate.parse(to);
        var fromDate = (from == null || from.isBlank()) ? toDate.minusDays(60) : LocalDate.parse(from);
        if (fromDate.isAfter(toDate)) {
            var temp = fromDate;
            fromDate = toDate;
            toDate = temp;
        }
        var normalizedPage = Math.max(1, page);
        var normalizedLimit = Math.max(1, Math.min(200, limit));
        var offset = (normalizedPage - 1) * normalizedLimit;
        var normalizedPeriod = (period == null || period.isBlank()) ? null : period.trim();
        var result = store.listOrdersSummary(fromDate, toDate, normalizedLimit, offset, normalizedPeriod);
        return ApiPayloads.pagedRange(fromDate, toDate, result);
    }

    public Map<String, Object> getOrder(long id) {
        var o = store.findOrderById(id);
        if (o == null) throw new BusinessException(BusinessErrorCode.NOT_FOUND, "notfound");
        return ApiPayloads.orderDetail(o, store.orderToApi(o));
    }
}
