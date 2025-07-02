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
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableStoreSourceGitlab.Builder.class)
public interface StoreSourceGitlab extends StoreSourceHttp {

  String KEY = "GITLAB";

  @Value.Derived
  @Override
  default String getType() {
    return Type.HTTP.name();
  }

  @Value.Check
  default StoreSourceGitlab apply() {
    Pattern pattern =
        Pattern.compile(
            "^(?:([\\w\\-\\.\\/:]+?)\\/)?([\\w\\-\\.]+)\\/([\\w\\-\\.]+)(?::([\\w\\-\\.]+))?$");
    Matcher matcher = pattern.matcher(getSrc());

    if (matcher.matches()) {
      String scheme = getInsecure() && Objects.nonNull(matcher.group(1)) ? "http" : "https";
      String host = Optional.ofNullable(matcher.group(1)).orElse("gitlab.com");
      String org = matcher.group(2);
      String repo = matcher.group(3);
      String optBranch = matcher.group(4);
      String branch = Strings.isNullOrEmpty(optBranch) ? "main" : optBranch;
      String root =
          Strings.isNullOrEmpty(getArchiveRoot())
              ? ""
              : getArchiveRoot().startsWith("/") ? getArchiveRoot() : "/" + getArchiveRoot();

      return new ImmutableStoreSourceGitlab.Builder()
          .from(this)
          .src(
              String.format(
                  "%s://%s/%s/%s/-/archive/%s/%3$s-%4$s.zip", scheme, host, org, repo, branch))
          .archiveRoot(String.format("/%s-%s%s", repo, branch, root))
          .label(String.format("%s[%s]", KEY, Path.of(getSrc())))
          .build();
    }

    return this;
  }
}
