package de.ii.xtraplatform.dropwizard.views;

import com.github.mustachejava.MustacheResolver;
import io.dropwizard.views.View;

public interface MustacheResolverRegistry {
    MustacheResolver getResolverForClass(Class<? extends View> viewClass);
}
