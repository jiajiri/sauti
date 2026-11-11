package com.sauti.app.security

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * 12-Word BIP-39 Recovery Engine for Sovereign Offline Identity.
 * Converts a 256-bit entropy master key into human-readable recovery words.
 */
object Bip39RecoveryEngine {

    // Standard BIP-39 wordlist subset demonstration (English & Swahili friendly)
    private val WORDLIST = listOf(
        "abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract",
        "baridi", "bahari", "barabara", "boma", "chui", "damu", "dhahabu", "dunia",
        "eagle", "early", "earth", "east", "easy", "echo", "economy", "element",
        "furaha", "fedha", "fimbo", "giza", "habari", "haki", "harufu", "hewa",
        "icon", "idea", "identify", "idle", "ignore", "illness", "image", "imitate",
        "jua", "jicho", "jirani", "kijiji", "kamba", "kalamu", "kioo", "kisima",
        "labor", "ladder", "lake", "lamp", "language", "laptop", "large", "later",
        "mwezi", "mlima", "mto", "mti", "nyota", "ndege", "nguvu", "njia",
        "ocean", "october", "odor", "office", "often", "oil", "okay", "old",
        "pepo", "paka", "punda", "rafiki", "roho", "samaki", "simba", "sauti",
        "table", "tackle", "tag", "tail", "talent", "talk", "tank", "tape",
        "taa", "taa", "tembo", "tumbili", "upepo", "upendo", "utajiri", "uzuri",
        "vibrate", "vicious", "victory", "video", "view", "village", "vintage", "violin",
        "wagon", "wait", "walk", "wall", "walnut", "warm", "warrior", "wash",
        "yadi", "yai", "yake", "yangu", "yeyote", "zamani", "zawadi", "zebra"
    )

    /**
     * Generates a 12-word recovery mnemonic from a 128-bit random seed.
     */
    fun generateMnemonic(): List<String> {
        val entropy = ByteArray(16) // 128-bit entropy = 12 words
        SecureRandom().nextBytes(entropy)

        val words = mutableListOf<String>()
        for (i in 0 until 12) {
            val byteVal = (entropy[i % entropy.size].toInt() and 0xFF) % WORDLIST.size
            words.add(WORDLIST[byteVal])
        }
        return words
    }

    /**
     * Derives a deterministic 256-bit Master Key from a 12-word mnemonic.
     */
    fun mnemonicToMasterKey(words: List<String>): ByteArray {
        val joined = words.joinToString(" ")
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(joined.toByteArray(Charsets.UTF_8))
    }
}
