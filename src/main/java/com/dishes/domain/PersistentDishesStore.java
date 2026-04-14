package com.dishes.domain;

import com.dishes.ExtensionSchemeRegistry;
import com.dishes.api.BusinessErrorCode;
import com.dishes.api.BusinessException;
import com.dishes.domain.finder.PersistentDishesFinder;
import com.dishes.domain.service.PersistentDishesDomainService;
import com.dishes.extension.DishCategory;
import com.dishes.extension.MealOrder;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;

@Primary
@Component
public class PersistentDishesStore implements DishesStore {
    private final ReactiveExtensionClient client;
    private final ExtensionSchemeRegistry extensionSchemeRegistry;
    private final PersistentDishesFinder finder;
    private final PersistentDishesDomainService domainService;

    private static final List<MealPeriod> PERIODS = List.of(
        new MealPeriod(1, "breakfast", "早餐", 0),
        new MealPeriod(2, "lunch", "午餐", 1),
        new MealPeriod(3, "dinner", "晚餐", 2)
    );

    public PersistentDishesStore(ReactiveExtensionClient client, ExtensionSchemeRegistry extensionSchemeRegistry, PersistentDishesFinder finder, PersistentDishesDomainService domainService) {
        this.client = client;
        this.extensionSchemeRegistry = extensionSchemeRegistry;
        this.finder = finder;
        this.domainService = domainService;
    }

    @Override
    public List<MealPeriod> listMealPeriods() { return PERIODS; }

    @Override
    public MealPeriod resolveMealPeriodByCode(String code) {
        if (code == null) return null;
        for (var mp : PERIODS) if (mp.code().equalsIgnoreCase(code)) return mp;
        return null;
    }

    @Override
    public List<Category> listCategories() { return finder.listCategories(); }

    @Override
    public Category createCategory(String name, String slug, int sortOrder) {
        extensionSchemeRegistry.ensureRegistered();
        var n = name == null ? "" : name.trim();
        if (n.isBlank()) throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "分类名称不能为空");
        var normalizedSlug = domainService.normalizeSlug(slug);
        domainService.ensureCategorySlugUnique(normalizedSlug, null, listCategories());
        var id = finder.nextId("category");
        var c = new DishCategory();
        var meta = new Metadata();
        meta.setName(String.valueOf(id));
        c.setMetadata(meta);
        var spec = new DishCategory.Spec();
        spec.setName(n); spec.setSlug(normalizedSlug); spec.setSortOrder(sortOrder);
        c.setSpec(spec);
        if (withIndexRepair(() -> client.create(c).block()) == null) throw new IllegalStateException("create category failed");
        return new Category(id, n, normalizedSlug, sortOrder);
    }

    @Override
    public Category updateCategory(long id, String name, String slug, int sortOrder) {
        extensionSchemeRegistry.ensureRegistered();
        var c = finder.getCategoryExt(id);
        if (c == null) return null;
        var n = name == null ? "" : name.trim();
        if (n.isBlank()) throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "分类名称不能为空");
        var normalizedSlug = domainService.normalizeSlug(slug);
        domainService.ensureCategorySlugUnique(normalizedSlug, id, listCategories());
        c.getSpec().setName(n); c.getSpec().setSlug(normalizedSlug); c.getSpec().setSortOrder(sortOrder);
        if (withIndexRepair(() -> client.update(c).block()) == null) throw new IllegalStateException("update category failed");
        return new Category(id, n, normalizedSlug, sortOrder);
    }

    @Override
    public boolean deleteCategory(long id) {
        extensionSchemeRegistry.ensureRegistered();
        if (listDishes(null).stream().anyMatch(d -> d.categoryId() == id)) return false;
        var c = finder.getCategoryExt(id);
        return c != null && withIndexRepair(() -> client.delete(c).block()) != null;
    }

    @Override
    public List<Dish> listDishes(Long categoryId) { return finder.listDishes(categoryId); }

    @Override
    public Dish createDish(long categoryId, String name, String imageUrl, int recommendationLevel, String description, boolean isAvailable, int sortOrder, List<Long> mealPeriodIds) {
        extensionSchemeRegistry.ensureRegistered();
        if (finder.getCategoryExt(categoryId) == null) throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "invalid category_id");
        domainService.validateDishInput(name, imageUrl, recommendationLevel, mealPeriodIds);
        var id = finder.nextId("dish");
        var d = new com.dishes.extension.Dish();
        var meta = new Metadata();
        meta.setName(String.valueOf(id));
        d.setMetadata(meta);
        var spec = new com.dishes.extension.Dish.Spec();
        spec.setCategoryId(categoryId); spec.setName(name == null ? "" : name.trim());
        spec.setImageUrl(domainService.trimToNull(imageUrl)); spec.setRecommendationLevel(recommendationLevel);
        spec.setDescription(domainService.trimToNull(description)); spec.setIsAvailable(isAvailable);
        spec.setSortOrder(sortOrder); spec.setMealPeriodIds(mealPeriodIds); d.setSpec(spec);
        if (withIndexRepair(() -> client.create(d).block()) == null) throw new IllegalStateException("create dish failed");
        var catName = finder.categoryNameMap().getOrDefault(categoryId, "");
        return new Dish(id, categoryId, catName, name == null ? "" : name.trim(), domainService.trimToNull(imageUrl), recommendationLevel, domainService.trimToNull(description), isAvailable, sortOrder, List.copyOf(mealPeriodIds));
    }

    @Override
    public Dish updateDish(long id, long categoryId, String name, String imageUrl, int recommendationLevel, String description, boolean isAvailable, int sortOrder, List<Long> mealPeriodIds) {
        extensionSchemeRegistry.ensureRegistered();
        if (finder.getCategoryExt(categoryId) == null) throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "invalid category_id");
        domainService.validateDishInput(name, imageUrl, recommendationLevel, mealPeriodIds);
        var d = finder.getDishExt(id);
        if (d == null) return null;
        var spec = d.getSpec();
        spec.setCategoryId(categoryId); spec.setName(name == null ? "" : name.trim());
        spec.setImageUrl(domainService.trimToNull(imageUrl)); spec.setRecommendationLevel(recommendationLevel);
        spec.setDescription(domainService.trimToNull(description)); spec.setIsAvailable(isAvailable);
        spec.setSortOrder(sortOrder); spec.setMealPeriodIds(mealPeriodIds);
        if (withIndexRepair(() -> client.update(d).block()) == null) throw new IllegalStateException("update dish failed");
        var catName = finder.categoryNameMap().getOrDefault(categoryId, "");
        return new Dish(id, categoryId, catName, name == null ? "" : name.trim(), domainService.trimToNull(imageUrl), recommendationLevel, domainService.trimToNull(description), isAvailable, sortOrder, List.copyOf(mealPeriodIds));
    }

    @Override
    public boolean deleteDish(long id) {
        extensionSchemeRegistry.ensureRegistered();
        var d = finder.getDishExt(id);
        return d != null && withIndexRepair(() -> client.delete(d).block()) != null;
    }

    @Override
    public Order replaceOrder(LocalDate date, String periodCode, String remark, List<OrderItem> items) {
        extensionSchemeRegistry.ensureRegistered();
        if (resolveMealPeriodByCode(periodCode) == null) {
            throw new BusinessException(BusinessErrorCode.INVALID_MEAL_PERIOD_CODE, "unknown meal_period_code");
        }
        var existing = finder.findMealOrderByDateAndPeriod(date, periodCode);
        var now = OffsetDateTime.now().toString();
        MealOrder target;
        if (existing == null) {
            var id = finder.nextId("order");
            target = new MealOrder();
            var meta = new Metadata(); meta.setName(String.valueOf(id)); target.setMetadata(meta);
            var spec = new MealOrder.Spec();
            spec.setOrderDate(date.toString()); spec.setMealPeriodCode(periodCode); spec.setRemark(remark);
            spec.setCreatedAt(now); spec.setUpdatedAt(now); spec.setItems(toExtItems(items)); target.setSpec(spec);
            if (withIndexRepair(() -> client.create(target).block()) == null) throw new IllegalStateException("create order failed");
        } else {
            target = existing;
            target.getSpec().setRemark(remark); target.getSpec().setUpdatedAt(now); target.getSpec().setItems(toExtItems(items));
            if (withIndexRepair(() -> client.update(target).block()) == null) throw new IllegalStateException("update order failed");
        }
        return toOrder(target);
    }

    private <T> T withIndexRepair(Supplier<T> action) {
        try {
            return action.get();
        } catch (Throwable ex) {
            if (containsNoIndicesError(ex)) {
                extensionSchemeRegistry.rebuildAll();
                return action.get();
            }
            throw ex;
        }
    }

    private boolean containsNoIndicesError(Throwable throwable) {
        var cursor = throwable;
        while (cursor != null) {
            var msg = cursor.getMessage();
            if (msg != null && msg.contains("No indices found for type")) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private static List<MealOrder.Item> toExtItems(List<OrderItem> items) {
        if (items == null) return List.of();
        var out = new ArrayList<MealOrder.Item>();
        for (var it : items) { var x = new MealOrder.Item(); x.setDishId(it.dishId()); x.setQuantity(it.quantity()); x.setNote(it.note()); out.add(x); }
        return out;
    }

    private Order toOrder(MealOrder ext) {
        var mp = resolveMealPeriodByCode(ext.getSpec().getMealPeriodCode());
        var items = new ArrayList<OrderItem>();
        if (ext.getSpec().getItems() != null) for (var it : ext.getSpec().getItems()) items.add(new OrderItem(it.getDishId(), it.getQuantity() == null ? 1.0 : it.getQuantity(), it.getNote()));
        return new Order(Long.parseLong(ext.getMetadata().getName()), LocalDate.parse(ext.getSpec().getOrderDate()), mp, ext.getSpec().getRemark(), items, items.size(), ext.getSpec().getCreatedAt(), ext.getSpec().getUpdatedAt());
    }

    @Override
    public List<Map<String, Object>> dayOverview(LocalDate date) {
        var out = new ArrayList<Map<String, Object>>();
        for (var mp : PERIODS) {
            var existing = finder.findMealOrderByDateAndPeriod(date, mp.code());
            var row = new LinkedHashMap<String, Object>();
            row.put("meal_period", Map.of("id", mp.id(), "code", mp.code(), "name", mp.name(), "sort_order", mp.sortOrder()));
            row.put("order", existing == null ? null : orderToApi(toOrder(existing)));
            out.add(row);
        }
        return out;
    }

    @Override
    public Map<String, Object> orderToApi(Order o) {
        var dishById = new HashMap<Long, DishesStore.Dish>();
        for (var d : listDishes(null)) dishById.put(d.id(), d);
        var items = new ArrayList<Map<String, Object>>();
        long lineId = 1;
        for (var it : o.items()) {
            var d = dishById.get(it.dishId());
            if (d == null) continue;
            var dish = new LinkedHashMap<String, Object>();
            dish.put("name", d.name());
            dish.put("image_url", d.imageUrl());
            dish.put("category_id", d.categoryId());
            dish.put("category_name", d.categoryName());
            var item = new LinkedHashMap<String, Object>();
            item.put("line_id", lineId++);
            item.put("dish_id", it.dishId());
            item.put("quantity", it.quantity());
            item.put("note", it.note());
            item.put("dish", dish);
            items.add(item);
        }
        var out = new LinkedHashMap<String, Object>();
        out.put("id", o.id());
        out.put("remark", o.remark());
        out.put("items", items);
        out.put("item_count", items.size());
        out.put("created_at", o.createdAt());
        out.put("updated_at", o.updatedAt());
        return out;
    }

    @Override
    public Map<String, Object> history(LocalDate from, LocalDate to, int limit, int offset) {
        var rows = new ArrayList<Map<String, Object>>();
        for (var ext : finder.listMealOrders()) {
            var d = LocalDate.parse(ext.getSpec().getOrderDate());
            if (d.isBefore(from) || d.isAfter(to)) continue;
            var o = toOrder(ext);
            rows.add(Map.of("order_date", d.toString(), "meal_period", Map.of("id", o.mealPeriod().id(), "code", o.mealPeriod().code(), "name", o.mealPeriod().name()), "order", orderToApi(o)));
        }
        rows.sort((a, b) -> ((String) b.get("order_date")).compareTo((String) a.get("order_date")));
        var total = rows.size(); var start = Math.min(Math.max(offset, 0), total); var end = Math.min(start + Math.max(limit, 0), total);
        return Map.of("items", rows.subList(start, end), "total", total, "limit", limit, "offset", offset);
    }

    @Override
    public List<Map<String, Object>> randomByPeriod(String periodCode, int count, Set<Long> excludeDishIds) {
        var mp = resolveMealPeriodByCode(periodCode);
        if (mp == null) throw new BusinessException(BusinessErrorCode.INVALID_MEAL_PERIOD_CODE, "unknown meal_period_code");
        var pool = new ArrayList<DishesStore.Dish>();
        for (var d : listDishes(null)) {
            if (!d.isAvailable()) continue;
            if (excludeDishIds != null && excludeDishIds.contains(d.id())) continue;
            if (d.mealPeriodIds() != null && d.mealPeriodIds().contains(mp.id())) pool.add(d);
        }
        java.util.Collections.shuffle(pool, ThreadLocalRandom.current());
        var out = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < Math.min(count, pool.size()); i++) {
            var d = pool.get(i);
            var row = new LinkedHashMap<String, Object>();
            row.put("dish_id", d.id());
            row.put("name", d.name());
            row.put("category_name", d.categoryName());
            row.put("image_url", d.imageUrl());
            row.put("recommendation_level", d.recommendationLevel());
            out.add(row);
        }
        return out;
    }

    @Override
    public Map<String, Object> listOrdersSummary(LocalDate from, LocalDate to, int limit, int offset, String periodCode) {
        var rows = new ArrayList<Map<String, Object>>();
        for (var ext : finder.listMealOrders()) {
            var d = LocalDate.parse(ext.getSpec().getOrderDate());
            if (d.isBefore(from) || d.isAfter(to)) continue;
            if (periodCode != null && !periodCode.isBlank() && !periodCode.equalsIgnoreCase(ext.getSpec().getMealPeriodCode())) continue;
            var mp = resolveMealPeriodByCode(ext.getSpec().getMealPeriodCode());
            rows.add(Map.of("id", Long.parseLong(ext.getMetadata().getName()), "orderDate", d.toString(), "mealPeriodCode", ext.getSpec().getMealPeriodCode(), "mealPeriodName", mp == null ? ext.getSpec().getMealPeriodCode() : mp.name(), "itemCount", ext.getSpec().getItems() == null ? 0 : ext.getSpec().getItems().size()));
        }
        rows.sort((a, b) -> ((String) b.get("orderDate")).compareTo((String) a.get("orderDate")));
        var total = rows.size(); var start = Math.min(Math.max(offset, 0), total); var end = Math.min(start + Math.max(limit, 0), total);
        return Map.of("items", rows.subList(start, end), "total", total, "limit", limit, "offset", offset);
    }

    @Override
    public Order findOrderById(long id) {
        var ext = finder.getMealOrderExt(String.valueOf(id));
        return ext == null ? null : toOrder(ext);
    }
}
