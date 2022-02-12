/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.dropwizard.domain;

import javax.ws.rs.core.MediaType;

/** @author fischer */
public class MediaTypeCharset {
  public static final String APPLICATION_JSON_UTF8 = MediaType.APPLICATION_JSON + ";charset=utf-8";
  public static final String TEXT_HTML_UTF8 = MediaType.TEXT_HTML + ";charset=utf-8";
}
