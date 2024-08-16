package org.arend.server;

import org.arend.ext.error.GeneralError;
import org.arend.naming.reference.TCDefReferable;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ArendTaskListener {
  /**
   * Invoked when the preparation work is done and the task is scheduled.
   *
   * @param definitions   the list of definitions that were actually scheduled for typechecking.
   *                      This includes definitions that were explicitly requested as well as their dependencies.
   */
  void taskScheduled(@NotNull List<? extends TCDefReferable> definitions);

  /**
   * Invoked when a definition was typechecked.
   *
   * @param definition  the definition that was typechecked.
   * @param errors      the list of errors that occurred during typechecking of {@param definition}.
   */
  void definitionChecked(@NotNull TCDefReferable definition, @NotNull List<GeneralError> errors);

  /**
   * Invoked if all definitions were successfully typechecked.
   */
  void taskFinished();

  /**
   * Invoked if the task was cancelled.
   */
  void taskCancelled();
}
