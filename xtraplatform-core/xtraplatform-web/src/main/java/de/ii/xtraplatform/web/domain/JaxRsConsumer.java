package de.ii.xtraplatform.web.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import java.util.Set;
import java.util.function.Consumer;

@AutoMultiBind
public interface JaxRsConsumer {
  Consumer<Set<Object>> getConsumer();
}
