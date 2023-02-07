/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.app.external;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @author zahnen
 */
public class XacmlRequest10 {
  public final Map<String, Object> Request;

  public XacmlRequest10(String user, String method, String path, byte[] body) {
    List<Attribute> subject =
        ImmutableList.of(new Attribute("urn:oasis:names:tc:xacml:1.0:subject:subject-id", user));
    List<Attribute> resource =
        ImmutableList.of(new Attribute("urn:oasis:names:tc:xacml:1.0:resource:resource-id", path));
    ImmutableList.Builder<Attribute> action =
        ImmutableList.<Attribute>builder()
            .add(new Attribute("urn:oasis:names:tc:xacml:1.0:action:action-id", method));
    if (method.equals("POST") || method.equals("PUT")) {
      action.add(new Attribute("payload", new String(body, StandardCharsets.UTF_8)));
    }

    Request =
        ImmutableMap.of(
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
                    action.build())));
  }

  static class Attribute {
    public final String AttributeId;
    public final String Value;
    public final String DataType;

    Attribute(String attributeId, String value) {
      AttributeId = attributeId;
      Value = value;
      DataType = "http://www.w3.org/2001/XMLSchema#string";
    }
  }
}
