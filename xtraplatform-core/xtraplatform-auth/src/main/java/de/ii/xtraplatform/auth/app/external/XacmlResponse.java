/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.app.external;

import com.google.common.base.Strings;
import java.util.List;
import java.util.Objects;

/**
 * @author zahnen
 */
public class XacmlResponse {
  public List<Decision> Response;

  boolean isAllowed() {
    return Objects.nonNull(Response)
        && !Response.isEmpty()
        && Strings.nullToEmpty(Response.get(0).Decision).equals("Permit");
  }

  boolean isNotApplicable() {
    return Objects.nonNull(Response)
        && !Response.isEmpty()
        && Strings.nullToEmpty(Response.get(0).Decision).equals("NotApplicable");
  }

  static class Decision {
    public String Decision;
  }
}
