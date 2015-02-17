package test.java.com.jetbrains.term.visitor;

import main.java.com.jetbrains.term.NotInScopeException;
import main.java.com.jetbrains.term.definition.Definition;
import main.java.com.jetbrains.term.definition.FunctionDefinition;
import main.java.com.jetbrains.term.expr.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FixVariablesTest {
    @Test(expected = NotInScopeException.class)
    public void fixVariablesVarOpen() {
        Expression expr = new VarExpression("x");
        expr.fixVariables(new ArrayList<String>(Arrays.asList("y")), new HashMap<String, Definition>());
    }

    @Test
    public void fixVariablesVarLocal() {
        Expression expr = new VarExpression("x");
        expr = expr.fixVariables(new ArrayList<String>(Arrays.asList("x")), new HashMap<String, Definition>());
        assertEquals(new IndexExpression(0), expr);
    }

    @Test
    public void fixVariablesVarGlobal() {
        Expression expr = new VarExpression("x");
        Map<String, Definition> defs = new HashMap<String, Definition>();
        Definition def = new FunctionDefinition("z", new NatExpression(), new ZeroExpression());
        defs.put("x", def);
        expr = expr.fixVariables(new ArrayList<String>(), defs);
        assertEquals(new DefCallExpression(def), expr);
    }

    @Test
    public void fixVariablesLam() {
        // \x. x y
        Expression expr = new LamExpression("x", new AppExpression(new VarExpression("x"), new VarExpression("y")));
        expr = expr.fixVariables(new ArrayList<String>(Arrays.asList("y", "z")), new HashMap<String, Definition>());
        assertEquals(new LamExpression("t", new AppExpression(new IndexExpression(0), new IndexExpression(2))), expr);
    }

    @Test
    public void fixVariablesLam2() {
        // \x y. (\z w. y z) y
        Expression expr = new LamExpression("x", new LamExpression("y", new AppExpression(new LamExpression("z", new LamExpression("w", new AppExpression(new VarExpression("y"), new VarExpression("z")))), new VarExpression("y"))));
        expr = expr.fixVariables(new ArrayList<String>(), new HashMap<String, Definition>());
        assertEquals(new LamExpression("x'", new LamExpression("y'", new AppExpression(new LamExpression("z'", new LamExpression("w'", new AppExpression(new IndexExpression(2), new IndexExpression(1)))), new IndexExpression(0)))), expr);
    }

    @Test
    public void fixVariablesPi() {
        // (x y : N) (z : N x -> N y) -> N z y x
        Expression nat = new NatExpression();
        Expression expr = new PiExpression("x", nat, new PiExpression("y", nat, new PiExpression("z", new PiExpression(new AppExpression(nat, new VarExpression("x")), new AppExpression(nat, new VarExpression("y"))), new AppExpression(new AppExpression(new AppExpression(nat, new VarExpression("z")), new VarExpression("y")), new VarExpression("x")))));
        expr = expr.fixVariables(new ArrayList<String>(), new HashMap<String, Definition>());
        assertEquals(new PiExpression("x'", nat, new PiExpression("y'", nat, new PiExpression("z'", new PiExpression(new AppExpression(nat, new IndexExpression(1)), new AppExpression(nat, new IndexExpression(0))), new AppExpression(new AppExpression(new AppExpression(nat, new IndexExpression(0)), new IndexExpression(1)), new IndexExpression(2))))), expr);
    }
}
