/*
Language: Arend
Author: JetBrains s.r.o
Website: http://arend.readthedocs.io/
Description: functional language with dependent types
*/
hljs.registerLanguage("arend", function (hljs) {
  var
    RE_LEXEMES = /\\?[a-zA-Z0-9-]+|[^\w\s()]+/,

    keywords = {
      keyword:
        "=> -> : | " +
        "\\lp \\lh \\level \\suc \\max " +
        "\\Prop \\Set \\Set0 \\h-Type \\1-Type \\2-Type \\3-Type \\oo-Type \\Type \\use \\coerce " +
        "\\let \\in " +
        "\\case \\return \\elim \\with " +
        "\\where \\truncated \\cowith \\extends " +
        "\\new \\Pi \\Sigma \\lam ",

      built_in:
        "Nat zero suc + * - Int pos neg fromNat I left right Path path inProp = @ coe iso",

      hole: "{?}"
    },

    DEF_FIXITY = {
      cN: "keyword",
      l: RE_LEXEMES,
      b: /\\(infixl|infixr|infix|fixl|fixr|fix)/,
      rE: !0
    },

    u = {
      cN: "title",
      b: (function () {
        var
          r = "~!@#$%^&*\\-+=<>?/|:;[\\]",
          c = r + "a-zA-Z_",
          i = r + "a-zA-Z0-9_",
          s = "(?!:|\\||=>)[" + c + "][" + i + "]*",
          a = "(:|\\||=>)[" + i + "]+"
        return new RegExp("(" + s + ")|(" + a + ")")
      })(),
      e: /./,
      rE: !0
    },

    COMMENT = {
      v: [
        hljs.C("--", "$"),
        hljs.C("{-", "-}", { c: ["self"] })
      ]
    },

    PARENS = {
      l: RE_LEXEMES,
      k: keywords,
      v: [{ b: /\(/, e: /\)/ }, { b: /{/, e: /}/ }],
      c: ["self"]
    },

    DEF_TELE = function (kw) {
      return {
        cN: "keyword",
        b: "\\\\" + kw,
        e: /\s*/,
        starts: {
          e: /(^|\s+)(=>|\\where|^|\|(\s+|$))/,
          eE: !0,
          c: [
            DEF_FIXITY,
            {
              cN: "params",
              v: [{ b: /\(/, e: /\)/ }, { b: /{/, e: /}/ }],
              l: RE_LEXEMES,
              k: keywords,
              c: [PARENS]
            }, {
              cN: "keyword",
              b: /:/,
              e: /\s*/,
              starts: {
                cN: "definition-type",
                l: RE_LEXEMES,
                k: keywords,
                eW: !0,
                rE: !0,
                c: [PARENS]
              }
            }, u, hljs.NM]
        }
      }
    },

    DEF_NAMED = function (kw) {
      return {
        cN: "keyword",
        b: "\\\\" + kw,
        e: /\s+/,
        eE: !0,
        starts: {
          cN: "title",
          b: /[\S]+/,
          e: /[\s]+/,
          eE: !0
        }
      }
    },

    DEF_IMPORT = {
      cN: "ns-command",
      l: RE_LEXEMES,
      b: /\\(open|import)/,
      k: "\\open \\import \\using \\as \\hiding",
      e: /$/
    }

  return {
    l: RE_LEXEMES,
    k: keywords,
    c: [DEF_IMPORT, DEF_TELE("func"), DEF_TELE("coerce"), DEF_TELE("data"), DEF_TELE("instance"), DEF_NAMED("module"), DEF_TELE("class"), DEF_TELE("record"), DEF_NAMED("import"), DEF_NAMED("open"), hljs.NM, COMMENT]
  }
})
