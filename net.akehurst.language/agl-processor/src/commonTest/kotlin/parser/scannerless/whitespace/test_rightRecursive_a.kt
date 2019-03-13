package parser.scannerless.whitespace

import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItemKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test

class test_rightRecursive_a : test_ScannerlessParserAbstract() {

    // S =  'a' | S1 ;
    // S1 = 'a' S ;
    // skip WS = "\s+" ;
    private fun S(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_S = b.rule("S").build()
        val r_S1 = b.rule("S1").concatenation(r_a, r_S)
        r_S.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CHOICE_EQUAL, -1, 0, arrayOf(r_a, r_S1))
        val r_WS = b.rule("WS").skip(true).concatenation(b.pattern("\\s+"))
        return b
    }

    @Test
    fun WSaWS() {
        val rrb = this.S()
        val goal = "S"
        val sentence = " a "

        val expected = """
            S { WS { '\s+' : ' ' } 'a' WS { '\s+' : ' ' }}
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }


    @Test
    fun WSaWSaWS() {
        val rrb = this.S()
        val goal = "S"
        val sentence = " a a "

        val expected = """
            S { WS { '\s+' : ' ' } S1 { 'a' WS { '\s+' : ' ' } S { 'a' WS { '\s+' : ' ' } } } }
        """.trimIndent()

        super.testStringResult(rrb, goal, sentence, expected)
    }

    @Test
    fun WSaWSaWSaWS() {
        val rrb = this.S()
        val goal = "S"
        val sentence = " a a a "

        val expected = """
            S { WS { '\s+' : ' ' }
                S1 {
                    'a' WS { '\s+' : ' ' }
                    S {
                        S1 {
                            'a' WS { '\s+' : ' ' }
                            S { 'a'  WS { '\s+' : ' ' } }
                        }
                    }
                }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun aWS500() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a ".repeat(500)

        val expected = "S { S1 { 'a' WS { '\\s+' : ' ' } ".repeat(499) + "S { 'a' WS { '\\s+' : ' ' } }" +" } }".repeat(499)


        super.test(rrb, goal, sentence, expected)
    }

}