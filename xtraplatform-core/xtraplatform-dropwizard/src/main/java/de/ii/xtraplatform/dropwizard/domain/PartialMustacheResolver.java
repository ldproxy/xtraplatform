package de.ii.xtraplatform.dropwizard.domain;

import io.dropwizard.views.View;

import java.io.Reader;

public interface PartialMustacheResolver {

    int getSortPriority();

    boolean canResolve(String templateName, Class<?> viewClass);

    Reader getReader(String templateName, Class<?> viewClass);
}
