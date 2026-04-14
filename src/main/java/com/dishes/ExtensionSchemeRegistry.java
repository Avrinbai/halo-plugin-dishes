package com.dishes;

import static run.halo.app.extension.index.IndexAttributeFactory.simpleAttribute;

import com.dishes.extension.Dish;
import com.dishes.extension.DishCategory;
import com.dishes.extension.DishesSettings;
import com.dishes.extension.MealOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import run.halo.app.extension.Extension;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.index.IndexSpec;

@Component
@SuppressWarnings("removal")
public class ExtensionSchemeRegistry {
    private static final Logger log = LoggerFactory.getLogger(ExtensionSchemeRegistry.class);

    private final SchemeManager schemeManager;

    public ExtensionSchemeRegistry(SchemeManager schemeManager) {
        this.schemeManager = schemeManager;
    }

    public synchronized void ensureRegistered() {
        ensureDishCategory();
        ensureDish();
        ensureMealOrder();
        ensureDishesSettings();
    }

    public synchronized void rebuildAll() {
        unregisterIfPresent(DishCategory.class);
        unregisterIfPresent(Dish.class);
        unregisterIfPresent(MealOrder.class);
        unregisterIfPresent(DishesSettings.class);
        ensureRegistered();
        log.warn("Rebuilt all extension schemes and indexes due to index inconsistency.");
    }

    private void ensureDishCategory() {
        if (hasScheme(DishCategory.class)) {
            return;
        }
        schemeManager.register(DishCategory.class, indexSpecs -> indexSpecs.add(new IndexSpec()
            .setName("spec.slug")
            .setIndexFunc(simpleAttribute(DishCategory.class, x ->
                x.getSpec() == null ? null : x.getSpec().getSlug()))
        ));
        log.info("Registered scheme for {}", DishCategory.class.getName());
    }

    private void ensureDish() {
        if (hasScheme(Dish.class)) {
            return;
        }
        schemeManager.register(Dish.class, indexSpecs -> indexSpecs.add(new IndexSpec()
            .setName("spec.categoryId")
            .setIndexFunc(simpleAttribute(Dish.class, x ->
                x.getSpec() == null || x.getSpec().getCategoryId() == null
                    ? null
                    : String.valueOf(x.getSpec().getCategoryId())))
        ));
        log.info("Registered scheme for {}", Dish.class.getName());
    }

    private void ensureMealOrder() {
        if (hasScheme(MealOrder.class)) {
            return;
        }
        schemeManager.register(MealOrder.class, indexSpecs -> {
            indexSpecs.add(new IndexSpec()
                .setName("spec.orderDate")
                .setIndexFunc(simpleAttribute(MealOrder.class, x ->
                    x.getSpec() == null ? null : x.getSpec().getOrderDate())));
            indexSpecs.add(new IndexSpec()
                .setName("spec.mealPeriodCode")
                .setIndexFunc(simpleAttribute(MealOrder.class, x ->
                    x.getSpec() == null ? null : x.getSpec().getMealPeriodCode())));
        });
        log.info("Registered scheme for {}", MealOrder.class.getName());
    }

    private void ensureDishesSettings() {
        if (hasScheme(DishesSettings.class)) {
            return;
        }
        schemeManager.register(DishesSettings.class);
        log.info("Registered scheme for {}", DishesSettings.class.getName());
    }

    private boolean hasScheme(Class<? extends Extension> type) {
        try {
            return schemeManager.get(type) != null;
        } catch (Exception ex) {
            return false;
        }
    }

    private void unregisterIfPresent(Class<? extends Extension> type) {
        try {
            Scheme scheme = schemeManager.get(type);
            if (scheme != null) {
                schemeManager.unregister(scheme);
                log.info("Unregistered scheme for {}", type.getName());
            }
        } catch (Exception ex) {
            log.debug("Skip unregister for {} due to: {}", type.getName(), ex.getMessage());
        }
    }
}
