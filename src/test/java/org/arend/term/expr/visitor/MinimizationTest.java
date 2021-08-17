package org.arend.term.expr.visitor;

import org.arend.core.subst.LevelPair;
import org.arend.term.prettyprint.MinimizedRepresentation;
import org.arend.typechecking.TypeCheckingTestCase;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class MinimizationTest extends TypeCheckingTestCase {

    private void checkType(String module, String expected) {
        typeCheckModule(module);
        var type = getDefinition("test").getTypeWithParams(new ArrayList<>(), LevelPair.STD);
        var minimizedConcrete = MinimizedRepresentation.generateMinimizedRepresentation(type, null, null);
        typeCheckExpr(minimizedConcrete, null); // printed expression should be type checkable
        assertEquals(expected, minimizedConcrete.toString());
    }

    @Test
    public void minimizeImplicitNat() {
        checkType("\\data D {x : Nat} (y : Nat) | d \n"
                + "\\func test : D {1} 2 => d",
                "D {1} 2");

    }

    @Test
    public void minimizeShouldNotInsertInferrableAguments() {
        checkType("\\data D {A : \\Type} (x : A) | d" +
                "\\func test : D {Nat} 1 => d", "D 1");
    }
}
