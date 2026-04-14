package com.dishes.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DishesStore {

    record MealPeriod(long id, String code, String name, int sortOrder) {}

    record Category(long id, String name, String slug, int sortOrder) {}

    record Dish(
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

    record OrderItem(long dishId, double quantity, String note) {}

    record Order(
        long id,
        LocalDate orderDate,
        MealPeriod mealPeriod,
        String remark,
        List<OrderItem> items,
        int itemCount,
        String createdAt,
        String updatedAt
    ) {}

    List<MealPeriod> listMealPeriods();
    MealPeriod resolveMealPeriodByCode(String code);

    List<Category> listCategories();
    Category createCategory(String name, String slug, int sortOrder);
    Category updateCategory(long id, String name, String slug, int sortOrder);
    boolean deleteCategory(long id);

    List<Dish> listDishes(Long categoryId);
    Dish createDish(long categoryId, String name, String imageUrl, int recommendationLevel, String description,
                    boolean isAvailable, int sortOrder, List<Long> mealPeriodIds);
    Dish updateDish(long id, long categoryId, String name, String imageUrl, int recommendationLevel, String description,
                    boolean isAvailable, int sortOrder, List<Long> mealPeriodIds);
    boolean deleteDish(long id);

    Order replaceOrder(LocalDate date, String periodCode, String remark, List<OrderItem> items);

    List<Map<String, Object>> dayOverview(LocalDate date);
    Map<String, Object> orderToApi(Order o);

    Map<String, Object> history(LocalDate from, LocalDate to, int limit, int offset);

    List<Map<String, Object>> randomByPeriod(String periodCode, int count, Set<Long> excludeDishIds);

    Map<String, Object> listOrdersSummary(LocalDate from, LocalDate to, int limit, int offset, String periodCode);
    Order findOrderById(long id);
}


