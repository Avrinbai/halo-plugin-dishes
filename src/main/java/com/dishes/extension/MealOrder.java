package com.dishes.extension;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@GVK(
    group = "dishes.plugin.halo.run",
    version = "v1alpha1",
    kind = "MealOrder",
    plural = "mealorders",
    singular = "mealorder"
)
public class MealOrder extends AbstractExtension {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Spec spec;

    @Data
    @Schema(name = "MealOrderSpec")
    public static class Spec {
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 10, maxLength = 10, example = "2026-04-13")
        private String orderDate;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 32, example = "lunch")
        private String mealPeriodCode;

        @Schema(description = "Remark", maxLength = 255)
        private String remark;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "ISO timestamp")
        private String createdAt;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "ISO timestamp")
        private String updatedAt;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        private List<Item> items;
    }

    @Data
    @Schema(name = "MealOrderItem")
    public static class Item {
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
        private Long dishId;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0.01")
        private Double quantity;

        @Schema(description = "Note", maxLength = 255)
        private String note;
    }
}


