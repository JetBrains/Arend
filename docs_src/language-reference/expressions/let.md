<h1 id="let">Let<a class="headerlink" href="#let" title="Permanent link">&para;</a></h1>

Let expressions allow us to introduce local variables.
Such expressions have the following syntax:

```arend
\let | x_1 => e_1
     ...
     | x_n => e_n
\in e_{n+1}
```

where `x_1`, ... `x_n` are variables and `e_1`, ... `e_{n+1}` are expressions.
Expression `e_{i+1}` can refer to variables `x_1`, ... `x_i`.
You can also write lambda parameters after the name of the variable.
That is, instead of `| x_i => e_i`, you can write `| x_i p^i_1 ... p^i_{n_i} => e_i`, where `p^i_1`, ... `p^i_{n_i}` are either variables or named parameters to which `e_i` can refer.
Such a clause is equivalent to `| x_i => \lam p^i_1 ... p^i_{n_i} => e_i`.

The expression `\let | x_1 => e_1 ... | x_n => e_n \in e` reduces to `e[e_1/x_1, ... e_n/x_n]`.
Its type is `\let | x_1 => e_1 ... | x_n => e_n \in E`, where `E` is the type of `e`.
