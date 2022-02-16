package de.ii.xtraplatform.base.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;

@AutoMultiBind(lazy = true)
public interface Lifecycle {

  default void onStart() {}

  default void onStop() {}
}
