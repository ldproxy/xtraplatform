/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Strings;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableStoreSourceGithub.Builder.class)
public interface StoreSourceGithub extends StoreSourceHttp {

  String KEY = "GITHUB";

  @Value.Check
  default StoreSourceGithub apply() {
    Pattern pattern = Pattern.compile("^([\\w\\-\\.]+)\\/([\\w\\-\\.]+)(:([\\w\\-\\.]+))?$");
    Matcher matcher = pattern.matcher(getSrc());

    if (!getSrc().startsWith("https://github.com") && matcher.matches()) {
      String org = matcher.group(1);
      String repo = matcher.group(2);
      String optBranch = matcher.group(4);
      String branch = Strings.isNullOrEmpty(optBranch) ? "main" : optBranch;
      String root =
          Strings.isNullOrEmpty(getArchiveRoot())
              ? ""
              : getArchiveRoot().startsWith("/") ? getArchiveRoot() : "/" + getArchiveRoot();

      return new ImmutableStoreSourceGithub.Builder()
          .from(this)
          .typeString(Type.HTTP_KEY)
          .src(
              String.format(
                  "https://github.com/%s/%s/archive/refs/heads/%s.zip", org, repo, branch))
          .archiveRoot(String.format("/%s-%s%s", repo, branch, root))
          .label(String.format("%s[%s]", KEY, Path.of(getSrc())))
          .build();
    }

    return this;
  }
}
