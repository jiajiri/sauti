package com.sauti.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatMarkdownParserTest {

    @Test
    fun testClosedCodeBlockExtraction() {
        val raw = """
            Hakikisha una faili:
            ```python
            import json
            def process_file(filename):
                return True
            ```
            Kitanzi kimekamilika.
        """.trimIndent()

        val blocks = parseMessageBlocks(raw)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is ContentBlock.TextBlock)
        assertTrue(blocks[1] is ContentBlock.CodeBlock)
        assertTrue(blocks[2] is ContentBlock.TextBlock)

        val codeBlock = blocks[1] as ContentBlock.CodeBlock
        assertEquals("python", codeBlock.language)
        assertEquals("import json\ndef process_file(filename):\n    return True", codeBlock.code)
    }

    @Test
    fun testStreamingUnclosedCodeFenceDoesNotCollapseToInlineBadge() {
        val streamingRaw = """
            Mfano wa msimbo:
            ```python
            import json
            data = json.load(f)
        """.trimIndent()

        val blocks = parseMessageBlocks(streamingRaw)
        assertEquals(2, blocks.size)
        assertTrue(blocks[0] is ContentBlock.TextBlock)
        assertTrue(blocks[1] is ContentBlock.CodeBlock)

        val codeBlock = blocks[1] as ContentBlock.CodeBlock
        assertEquals("python", codeBlock.language)
        assertEquals("import json\ndata = json.load(f)", codeBlock.code)
    }

    @Test
    fun testInlineCodeWithoutTripleBackticks() {
        val raw = "Tumia amri ya `pip install` au `python -m json.tool` kwenye terminal."
        val blocks = parseMessageBlocks(raw)

        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is ContentBlock.TextBlock)
        assertEquals(raw, (blocks[0] as ContentBlock.TextBlock).content)
    }
}
