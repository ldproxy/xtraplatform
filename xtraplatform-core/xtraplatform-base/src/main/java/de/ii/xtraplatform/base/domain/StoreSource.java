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
import de.ii.xtraplatform.base.domain.StoreSource.Type;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = StoreSource.TYPE_PROP,
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = StoreSourceFs.class, name = Type.FS_KEY),
  @JsonSubTypes.Type(value = StoreSourceCfg32.class, name = StoreSourceCfg32.KEY),
  @JsonSubTypes.Type(value = StoreSourceDefault32.class, name = StoreSourceDefault32.KEY),
  @JsonSubTypes.Type(value = StoreSourceCache32.class, name = StoreSourceCache32.KEY),
  @JsonSubTypes.Type(value = StoreSourceProj32.class, name = StoreSourceProj32.KEY),
  @JsonSubTypes.Type(value = StoreSourceEmpty.class, name = Type.EMPTY_KEY),
})
public interface StoreSource {

  String TYPE_PROP = "type";
  String MODE_PROP = "mode";
  String ZIP_SUFFIX = ".zip";

  enum Type {
    FS(Type.FS_KEY),
    EMPTY(Type.EMPTY_KEY);

    public static final String FS_KEY = "FS";
    public static final String EMPTY_KEY = "EMPTY";

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
    DEFAULTS,
    ENTITIES,
    OVERRIDES,
    RESOURCES;

    public String getPrefix() {
      return Objects.equals(this, ALL) ? "" : this.name().toLowerCase(Locale.ROOT);
    }

    public String getLabel() {
      return this.name().charAt(0) + this.name().substring(1).toLowerCase(Locale.ROOT);
    }

    public static boolean isEvent(String prefix) {
      return Objects.equals(prefix, DEFAULTS.getPrefix())
          || Objects.equals(prefix, ENTITIES.getPrefix())
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
    return getContent() != Content.ALL;
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isArchive() {
    return getSrc().toLowerCase(Locale.ROOT).endsWith(ZIP_SUFFIX);
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default String getLabel() {
    return String.format("%s(%s)", getType(), Path.of(getSrc()));
  }
}
