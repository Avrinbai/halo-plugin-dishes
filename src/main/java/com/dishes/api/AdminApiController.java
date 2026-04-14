package com.dishes.api;

import com.dishes.service.AdminFacadeService;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/apis/plugins/dishes/admin", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminApiController {

    private final AdminFacadeService adminFacadeService;

    public AdminApiController(AdminFacadeService adminFacadeService) {
        this.adminFacadeService = adminFacadeService;
    }

    @GetMapping("/categories")
    public Envelope<Map<String, Object>> listCategories() {
        return Envelope.ok(adminFacadeService.listCategories());
    }

    @PostMapping("/categories")
    public Envelope<Map<String, Object>> createCategory(@RequestBody CategoryCreateReq req) {
        return Envelope.ok(adminFacadeService.createCategory(req.name(), req.slug(), req.sortOrder()));
    }

    @PutMapping("/categories/{id}")
    public Envelope<Map<String, Object>> updateCategory(@PathVariable("id") long id, @RequestBody CategoryCreateReq req) {
        return Envelope.ok(adminFacadeService.updateCategory(id, req.name(), req.slug(), req.sortOrder()));
    }

    @DeleteMapping("/categories/{id}")
    public Envelope<Map<String, Object>> deleteCategory(@PathVariable("id") long id) {
        return Envelope.ok(adminFacadeService.deleteCategory(id));
    }

    @GetMapping("/dishes")
    public Envelope<Map<String, Object>> listDishes() {
        return Envelope.ok(adminFacadeService.listDishes());
    }

    @PostMapping("/dishes")
    public Envelope<Map<String, Object>> createDish(@RequestBody DishCreateReq req) {
        return Envelope.ok(adminFacadeService.createDish(
            req.categoryId(),
            req.name(),
            req.imageUrl(),
            req.recommendationLevel(),
            req.description(),
            req.isAvailable(),
            req.sortOrder(),
            req.mealPeriodIds() == null ? List.of() : req.mealPeriodIds()
        ));
    }

    @PutMapping("/dishes/{id}")
    public Envelope<Map<String, Object>> updateDish(@PathVariable("id") long id, @RequestBody DishCreateReq req) {
        return Envelope.ok(adminFacadeService.updateDish(
            id,
            req.categoryId(),
            req.name(),
            req.imageUrl(),
            req.recommendationLevel(),
            req.description(),
            req.isAvailable(),
            req.sortOrder(),
            req.mealPeriodIds() == null ? List.of() : req.mealPeriodIds()
        ));
    }

    @DeleteMapping("/dishes/{id}")
    public Envelope<Map<String, Object>> deleteDish(@PathVariable("id") long id) {
        return Envelope.ok(adminFacadeService.deleteDish(id));
    }

    @GetMapping("/orders")
    public Envelope<Map<String, Object>> listOrders(
        @RequestParam(name = "from", required = false) String from,
        @RequestParam(name = "to", required = false) String to,
        @RequestParam(name = "period", required = false) String period,
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "limit", defaultValue = "30") int limit
    ) {
        return Envelope.ok(adminFacadeService.listOrders(from, to, period, page, limit));
    }

    @GetMapping("/orders/{id}")
    public Envelope<Map<String, Object>> getOrder(@PathVariable("id") long id) {
        return Envelope.ok(adminFacadeService.getOrder(id));
    }

    @GetMapping("/settings")
    public Envelope<Map<String, Object>> getSettings() {
        return Envelope.ok(adminFacadeService.getSettings());
    }

    @PutMapping("/settings")
    public Envelope<Map<String, Object>> updateSettings(@RequestBody SettingsUpdateReq req) {
        var basic = req.basic();
        var notify = req.notifyConfig();
        return Envelope.ok(adminFacadeService.updateSettings(
            basic == null ? null : basic.accessMode(),
            basic == null ? null : basic.accessPassword(),
            basic == null ? null : basic.publicAccessUrl(),
            basic == null ? null : basic.publicLogoUrl(),
            notify == null ? null : notify.enabled(),
            notify == null ? null : notify.channel(),
            notify == null ? null : notify.webhookUrl(),
            notify == null ? null : notify.orderNowEnabled(),
            notify == null ? null : notify.orderReservationEnabled()
        ));
    }

    public record CategoryCreateReq(String name, String slug, int sortOrder) {
        public CategoryCreateReq {
            if (name == null) name = "";
            if (slug == null) slug = "";
        }
    }

    public record DishCreateReq(
        long categoryId,
        String name,
        String imageUrl,
        int recommendationLevel,
        String description,
        boolean isAvailable,
        int sortOrder,
        List<Long> mealPeriodIds
    ) {
        public DishCreateReq {
            if (name == null) name = "";
        }
    }

    public record SettingsUpdateReq(BasicSettingsReq basic, NotifySettingsReq notifyConfig) {}

    public record BasicSettingsReq(String accessMode, String accessPassword, String publicAccessUrl, String publicLogoUrl) {}

    public record NotifySettingsReq(
        Boolean enabled,
        String channel,
        String webhookUrl,
        Boolean orderNowEnabled,
        Boolean orderReservationEnabled
    ) {}
}

