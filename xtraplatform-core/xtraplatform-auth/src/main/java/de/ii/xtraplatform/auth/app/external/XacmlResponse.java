/*
 * Copyright 2018-2020 interactive instruments GmbH
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

/**
 * @author zahnen
 */
public class XacmlResponse {
  public List<XacmlResponse.Response> Response;

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

  boolean isIndeterminate() {
    return Objects.nonNull(Response)
        && !Response.isEmpty()
        && Strings.nullToEmpty(Response.get(0).Decision).equals("Indeterminate");
  }

  String getStatus() {
    if (Objects.nonNull(Response)
        && !Response.isEmpty()
        && Objects.nonNull(Response.get(0).Status)) {
      String code =
          Objects.nonNull(Response.get(0).Status.StatusCode)
              ? Strings.nullToEmpty(Response.get(0).Status.StatusCode.Value)
              : "";
      return String.format(
          "%s: %s", code, Strings.nullToEmpty(Response.get(0).Status.StatusMessage));
    }
    return null;
  }

  Map<String, String> getObligations() {
    if (Objects.nonNull(Response)
        && !Response.isEmpty()
        && Objects.nonNull(Response.get(0).Obligations)) {
      Map<String, String> obligations = new LinkedHashMap<>();
      for (Obligation obligation : Response.get(0).Obligations) {
        if (Objects.nonNull(obligation.AttributeAssignment)) {
          for (Attribute attribute : obligation.AttributeAssignment) {
            if (Objects.nonNull(attribute.AttributeId) && Objects.nonNull(attribute.Value)) {
              obligations.put(attribute.AttributeId, attribute.Value);
            }
          }
        }
      }
      return obligations;
    }
    return Map.of();
  }

  static class Response {
    public String Decision;
    public Status Status;
    public List<Obligation> Obligations;
  }

  static class Status {
    public StatusCode StatusCode;
    public String StatusMessage;
  }

  static class StatusCode {
    public String Value;
  }

  static class Obligation {
    public String Id;
    public List<Attribute> AttributeAssignment;
  }

  static class Attribute {
    public String AttributeId;
    public String DataType;
    public String Value;
  }
}
