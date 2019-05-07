package parser.scannerless.choicePriority

import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItemKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_ifThenElse_Simple : test_ScannerlessParserAbstract() {

    // S =  expr ;
    // ifthenelse = 'if' expr 'then' expr 'else' expr ;
    // ifthen = 'if' expr 'then' expr ;
    // expr = var < conditional ;
    // conditional = ifthenelse < ifthen ;
    // var = 'V' ;
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_expr = b.rule("expr").build()
        val r_if = b.literal("if")
        val r_then = b.literal("then")
        val r_else = b.literal("else")
        val r_var = b.rule("var").choiceEqual(b.literal("W"),b.literal("X"),b.literal("Y"),b.literal("Z"))
        val r_ifthen = b.rule("ifthen").concatenation(r_if,r_expr,r_then,r_expr)
        val r_ifthenelse = b.rule("ifthenelse").concatenation(r_if,r_expr,r_then,r_expr,r_else,r_expr)
        val r_conditional = b.rule("conditional").choicePriority(r_ifthen,r_ifthenelse)
        b.rule(r_expr).choiceEqual(r_var, r_conditional)
        b.rule("S").concatenation(r_expr)
        return b
    }

    @Test
    fun empty_fails() {
        val rrb = this.S()
        val goal = "S"
        val sentence = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

    @Test
    fun ifthenelse() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "ifVthenVelseV"

        val expected = """
            S {
              expr {
                conditional {
                    ifthenelse {
                      'if'
                      expr { var { 'V' } }
                      'then'
                      expr { var { 'V' } }
                      'else'
                      expr { var { 'V' } }
                    }
                }
              }
            }
        """.trimIndent()

        //NOTE: season 35, long expression is dropped in favour of the shorter one!

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun ifthen() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "ifVthenV"

        val expected = """
            S {
              expr {
                conditional {
                    ifthen {
                      'if'
                      expr { var { 'V' } }
                      'then'
                      expr { var { 'V' } }
                    }
                }
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun ifthenelseifthen() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "ifVthenVelseifVthenV"

        val expected = """
            S {
              expr {
                conditional {
                    ifthenelse {
                      'if'
                      expr { var { 'V' } }
                      'then'
                      expr { var { 'V' } }
                      'else'
                      expr {
                        conditional {
                            ifthen {
                              'if'
                              expr { var { 'V'} }
                              'then'
                              expr { var { 'V' } }
                            }
                        }
                      }
                    }
                }
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun ifthenifthenelse() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "ifWthenifXthenYelseZ"

        val expected1 = """
            S {
              expr {
                conditional {
                    ifthen {
                      'if'
                      expr { var { 'V' } }
                      'then'
                      expr {
                        conditional {
                            ifthenelse {
                              'if'
                              expr { var { 'V' } }
                              'then'
                              expr { var { 'V' } }
                              'else'
                              expr { var { 'V' } }
                            }
                        }
                      }
                    }
                }
              }
            }
        """.trimIndent()


        super.testStringResult(rrb, goal, sentence, expected1)
        //super.testStringResult(rrb, goal, sentence, expected1, expected2)
    }


}
