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
    kind = "DishesSettings",
    plural = "dishessettings",
    singular = "dishesettings"
)
public class DishesSettings extends AbstractExtension {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Spec spec;

    @Data
    @Schema(name = "DishesSettingsSpec")
    public static class Spec {
        /** none | password */
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"none", "password"})
        private String accessMode;

        @Schema(description = "前台访问密码（accessMode=password 时生效）")
        private String accessPassword;

        @Schema(description = "自定义前台访问链接（可选）")
        private String publicAccessUrl;

        @Schema(description = "前台 Logo（附件库图片 URL，可选）")
        private String publicLogoUrl;

        @Schema(description = "是否开启通知功能（预留）")
        private Boolean notifyEnabled;

        @Schema(description = "通知方式（预留）")
        private String notifyChannel;

        @Schema(description = "企业微信群机器人 Webhook 地址")
        private String notifyWebhookUrl;

        @Schema(description = "是否开启立即点菜通知")
        private Boolean notifyOrderNowEnabled;

        @Schema(description = "是否开启预约点菜通知")
        private Boolean notifyOrderReservationEnabled;
    }
}

