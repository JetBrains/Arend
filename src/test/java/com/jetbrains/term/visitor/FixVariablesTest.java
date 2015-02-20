package test.java.com.jetbrains.term.visitor;

import main.java.com.jetbrains.term.NotInScopeException;
import main.java.com.jetbrains.term.definition.Argument;
import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.definition.FunctionDefinition;
import main.java.com.jetbrains.term.expr.Expression;
import org.junit.Test;

import static main.java.com.jetbrains.term.expr.Expression.*;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FixVariablesTest {
    @Test(expected = NotInScopeException.class)
    public void fixVariablesVarOpen() {
        Expression expr = Var("x");
        expr.fixVariables(new ArrayList<String>(Arrays.asList("y")), new HashMap<String, Definition>());
    }

    @Test
    public void fixVariablesVarLocal() {
        Expression expr = Var("x");
        expr = expr.fixVariables(new ArrayList<String>(Arrays.asList("x")), new HashMap<String, Definition>());
        assertEquals(Index(0), expr);
    }

    @Test
    public void fixVariablesVarGlobal() {
        Expression expr = Var("x");
        Map<String, Definition> defs = new HashMap<String, Definition>();
        Definition def = new FunctionDefinition("z", new Argument[0], Nat(), Zero());
        defs.put("x", def);
        expr = expr.fixVariables(new ArrayList<String>(), defs);
        assertEquals(DefCall(def), expr);
    }

    @Test
    public void fixVariablesLam() {
        // \x. x y
        Expression expr = Lam("x", Apps(Var("x"), Var("y")));
        expr = expr.fixVariables(new ArrayList<String>(Arrays.asList("y", "z")), new HashMap<String, Definition>());
        assertEquals(Lam("t", Apps(Index(0), Index(2))), expr);
    }

    @Test
    public void fixVariablesLam2() {
        // \x y. (\z w. y z) y
        Expression expr = Lam("x", Lam("y", Apps(Lam("z", Lam("w", Apps(Var("y"), Var("z")))), Var("y"))));
        expr = expr.fixVariables(new ArrayList<String>(), new HashMap<String, Definition>());
        assertEquals(Lam("x'", Lam("y'", Apps(Lam("z'", Lam("w'", Apps(Index(2), Index(1)))), Index(0)))), expr);
    }

    @Test
    public void fixVariablesPi() {
        // (x y : N) (z : N x -> N y) -> N z y x
        Expression nat = Nat();
        Expression expr = Pi("x", nat, Pi("y", nat, Pi("z", Pi(Apps(nat, Var("x")), Apps(nat, Var("y"))), Apps(nat, Var("z"), Var("y"), Var("x")))));
        expr = expr.fixVariables(new ArrayList<String>(), new HashMap<String, Definition>());
        assertEquals(Pi("x'", nat, Pi("y'", nat, Pi("z'", Pi(Apps(nat, Index(1)), Apps(nat, Index(0))), Apps(nat, Index(0), Index(1), Index(2))))), expr);
    }
}
