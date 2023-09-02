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
@JsonDeserialize(builder = ImmutableStoreSourceGitea.Builder.class)
public interface StoreSourceGitea extends StoreSourceHttp {

  String KEY = "GITEA";

  @Value.Derived
  @Override
  default Type getType() {
    return Type.HTTP;
  }

  @Value.Check
  default StoreSourceGitea apply() {
    Pattern pattern =
        Pattern.compile("^([\\w\\-\\.]+)\\/([\\w\\-\\.]+)\\/([\\w\\-\\.]+)(:([\\w\\-\\.]+))?$");
    Matcher matcher = pattern.matcher(getSrc());

    if (matcher.matches()) {
      String host = matcher.group(1);
      String org = matcher.group(2);
      String repo = matcher.group(3);
      String optBranch = matcher.group(4);
      String branch = Strings.isNullOrEmpty(optBranch) ? "main" : optBranch;
      String root =
          Strings.isNullOrEmpty(getArchiveRoot())
              ? ""
              : getArchiveRoot().startsWith("/") ? getArchiveRoot() : "/" + getArchiveRoot();

      return new ImmutableStoreSourceGitea.Builder()
          .from(this)
          .typeString(Type.HTTP_KEY)
          .src(String.format("https://%s/%s/%s/archive/%s.zip", host, org, repo, branch))
          .archiveRoot(String.format("/%s%s", repo, root))
          .label(String.format("%s[%s]", KEY, Path.of(getSrc())))
          .build();
    }

    return this;
  }
}
