package com.dishes.extension;

import io.swagger.v3.oas.annotations.media.Schema;
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
    kind = "DishCategory",
    plural = "dishcategories",
    singular = "dishcategory"
)
public class DishCategory extends AbstractExtension {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Spec spec;

    @Data
    @Schema(name = "DishCategorySpec")
    public static class Spec {
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64)
        private String name;

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 64)
        private String slug;

        @Schema(defaultValue = "0")
        private Integer sortOrder;
    }
}


