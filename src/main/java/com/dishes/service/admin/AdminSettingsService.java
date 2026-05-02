package com.dishes.service.admin;

import com.dishes.SiteRouter;
import com.dishes.extension.DishesSettings;
import com.dishes.service.DishesSettingsService;
import com.dishes.service.dto.ApiPayloads;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminSettingsService {

    private final DishesSettingsService settingsService;

    public AdminSettingsService(DishesSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public Map<String, Object> getSettings() {
        var ext = settingsService.getOrInitSettings();
        var spec = ext.getSpec();
        var mode = settingsService.normalizeAccessMode(spec.getAccessMode());
        return ApiPayloads.settingsPayload(spec, mode);
    }

    public Map<String, Object> updateSettings(
        String accessMode,
        String accessPassword,
        String publicAccessUrl,
        String publicLogoUrl,
        String publicSiteTitle,
        String publicBrandTitle,
        String publicBrandSubtitle,
        String publicDomainWhitelist,
        Boolean notifyEnabled,
        String notifyChannel,
        String notifyWebhookUrl,
        Boolean notifyOrderNowEnabled,
        Boolean notifyOrderReservationEnabled
    ) {
        var ext = settingsService.getOrInitSettings();
        var spec = ext.getSpec();
        if (accessMode != null || accessPassword != null || publicAccessUrl != null || publicLogoUrl != null
            || publicSiteTitle != null || publicBrandTitle != null || publicBrandSubtitle != null || publicDomainWhitelist != null) {
            spec.setAccessMode(settingsService.normalizeAccessMode(accessMode));
            spec.setAccessPassword(accessPassword == null ? "" : accessPassword.trim());
            spec.setPublicAccessUrl(settingsService.normalizePublicAccessUrl(publicAccessUrl));
            spec.setPublicLogoUrl(settingsService.normalizeLogoUrl(publicLogoUrl));
           
            if (publicSiteTitle != null) {
                spec.setPublicSiteTitle(settingsService.normalizePublicText(publicSiteTitle, 80));
            }
            if (publicBrandTitle != null) {
                spec.setPublicBrandTitle(settingsService.normalizePublicText(publicBrandTitle, 60));
            }
            if (publicBrandSubtitle != null) {
                spec.setPublicBrandSubtitle(settingsService.normalizePublicText(publicBrandSubtitle, 400));
            }
            spec.setPublicDomainWhitelist(settingsService.normalizeDomainWhitelist(publicDomainWhitelist));
            syncSiteRouterCache(spec);
        }
        if (notifyEnabled != null || notifyChannel != null || notifyWebhookUrl != null
            || notifyOrderNowEnabled != null || notifyOrderReservationEnabled != null) {
            spec.setNotifyEnabled(Boolean.TRUE.equals(notifyEnabled));
            spec.setNotifyChannel(notifyChannel == null ? "" : notifyChannel.trim());
            spec.setNotifyWebhookUrl(notifyWebhookUrl == null ? "" : notifyWebhookUrl.trim());
            spec.setNotifyOrderNowEnabled(notifyOrderNowEnabled == null || notifyOrderNowEnabled);
            spec.setNotifyOrderReservationEnabled(Boolean.TRUE.equals(notifyOrderReservationEnabled));
        }
        ext.setSpec(spec);
        settingsService.saveSettings(ext);
        return ApiPayloads.updatedOnly();
    }

    private void syncSiteRouterCache(DishesSettings.Spec spec) {
        SiteRouter.updateConfiguredPublicPath(spec.getPublicAccessUrl());
        SiteRouter.updateConfiguredPublicLogoUrl(spec.getPublicLogoUrl());
        SiteRouter.updateConfiguredPublicBranding(spec.getPublicSiteTitle(), spec.getPublicBrandTitle(), spec.getPublicBrandSubtitle());
    }
}
