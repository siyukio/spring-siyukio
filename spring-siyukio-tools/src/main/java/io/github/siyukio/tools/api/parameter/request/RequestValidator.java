package io.github.siyukio.tools.api.parameter.request;

import io.github.siyukio.tools.api.definition.ApiDefinition;
import io.github.siyukio.tools.api.definition.ApiRequestParameter;
import io.github.siyukio.tools.api.definition.ApiSchema;
import io.github.siyukio.tools.api.parameter.request.basic.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Validates whether the request parameters are valid.
 *
 * @author Buddy
 */

public interface RequestValidator {

    private static RequestValidator createObjectRequestValidator(ApiRequestParameter apiRequestParameter, String parent) {
        Map<String, RequestValidator> subRequestValidatorMap = new HashMap<>();
        //
        if (!CollectionUtils.isEmpty(apiRequestParameter.properties())) {
            RequestValidator requestValidator;
            String currentPath = StringUtils.hasText(parent) ? parent + "." + apiRequestParameter.name() : apiRequestParameter.name();
            for (ApiRequestParameter childApiRequestParameter : apiRequestParameter.properties()) {
                switch (childApiRequestParameter.schema().type()) {
                    case ARRAY:
                        requestValidator = createCollectionRequestValidator(childApiRequestParameter, currentPath);
                        break;
                    case OBJECT:
                        requestValidator = createObjectRequestValidator(childApiRequestParameter, currentPath);
                        break;
                    default:
                        Object defaultValue = childApiRequestParameter.schema().defaultValue();
                        RequestValidator typeRequestValidator = createTypeRequestValidator(childApiRequestParameter, currentPath);
                        requestValidator = new BasicRequestValidator(typeRequestValidator, defaultValue,
                                currentPath, childApiRequestParameter.error());
                        break;
                }
                subRequestValidatorMap.put(requestValidator.getName(), requestValidator);
            }
        }
        return new ObjectRequestValidator(apiRequestParameter.name()
                , apiRequestParameter.required(),
                subRequestValidatorMap,
                apiRequestParameter.schema().additionalProperties(),
                parent, apiRequestParameter.error());
    }

    private static RequestValidator createCollectionRequestValidator(ApiRequestParameter apiRequestParameter, String parent) {
        RequestValidator requestValidator;
        assert apiRequestParameter.items() != null;
        ApiSchema.Type itemsType = apiRequestParameter.items().schema().type();
        parent = parent + "." + apiRequestParameter.name();
        if (itemsType == ApiSchema.Type.OBJECT) {
            requestValidator = createObjectRequestValidator(apiRequestParameter.items(), parent);
        } else {
            RequestValidator typeRequestValidator = createArrayItemRequestValidator(apiRequestParameter.items(), parent);
            requestValidator = new BasicRequestValidator(typeRequestValidator, null,
                    parent, apiRequestParameter.error());
        }
        return new ArrayRequestValidator(apiRequestParameter.name(),
                apiRequestParameter.required(),
                requestValidator,
                apiRequestParameter.schema().maxItems(),
                apiRequestParameter.schema().minItems(),
                parent,
                apiRequestParameter.error());
    }

    private static RequestValidator createArrayItemRequestValidator(ApiRequestParameter apiRequestParameter, String parent) {
        ApiSchema apiSchema = apiRequestParameter.schema();

        RequestValidator requestValidator;
        switch (apiSchema.type()) {
            case BOOLEAN:
                requestValidator = new BooleanRequestValidator(apiRequestParameter.name(),
                        apiRequestParameter.required(),
                        parent, apiRequestParameter.error());
                break;
            case INTEGER:
                requestValidator = new IntegerRequestValidator(apiRequestParameter.name(),
                        apiRequestParameter.required(),
                        apiSchema.maximum(), apiSchema.minimum(), parent,
                        apiRequestParameter.error());
                break;
            case NUMBER:
                requestValidator = new NumberRequestValidator(apiRequestParameter.name(),
                        apiRequestParameter.required(),
                        apiSchema.maximum(), apiSchema.minimum(), parent,
                        apiRequestParameter.error());
                break;
            default:
                if (StringUtils.hasText(apiSchema.pattern())) {
                    requestValidator = new PatternRequestValidator(apiRequestParameter.name(),
                            apiRequestParameter.required(),
                            apiSchema.pattern(), parent,
                            apiRequestParameter.error());
                } else if (StringUtils.hasText(apiSchema.format()) && apiSchema.format().equals("date-time")) {
                    requestValidator = new DateRequestValidator(apiRequestParameter.name(),
                            apiRequestParameter.required(), parent,
                            apiRequestParameter.error());
                } else {
                    requestValidator = new StringRequestValidator(apiRequestParameter.name(),
                            apiRequestParameter.required(),
                            apiSchema.maxLength(), apiSchema.minLength(),
                            parent, apiRequestParameter.error());
                }
                break;
        }
        return requestValidator;
    }

    private static RequestValidator createTypeRequestValidator(ApiRequestParameter apiRequestParameter, String parent) {
        ApiSchema apiSchema = apiRequestParameter.schema();
        RequestValidator requestValidator;
        switch (apiSchema.type()) {
            case BOOLEAN:
                requestValidator = new BooleanRequestValidator(apiRequestParameter.name(),
                        apiRequestParameter.required(), parent, apiRequestParameter.error());
                break;
            case INTEGER:
                requestValidator = new IntegerRequestValidator(apiRequestParameter.name(),
                        apiRequestParameter.required(),
                        apiSchema.maximum(), apiSchema.minimum(), parent, apiRequestParameter.error());
                break;
            case NUMBER:
                requestValidator = new NumberRequestValidator(apiRequestParameter.name(),
                        apiRequestParameter.required(),
                        apiSchema.maximum(), apiSchema.minimum(), parent, apiRequestParameter.error());
                break;
            default:
                if (StringUtils.hasText(apiSchema.pattern())) {
                    requestValidator = new PatternRequestValidator(apiRequestParameter.name(),
                            apiRequestParameter.required(),
                            apiSchema.pattern(), parent, apiRequestParameter.error());
                } else if (StringUtils.hasText(apiSchema.format()) && apiSchema.format().equals("date-time")) {
                    requestValidator = new DateRequestValidator(apiRequestParameter.name(),
                            apiRequestParameter.required(), parent, apiRequestParameter.error());
                } else {
                    requestValidator = new StringRequestValidator(apiRequestParameter.name(),
                            apiRequestParameter.required(),
                            apiSchema.maxLength(), apiSchema.minLength(), parent, apiRequestParameter.error());
                }
                break;
        }
        return requestValidator;
    }

    static RequestValidator createRequestValidator(ApiDefinition apiDefinition) {
        return createObjectRequestValidator(apiDefinition.requestBodyParameter(), "");
    }

    String getName();

    boolean isRequired();

    <T> T validate(T value);

}
