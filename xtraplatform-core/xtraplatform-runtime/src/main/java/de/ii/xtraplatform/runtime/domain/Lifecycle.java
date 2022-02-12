package de.ii.xtraplatform.runtime.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;

@AutoMultiBind
public interface Lifecycle {

  default void onStart() {}

  default void onStop() {}
}
