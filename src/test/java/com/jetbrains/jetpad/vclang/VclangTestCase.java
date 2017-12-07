package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocStringBuilder;
import com.jetbrains.jetpad.vclang.frontend.storage.PreludeStorage;
import com.jetbrains.jetpad.vclang.module.scopeprovider.SimpleModuleScopeProvider;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.term.Group;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.text;
import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.vList;
import static org.junit.Assert.assertThat;

public abstract class VclangTestCase {
  private static Group LOADED_PRELUDE  = null;
  protected Group prelude = null;

  protected final SimpleModuleScopeProvider moduleScopeProvider = new SimpleModuleScopeProvider();

  protected final List<GeneralError> errorList = new ArrayList<>();
  protected final ListErrorReporter errorReporter = new ListErrorReporter(errorList);

  public VclangTestCase() {
    loadPrelude();
  }

  protected void loadPrelude() {
    if (prelude != null) throw new IllegalStateException();

    if (LOADED_PRELUDE == null) {
      PreludeStorage preludeStorage = new PreludeStorage(null);

      ListErrorReporter internalErrorReporter = new ListErrorReporter();
      LOADED_PRELUDE = preludeStorage.loadSource(preludeStorage.preludeSourceId, internalErrorReporter).group;
      assertThat("Failed loading Prelude", internalErrorReporter.getErrorList(), containsErrors(0));
    }

    prelude = LOADED_PRELUDE;
    moduleScopeProvider.registerModule(PreludeStorage.PRELUDE_MODULE_PATH, prelude);
  }

  public GlobalReferable get(Scope scope, String path) {
    Referable referable = Scope.Utils.resolveName(scope, Arrays.asList(path.split("\\.")));
    return referable instanceof GlobalReferable ? (GlobalReferable) referable : null;
  }

  @SafeVarargs
  protected final void assertThatErrorsAre(Matcher<? super GeneralError>... matchers) {
    assertThat(errorList, Matchers.contains(matchers));
  }


  protected static Matcher<? super Collection<? extends GeneralError>> containsErrors(final int n) {
    return new TypeSafeDiagnosingMatcher<Collection<? extends GeneralError>>() {
      @Override
      protected boolean matchesSafely(Collection<? extends GeneralError> errors, Description description) {
        if (errors.size() == 0) {
          description.appendText("there were no errors");
        } else {
          List<Doc> docs = new ArrayList<>(errors.size() + 1);
          docs.add(text("there were errors:"));
          for (GeneralError error : errors) {
            docs.add(error.getDoc(PrettyPrinterConfig.DEFAULT));
          }
          description.appendText(DocStringBuilder.build(vList(docs)));
        }
        return n < 0 ? !errors.isEmpty() : errors.size() == n;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("expected number of errors: ").appendValue(n);
      }
    };
  }
}
