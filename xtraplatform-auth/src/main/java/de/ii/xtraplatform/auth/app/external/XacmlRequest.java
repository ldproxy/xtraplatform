/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.app.external;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.auth.domain.User;
import de.ii.xtraplatform.base.domain.AuthConfiguration.GeoXacmlVersion;
import de.ii.xtraplatform.base.domain.AuthConfiguration.XacmlJsonVersion;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author zahnen
 */
public class XacmlRequest {
  // TODO
  private static final String PREFIX_GEO = "ldproxy:feature:geometry";

  public final Map<String, Object> Request;

  public XacmlRequest(
      XacmlJsonVersion version,
      String resourceId,
      Map<String, Object> resourceAttributes,
      String actionId,
      Map<String, Object> actionAttributes,
      Optional<User> user,
      GeoXacmlVersion geoXacmlVersion) {
    ImmutableList.Builder<Attribute> subject =
        ImmutableList.<Attribute>builder()
            .add(
                new Attribute(
                    "urn:oasis:names:tc:xacml:1.0:subject:subject-id",
                    user.map(User::getName).orElse("UNKNOWN")));
    if (user.isPresent()) {
      String apiId = resourceAttributes.getOrDefault("ldproxy:api:id", "").toString();

      subject.add(new Attribute("ldproxy:claim:permissions", user.get().getPermissions(apiId)));
      if (!user.get().getScopes().isEmpty()) {
        subject.add(new Attribute("ldproxy:claim:scopes", user.get().getScopes()));
      }
      if (!user.get().getAudience().isEmpty()) {
        subject.add(new Attribute("ldproxy:claim:audience", user.get().getAudience()));
      }
    }

    ImmutableList.Builder<Attribute> resource =
        ImmutableList.<Attribute>builder()
            .add(new Attribute("urn:oasis:names:tc:xacml:1.0:resource:resource-id", resourceId));
    resourceAttributes.forEach((id, value) -> add(id, value, resource, geoXacmlVersion));

    ImmutableList.Builder<Attribute> action =
        ImmutableList.<Attribute>builder()
            .add(new Attribute("urn:oasis:names:tc:xacml:1.0:action:action-id", actionId));
    actionAttributes.forEach((id, value) -> add(id, value, action, geoXacmlVersion));

    Request =
        version == XacmlJsonVersion._1_0
            ? request10(subject.build(), resource.build(), action.build())
            : request11(subject.build(), resource.build(), action.build());
  }

  private static void add(
      String id, Object value, Builder<Attribute> category, GeoXacmlVersion geoXacmlVersion) {
    if (value instanceof Collection<?> && ((Collection<?>) value).isEmpty()) {
      return;
    }
    if (geoXacmlVersion != GeoXacmlVersion.NONE && Objects.equals(id, PREFIX_GEO)) {
      category.add(
          new Attribute(
              id,
              value,
              geoXacmlVersion == GeoXacmlVersion._1_0
                  ? "urn:ogc:def:dataType:geoxacml:1.0:geometry"
                  : "urn:ogc:def:geoxacml:3.0:data-type:geometry"));
      return;
    }
    category.add(new Attribute(id, value));
  }

  private static Map<String, Object> request11(
      List<Attribute> subject, List<Attribute> resource, List<Attribute> action) {
    return ImmutableMap.of(
        "AccessSubject", ImmutableMap.of("Attribute", subject),
        "Resource", ImmutableMap.of("Attribute", resource),
        "Action", ImmutableMap.of("Attribute", action));
  }

  private static Map<String, Object> request10(
      List<Attribute> subject, List<Attribute> resource, List<Attribute> action) {
    return ImmutableMap.of(
        "Category",
        ImmutableList.of(
            ImmutableMap.of(
                "CategoryId",
                "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject",
                "Attribute",
                subject),
            ImmutableMap.of(
                "CategoryId",
                "urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
                "Attribute",
                resource),
            ImmutableMap.of(
                "CategoryId",
                "urn:oasis:names:tc:xacml:3.0:attribute-category:action",
                "Attribute",
                action)));
  }

  static class Attribute {
    public final String AttributeId;
    public final Object Value;
    public final String DataType;

    Attribute(String attributeId, String value) {
      this(attributeId, value, "http://www.w3.org/2001/XMLSchema#string");
    }

    Attribute(String attributeId, Object value) {
      this(attributeId, value, "http://www.w3.org/2001/XMLSchema#" + inferType(value));
    }

    Attribute(String attributeId, Object value, String dataType) {
      AttributeId = attributeId;
      Value = value;
      DataType = dataType;
    }

    private static String inferType(Object value) {
      if (isBool(value)) {
        return "boolean";
      }
      if (isDouble(value)) {
        return "double";
      }
      if (isInt(value)) {
        return "integer";
      }
      if (value instanceof Collection) {
        if (((Collection<?>) value).stream().anyMatch(Attribute::isBool)) {
          return "boolean";
        }
        if (((Collection<?>) value).stream().anyMatch(Attribute::isDouble)) {
          return "double";
        }
        if (((Collection<?>) value).stream().anyMatch(Attribute::isInt)) {
          return "integer";
        }
      }

      return "string";
    }

    private static boolean isBool(Object value) {
      return value instanceof Boolean;
    }

    private static boolean isDouble(Object value) {
      return value instanceof Double || value instanceof Float;
    }

    private static boolean isInt(Object value) {
      return value instanceof Integer || value instanceof Long;
    }
  }
}
