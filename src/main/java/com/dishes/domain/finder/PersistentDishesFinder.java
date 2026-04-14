package com.dishes.domain.finder;

import com.dishes.ExtensionSchemeRegistry;
import com.dishes.api.BusinessErrorCode;
import com.dishes.api.BusinessException;
import com.dishes.domain.DishesStore;
import com.dishes.extension.Dish;
import com.dishes.extension.DishCategory;
import com.dishes.extension.MealOrder;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.ListOptions;
import run.halo.app.extension.ReactiveExtensionClient;

@Component
public class PersistentDishesFinder {
    private final ReactiveExtensionClient client;
    private final ExtensionSchemeRegistry extensionSchemeRegistry;

    public PersistentDishesFinder(ReactiveExtensionClient client, ExtensionSchemeRegistry extensionSchemeRegistry) {
        this.client = client;
        this.extensionSchemeRegistry = extensionSchemeRegistry;
    }

    public long nextId(String scope) {
        return switch (scope) {
            case "category" -> nextIdFromExisting(DishCategory.class);
            case "dish" -> nextIdFromExisting(Dish.class);
            case "order" -> nextIdFromExisting(MealOrder.class);
            default -> throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "Unknown id scope: " + scope);
        };
    }

    public List<DishesStore.Category> listCategories() {
        extensionSchemeRegistry.ensureRegistered();
        var list = listAll(DishCategory.class);
        var out = new ArrayList<DishesStore.Category>();
        for (var c : list) {
            out.add(new DishesStore.Category(
                Long.parseLong(c.getMetadata().getName()),
                c.getSpec().getName(),
                c.getSpec().getSlug(),
                c.getSpec().getSortOrder() == null ? 0 : c.getSpec().getSortOrder()
            ));
        }
        out.sort(Comparator.comparingInt(DishesStore.Category::sortOrder).thenComparingLong(DishesStore.Category::id));
        return out;
    }

    public DishCategory getCategoryExt(long id) {
        extensionSchemeRegistry.ensureRegistered();
        return client.fetch(DishCategory.class, String.valueOf(id)).block();
    }

    public Map<Long, String> categoryNameMap() {
        var map = new HashMap<Long, String>();
        for (var c : listCategories()) map.put(c.id(), c.name());
        return map;
    }

    public List<DishesStore.Dish> listDishes(Long categoryId) {
        extensionSchemeRegistry.ensureRegistered();
        var list = listAll(Dish.class);
        var catNames = categoryNameMap();
        var out = new ArrayList<DishesStore.Dish>();
        for (var d : list) {
            var spec = d.getSpec();
            var cid = spec.getCategoryId();
            if (categoryId != null && !Objects.equals(categoryId, cid)) continue;
            out.add(new DishesStore.Dish(
                Long.parseLong(d.getMetadata().getName()),
                cid,
                catNames.getOrDefault(cid, ""),
                spec.getName(),
                spec.getImageUrl(),
                spec.getRecommendationLevel() == null ? 3 : spec.getRecommendationLevel(),
                spec.getDescription(),
                spec.getIsAvailable() == null || spec.getIsAvailable(),
                spec.getSortOrder() == null ? 0 : spec.getSortOrder(),
                spec.getMealPeriodIds() == null ? List.of() : List.copyOf(spec.getMealPeriodIds())
            ));
        }
        out.sort(Comparator.comparingInt(DishesStore.Dish::sortOrder).thenComparingLong(DishesStore.Dish::id));
        return out;
    }

    public Dish getDishExt(long id) {
        extensionSchemeRegistry.ensureRegistered();
        return client.fetch(Dish.class, String.valueOf(id)).block();
    }

    public MealOrder getMealOrderExt(String name) {
        extensionSchemeRegistry.ensureRegistered();
        return client.fetch(MealOrder.class, name).block();
    }

    public List<MealOrder> listMealOrders() {
        extensionSchemeRegistry.ensureRegistered();
        return listAll(MealOrder.class);
    }

    public MealOrder findMealOrderByDateAndPeriod(LocalDate date, String periodCode) {
        extensionSchemeRegistry.ensureRegistered();
        for (var ext : listMealOrders()) {
            if (ext.getSpec() == null) continue;
            if (!Objects.equals(ext.getSpec().getOrderDate(), date.toString())) continue;
            if (!Objects.equals(ext.getSpec().getMealPeriodCode(), periodCode)) continue;
            return ext;
        }
        return null;
    }

    private <T extends AbstractExtension> long nextIdFromExisting(Class<T> type) {
        extensionSchemeRegistry.ensureRegistered();
        var list = listAll(type);
        long max = 0L;
        for (var ext : list) {
            var meta = ext.getMetadata();
            if (meta == null || meta.getName() == null) continue;
            try {
                max = Math.max(max, Long.parseLong(meta.getName()));
            } catch (NumberFormatException ignored) {
            }
        }
        return max + 1L;
    }

    private <T extends AbstractExtension> List<T> listAll(Class<T> type) {
        return client.listAll(type, ListOptions.builder().build(), Sort.by("metadata.name"))
            .collectList()
            .blockOptional()
            .orElseGet(List::of);
    }
}
