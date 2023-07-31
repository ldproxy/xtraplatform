/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.base.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.base.domain.StoreSource.Type;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = StoreSource.TYPE_PROP,
    defaultImpl = StoreSourcePartial.class,
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = StoreSourceFs.class, name = Type.FS_KEY),
  @JsonSubTypes.Type(value = StoreSourceHttp.class, name = Type.HTTP_KEY),
  @JsonSubTypes.Type(value = StoreSourceGithub.class, name = StoreSourceGithub.KEY),
  @JsonSubTypes.Type(value = StoreSourceDefault.class, name = StoreSourceDefault.KEY),
  @JsonSubTypes.Type(value = StoreSourceEmpty.class, name = Type.EMPTY_KEY),
  @JsonSubTypes.Type(value = StoreSourceFsV3.class, name = StoreSourceFsV3.KEY),
  @JsonSubTypes.Type(value = StoreSourceHttpV3.class, name = StoreSourceHttpV3.KEY),
  @JsonSubTypes.Type(value = StoreSourceGithubV3.class, name = StoreSourceGithubV3.KEY),
})
public interface StoreSource {

  String TYPE_PROP = "type";
  String MODE_PROP = "mode";
  String ZIP_SUFFIX = ".zip";

  enum Type {
    EMPTY(Type.EMPTY_KEY),
    FS(Type.FS_KEY),
    HTTP(Type.HTTP_KEY);

    public static final String EMPTY_KEY = "EMPTY";
    public static final String FS_KEY = "FS";
    public static final String HTTP_KEY = "HTTP";

    private final String key;

    Type(String key) {
      this.key = key;
    }

    public String key() {
      return key;
    }
  }

  enum Content {
    ALL,
    NONE,
    CFG,
    ENTITIES,
    DEFAULTS,
    INSTANCES_OLD,
    INSTANCES,
    OVERRIDES,
    RESOURCES,
    MULTI;

    public String getPrefix() {
      return Objects.equals(this, ALL)
          ? ""
          : Objects.equals(this, INSTANCES_OLD) ? "entities" : this.name().toLowerCase(Locale.ROOT);
    }

    public String getLabel() {
      return this.name().charAt(0) + this.name().substring(1).toLowerCase(Locale.ROOT);
    }

    public boolean isEvent() {
      return Objects.equals(this, DEFAULTS)
          || Objects.equals(this, INSTANCES_OLD)
          || Objects.equals(this, INSTANCES)
          || Objects.equals(this, OVERRIDES);
    }

    public static boolean isEvent(String prefix) {
      return Objects.equals(prefix, DEFAULTS.getPrefix())
          || Objects.equals(prefix, INSTANCES_OLD.getPrefix())
          || Objects.equals(prefix, INSTANCES.getPrefix())
          || Objects.equals(prefix, OVERRIDES.getPrefix());
    }
  }

  enum Mode {
    RO,
    RW,
  }

  @JsonProperty(StoreSource.TYPE_PROP)
  String getTypeString();

  @JsonIgnore
  @Value.Derived
  default Type getType() {
    return Type.valueOf(getTypeString());
  }

  @Value.Default
  default Content getContent() {
    return Content.ALL;
  }

  @JsonProperty(StoreSource.MODE_PROP)
  @Value.Default
  default Mode getDesiredMode() {
    return Mode.RO;
  }

  @JsonIgnore
  @Value.Derived
  default Mode getMode() {
    return isArchive() ? Mode.RO : getDesiredMode();
  }

  String getSrc();

  Optional<String> getPrefix();

  @Value.Default
  default String getArchiveRoot() {
    return "/";
  }

  @Value.Default
  default boolean getArchiveCache() {
    return true;
  }

  boolean isWatchable();

  List<StoreSourcePartial> getParts();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isWritable() {
    return getMode() == Mode.RW;
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isSingleContent() {
    return getContent() != Content.ALL
        && getContent() != Content.MULTI
        && getContent() != Content.ENTITIES;
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isArchive() {
    return getSrc().toLowerCase(Locale.ROOT).endsWith(ZIP_SUFFIX);
  }

  @JsonIgnore
  @Value.Default
  @Value.Auxiliary
  default String getLabel() {
    return String.format("%s[%s]", getType(), Path.of(getSrc()));
  }

  @JsonIgnore
  @Value.Lazy
  @Value.Auxiliary
  default String getLabelSpaces() {
    return getLabel().replace('[', ' ').replace("]", "");
  }

  @JsonDeserialize(builder = ImmutableStoreSourcePartial.Builder.class)
  default List<StoreSource> explode() {
    return List.of(this);
  }
}
