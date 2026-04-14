package com.dishes.service;

import com.dishes.api.BusinessErrorCode;
import com.dishes.api.BusinessException;
import com.dishes.domain.DishesStore;
import com.dishes.service.dto.ApiPayloads;
import com.dishes.service.publics.OrderNotifyService;
import com.dishes.service.publics.PublicAccessService;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;

@Service
public class PublicFacadeService {

    private final DishesStore store;
    private final PublicAccessService publicAccessService;
    private final DishesSettingsService settingsService;
    private final OrderNotifyService orderNotifyService;

    public PublicFacadeService(
        DishesStore store,
        PublicAccessService publicAccessService,
        DishesSettingsService settingsService,
        OrderNotifyService orderNotifyService
    ) {
        this.store = store;
        this.publicAccessService = publicAccessService;
        this.settingsService = settingsService;
        this.orderNotifyService = orderNotifyService;
    }

    public Map<String, Object> dishes(Long categoryId, String token) {
        publicAccessService.ensureAccess(token);
        return ApiPayloads.itemList(store.listDishes(categoryId));
    }

    public Map<String, Object> today(String date, String token) {
        publicAccessService.ensureAccess(token);
        var targetDate = parseOrderDateOrToday(date);
        return Map.of("date", targetDate.toString(), "periods", store.dayOverview(targetDate));
    }

    public Map<String, Object> history(String from, String to, int limit, int offset, String token) {
        publicAccessService.ensureAccess(token);
        var fromDate = LocalDate.parse(from);
        var toDate = LocalDate.parse(to);
        var result = store.history(fromDate, toDate, limit, offset);
        return buildPagedHistoryResponse(fromDate, toDate, result);
    }

    public Map<String, Object> storeOrder(
        String orderDate,
        String mealPeriodCode,
        String remark,
        List<DishesStore.OrderItem> items,
        String token,
        ServerHttpRequest request
    ) {
        publicAccessService.ensureAccess(token);
        var date = parseOrderDateOrToday(orderDate);
        if (mealPeriodCode == null || mealPeriodCode.isBlank()) {
            throw new BusinessException(BusinessErrorCode.INVALID_MEAL_PERIOD_CODE, "meal_period_code is required");
        }
        var o = store.replaceOrder(date, mealPeriodCode, remark, items);
        orderNotifyService.tryPushNotify(settingsService.getOrInitSettings().getSpec(), date, mealPeriodCode, items, request);
        return Map.of("order_date", date.toString(), "meal_period_code", mealPeriodCode, "order", store.orderToApi(o));
    }

    public record OrderItemInput(
        @JsonProperty("dish_id") long dishId,
        @JsonProperty("quantity") double quantity,
        @JsonProperty("note") String note
    ) {}

    public List<DishesStore.OrderItem> normalizeOrderItemInputs(List<OrderItemInput> items) {
        var normalized = new ArrayList<DishesStore.OrderItem>();
        if (items == null) return normalized;
        for (var it : items) {
            if (it.dishId() < 1) continue;
            var qty = it.quantity() <= 0 ? 1.0 : it.quantity();
            normalized.add(new DishesStore.OrderItem(it.dishId(), qty, it.note()));
        }
        return normalized;
    }

    public Map<String, Object> randomByPeriod(String code, int count, String exclude, String token) {
        publicAccessService.ensureAccess(token);
        var excludeIds = parseExcludeIds(exclude);
        return Map.of("meal_period_code", code, "items", store.randomByPeriod(code, count, excludeIds));
    }

    public Map<String, Object> accessStatus(String token) {
        return publicAccessService.accessStatus(token);
    }

    public Map<String, Object> verifyPassword(String password) {
        return publicAccessService.verifyPassword(password);
    }

    private Set<Long> parseExcludeIds(String exclude) {
        var excludeIds = new HashSet<Long>();
        if (exclude == null || exclude.isBlank()) return excludeIds;
        for (var s : exclude.split(",")) {
            try {
                excludeIds.add(Long.parseLong(s.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return excludeIds;
    }

    private LocalDate parseOrderDateOrToday(String date) {
        return (date == null || date.isBlank()) ? LocalDate.now() : LocalDate.parse(date);
    }

    private Map<String, Object> buildPagedHistoryResponse(LocalDate fromDate, LocalDate toDate, Map<String, Object> historyResult) {
        return ApiPayloads.pagedRange(fromDate, toDate, historyResult);
    }

}

