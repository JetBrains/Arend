<h1 id="let">Let<a class="headerlink" href="#let" title="Permanent link">&para;</a></h1>

Let expressions allow us to introduce local variables.
Such expressions have the following syntax:

```arend
\let | p_1 => e_1
     ...
     | p_n => e_n
\in e_{n+1}
```

where `p_1`, ... `p_n` are patterns and `e_1`, ... `e_{n+1}` are expressions.
Every pattern is either a variable or an expressions of the form `(p_1', ... p_k')`, where `p_1'`, ... `p_k'` are patterns.
If `p_i` is not a variable, then the type of `e_i` must be either a sigma type or a record.
If `p_i` has subpatterns, then corresponding fields of `e_i` also must have such a type.
The type of `e_i` can be explictly specified as follows: `| p_i : E_i => e_i`.

Expression `e_{i+1}` can refer to variables in patterns `p_1`, ... `p_i`.
You can also write lambda parameters after a pattern if it is a variable.
That is, instead of `| x_i => e_i`, you can write `| x_i p^i_1 ... p^i_{n_i} => e_i`, where `p^i_1`, ... `p^i_{n_i}` are either variables or named parameters to which `e_i` can refer.
Such a clause is equivalent to `| x_i => \lam p^i_1 ... p^i_{n_i} => e_i`.

The expression `\let | x_1 => e_1 ... | x_n => e_n \in e` has type `\let | x_1 => e_1 ... | x_n => e_n \in E`.
It reduces to `e[e_1/x_1, ... e_n/x_n]`.
If `x_i` is not a variable, then corresponding variables are replaced with projections of `e_i`.
