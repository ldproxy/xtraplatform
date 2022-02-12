/*
 * Copyright 2015-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.dropwizard.app.amdatu;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Parse the {@link Constants#WEB_RESOURCE_KEY} header of a bundle manifest.
 *
 * <p>Allowed syntax for this header is (in pseudo-BNF):
 *
 * <pre>
 * header ::= entries
 *
 * entries ::= entry (',' entries)?
 *
 * entry ::= alias | alias ';' path | alias ';' path ';' contextId
 *
 * alias ::= '/'? URLCHAR*
 *
 * path ::= '/'? URLCHAR*
 *
 * contextId ::= URLCHAR*
 *
 * URLCHAR ::= all characters accepted in a URL (see RFC 1738).
 * </pre>
 *
 * <p>The <tt>alias</tt>, <tt>path</tt> and <tt>contextId</tt> have the following semantics:
 *
 * <ol>
 *   <li>The <tt>alias</tt> represents the web-alias used to access the resources. This alias is
 *       mandatory and may start with a slash (if the leading slash is omitted, it will be prepended
 *       automatically);
 *   <li>The <tt>path</tt> represents the internal path (relative to the registering bundle!) to
 *       access the actual resources. This path is optional, and if omitted the <tt>alias</tt> key
 *       will be used as internal path;
 *   <li>The <tt>contextId</tt> represents the specific context identifier to use in the
 *       registration of the resources. See HttpService specification for more information about the
 *       exact semantics of this key. If omitted, it will be defaulted to an empty string.
 * </ol>
 *
 * <p>Multiple entries can be defined, in which the <tt>alias</tt> <em>must</em> be unique in the
 * context of the HTTP service. There is currently no way to tell whether a resource registration
 * succeeded.
 */
public class ResourceKeyParser {
  /**
   * Parses the entries for the given {@link Constants#WEB_RESOURCE_KEY} value.
   *
   * @param key the string value of the {@link Constants#WEB_RESOURCE_KEY}, can be <code>null</code>
   *     or empty.
   * @return a map of entries, the key denotes the context ID and the value is a list of register
   *     items.
   * @throws InvalidEntryException when a entry in the given argument is considered invalid.
   */
  public static Map<String, ResourceEntry> getEntries(String key) throws InvalidEntryException {
    Map<String, ResourceEntry> result = new HashMap<>();

    if ((key != null) && !"".equals(key.trim())) {
      Scanner scanner = new Scanner(key);
      scanner.useDelimiter(",\\s*");

      while (scanner.hasNext()) {
        Entry entry = new Entry(scanner.next());

        ResourceEntry rEntry = new ResourceEntry(entry.m_alias);
        rEntry.addPath(entry.m_path);
        result.put(entry.m_alias, rEntry);
      }

      scanner.close();
    }

    return result;
  }

  static class Entry {
    final String m_alias;
    final String m_path;

    Entry(String entry) throws InvalidEntryException {
      if (entry == null) {
        throw new InvalidEntryException("Cannot be null!", entry);
      }

      String[] elements = entry.split("\\s*;\\s*");
      if ((elements.length < 1) || (elements.length > 2)) {
        throw new InvalidEntryException(
            "Not enough arguments, at least one at most two arguments expected!", entry);
      }
      String alias = elements[0].trim();
      if ("".equals(alias)) {
        throw new InvalidEntryException("Alias (1st element) cannot be empty!", entry);
      }
      String path = (elements.length > 1) ? elements[1].trim() : alias;
      if ("".equals(path)) {
        throw new InvalidEntryException("Path (2nd element) cannot be empty!", entry);
      }

      m_alias = createValidEntry(alias);
      m_path = createValidEntry(path);
    }

    private static String createValidEntry(String entry) {
      if (!entry.startsWith("/")) {
        return "/" + entry;
      }
      return entry;
    }
  }
}
