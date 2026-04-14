package com.dishes.domain;

import com.dishes.api.BusinessErrorCode;
import com.dishes.api.BusinessException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class InMemoryDishesStore {

    public record MealPeriod(long id, String code, String name, int sortOrder) {}

    public record Category(long id, String name, String slug, int sortOrder) {}

    public record Dish(
        long id,
        long categoryId,
        String categoryName,
        String name,
        String imageUrl,
        int recommendationLevel,
        String description,
        boolean isAvailable,
        int sortOrder,
        List<Long> mealPeriodIds
    ) {}

    public record OrderItem(long dishId, double quantity, String note) {}

    public record Order(
        long id,
        LocalDate orderDate,
        MealPeriod mealPeriod,
        String remark,
        List<OrderItem> items,
        int itemCount,
        String createdAt,
        String updatedAt
    ) {}

    private final AtomicLong idSeq = new AtomicLong(1);

    private final CopyOnWriteArrayList<MealPeriod> mealPeriods = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Category> categories = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Dish> dishes = new CopyOnWriteArrayList<>();

    // key = date + "::" + mealPeriod.code
    private final Map<String, Order> ordersByKey = new ConcurrentHashMap<>();

    public InMemoryDishesStore() {
        mealPeriods.add(new MealPeriod(1, "breakfast", "早餐", 0));
        mealPeriods.add(new MealPeriod(2, "lunch", "午餐", 1));
        mealPeriods.add(new MealPeriod(3, "dinner", "晚餐", 2));

        var catId = idSeq.getAndIncrement();
        categories.add(new Category(catId, "默认分类", "default", 0));
        dishes.add(new Dish(
            idSeq.getAndIncrement(),
            catId,
            "默认分类",
            "示例菜品",
            null,
            3,
            null,
            true,
            0,
            List.of(2L, 3L)
        ));
    }

    public List<MealPeriod> listMealPeriods() {
        return List.copyOf(mealPeriods);
    }

    public MealPeriod resolveMealPeriodByCode(String code) {
        for (var mp : mealPeriods) {
            if (mp.code().equalsIgnoreCase(code)) return mp;
        }
        return null;
    }

    public List<Category> listCategories() {
        return List.copyOf(categories);
    }

    public Category createCategory(String name, String slug, int sortOrder) {
        var id = idSeq.getAndIncrement();
        var c = new Category(id, name, slug, sortOrder);
        categories.add(c);
        return c;
    }

    public Category updateCategory(long id, String name, String slug, int sortOrder) {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).id() == id) {
                var c = new Category(id, name, slug, sortOrder);
                categories.set(i, c);
              
                for (int j = 0; j < dishes.size(); j++) {
                    var d = dishes.get(j);
                    if (d.categoryId() == id) {
                        dishes.set(j, new Dish(
                            d.id(),
                            d.categoryId(),
                            c.name(),
                            d.name(),
                            d.imageUrl(),
                            d.recommendationLevel(),
                            d.description(),
                            d.isAvailable(),
                            d.sortOrder(),
                            d.mealPeriodIds()
                        ));
                    }
                }
                return c;
            }
        }
        return null;
    }

    public boolean deleteCategory(long id) {
      
        for (var d : dishes) {
            if (d.categoryId() == id) return false;
        }
        return categories.removeIf(c -> c.id() == id);
    }

    public List<Dish> listDishes(Long categoryId) {
        if (categoryId == null) return List.copyOf(dishes);
        var out = new ArrayList<Dish>();
        for (var d : dishes) {
            if (d.categoryId() == categoryId) out.add(d);
        }
        return out;
    }

    public Dish createDish(
        long categoryId,
        String name,
        String imageUrl,
        int recommendationLevel,
        String description,
        boolean isAvailable,
        int sortOrder,
        List<Long> mealPeriodIds
    ) {
        var cat = categories.stream().filter(c -> c.id() == categoryId).findFirst().orElse(null);
        if (cat == null) throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "invalid category_id");
        var id = idSeq.getAndIncrement();
        var dish = new Dish(
            id,
            categoryId,
            cat.name(),
            name,
            imageUrl,
            recommendationLevel,
            description,
            isAvailable,
            sortOrder,
            mealPeriodIds == null ? List.of() : List.copyOf(mealPeriodIds)
        );
        dishes.add(dish);
        return dish;
    }

    public Dish updateDish(
        long id,
        long categoryId,
        String name,
        String imageUrl,
        int recommendationLevel,
        String description,
        boolean isAvailable,
        int sortOrder,
        List<Long> mealPeriodIds
    ) {
        var cat = categories.stream().filter(c -> c.id() == categoryId).findFirst().orElse(null);
        if (cat == null) throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "invalid category_id");
        for (int i = 0; i < dishes.size(); i++) {
            if (dishes.get(i).id() == id) {
                var dish = new Dish(
                    id,
                    categoryId,
                    cat.name(),
                    name,
                    imageUrl,
                    recommendationLevel,
                    description,
                    isAvailable,
                    sortOrder,
                    mealPeriodIds == null ? List.of() : List.copyOf(mealPeriodIds)
                );
                dishes.set(i, dish);
                return dish;
            }
        }
        return null;
    }

    public boolean deleteDish(long id) {
        return dishes.removeIf(d -> d.id() == id);
    }

    private static String nowIso() {
        return java.time.OffsetDateTime.now().toString();
    }

    private static String key(LocalDate date, String periodCode) {
        return date + "::" + periodCode.toLowerCase(Locale.ROOT);
    }

    public Order replaceOrder(
        LocalDate date,
        String periodCode,
        String remark,
        List<OrderItem> items
    ) {
        var mp = resolveMealPeriodByCode(periodCode);
        if (mp == null) throw new BusinessException(BusinessErrorCode.INVALID_MEAL_PERIOD_CODE, "unknown meal_period_code");
        var k = key(date, periodCode);
        var prev = ordersByKey.get(k);
        var id = prev != null ? prev.id() : idSeq.getAndIncrement();
        var createdAt = prev != null ? prev.createdAt() : nowIso();
        var updatedAt = nowIso();
        var itemCount = items == null ? 0 : items.size();
        var o = new Order(
            id,
            date,
            mp,
            remark,
            items == null ? List.of() : List.copyOf(items),
            itemCount,
            createdAt,
            updatedAt
        );
        ordersByKey.put(k, o);
        return o;
    }

    public List<Map<String, Object>> dayOverview(LocalDate date) {
        var out = new ArrayList<Map<String, Object>>();
        for (var mp : mealPeriods) {
            var o = ordersByKey.get(key(date, mp.code()));
            var row = new LinkedHashMap<String, Object>();
            row.put(
                "meal_period",
                Map.of(
                    "id", mp.id(),
                    "code", mp.code(),
                    "name", mp.name(),
                    "sort_order", mp.sortOrder()));
            row.put("order", o == null ? null : orderToApi(o));
            out.add(row);
        }
        return out;
    }

    public Map<String, Object> orderToApi(Order o) {
        
        var dishById = new HashMap<Long, Dish>();
        for (var d : dishes) dishById.put(d.id(), d);

        var items = new ArrayList<Map<String, Object>>();
        long lineId = 1;
        for (var it : o.items()) {
            var d = dishById.get(it.dishId());
            if (d == null) continue;
            var dishMap = new LinkedHashMap<String, Object>();
            dishMap.put("name", d.name());
            dishMap.put("image_url", d.imageUrl());
            dishMap.put("category_id", d.categoryId());
            dishMap.put("category_name", d.categoryName());
            var lineMap = new LinkedHashMap<String, Object>();
            lineMap.put("line_id", lineId++);
            lineMap.put("dish_id", it.dishId());
            lineMap.put("quantity", it.quantity());
            lineMap.put("note", it.note());
            lineMap.put("dish", dishMap);
            items.add(lineMap);
        }

        var orderMap = new LinkedHashMap<String, Object>();
        orderMap.put("id", o.id());
        orderMap.put("remark", o.remark());
        orderMap.put("items", items);
        orderMap.put("item_count", items.size());
        orderMap.put("created_at", o.createdAt());
        orderMap.put("updated_at", o.updatedAt());
        return orderMap;
    }

    public Order findOrderById(long id) {
        for (var o : ordersByKey.values()) {
            if (o.id() == id) return o;
        }
        return null;
    }

    public Map<String, Object> history(LocalDate from, LocalDate to, int limit, int offset) {
        var all = new ArrayList<Map<String, Object>>();
        for (var entry : ordersByKey.values()) {
            var d = entry.orderDate();
            if (d.isBefore(from) || d.isAfter(to)) continue;
            all.add(Map.of(
                "order_date", d.toString(),
                "meal_period", Map.of(
                    "id", entry.mealPeriod().id(),
                    "code", entry.mealPeriod().code(),
                    "name", entry.mealPeriod().name()
                ),
                "order", orderToApi(entry)
            ));
        }
        all.sort((a, b) -> {
            var da = (String) a.get("order_date");
            var db = (String) b.get("order_date");
            var cmp = db.compareTo(da);
            if (cmp != 0) return cmp;
            var pa = (Map<String, Object>) a.get("meal_period");
            var pb = (Map<String, Object>) b.get("meal_period");
            return ((String) pb.get("code")).compareTo((String) pa.get("code"));
        });

        var total = all.size();
        var start = Math.min(Math.max(offset, 0), total);
        var end = Math.min(start + Math.max(limit, 0), total);
        var page = all.subList(start, end);
        return Map.of(
            "items", page,
            "total", total,
            "limit", limit,
            "offset", offset
        );
    }

    public List<Map<String, Object>> listOrdersSummary() {
        var out = new ArrayList<Map<String, Object>>();
        for (var o : ordersByKey.values()) {
            out.add(Map.of(
                "id", o.id(),
                "orderDate", o.orderDate().toString(),
                "mealPeriodCode", o.mealPeriod().code(),
                "itemCount", o.itemCount()
            ));
        }
        out.sort((a, b) -> ((String) b.get("orderDate")).compareTo((String) a.get("orderDate")));
        return out;
    }

    public Map<String, Object> listOrdersSummary(LocalDate from, LocalDate to, int limit, int offset) {
        var all = new ArrayList<Map<String, Object>>();
        for (var o : ordersByKey.values()) {
            var d = o.orderDate();
            if (d.isBefore(from) || d.isAfter(to)) continue;
            all.add(Map.of(
                "id", o.id(),
                "orderDate", d.toString(),
                "mealPeriodCode", o.mealPeriod().code(),
                "mealPeriodName", o.mealPeriod().name(),
                "itemCount", o.itemCount()
            ));
        }
        all.sort((a, b) -> {
            var da = (String) a.get("orderDate");
            var db = (String) b.get("orderDate");
            var cmp = db.compareTo(da);
            if (cmp != 0) return cmp;
            return ((String) a.get("mealPeriodCode")).compareTo((String) b.get("mealPeriodCode"));
        });

        var total = all.size();
        var start = Math.min(Math.max(offset, 0), total);
        var end = Math.min(start + Math.max(limit, 0), total);
        var page = all.subList(start, end);
        return Map.of(
            "items", page,
            "total", total,
            "limit", limit,
            "offset", offset
        );
    }

    public List<Map<String, Object>> randomByPeriod(String periodCode, int count, Set<Long> excludeDishIds) {
        var mp = resolveMealPeriodByCode(periodCode);
        if (mp == null) throw new BusinessException(BusinessErrorCode.INVALID_MEAL_PERIOD_CODE, "unknown meal_period_code");
        var pool = new ArrayList<Dish>();
        for (var d : dishes) {
            if (!d.isAvailable()) continue;
            if (excludeDishIds != null && excludeDishIds.contains(d.id())) continue;
            if (d.mealPeriodIds() != null && d.mealPeriodIds().contains(mp.id())) {
                pool.add(d);
            }
        }
        Collections.shuffle(pool, ThreadLocalRandom.current());
        var out = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < Math.min(count, pool.size()); i++) {
            var d = pool.get(i);
            var m = new LinkedHashMap<String, Object>();
            m.put("dish_id", d.id());
            m.put("name", d.name());
            m.put("category_name", d.categoryName());
            m.put("image_url", d.imageUrl());
            m.put("recommendation_level", d.recommendationLevel());
            out.add(m);
        }
        return out;
    }
}


