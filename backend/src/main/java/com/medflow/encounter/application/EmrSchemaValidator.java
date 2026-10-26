package com.medflow.encounter.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * 后端模板校验：隐藏字段剔除 -> 类型/范围 -> 条件必填。前端提示不是权威。
 */
@Component
public class EmrSchemaValidator {

    public List<Map<String, String>> validate(Map<String, Object> schema, Map<String, Object> content) {
        List<Map<String, String>> errors = new ArrayList<>();
        Map<String, Object> values = content == null ? Map.of() : content;
        for (Map<String, Object> field : fields(schema)) {
            if (!visible(field, values)) {
                continue;
            }
            String key = String.valueOf(field.get("key"));
            String label = field.get("label") == null ? key : String.valueOf(field.get("label"));
            Object value = values.get(key);
            boolean required = Boolean.TRUE.equals(field.get("required")) || requiredWhen(field, values);
            if (isBlank(value)) {
                if (required) {
                    add(errors, key, label + "不能为空");
                }
                continue;
            }
            validateType(field, label, value, errors);
            Object maxLength = field.get("maxLength");
            if (maxLength instanceof Number number && value.toString().length() > number.intValue()) {
                add(errors, key, label + "长度不能超过 " + number.intValue());
            }
        }
        return errors;
    }

    public Map<String, Object> fieldLabels(Map<String, Object> schema) {
        Map<String, Object> labels = new LinkedHashMap<>();
        for (Map<String, Object> field : fields(schema)) {
            labels.put(String.valueOf(field.get("key")),
                    field.get("label") == null ? field.get("key") : field.get("label"));
        }
        return labels;
    }

    private void validateType(Map<String, Object> field, String label, Object value,
            List<Map<String, String>> errors) {
        String key = String.valueOf(field.get("key"));
        String type = String.valueOf(field.getOrDefault("type", "text"));
        switch (type) {
            case "number" -> {
                Double number = toNumber(value);
                if (number == null) {
                    add(errors, key, label + "必须是数字");
                    return;
                }
                if (field.get("min") instanceof Number min && number < min.doubleValue()) {
                    add(errors, key, label + "不能小于 " + min);
                }
                if (field.get("max") instanceof Number max && number > max.doubleValue()) {
                    add(errors, key, label + "不能大于 " + max);
                }
            }
            case "date" -> {
                try {
                    LocalDate date = LocalDate.parse(value.toString());
                    Object max = field.get("max");
                    if ("$today".equals(max) && date.isAfter(LocalDate.now())) {
                        add(errors, key, label + "不能晚于今天");
                    }
                } catch (RuntimeException ex) {
                    add(errors, key, label + "日期格式不正确");
                }
            }
            case "boolean" -> {
                if (!(value instanceof Boolean) && !"true".equals(value) && !"false".equals(value)) {
                    add(errors, key, label + "必须是布尔值");
                }
            }
            case "select", "radio" -> {
                List<String> options = options(field);
                if (!options.isEmpty() && !options.contains(value.toString())) {
                    add(errors, key, label + "选项不合法");
                }
            }
            case "checkbox" -> {
                if (!(value instanceof Collection<?> collection)) {
                    add(errors, key, label + "必须是多选值");
                    return;
                }
                List<String> options = options(field);
                for (Object item : collection) {
                    if (!options.isEmpty() && !options.contains(item.toString())) {
                        add(errors, key, label + "包含非法选项");
                        return;
                    }
                }
            }
            default -> {
                // text / textarea 不需要额外类型判断
            }
        }
    }

    private boolean visible(Map<String, Object> field, Map<String, Object> values) {
        return condition(field.get("visibleWhen"), values);
    }

    private boolean requiredWhen(Map<String, Object> field, Map<String, Object> values) {
        Object condition = field.get("requiredWhen");
        return condition != null && condition(condition, values);
    }

    private boolean condition(Object raw, Map<String, Object> values) {
        if (!(raw instanceof Map<?, ?> condition)) {
            return true;
        }
        Object actual = values.get(String.valueOf(condition.get("field")));
        Object expected = condition.get("value");
        String op = String.valueOf(condition.containsKey("op") ? condition.get("op") : "eq");
        return switch (op) {
            case "eq" -> String.valueOf(actual).equals(String.valueOf(expected));
            case "ne" -> !String.valueOf(actual).equals(String.valueOf(expected));
            case "gte" -> toNumber(actual) != null && toNumber(expected) != null
                    && toNumber(actual) >= toNumber(expected);
            case "lte" -> toNumber(actual) != null && toNumber(expected) != null
                    && toNumber(actual) <= toNumber(expected);
            case "in" -> expected instanceof Collection<?> collection
                    && collection.stream().map(item -> String.valueOf(item)).anyMatch(item -> item.equals(String.valueOf(actual)));
            default -> true;
        };
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fields(Map<String, Object> schema) {
        List<Map<String, Object>> result = new ArrayList<>();
        Object sections = schema == null ? null : schema.get("sections");
        if (!(sections instanceof List<?> list)) {
            return result;
        }
        for (Object section : list) {
            if (section instanceof Map<?, ?> map) {
                Object fields = map.get("fields");
                if (fields instanceof List<?> fieldList) {
                    for (Object field : fieldList) {
                        if (field instanceof Map<?, ?> fieldMap) {
                            result.add((Map<String, Object>) fieldMap);
                        }
                    }
                }
            }
        }
        return result;
    }

    private List<String> options(Map<String, Object> field) {
        List<String> result = new ArrayList<>();
        if (field.get("options") instanceof List<?> options) {
            for (Object option : options) {
                if (option instanceof Map<?, ?> map) {
                    result.add(String.valueOf(map.get("value")));
                } else {
                    result.add(String.valueOf(option));
                }
            }
        }
        return result;
    }

    private boolean isBlank(Object value) {
        return value == null || (value instanceof String text && text.isBlank())
                || (value instanceof Collection<?> collection && collection.isEmpty());
    }

    private Double toNumber(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? null : Double.valueOf(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void add(List<Map<String, String>> errors, String field, String message) {
        errors.add(Map.of("field", "content." + field, "message", message));
    }
}
