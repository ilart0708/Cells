package com.cells.backend.security;

import com.cells.backend.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fails when someone adds an endpoint without an explicit role rule.
 * To allow a new endpoint for ANY logged-in user (or a public one), add its path below
 * on purpose, so the decision is visible in code review.
 */
class EndpointSecurityGuardTest extends IntegrationTestBase {

    private static final Set<String> NO_ROLE_RULE_NEEDED = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/auth/me",
            "/api/v1/probe/any");

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;
//
//    @Test
//    void everyEndpointHasARoleRuleOrIsExplicitlyAllowListed() {
//        List<String> unprotected = new ArrayList<>();
//
//        handlerMapping.getHandlerMethods().forEach((info, handler) -> {
//            Class<?> controller = ClassUtils.getUserClass(handler.getBeanType());
//            if (!controller.getPackageName().startsWith("com.cells.backend")) {
//                return;
//            }
//
//            Set<String> paths = info.getPathPatternsCondition() == null
//                    ? Set.of()
//                    : info.getPathPatternsCondition().getPatternValues();
//
//            boolean allowListed = !paths.isEmpty() && NO_ROLE_RULE_NEEDED.containsAll(paths);
//            boolean annotated =
//                    AnnotatedElementUtils.hasAnnotation(handler.getMethod(), PreAuthorize.class)
//                            || AnnotatedElementUtils.hasAnnotation(controller, PreAuthorize.class);
//
//            if (!allowListed && !annotated) {
//                unprotected.add(info + " -> " + controller.getSimpleName()
//                        + "#" + handler.getMethod().getName());
//            }
//        });
//
//        assertThat(unprotected)
//                .as("Endpoints with no @PreAuthorize and not on the allow-list")
//                .isEmpty();
//    }
}