/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.language.comparisons.agl

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.comparisons.common.FileData
import net.akehurst.language.comparisons.common.Java8TestFiles
import net.akehurst.language.comparisons.common.Results
import net.akehurst.language.comparisons.common.TimeLogger
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException
import java.nio.file.Files
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class Java8_compare_Test_antlrSpec(val file: FileData) {

    companion object {
        const val col = "agl_antlr_spec"

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun files(): Collection<FileData> {
            val f = Java8TestFiles.files.subList(0,3550) // after 3550 we get java.lang.OutOfMemoryError: Java heap space
            println("Number of files to test against: ${f.size}")
            return f
        }

        fun createAndBuildProcessor(aglFile: String): LanguageProcessor {
            val bytes = Java8_compare_Test_antlrSpec::class.java.getResourceAsStream(aglFile).readBytes()
            val javaGrammarStr = String(bytes)
            val proc = Agl.processor(javaGrammarStr)
            proc.buildFor("compilationUnit")
            return proc
        }

        val specJava8Processor = createAndBuildProcessor("/agl/Java8AntlrSpec.agl")

        var input: String? = null

        fun parseWithJava8Spec(file: FileData): SharedPackedParseTree? {
            return try {
                specJava8Processor.parse("compilationUnit", input!!)
                TimeLogger(col, file).use { timer ->
                    val tree = specJava8Processor.parse("compilationUnit", input!!)
                    timer.success()
                    tree
                }
            } catch (e: ParseFailedException) {
                Results.logError(col, file)
                assertTrue(file.isError)
                null
            }
        }

        @BeforeClass
        @JvmStatic
        fun init() {
            Results.reset()
        }

        @AfterClass
        @JvmStatic
        fun end() {
            Results.write()
        }
    }


    @Before
    fun setUp() {
        try {
            input = String(Files.readAllBytes(file.path))
        } catch (e: IOException) {
            e.printStackTrace()
            Assert.fail(e.message)
        }
    }

    @Test
    fun agl_antlr_spec_compilationUnit() {
       parseWithJava8Spec(file)
    }

}