/*
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.domain;

import io.dropwizard.views.View;
import io.dropwizard.views.ViewRenderer;
import java.io.IOException;
import java.io.OutputStreamWriter;

public interface MustacheRenderer extends ViewRenderer {
  void render(View view, OutputStreamWriter writer) throws IOException;
}
