package org.arend.typechecking.doubleChecker;

import org.arend.core.definition.Definition;
import org.arend.ext.error.ErrorReporter;
import org.arend.naming.reference.LocatedReferable;
import org.arend.naming.reference.TCDefReferable;
import org.arend.term.group.Group;
import org.arend.term.group.Statement;
import org.arend.typechecking.error.local.LocalErrorReporter;

public class CoreModuleChecker {
  private final ErrorReporter myErrorReporter;
  private final CoreDefinitionChecker myChecker;

  public CoreModuleChecker(ErrorReporter errorReporter) {
    myErrorReporter = errorReporter;
    myChecker = new CoreDefinitionChecker(errorReporter);
  }

  public boolean checkGroup(Group group) {
    LocatedReferable ref = group.getReferable();
    Definition def = ref instanceof TCDefReferable ? ((TCDefReferable) ref).getTypechecked() : null;
    boolean ok = true;
    if (def != null) {
      myChecker.setErrorReporter(new LocalErrorReporter(ref, myErrorReporter));
      if (!myChecker.check(def)) {
        ok = false;
      }
    }

    for (Statement statement : group.getStatements()) {
      Group subgroup = statement.getGroup();
      if (subgroup != null && !checkGroup(subgroup)) {
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
