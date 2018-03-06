package com.jetbrains.jetpad.vclang;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.error.ListErrorReporter;
import com.jetbrains.jetpad.vclang.error.doc.Doc;
import com.jetbrains.jetpad.vclang.error.doc.DocStringBuilder;
import com.jetbrains.jetpad.vclang.frontend.ConcreteReferableProvider;
import com.jetbrains.jetpad.vclang.library.Library;
import com.jetbrains.jetpad.vclang.library.LibraryManager;
import com.jetbrains.jetpad.vclang.module.scopeprovider.EmptyModuleScopeProvider;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.naming.scope.Scope;
import com.jetbrains.jetpad.vclang.prelude.PreludeFileLibrary;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrinterConfig;
import com.jetbrains.jetpad.vclang.typechecking.SimpleTypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.TypecheckerState;
import com.jetbrains.jetpad.vclang.typechecking.Typechecking;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.text;
import static com.jetbrains.jetpad.vclang.error.doc.DocFactory.vList;
import static org.junit.Assert.assertThat;

public abstract class VclangTestCase {
  protected LibraryManager libraryManager;
  protected Library preludeLibrary;

  protected final TypecheckerState typecheckerState = new SimpleTypecheckerState();
  protected final List<GeneralError> errorList = new ArrayList<>();
  protected final ListErrorReporter errorReporter = new ListErrorReporter(errorList);
  protected final Typechecking typechecking = new Typechecking(typecheckerState, ConcreteReferableProvider.INSTANCE, errorReporter);

  @Before
  public void loadPrelude() {
    libraryManager = new LibraryManager(name -> { throw new IllegalStateException(); }, EmptyModuleScopeProvider.INSTANCE, System.err::println);
    preludeLibrary = new PreludeFileLibrary(null, typecheckerState);
    libraryManager.setModuleScopeProvider(preludeLibrary.getModuleScopeProvider());
    libraryManager.loadLibrary(preludeLibrary);
    preludeLibrary.typecheck(typechecking, System.err::println);
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
