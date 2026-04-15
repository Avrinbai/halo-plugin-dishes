package com.dishes.service;

import com.dishes.service.admin.AdminMenuService;
import com.dishes.service.admin.AdminOrderService;
import com.dishes.service.admin.AdminSettingsService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminFacadeService {

    private final AdminMenuService adminMenuService;
    private final AdminOrderService adminOrderService;
    private final AdminSettingsService adminSettingsService;

    public AdminFacadeService(
        AdminMenuService adminMenuService,
        AdminOrderService adminOrderService,
        AdminSettingsService adminSettingsService
    ) {
        this.adminMenuService = adminMenuService;
        this.adminOrderService = adminOrderService;
        this.adminSettingsService = adminSettingsService;
    }

    public Map<String, Object> listCategories() {
        return adminMenuService.listCategories();
    }

    public Map<String, Object> createCategory(String name, String slug, int sortOrder) {
        return adminMenuService.createCategory(name, slug, sortOrder);
    }

    public Map<String, Object> updateCategory(long id, String name, String slug, int sortOrder) {
        return adminMenuService.updateCategory(id, name, slug, sortOrder);
    }

    public Map<String, Object> deleteCategory(long id) {
        return adminMenuService.deleteCategory(id);
    }

    public Map<String, Object> listDishes() {
        return adminMenuService.listDishes();
    }

    public Map<String, Object> createDish(
        long categoryId,
        String name,
        String imageUrl,
        int recommendationLevel,
        String description,
        boolean isAvailable,
        int sortOrder,
        List<Long> mealPeriodIds
    ) {
        return adminMenuService.createDish(
            categoryId,
            name,
            imageUrl,
            recommendationLevel,
            description,
            isAvailable,
            sortOrder,
            mealPeriodIds
        );
    }

    public Map<String, Object> updateDish(
        long id,
        long categoryId,
        String name,
        String imageUrl,
        int recommendationLevel,
        String description,
        boolean isAvailable,
        int sortOrder,
        List<Long> mealPeriodIds
    ) {
        return adminMenuService.updateDish(
            id,
            categoryId,
            name,
            imageUrl,
            recommendationLevel,
            description,
            isAvailable,
            sortOrder,
            mealPeriodIds
        );
    }

    public Map<String, Object> deleteDish(long id) {
        return adminMenuService.deleteDish(id);
    }

    public Map<String, Object> listOrders(String from, String to, String period, int page, int limit) {
        return adminOrderService.listOrders(from, to, period, page, limit);
    }

    public Map<String, Object> getOrder(long id) {
        return adminOrderService.getOrder(id);
    }

    public Map<String, Object> getSettings() {
        return adminSettingsService.getSettings();
    }

    public Map<String, Object> updateSettings(
        String accessMode,
        String accessPassword,
        String publicAccessUrl,
        String publicLogoUrl,
        String publicDomainWhitelist,
        Boolean notifyEnabled,
        String notifyChannel,
        String notifyWebhookUrl,
        Boolean notifyOrderNowEnabled,
        Boolean notifyOrderReservationEnabled
    ) {
        return adminSettingsService.updateSettings(
            accessMode,
            accessPassword,
            publicAccessUrl,
            publicLogoUrl,
            publicDomainWhitelist,
            notifyEnabled,
            notifyChannel,
            notifyWebhookUrl,
            notifyOrderNowEnabled,
            notifyOrderReservationEnabled
        );
    }
}

