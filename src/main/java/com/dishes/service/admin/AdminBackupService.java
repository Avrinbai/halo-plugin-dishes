package com.dishes.service.admin;

import com.dishes.ExtensionSchemeRegistry;
import com.dishes.api.BusinessErrorCode;
import com.dishes.api.BusinessException;
import com.dishes.domain.DishesStore;
import com.dishes.domain.finder.PersistentDishesFinder;
import com.dishes.extension.MealOrder;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;
import run.halo.app.extension.Extension;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;

/**
 * 覆盖式导入：先删除订单、菜品、分类，再按备份重建。导入过程串行化，避免并发破坏扩展索引。
 */
@Service
public class AdminBackupService {

    public static final String MANIFEST_ENTRY = "manifest.json";
    public static final String FORMAT_KEY = "halo-plugin-dishes-backup";
    public static final int FORMAT_VERSION = 1;

    private static final long MAX_IMPORT_ZIP_BYTES = 52_428_800L;

    private static final long PURGE_INDEX_PAUSE_MS = 120L;

    private static final int PURGE_ROUNDS_PER_PHASE = 100;

    private final DishesStore store;
    private final PersistentDishesFinder finder;
    private final ReactiveExtensionClient client;
    private final ExtensionSchemeRegistry extensionSchemeRegistry;
 
    private final ObjectMapper objectMapper = createBackupObjectMapper();

    public AdminBackupService(
        DishesStore store,
        PersistentDishesFinder finder,
        ReactiveExtensionClient client,
        ExtensionSchemeRegistry extensionSchemeRegistry
    ) {
        this.store = store;
        this.finder = finder;
        this.client = client;
        this.extensionSchemeRegistry = extensionSchemeRegistry;
    }

    private static ObjectMapper createBackupObjectMapper() {
        var om = new ObjectMapper();
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return om;
    }

    public byte[] exportZip(boolean includeOrders) {
        extensionSchemeRegistry.ensureRegistered();
        var cats = store.listCategories();
        var dishes = store.listDishes(null);
        var manifest = new LinkedHashMap<String, Object>();
        manifest.put("format", FORMAT_KEY);
        manifest.put("version", FORMAT_VERSION);
        manifest.put("exported_at", OffsetDateTime.now().toString());
        manifest.put("include_orders", includeOrders);
        manifest.put("categories", categoriesPayload(cats));
        manifest.put("dishes", dishesPayload(dishes));
        if (includeOrders) {
            manifest.put("orders", ordersPayload(finder.listMealOrders()));
        } else {
            manifest.put("orders", List.of());
        }
        byte[] json;
        try {
            json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest);
        } catch (Exception e) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "序列化备份失败");
        }
        try (var bos = new ByteArrayOutputStream(json.length + 512);
             var zos = new ZipOutputStream(bos)) {
            zos.putNextEntry(new ZipEntry(MANIFEST_ENTRY));
            zos.write(json);
            zos.closeEntry();
            zos.finish();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "打包 ZIP 失败");
        }
    }

    private List<Map<String, Object>> categoriesPayload(List<DishesStore.Category> cats) {
        var out = new ArrayList<Map<String, Object>>();
        for (var c : cats) {
            var row = new LinkedHashMap<String, Object>();
            row.put("legacy_id", c.id());
            row.put("name", c.name());
            row.put("slug", c.slug());
            row.put("sort_order", c.sortOrder());
            out.add(row);
        }
        return out;
    }

    /** 菜品图片仅写入 {@code image_url}（站内路径或完整 URL），不抓取或内联二进制。 */
    private List<Map<String, Object>> dishesPayload(List<DishesStore.Dish> dishes) {
        var out = new ArrayList<Map<String, Object>>();
        for (var d : dishes) {
            var row = new LinkedHashMap<String, Object>();
            row.put("legacy_id", d.id());
            row.put("category_id", d.categoryId());
            row.put("name", d.name());
            row.put("image_url", d.imageUrl());
            row.put("recommendation_level", d.recommendationLevel());
            row.put("description", d.description());
            row.put("is_available", d.isAvailable());
            row.put("sort_order", d.sortOrder());
            row.put("meal_period_ids", d.mealPeriodIds());
            out.add(row);
        }
        return out;
    }

    private List<Map<String, Object>> ordersPayload(List<MealOrder> list) {
        var sorted = new ArrayList<>(list);
        sorted.sort(
            Comparator.comparing((MealOrder m) -> m.getSpec().getOrderDate())
                .thenComparing(m -> m.getSpec().getMealPeriodCode())
        );
        var out = new ArrayList<Map<String, Object>>();
        for (var ext : sorted) {
            var spec = ext.getSpec();
            var row = new LinkedHashMap<String, Object>();
            row.put("legacy_id", Long.parseLong(ext.getMetadata().getName()));
            row.put("order_date", spec.getOrderDate());
            row.put("meal_period_code", spec.getMealPeriodCode());
            row.put("remark", spec.getRemark());
            row.put("created_at", spec.getCreatedAt());
            row.put("updated_at", spec.getUpdatedAt());
            var items = new ArrayList<Map<String, Object>>();
            if (spec.getItems() != null) {
                for (var it : spec.getItems()) {
                    var li = new LinkedHashMap<String, Object>();
                    li.put("dish_id", it.getDishId());
                    li.put("quantity", it.getQuantity());
                    li.put("note", it.getNote());
                    items.add(li);
                }
            }
            row.put("items", items);
            out.add(row);
        }
        return out;
    }

    public synchronized Map<String, Object> importZip(byte[] zipBytes) {
        if (zipBytes == null || zipBytes.length == 0) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "空文件");
        }
        if (zipBytes.length > MAX_IMPORT_ZIP_BYTES) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "ZIP 过大（上限约 50MB）");
        }
        if (zipBytes.length < 4 || zipBytes[0] != 'P' || zipBytes[1] != 'K') {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "不是有效的 ZIP 文件");
        }
        byte[] manifestBytes = extractManifest(zipBytes);
        if (manifestBytes == null || manifestBytes.length == 0) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "ZIP 内缺少 manifest.json");
        }
        Map<String, Object> root;
        try {
            @SuppressWarnings("unchecked")
            var m = objectMapper.readValue(manifestBytes, Map.class);
            root = m;
        } catch (Exception e) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "manifest.json 解析失败");
        }
        if (!FORMAT_KEY.equals(String.valueOf(root.getOrDefault("format", "")))) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "备份格式不匹配");
        }
        var ver = asInt(root.get("version"), -1);
        if (ver != FORMAT_VERSION) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "不支持的备份版本: " + ver);
        }
        List<Map<String, Object>> catList = castMapObjectList(root.get("categories"));
        List<Map<String, Object>> dishList = castMapObjectList(root.get("dishes"));
        List<Map<String, Object>> orderList = castMapObjectList(root.get("orders"));
        validateManifest(catList, dishList, orderList);

        extensionSchemeRegistry.ensureRegistered();
        wipeAll();

        var catOldToNew = new HashMap<Long, Long>();
        List<Map<String, Object>> sortedCats = new ArrayList<>(catList);
        sortedCats.sort(
            Comparator.<Map<String, Object>>comparingInt(c -> asInt(c.get("sort_order"), 0))
                .thenComparingLong(c -> asLong(c.get("legacy_id"), 0))
        );
        for (var row : sortedCats) {
            var legacyId = asLong(row.get("legacy_id"), -1);
            var name = stringOrEmpty(row.get("name"));
            var slug = stringOrEmpty(row.get("slug"));
            var sortOrder = asInt(row.get("sort_order"), 0);
            var created = store.createCategory(name, slug, sortOrder);
            catOldToNew.put(legacyId, created.id());
        }

        var dishOldToNew = new HashMap<Long, Long>();
        List<Map<String, Object>> sortedDishes = new ArrayList<>(dishList);
        sortedDishes.sort(
            Comparator.<Map<String, Object>>comparingInt(d -> asInt(d.get("sort_order"), 0))
                .thenComparingLong(d -> asLong(d.get("legacy_id"), 0))
        );
        for (var row : sortedDishes) {
            var legacyId = asLong(row.get("legacy_id"), -1);
            var oldCat = asLong(row.get("category_id"), -1);
            var newCat = catOldToNew.get(oldCat);
            if (newCat == null) {
                throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "菜品引用了不存在的分类 ID: " + oldCat);
            }
            var name = stringOrEmpty(row.get("name"));
            var imageUrl = resolveImportImageUrl(row);
            var rec = asInt(row.get("recommendation_level"), 3);
            var desc = row.get("description") == null ? null : Objects.toString(row.get("description"), null);
            var available = parseBooleanFlag(row.get("is_available"), true);
            var sortOrder = asInt(row.get("sort_order"), 0);
            var mealIds = parseMealPeriodIds(row.get("meal_period_ids"));
            var created = store.createDish(newCat, name, imageUrl, rec, desc == null ? null : desc, available, sortOrder, mealIds);
            dishOldToNew.put(legacyId, created.id());
        }

        int importedOrders = 0;
        if (!orderList.isEmpty()) {
            var sortedOrders = new ArrayList<>(orderList);
            sortedOrders.sort(
                Comparator.comparing((Map<String, Object> m) -> stringOrEmpty(m.get("order_date")))
                    .thenComparing(m -> stringOrEmpty(m.get("meal_period_code")))
            );
            for (var row : sortedOrders) {
                if (importOneOrder(row, dishOldToNew)) {
                    importedOrders++;
                }
            }
        }

        var result = new LinkedHashMap<String, Object>();
        result.put("imported_categories", catOldToNew.size());
        result.put("imported_dishes", dishOldToNew.size());
        result.put("imported_orders", importedOrders);
        return result;
    }

    /** @return  */
    private boolean importOneOrder(Map<String, Object> row, Map<Long, Long> dishOldToNew) {
        var dateStr = stringOrEmpty(row.get("order_date"));
        var period = stringOrEmpty(row.get("meal_period_code")).trim().toLowerCase(Locale.ROOT);
        var remark = row.get("remark") == null ? null : Objects.toString(row.get("remark"), null);
        var createdAt = stringOrEmpty(row.get("created_at"));
        var updatedAt = stringOrEmpty(row.get("updated_at"));
        if (createdAt.isBlank()) {
            createdAt = OffsetDateTime.now().toString();
        }
        if (updatedAt.isBlank()) {
            updatedAt = createdAt;
        }
        @SuppressWarnings("unchecked")
        var rawItems = (List<Map<String, Object>>) row.get("items");
        var items = new ArrayList<MealOrder.Item>();
        if (rawItems != null) {
            for (var it : rawItems) {
                var oldDish = asLong(it.get("dish_id"), -1);
                var newDish = dishOldToNew.get(oldDish);
                if (newDish == null) {
                    continue;
                }
                var qty = asDouble(it.get("quantity"), 1.0);
                if (qty <= 0) {
                    qty = 1.0;
                }
                var note = it.get("note") == null ? null : Objects.toString(it.get("note"), null);
                var mi = new MealOrder.Item();
                mi.setDishId(newDish);
                mi.setQuantity(qty);
                mi.setNote(note);
                items.add(mi);
            }
        }
        if (items.isEmpty()) {
            return false;
        }
        var id = finder.nextId("order");
        var mo = new MealOrder();
        var meta = new Metadata();
        meta.setName(String.valueOf(id));
        mo.setMetadata(meta);
        var spec = new MealOrder.Spec();
        spec.setOrderDate(dateStr);
        spec.setMealPeriodCode(period);
        spec.setRemark(remark);
        spec.setCreatedAt(createdAt);
        spec.setUpdatedAt(updatedAt);
        spec.setItems(items);
        mo.setSpec(spec);
        if (withIndexRepair(() -> client.create(mo).block()) == null) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "写入订单失败");
        }
        return true;
    }

    private static String resolveImportImageUrl(Map<String, Object> row) {
        var url = row.get("image_url");
        return url == null ? null : domainServiceTrim(url);
    }

    private static String domainServiceTrim(Object url) {
        if (url == null) {
            return null;
        }
        var t = Objects.toString(url, "").trim();
        return t.isEmpty() ? null : t;
    }

    private void validateManifest(List<Map<String, Object>> cats, List<Map<String, Object>> dishes, List<Map<String, Object>> orders) {
        var catIds = new HashMap<Long, Boolean>();
        var slugSeen = new HashMap<String, Boolean>();
        for (var c : cats) {
            var id = asLong(c.get("legacy_id"), -1);
            if (id <= 0) {
                throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "分类 legacy_id 无效");
            }
            if (catIds.put(id, true) != null) {
                throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "分类 legacy_id 重复: " + id);
            }
            if (stringOrEmpty(c.get("name")).isBlank()) {
                throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "分类名称不能为空");
            }
            var slug = stringOrEmpty(c.get("slug")).trim().toLowerCase(Locale.ROOT);
            if (slug.isBlank()) {
                throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "分类 slug 不能为空");
            }
            if (slugSeen.put(slug, true) != null) {
                throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "分类 slug 重复: " + slug);
            }
        }
        var dishIds = new HashMap<Long, Boolean>();
        for (var d : dishes) {
            var id = asLong(d.get("legacy_id"), -1);
            if (id <= 0) {
                throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "菜品 legacy_id 无效");
            }
            if (dishIds.put(id, true) != null) {
                throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "菜品 legacy_id 重复: " + id);
            }
            var cid = asLong(d.get("category_id"), -1);
            if (!catIds.containsKey(cid)) {
                throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "菜品引用了备份中不存在的分类: " + cid);
            }
            if (stringOrEmpty(d.get("name")).isBlank()) {
                throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "菜品名称不能为空");
            }
            var mp = parseMealPeriodIds(d.get("meal_period_ids"));
            if (mp.isEmpty()) {
                throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "菜品必须包含餐段 meal_period_ids");
            }
        }
        for (var o : orders) {
            if (stringOrEmpty(o.get("order_date")).isBlank()) {
                throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "订单 order_date 不能为空");
            }
            var pc = stringOrEmpty(o.get("meal_period_code")).trim().toLowerCase(Locale.ROOT);
            if (pc.isBlank()) {
                throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "订单 meal_period_code 不能为空");
            }
            if (!("breakfast".equals(pc) || "lunch".equals(pc) || "dinner".equals(pc))) {
                throw new BusinessException(BusinessErrorCode.BAD_REQUEST, "订单餐段不合法: " + pc);
            }
        }
    }

    private void wipeAll() {
        for (var ext : finder.listMealOrders()) {
            withIndexRepair(() -> client.delete(ext).block());
        }
        purgeAllDishesExtension();
        purgeAllCategoryExtensions();
    }

    private void purgeAllDishesExtension() {
        purgeUntilEmpty(finder::listAllDishExtensions, "清空菜品失败，请稍后重试或检查索引");
    }

    private void purgeAllCategoryExtensions() {
        purgeUntilEmpty(finder::listAllDishCategoryExtensions, "清空分类失败，请稍后重试或检查索引");
    }

    private <T extends Extension> void purgeUntilEmpty(Supplier<List<T>> listSupplier, String failMessage) {
        for (int phase = 0; phase < 2; phase++) {
            if (phase == 1) {
                extensionSchemeRegistry.rebuildAll();
                pauseForIndexer(PURGE_INDEX_PAUSE_MS * 3);
            }
            for (int round = 0; round < PURGE_ROUNDS_PER_PHASE; round++) {
                List<T> list = listSupplier.get();
                if (list == null || list.isEmpty()) {
                    return;
                }
                for (T ext : list) {
                    withIndexRepair(() -> client.delete(ext).block());
                }
                pauseForIndexer(PURGE_INDEX_PAUSE_MS);
            }
        }
        List<T> tail = listSupplier.get();
        if (tail != null && !tail.isEmpty()) {
            throw new BusinessException(BusinessErrorCode.BAD_REQUEST, failMessage);
        }
    }

    private static void pauseForIndexer(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castMapObjectList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        var out = new ArrayList<Map<String, Object>>();
        for (Object o : list) {
            if (o instanceof Map<?, ?> m) {
                out.add((Map<String, Object>) m);
            }
        }
        return out;
    }

    private static byte[] extractManifest(byte[] zipBytes) {
        try (var zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (e.isDirectory()) {
                    continue;
                }
                var name = e.getName();
                if (MANIFEST_ENTRY.equals(name) || name.endsWith("/" + MANIFEST_ENTRY)) {
                    return zis.readAllBytes();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static List<Long> parseMealPeriodIds(Object raw) {
        var out = new ArrayList<Long>();
        if (raw instanceof List<?> list) {
            for (var x : list) {
                if (x instanceof Number n) {
                    out.add(n.longValue());
                } else if (x != null) {
                    try {
                        out.add(Long.parseLong(x.toString()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return out;
    }

    private static String stringOrEmpty(Object v) {
        return v == null ? "" : Objects.toString(v, "");
    }

    private static long asLong(Object v, long def) {
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(Objects.toString(v, ""));
        } catch (Exception e) {
            return def;
        }
    }

    private static int asInt(Object v, int def) {
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(Objects.toString(v, ""));
        } catch (Exception e) {
            return def;
        }
    }

    private static double asDouble(Object v, double def) {
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(Objects.toString(v, ""));
        } catch (Exception e) {
            return def;
        }
    }

    private static boolean parseBooleanFlag(Object raw, boolean defaultTrue) {
        if (raw == null) {
            return defaultTrue;
        }
        if (raw instanceof Boolean b) {
            return b;
        }
        if (raw instanceof Number n) {
            return n.intValue() != 0;
        }
        var s = raw.toString().trim();
        if (s.isEmpty()) {
            return defaultTrue;
        }
        if ("false".equalsIgnoreCase(s) || "0".equals(s) || "no".equalsIgnoreCase(s)) {
            return false;
        }
        if ("true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s)) {
            return true;
        }
        return defaultTrue;
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
}
