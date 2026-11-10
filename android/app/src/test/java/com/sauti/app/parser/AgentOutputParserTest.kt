package com.sauti.app.parser

import org.junit.Assert.assertEquals
import org.junit.Test

class AgentOutputParserTest {

    @Test
    fun testExtractCommitText_withThoughtAndFinalOutput() {
        val modelRawOutput = """
            <thought>
            User is saying 'niko msasani karibu na bank ya nbc'.
            Capitalize proper nouns and translate bank.
            </thought>
            <final_output>
            Niko Msasani karibu na benki ya NBC.
            </final_output>
        """.trimIndent()

        val extracted = AgentOutputParser.extractCommitText(modelRawOutput)
        assertEquals("Niko Msasani karibu na benki ya NBC.", extracted)
    }

    @Test
    fun testExtractCommitText_withToolCallAndThought() {
        val modelRawOutput = """
            <thought>
            Looking up word in local dictionary.
            </thought>
            <tool_call>
            {"tool": "local_dictionary_lookup", "parameters": {"query_word": "foleni"}}
            </tool_call>
            <final_output>
            Niko kwenye foleni ya magari na nimechelewa, mambo ni mabaya sana.
            </final_output>
        """.trimIndent()

        val extracted = AgentOutputParser.extractCommitText(modelRawOutput)
        assertEquals("Niko kwenye foleni ya magari na nimechelewa, mambo ni mabaya sana.", extracted)
    }

    @Test
    fun testExtractCommitText_fallbackWhenNoFinalOutputTags() {
        val modelRawOutput = """
            <thought>
            Simple correction.
            </thought>
            Samahani, nimechelewa mzee wangu.
        """.trimIndent()

        val extracted = AgentOutputParser.extractCommitText(modelRawOutput)
        assertEquals("Samahani, nimechelewa mzee wangu.", extracted)
    }
}
