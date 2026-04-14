package com.dishes.service.publics;

import com.dishes.api.BusinessErrorCode;
import com.dishes.api.BusinessException;
import com.dishes.service.DishesSettingsService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PublicAccessService {

    private final DishesSettingsService settingsService;

    public PublicAccessService(DishesSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void ensureAccess(String token) {
        var settings = settingsService.getOrInitSettings().getSpec();
        var mode = settingsService.normalizeAccessMode(settings.getAccessMode());
        if ("none".equals(mode)) return;
        if ("password".equals(mode) && verifyPasswordToken(token, settings.getAccessPassword())) return;
        throw new BusinessException(BusinessErrorCode.ACCESS_DENIED, "需要密码验证");
    }

    public Map<String, Object> accessStatus(String token) {
        var settings = settingsService.getOrInitSettings().getSpec();
        var mode = settingsService.normalizeAccessMode(settings.getAccessMode());
        var granted = switch (mode) {
            case "none" -> true;
            case "password" -> verifyPasswordToken(token, settings.getAccessPassword());
            default -> true;
        };
        return Map.of("access_mode", mode, "granted", granted, "password_required", "password".equals(mode));
    }

    public Map<String, Object> verifyPassword(String password) {
        var settings = settingsService.getOrInitSettings().getSpec();
        var mode = settingsService.normalizeAccessMode(settings.getAccessMode());
        if (!"password".equals(mode)) {
            return Map.of("granted", true, "token", "");
        }
        var actual = settings.getAccessPassword() == null ? "" : settings.getAccessPassword();
        var input = password == null ? "" : password;
        if (!Objects.equals(actual, input)) throw new BusinessException(BusinessErrorCode.PASSWORD_INVALID, "密码错误");
        return Map.of("granted", true, "token", passwordToken(actual));
    }

    private boolean verifyPasswordToken(String token, String password) {
        var actual = passwordToken(password == null ? "" : password);
        return token != null && token.equals(actual);
    }

    private String passwordToken(String password) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            var bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            var sb = new StringBuilder();
            for (var b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
