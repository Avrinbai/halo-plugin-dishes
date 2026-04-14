package com.dishes.api;

import com.dishes.service.PublicFacadeService;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {"/apis/plugins/dishes/public", "/plugins/dishes/public"}, produces = MediaType.APPLICATION_JSON_VALUE)
public class PublicApiController {

    private final PublicFacadeService publicFacadeService;

    public PublicApiController(PublicFacadeService publicFacadeService) {
        this.publicFacadeService = publicFacadeService;
    }

    @GetMapping("/dishes")
    public Envelope<Map<String, Object>> listDishes(
        @RequestParam(name = "category_id", required = false) Long categoryId,
        @RequestHeader(name = "X-Dishes-Access-Token", required = false) String token
    ) {
        return Envelope.ok(publicFacadeService.dishes(categoryId, token));
    }

    @GetMapping("/meal-orders/today")
    public Envelope<Map<String, Object>> getTodayOrders(
        @RequestParam(name = "date", required = false) String date,
        @RequestHeader(name = "X-Dishes-Access-Token", required = false) String token
    ) {
        return Envelope.ok(publicFacadeService.today(date, token));
    }

    @GetMapping("/meal-orders/history")
    public Envelope<Map<String, Object>> getOrderHistory(
        @RequestParam("from") String from,
        @RequestParam("to") String to,
        @RequestParam(name = "limit", defaultValue = "30") int limit,
        @RequestParam(name = "offset", defaultValue = "0") int offset,
        @RequestHeader(name = "X-Dishes-Access-Token", required = false) String token
    ) {
        return Envelope.ok(publicFacadeService.history(from, to, limit, offset, token));
    }

    public record MealOrderReq(
        @JsonProperty("order_date") String orderDate,
        @JsonProperty("meal_period_code") String mealPeriodCode,
        String remark,
        @JsonProperty("set_timestamps_to_order_date") boolean setTimestampsToOrderDate,
        List<PublicFacadeService.OrderItemInput> items
    ) {}

    @PostMapping("/meal-orders")
    public Envelope<Map<String, Object>> storeOrder(
        @RequestBody MealOrderReq req,
        @RequestHeader(name = "X-Dishes-Access-Token", required = false) String token,
        ServerHttpRequest request
    ) {
        return Envelope.ok(publicFacadeService.storeOrder(
            req.orderDate(),
            req.mealPeriodCode(),
            req.remark(),
            publicFacadeService.normalizeOrderItemInputs(req.items()),
            token,
            request
        ));
    }

    @GetMapping("/recommendations/random-by-period")
    public Envelope<Map<String, Object>> randomByPeriod(
        @RequestParam("code") String code,
        @RequestParam(name = "count", defaultValue = "5") int count,
        @RequestParam(name = "exclude", required = false) String exclude,
        @RequestHeader(name = "X-Dishes-Access-Token", required = false) String token
    ) {
        return Envelope.ok(publicFacadeService.randomByPeriod(code, count, exclude, token));
    }

    @GetMapping("/access/status")
    public Envelope<Map<String, Object>> getAccessStatus(
        @RequestHeader(name = "X-Dishes-Access-Token", required = false) String token
    ) {
        return Envelope.ok(publicFacadeService.accessStatus(token));
    }

    public record PasswordVerifyReq(String password) {}

    @PostMapping("/access/password-verify")
    public Envelope<Map<String, Object>> verifyPassword(@RequestBody PasswordVerifyReq req) {
        return Envelope.ok(publicFacadeService.verifyPassword(req.password()));
    }
}

