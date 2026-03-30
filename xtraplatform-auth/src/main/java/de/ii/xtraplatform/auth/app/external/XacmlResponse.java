/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.app.external;

import com.google.common.base.Strings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("PMD.CognitiveComplexity")
public class XacmlResponse {
  public List<XacmlResponse.Response> response;

  boolean isAllowed() {
    return Objects.nonNull(response)
        && !response.isEmpty()
        && "Permit".equals(Strings.nullToEmpty(response.get(0).decision));
  }

  boolean isNotApplicable() {
    return Objects.nonNull(response)
        && !response.isEmpty()
        && "NotApplicable".equals(Strings.nullToEmpty(response.get(0).decision));
  }

  boolean isIndeterminate() {
    return Objects.nonNull(response)
        && !response.isEmpty()
        && "Indeterminate".equals(Strings.nullToEmpty(response.get(0).decision));
  }

  String getStatus() {
    if (Objects.nonNull(response)
        && !response.isEmpty()
        && Objects.nonNull(response.get(0).status)) {
      String code =
          Objects.nonNull(response.get(0).status.statusCode)
              ? Strings.nullToEmpty(response.get(0).status.statusCode.value)
              : "";
      return String.format(
          "%s: %s", code, Strings.nullToEmpty(response.get(0).status.statusMessage));
    }
    return null;
  }

  @SuppressWarnings(("PMD.AvoidDeeplyNestedIfStmts"))
  Map<String, String> getObligations() {
    if (Objects.nonNull(response)
        && !response.isEmpty()
        && Objects.nonNull(response.get(0).obligations)) {
      Map<String, String> obligations = new LinkedHashMap<>();
      for (Obligation obligation : response.get(0).obligations) {
        if (Objects.nonNull(obligation.attributeAssignment)) {
          for (Attribute attribute : obligation.attributeAssignment) {
            if (Objects.nonNull(attribute.attributeId) && Objects.nonNull(attribute.value)) {
              obligations.put(attribute.attributeId, attribute.value);
            }
          }
        }
      }
      return obligations;
    }
    return Map.of();
  }

  static class Response {
    public String decision;
    public Status status;
    public List<Obligation> obligations;
  }

  static class Status {
    public StatusCode statusCode;
    public String statusMessage;
  }

  static class StatusCode {
    public String value;
  }

  static class Obligation {
    public String id;
    public List<Attribute> attributeAssignment;
  }

  static class Attribute {
    public String attributeId;
    public String value;
  }
}
