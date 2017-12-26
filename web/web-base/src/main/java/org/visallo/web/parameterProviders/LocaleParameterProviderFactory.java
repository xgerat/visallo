package org.visallo.web.parameterProviders;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.visallo.webster.HandlerChain;
import org.visallo.webster.parameterProviders.ParameterProvider;
import org.visallo.webster.parameterProviders.ParameterProviderFactory;
import org.visallo.core.config.Configuration;
import org.visallo.core.model.user.UserRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Locale;

@Singleton
public class LocaleParameterProviderFactory extends ParameterProviderFactory<Locale> {
    private final ParameterProvider<Locale> parameterProvider;

    @Inject
    public LocaleParameterProviderFactory(
            UserRepository userRepository,
            Configuration configuration
    ) {
        parameterProvider = new VisalloBaseParameterProvider<Locale>(userRepository, configuration) {
            @Override
            public Locale getParameter(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
                return getLocale(request);
            }
        };
    }

    @Override
    public boolean isHandled(Method handleMethod, Class<? extends Locale> parameterType, Annotation[] parameterAnnotations) {
        return Locale.class.isAssignableFrom(parameterType);
    }

    @Override
    public ParameterProvider<Locale> createParameterProvider(Method handleMethod, Class<?> parameterType, Annotation[] parameterAnnotations) {
        return parameterProvider;
    }
}
