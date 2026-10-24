package com.project.course_enrollment.config;

import com.project.course_enrollment.domain.CourseOffering;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the single {@link CourseOffering} from configuration properties.
 *
 * <p>Defaults are provided so the application and tests run without extra configuration:
 * {@code course.offering.id} and {@code course.offering.capacity} (fixed, positive).</p>
 */
@Configuration
public class CourseOfferingConfig {

    @Bean
    public CourseOffering courseOffering(
            @Value("${course.offering.id:CS101}") String id,
            @Value("${course.offering.capacity:2}") int capacity) {
        return new CourseOffering(id, capacity);
    }
}
