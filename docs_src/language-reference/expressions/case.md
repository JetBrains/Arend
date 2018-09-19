<h1 id="case">Case<a class="headerlink" href="#case" title="Permanent link">&para;</a></h1>

The basic syntax of case expressions looks like this:

```arend
\case e_1, ... e_n \with {
  | p_1^1, ... p_n^1 => d_1
  ...
  | p_1^k, ... p_n^k => d_k
}
```

where `e_1`, ... `e_n`, `d_1`, ... `d_k` are expressions and `p_1^1`, ... `p_n^k` are patterns.
Such an expression reduces in the same way as functions defined by pattern matching (see [this section](/language-reference/definitions/functions/#pattern-matching)).
If the typechecker does not know the type of a case expression, it must be specified explicitly: `\case e_1, ... e_n \return T \with { ... }`.

The general syntax of case expressions looks like this: `\case e_1 \as x_1 : E_1, ... e_n \as x_n : E_n \return T \with { ... }`,
where `x_1`, ... `x_n` are variables and `E_1`, ... `E_n` are expressions.
The parts `\as x_i` and `: E_i` can be omitted.
Expressions `E_i` can refer to `x_1`, ... `x_{i-1}` and `T` can refer to `x_1`, ... `x_n`.
In this case, `e_i` must have type `E_i[e_1/x_1, ... e_{i-1}/x_{i-1}]`.
The type of the case expression is `T[e_1/x_1, ... e_n/x_n]`.
