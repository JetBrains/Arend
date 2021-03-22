package org.arend.frontend.library;

import org.arend.ext.error.ErrorReporter;
import org.arend.extImpl.DefinitionRequester;
import org.arend.library.Library;
import org.arend.library.LibraryManager;
import org.arend.library.resolver.LibraryResolver;
import org.arend.typechecking.instance.provider.InstanceProviderSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Stack;

public class TimedLibraryManager extends LibraryManager {
  private final Stack<Long> times = new Stack<>();

  public TimedLibraryManager(LibraryResolver libraryResolver, @Nullable InstanceProviderSet instanceProviderSet, ErrorReporter typecheckingErrorReporter, ErrorReporter libraryErrorReporter, DefinitionRequester definitionRequester) {
    super(libraryResolver, instanceProviderSet, typecheckingErrorReporter, libraryErrorReporter, definitionRequester, null);
  }

  public static @NotNull String timeToString(long time) {
    if (time < 10000) {
      return time + "ms";
    }
    if (time < 60000) {
      return time / 1000 + ("." + (time / 100 % 10)) + "s";
    }

    long seconds = time / 1000;
    return (seconds / 60) + "m" + (seconds % 60) + "s";
  }

  @Override
  protected void beforeLibraryLoading(@NotNull Library library) {
    System.out.println("[INFO] Loading library " + library.getName());
    times.push(System.currentTimeMillis());
  }

  @Override
  protected void afterLibraryLoading(@NotNull Library library, boolean successful) {
    long time = System.currentTimeMillis() - times.pop();
    System.err.flush();
    System.out.println("[INFO] " + (successful ? "Loaded " : "Failed loading ") + "library " + library.getName() + (successful ? " (" + timeToString(time) + ")" : ""));
  }
}
