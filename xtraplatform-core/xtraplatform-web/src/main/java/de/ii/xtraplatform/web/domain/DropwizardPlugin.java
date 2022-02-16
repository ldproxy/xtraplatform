package de.ii.xtraplatform.web.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import de.ii.xtraplatform.base.domain.AppConfiguration;
import io.dropwizard.setup.Environment;

@AutoMultiBind
public interface DropwizardPlugin {

  void init(AppConfiguration configuration, Environment environment);
}
