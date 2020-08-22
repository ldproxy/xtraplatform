/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.auth.app.external;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * @author zahnen
 */
public class XacmlRequest {
    public final Map<String, Map<String, List<Attribute>>> Request;

    public XacmlRequest(String user, String method, String path, byte[] body) {
        List<Attribute> subject = ImmutableList.of(new Attribute("urn:oasis:names:tc:xacml:1.0:subject:subject-id", user));
        List<Attribute> resource = ImmutableList.of(new Attribute("urn:oasis:names:tc:xacml:1.0:resource:resource-id", path));
        ImmutableList.Builder<Attribute> action = ImmutableList.<Attribute>builder().add(new Attribute("urn:oasis:names:tc:xacml:1.0:action:action-id", method));
        if (method.equals("POST") || method.equals("PUT")) {
            action.add(new Attribute("payload", Base64.getEncoder().encodeToString(body)));
        }

        Request = ImmutableMap.of(
                "AccessSubject", ImmutableMap.of("Attribute", subject),
                "Resource", ImmutableMap.of("Attribute", resource),
                "Action", ImmutableMap.of("Attribute", action.build())
        );
    }

    static class Attribute {
        public final String AttributeId;
        public final String Value;

        Attribute(String attributeId, String value) {
            AttributeId = attributeId;
            Value = value;
        }
    }
}
