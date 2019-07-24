<h1 id="let">Let<a class="headerlink" href="#let" title="Permanent link">&para;</a></h1>

Let expressions allow to introduce local variables.
Such expressions have the following syntax:

```arend
\let | p_1 => e_1
     ...
     | p_n => e_n
\in e_{n+1}
```

where `p_1`, ... `p_n` are patterns and `e_1`, ... `e_{n+1}` are expressions.

Every pattern is either a variable or an expression of the form `(p_1', ... p_k')`,
where `p_1'`, ... `p_k'` are patterns. This implies that if `p_i` is a tuple of k subpatterns,
then the type of `e_i` must be either a k-fold Sigma type or a record with k fields.
Note that because of eta-equivalence for Sigma types and records the structure of
`e_i` does not matter: for example, expression `\let (x,y)=> z \in e` evaluates to
`e[z.1/x,z.2/y]` if type of `z` is Sigma type (and with fields instead of projections
`z.1` and `z.2` in case of a record). In general, if pattern `p_i` contains
variables `x_i^1, ... x_i^{n_i}` expression 
`\let | p_1 => e_1 ... | p_n => e_n \in e` evaluates to `e[... proj_i^j(e_i)/x_i^j ,,,]`,
where `proj_i^j` is the sequence of projections and field access expressions, corresponding to 
`j`-th variable of `p_i`. 

The expression `\let | x_1 => e_1 ... | x_n => e_n \in e` has type 
`\let | x_1 => e_1 ... | x_n => e_n \in E`, where `E` is the type of `e`.
 
The type of `e_i` can be explicitly specified as follows: `| p_i : E_i => e_i`.

It is also allowed to write lambda parameters after a pattern if it is a variable.
That is, instead of `| x_i => e_i`, you can write `| x_i p^i_1 ... p^i_{n_i} => e_i`,
where `p^i_1`, ... `p^i_{n_i}` are either variables or named parameters to which `e_i` can refer.
Such a clause is equivalent to `| x_i => \lam p^i_1 ... p^i_{n_i} => e_i`.

Let expressions also can be _strict_.
This means that expressions `e_1`, ... `e_n` will be evaluated immediately when the let expression is evaluated.
To define a strict let expression, use the keyword `\let!` instead of `\let`.
