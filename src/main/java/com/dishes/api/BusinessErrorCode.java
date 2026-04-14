package com.dishes.api;

public enum BusinessErrorCode {
    NOT_FOUND("DISHES_NOT_FOUND"),
    CATEGORY_DELETE_CONFLICT("DISHES_CATEGORY_DELETE_CONFLICT"),
    ACCESS_DENIED("DISHES_ACCESS_DENIED"),
    PASSWORD_INVALID("DISHES_PASSWORD_INVALID"),
    INVALID_MEAL_PERIOD_CODE("DISHES_INVALID_MEAL_PERIOD_CODE"),
    BAD_REQUEST("DISHES_BAD_REQUEST");

    private final String value;

    BusinessErrorCode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
