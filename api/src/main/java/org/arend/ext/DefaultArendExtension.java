package org.arend.ext;

import org.arend.ext.typechecking.GoalSolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * This extension is used for libraries without extension classes.
 */
public class DefaultArendExtension implements ArendExtension {
  private Map<String, ArendExtension> dependencies = Collections.emptyMap();

  @Override
  public void setDependencies(@NotNull Map<String, ArendExtension> dependencies) {
    this.dependencies = dependencies;
  }

  @Override
  public @Nullable GoalSolver getGoalSolver() {
    for (ArendExtension extension : dependencies.values()) {
      GoalSolver solver = extension.getGoalSolver();
      if (solver != null) {
        return solver;
      }
    }
    return null;
  }
}
