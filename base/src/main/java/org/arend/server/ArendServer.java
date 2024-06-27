package org.arend.server;

import org.arend.ext.error.ErrorReporter;
import org.arend.module.ModuleLocation;
import org.arend.naming.reference.TCDefReferable;
import org.arend.naming.resolving.ResolverListener;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface ArendServer {
  /**
   * Resolves the content of {@param group} and updates {@param module} if necessary.
   *
   * @param modificationStamp   tracks the version of the module. If the current modification stamp is
   *                            greater than {@param modificationStamp}, the module will not be updated.
   * @param module              the module to be updated.
   * @param group               the new content of the module.
   * @param resolverListener    listener for resolver events.
   * @param errorReporter       error reporter for resolver errors.
   */
  void updateModule(long modificationStamp, @NotNull ModuleLocation module, @NotNull ConcreteGroup group, @NotNull ResolverListener resolverListener, @NotNull ErrorReporter errorReporter);

  /**
   * Deletes the specified module.
   */
  void deleteModule(@NotNull ModuleLocation module);

  /**
   * Schedules a new typechecking task.
   * If the set of definitions of the new task intersects with the definitions of some old one, the old task will be cancelled.
   */
  void scheduleTask(@NotNull Collection<? extends TCDefReferable> definitions, @NotNull ArendTaskListener listener);
}
