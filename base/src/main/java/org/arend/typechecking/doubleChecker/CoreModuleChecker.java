package org.arend.typechecking.doubleChecker;

import org.arend.core.definition.Definition;
import org.arend.ext.error.ErrorReporter;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.term.group.Group;
import org.arend.typechecking.TypecheckerState;
import org.arend.typechecking.error.local.LocalErrorReporter;

public class CoreModuleChecker {
  private final ErrorReporter myErrorReporter;
  private final TypecheckerState myState;
  private final CoreDefinitionChecker myChecker;

  public CoreModuleChecker(ErrorReporter errorReporter, TypecheckerState state) {
    myErrorReporter = errorReporter;
    myState = state;
    myChecker = new CoreDefinitionChecker(errorReporter);
  }

  public boolean checkGroup(Group group) {
    LocatedReferable ref = group.getReferable();
    Definition def = ref instanceof TCReferable ? myState.getTypechecked((TCReferable) ref) : null;
    boolean ok = true;
    if (def != null) {
      myChecker.setErrorReporter(new LocalErrorReporter(ref, myErrorReporter));
      if (!myChecker.check(def)) {
        ok = false;
      }
    }

    for (Group subgroup : group.getSubgroups()) {
      if (!checkGroup(subgroup)) {
        ok = false;
      }
    }
    for (Group subgroup : group.getDynamicSubgroups()) {
      if (!checkGroup(subgroup)) {
        ok = false;
      }
    }

    return ok;
  }
}
