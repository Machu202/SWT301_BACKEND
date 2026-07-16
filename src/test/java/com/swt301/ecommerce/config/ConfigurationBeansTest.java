package com.swt301.ecommerce.config;

import com.cloudinary.Cloudinary;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.assertj.core.api.Assertions.*;

class ConfigurationBeansTest {
    @Test void corsMappingCanBeRegisteredWithoutChangingRuntimeConfiguration() {
        assertThatCode(() -> new CorsConfig().addCorsMappings(new CorsRegistry())).doesNotThrowAnyException();
    }

    @Test void cloudinaryBeanUsesInjectedValuesWithoutExposingThem() {
        CloudinaryConfig config = new CloudinaryConfig();
        ReflectionTestUtils.setField(config, "cloudName", "test-cloud");
        ReflectionTestUtils.setField(config, "apiKey", "test-key");
        ReflectionTestUtils.setField(config, "apiSecret", "test-secret");
        Cloudinary cloudinary = config.cloudinary();
        assertThat(cloudinary).isNotNull();
        Object internalConfig = ReflectionTestUtils.getField(cloudinary, "config");
        assertThat(ReflectionTestUtils.getField(internalConfig, "cloudName")).isEqualTo("test-cloud");
    }

    @Test void swaggerDefinesBearerJwtScheme() {
        OpenAPI api = new SwaggerConfig().customOpenAPI();
        assertThat(api.getInfo().getTitle()).isEqualTo("Online Ordering API");
        assertThat(api.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(api.getComponents().getSecuritySchemes().get("bearerAuth").getScheme()).isEqualTo("bearer");
    }
}
