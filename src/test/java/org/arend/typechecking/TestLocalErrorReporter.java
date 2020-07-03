package org.arend.typechecking;

import org.arend.ext.error.ErrorReporter;
import org.arend.ext.reference.Precedence;
import org.arend.naming.reference.GlobalReferable;
import org.arend.typechecking.error.local.LocalErrorReporter;
import org.jetbrains.annotations.NotNull;

public class TestLocalErrorReporter extends LocalErrorReporter {
  public TestLocalErrorReporter(ErrorReporter errorReporter) {
    super(new GlobalReferable() {
        @NotNull
        @Override
        public Precedence getPrecedence() {
                                              return Precedence.DEFAULT;
                                                                        }

      @Override
      public @NotNull Kind getKind() {
        return Kind.OTHER;
      }

      @NotNull
        @Override
        public String textRepresentation() {
                                               return "testDefinition";
                                                                       }
      }, errorReporter);
  }
}
