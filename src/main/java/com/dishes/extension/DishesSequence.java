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
    kind = "DishesSequence",
    plural = "dishessequences",
    singular = "dishessequence"
)
public class DishesSequence extends AbstractExtension {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Spec spec;

    @Data
    @Schema(name = "DishesSequenceSpec")
    public static class Spec {
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
        private Long nextId;
    }
}


