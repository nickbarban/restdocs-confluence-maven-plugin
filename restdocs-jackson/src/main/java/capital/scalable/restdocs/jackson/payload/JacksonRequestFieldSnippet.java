package capital.scalable.restdocs.jackson.payload;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.HandlerMethod;

public class JacksonRequestFieldSnippet extends AbstractJacksonFieldSnippet {

    protected JacksonRequestFieldSnippet() {
        super("request");
    }

    @Override
    protected Type getType(HandlerMethod method) {
        for (MethodParameter param : method.getMethodParameters()) {
            if (isRequestBody(param)) {
                return getType(param);
            }
        }
        return null;
    }

    private boolean isRequestBody(MethodParameter param) {
        return param.getParameterAnnotation(RequestBody.class) != null;
    }

    private Type getType(final MethodParameter param) {
        if (param.getParameterType() == List.class) {
            return new GenericArrayType() {

                @Override
                public Type getGenericComponentType() {
                    return firstGenericType(param);
                }
            };
        } else {
            return param.getParameterType();
        }
    }
}