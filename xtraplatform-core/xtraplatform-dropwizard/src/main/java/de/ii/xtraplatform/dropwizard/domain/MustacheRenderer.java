package de.ii.xtraplatform.dropwizard.domain;

import io.dropwizard.views.View;
import io.dropwizard.views.ViewRenderer;

import java.io.IOException;
import java.io.OutputStreamWriter;

public interface MustacheRenderer extends ViewRenderer {
    void render(View view, OutputStreamWriter writer) throws IOException;
}
