/*
 * Copyright 2026 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.web.app;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.jakarta.rs.xml.JacksonXmlBindXMLProvider;
import com.fasterxml.jackson.jakarta.rs.xml.XMLMapperConfigurator;
import jakarta.ws.rs.ext.ContextResolver;

/**
 * Supplies the XML mapper that honours Jakarta XML Binding annotations to every Jackson XML
 * provider. Besides the registered {@link JacksonXmlBindXMLProvider}, Jersey also discovers the
 * plain {@code JacksonXMLProvider} via {@code META-INF/services}; both are equally specific for
 * XML, so which one writes a response is not defined. Both ask a context resolver for their mapper
 * first, so with this resolver they write the same XML.
 */
public class XmlMapperResolver implements ContextResolver<XmlMapper> {

  private final XmlMapper mapper;

  public XmlMapperResolver() {
    this.mapper =
        new XMLMapperConfigurator(null, JacksonXmlBindXMLProvider.DEFAULT_ANNOTATIONS)
            .getDefaultMapper();
  }

  @Override
  public XmlMapper getContext(Class<?> type) {
    return mapper;
  }
}
