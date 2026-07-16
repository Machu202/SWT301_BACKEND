package com.swt301.ecommerce.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class EndpointContractTest {
    private static final List<Class<?>> CONTROLLERS = List.of(
            AddressController.class, AdminOrderController.class, AuthController.class,
            CartController.class, OrderController.class, ProductController.class,
            PublicProductController.class, ReferenceDataController.class,
            UserProfileController.class, VoucherController.class
    );

    @Test void everyPublicControllerMethodHasAnHttpMapping() {
        List<String> missing = new ArrayList<>();
        for (Class<?> controller : CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) continue;
                if (!hasMapping(method)) missing.add(controller.getSimpleName() + "." + method.getName());
            }
        }
        assertThat(missing).isEmpty();
    }

    @Test void endpointMethodAndPathCombinationsAreUniqueAndExpectedCountIsStable() {
        Set<String> signatures = new LinkedHashSet<>();
        int methodCount = 0;
        for (Class<?> controller : CONTROLLERS) {
            String base = basePath(controller);
            for (Method method : controller.getDeclaredMethods()) {
                if (!java.lang.reflect.Modifier.isPublic(method.getModifiers()) || !hasMapping(method)) continue;
                methodCount++;
                String signature = httpMethod(method) + " " + normalize(base + methodPath(method));
                assertThat(signatures.add(signature)).as("duplicate endpoint %s", signature).isTrue();
            }
        }
        assertThat(methodCount).isEqualTo(40);
        assertThat(signatures).contains(
                "POST /api/auth/login",
                "POST /api/auth/register",
                "GET /api/products",
                "GET /api/reference/categories",
                "POST /api/orders/checkout",
                "PUT /api/admin/orders/{id}/payment",
                "DELETE /api/carts/items"
        );
    }

    private boolean hasMapping(Method method) {
        return method.isAnnotationPresent(GetMapping.class) || method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class) || method.isAnnotationPresent(DeleteMapping.class)
                || method.isAnnotationPresent(PatchMapping.class) || method.isAnnotationPresent(RequestMapping.class);
    }

    private String basePath(Class<?> type) {
        RequestMapping mapping = type.getAnnotation(RequestMapping.class);
        return mapping == null || mapping.value().length == 0 ? "" : mapping.value()[0];
    }

    private String methodPath(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) return first(method.getAnnotation(GetMapping.class).value());
        if (method.isAnnotationPresent(PostMapping.class)) return first(method.getAnnotation(PostMapping.class).value());
        if (method.isAnnotationPresent(PutMapping.class)) return first(method.getAnnotation(PutMapping.class).value());
        if (method.isAnnotationPresent(DeleteMapping.class)) return first(method.getAnnotation(DeleteMapping.class).value());
        if (method.isAnnotationPresent(PatchMapping.class)) return first(method.getAnnotation(PatchMapping.class).value());
        return first(method.getAnnotation(RequestMapping.class).value());
    }

    private String httpMethod(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) return "GET";
        if (method.isAnnotationPresent(PostMapping.class)) return "POST";
        if (method.isAnnotationPresent(PutMapping.class)) return "PUT";
        if (method.isAnnotationPresent(DeleteMapping.class)) return "DELETE";
        if (method.isAnnotationPresent(PatchMapping.class)) return "PATCH";
        RequestMethod[] methods = method.getAnnotation(RequestMapping.class).method();
        return methods.length == 0 ? "ANY" : methods[0].name();
    }

    private String first(String[] values) { return values.length == 0 ? "" : values[0]; }
    private String normalize(String path) { return path.replaceAll("//+", "/"); }
}
