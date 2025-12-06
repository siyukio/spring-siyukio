package io.github.siyukio.application.dto.parameter;

import com.fasterxml.jackson.databind.EnumNamingStrategies;
import com.fasterxml.jackson.databind.annotation.EnumNaming;

/**
 * @author Bugee
 */
@EnumNaming(EnumNamingStrategies.CamelCaseStrategy.class)
public enum LoginType {
    USERNAME,
    PHONE,
    EMAIL,
    GOOGLE,
    APPLE
}
