package com.banter.android.bfmd

import org.junit.Assert.*
import org.junit.Test

/** Spec examples from docs/bfmd.md */
class BfmdTest {
    @Test
    fun exampleA_boldRemainsAfterCloseSC() {
        val src = "<{s:36},{c=0xFFFFFF},{b:t}> Text Goes Here <{!s},{!c}> This is still bold."
        val runs = parse(src).runs
        assertEquals(2, runs.size)
        assertEquals(" Text Goes Here ", runs[0].text)
        assertEquals(36f, runs[0].style.size)
        assertEquals(Rgba.rgb(255, 255, 255), runs[0].style.color)
        assertTrue(runs[0].style.bold)
        assertEquals(" This is still bold.", runs[1].text)
        assertNull(runs[1].style.size)
        assertNull(runs[1].style.color)
        assertTrue(runs[1].style.bold)
    }

    @Test
    fun exampleB_sizeReplace() {
        val runs = parse("<{s:36}> Text is size 36 <{s:42}> This is size 42.").runs
        assertEquals(36f, runs[0].style.size)
        assertEquals(42f, runs[1].style.size)
    }

    @Test
    fun exampleC_eofCloses() {
        val runs = parse("<{b:t},{i:t}>hello").runs
        assertEquals(1, runs.size)
        assertTrue(runs[0].style.bold && runs[0].style.italic)
    }

    @Test
    fun exampleD_escapedAngle() {
        val runs = parse("""use \<{s:12}> as text""").runs
        assertEquals(1, runs.size)
        assertEquals("use <{s:12}> as text", runs[0].text)
        assertTrue(runs[0].style.isDefault())
    }

    @Test
    fun plainTextStripsCommands() {
        assertEquals("bold plain", plainText("<{b:t}>bold <{!b}>plain"))
    }

    @Test
    fun plainMessageIsValid() {
        val src = "hello from banter"
        assertEquals(src, plainText(src))
    }
}
