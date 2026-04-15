package com.dishes.service.publics;

import com.dishes.api.BusinessErrorCode;
import com.dishes.api.BusinessException;
import com.dishes.service.DishesSettingsService;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;

@Service
public class PublicDomainWhitelistService {

    private final DishesSettingsService settingsService;

    public PublicDomainWhitelistService(DishesSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void ensureAllowed(ServerHttpRequest request) {
        var spec = settingsService.safeReadSpec();
        var whitelist = parseWhitelist(spec.getPublicDomainWhitelist());
        if (whitelist.isEmpty()) {
            return;
        }

        var originHost = extractHost(request.getHeaders().getFirst("Origin"));
        var refererHost = extractHost(request.getHeaders().getFirst("Referer"));
        var host = request.getHeaders().getFirst("Host");
        var hostOnly = host == null ? "" : host.toLowerCase(Locale.ROOT).split(":")[0].trim();

        if (matchesAny(originHost, whitelist) || matchesAny(refererHost, whitelist) || matchesAny(hostOnly, whitelist)) {
            return;
        }

        throw new BusinessException(
            BusinessErrorCode.ACCESS_DENIED,
            "请求来源域名不在白名单内"
        );
    }

    private Set<String> parseWhitelist(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        return raw
            .replace("\r\n", "\n")
            .replace(",", "\n")
            .lines()
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .map(this::normalizeHostRule)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toSet());
    }

    private String normalizeHostRule(String raw) {
        var s = raw.toLowerCase(Locale.ROOT).trim();
        if (s.startsWith("http://") || s.startsWith("https://")) {
            try {
                var uri = URI.create(s);
                s = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            } catch (Exception ignored) {
                return "";
            }
        }
        if (s.contains("/")) {
            s = s.substring(0, s.indexOf('/'));
        }
        if (s.contains(":")) {
            s = s.substring(0, s.indexOf(':'));
        }
        return s.trim();
    }

    private String extractHost(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) return "";
        try {
            var host = URI.create(sourceUrl).getHost();
            return host == null ? "" : host.toLowerCase(Locale.ROOT);
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean matchesAny(String host, Set<String> whitelist) {
        if (host == null || host.isBlank()) return false;
        var target = host.toLowerCase(Locale.ROOT).trim();
        for (var rule : whitelist) {
            if (rule.startsWith("*.")) {
                var suffix = rule.substring(1);
                if (target.endsWith(suffix) && target.length() > suffix.length()) {
                    return true;
                }
                continue;
            }
            if (target.equals(rule)) {
                return true;
            }
        }
        return false;
    }
}
