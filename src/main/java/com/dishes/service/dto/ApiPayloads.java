package com.dishes.service.dto;

import com.dishes.domain.DishesStore;
import com.dishes.extension.DishesSettings;
import java.time.LocalDate;
import java.util.Map;

public final class ApiPayloads {

    private ApiPayloads() {}

    public static Map<String, Object> idCreated(long id) {
        return Map.of("id", id);
    }

    public static Map<String, Object> updated(long id) {
        return Map.of("updated", true, "id", id);
    }

    public static Map<String, Object> deleted(long id) {
        return Map.of("deleted", true, "id", id);
    }

    public static Map<String, Object> updatedOnly() {
        return Map.of("updated", true);
    }

    public static Map<String, Object> itemList(Object items) {
        return Map.of("items", items);
    }

    public static Map<String, Object> pagedRange(LocalDate fromDate, LocalDate toDate, Map<String, Object> result) {
        return Map.of(
            "range", Map.of("from", fromDate.toString(), "to", toDate.toString()),
            "items", result.get("items"),
            "meta", Map.of("total", result.get("total"), "limit", result.get("limit"), "offset", result.get("offset"))
        );
    }

    public static Map<String, Object> orderDetail(DishesStore.Order order, Object orderApi) {
        return Map.of(
            "id", order.id(),
            "orderDate", order.orderDate().toString(),
            "mealPeriod", Map.of(
                "id", order.mealPeriod().id(),
                "code", order.mealPeriod().code(),
                "name", order.mealPeriod().name()
            ),
            "order", orderApi
        );
    }

    public static Map<String, Object> settingsPayload(DishesSettings.Spec spec, String mode) {
        return Map.of(
            "basic", Map.of(
                "accessMode", mode,
                "accessPassword", spec.getAccessPassword() == null ? "" : spec.getAccessPassword(),
                "accessPasswordSet", spec.getAccessPassword() != null && !spec.getAccessPassword().isBlank(),
                "publicAccessUrl", spec.getPublicAccessUrl() == null ? "" : spec.getPublicAccessUrl(),
                "publicLogoUrl", spec.getPublicLogoUrl() == null ? "" : spec.getPublicLogoUrl(),
                "publicSiteTitle", spec.getPublicSiteTitle() == null ? "" : spec.getPublicSiteTitle(),
                "publicBrandTitle", spec.getPublicBrandTitle() == null ? "" : spec.getPublicBrandTitle(),
                "publicBrandSubtitle", spec.getPublicBrandSubtitle() == null ? "" : spec.getPublicBrandSubtitle(),
                "publicDomainWhitelist", spec.getPublicDomainWhitelist() == null ? "" : spec.getPublicDomainWhitelist(),
                "defaultPublicAccessUrl", "/dishes"
            ),
            "notify", Map.of(
                "enabled", spec.getNotifyEnabled() != null && spec.getNotifyEnabled(),
                "channel", spec.getNotifyChannel() == null ? "" : spec.getNotifyChannel(),
                "webhookUrl", spec.getNotifyWebhookUrl() == null ? "" : spec.getNotifyWebhookUrl(),
                "orderNowEnabled", spec.getNotifyOrderNowEnabled() == null || spec.getNotifyOrderNowEnabled(),
                "orderReservationEnabled", spec.getNotifyOrderReservationEnabled() != null && spec.getNotifyOrderReservationEnabled()
            )
        );
    }
}
