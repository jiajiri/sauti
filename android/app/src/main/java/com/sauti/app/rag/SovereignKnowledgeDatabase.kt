package com.sauti.app.rag

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/**
 * On-Device Sovereign SQLite RAG & Knowledge Store.
 * Reshaped from DSE-AI-ENGINE design patterns for local, zero-internet retrieval.
 * Employs SQLite FTS5 for sub-millisecond lexical search across knowledge chunks.
 */
class SovereignKnowledgeDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val TAG = "SovereignKnowledgeDB"
        private const val DATABASE_NAME = "sauti_knowledge.db"
        private const val DATABASE_VERSION = 2

        const val TABLE_KNOWLEDGE = "rag_knowledge_base"
        const val TABLE_FTS = "rag_fts_index"

        const val COL_ID = "id"
        const val COL_CATEGORY = "category"
        const val COL_TITLE = "title"
        const val COL_CONTENT = "chunk_content"
        const val COL_TOKEN_COUNT = "token_count"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_KNOWLEDGE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_TITLE TEXT NOT NULL,
                $COL_CONTENT TEXT NOT NULL,
                $COL_TOKEN_COUNT INTEGER DEFAULT 0
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS $TABLE_FTS USING fts5(
                $COL_TITLE,
                $COL_CONTENT,
                content='$TABLE_KNOWLEDGE',
                content_rowid='$COL_ID'
            );
            """.trimIndent()
        )

        // Seed foundational sovereign knowledge base (Swahili & English tech, coding, and health domains)
        seedFoundationalKnowledge(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_KNOWLEDGE")
        onCreate(db)
    }

    private fun seedFoundationalKnowledge(db: SQLiteDatabase) {
        val entries = listOf(
            Triple(
                "TECHNOLOGY",
                "Sauti Sovereign Architecture",
                "Sauti ni jukwaa la akili bandia (AI) linalofanya kazi ndani ya kifaa (on-device) bila mtandao wa intaneti. Inatumia mifano ya Whisper.cpp kwa sauti na Llama.cpp yenye viwango vya Qwen na SmolLM2."
            ),
            Triple(
                "CODING",
                "Kotlin Flow na Coroutines",
                "Katika lugha ya Kotlin, StateFlow na Flow ni mifumo ya kupokea na kutoa mikondo ya data bila kuzuia mfumo (asynchronous streaming). Kwa mfano:\n```kotlin\nval flow = flow {\n    emit(item)\n}\n```\nInasaidia kutoa maneno moja kwa moja kwenye kiolesura cha mtumiaji."
            ),
            Triple(
                "CODING",
                "Python Function ya Jumla (Sum)",
                "Kuandika function ya Python ya kuhesabu jumla ya namba kwenye orodha (list), tumia amri ya 'def' na kitanzi (loop) au 'sum()':\n```python\ndef hesabu_jumla(namba):\n    \"\"\"Inapokea orodha ya namba na kurudisha jumla yake.\"\"\"\n    jumla = 0\n    for n in namba:\n        jumla += n\n    return jumla\n\n# Mfano wa matumizi:\nnamba = [1, 2, 3, 4, 5]\nprint(\"Jumla:\", hesabu_jumla(namba))\n```"
            ),
            Triple(
                "GEOGRAPHY",
                "Maziwa Makuu ya Afrika Mashariki",
                "Maziwa makuu matatu ya ukanda wa Afrika Mashariki ni:\n1. Ziwa Victoria - linazungukwa na nchi tatu: Tanzania, Kenya, na Uganda.\n2. Ziwa Tanganyika - ni ziwa refu na lenye kina kirefu zaidi barani Afrika, linazungukwa na nchi nne: Tanzania, Jamhuri ya Kidemokrasia ya Kongo (DRC), Burundi, na Zambia.\n3. Ziwa Nyasa (Lake Malawi) - linazungukwa na nchi tatu: Tanzania, Malawi, na Msumbiji (Mozambique)."
            ),
            Triple(
                "GEOGRAPHY",
                "Milima Mirefu ya Afrika Mashariki",
                "Milima mikuu ya Afrika Mashariki:\n1. Mlima Kilimanjaro - Mlima mrefu zaidi barani Afrika (mita 5,895) uliopo nchini Tanzania.\n2. Mlima Kenya - Mlima wa pili kwa urefu barani Afrika (mita 5,199) uliopo Kenya.\n3. Milima ya Rwenzori (Milima ya Mwezi) - Ipo kwenye mpaka wa Uganda na DRC."
            ),
            Triple(
                "MATHEMATICS",
                "Nukuu za Hisabati na Milinganyo",
                "Ili kutatua milinganyo ya msingi ya hisabati, fuata kanuni ya PEMDAS/BODMAS: kwanza Mabano (Brackets), Kipeo (Orders/Exponents), Kuzidisha na Kugawanya (Multiplication & Division), kisha Kujumlisha na Kutoa (Addition & Subtraction)."
            ),
            Triple(
                "ETHICS_SAFETY",
                "Maadili na Usalama wa Mtumiaji",
                "Sauti inazingatia sheria na maadili ya jamii. Maswali yanayohusiana na uhalifu, uizi (theft), au madhara kwa watu hukataliwa mara moja na kutoa mwongozo salama."
            )
        )

        for ((category, title, content) in entries) {
            val stmt = db.compileStatement(
                "INSERT INTO $TABLE_KNOWLEDGE ($COL_CATEGORY, $COL_TITLE, $COL_CONTENT, $COL_TOKEN_COUNT) VALUES (?, ?, ?, ?)"
            )
            stmt.bindString(1, category)
            stmt.bindString(2, title)
            stmt.bindString(3, content)
            stmt.bindLong(4, content.split("\\s+".toRegex()).size.toLong())
            val rowId = stmt.executeInsert()

            val ftsStmt = db.compileStatement(
                "INSERT INTO $TABLE_FTS (rowid, $COL_TITLE, $COL_CONTENT) VALUES (?, ?, ?)"
            )
            ftsStmt.bindLong(1, rowId)
            ftsStmt.bindString(2, title)
            ftsStmt.bindString(3, content)
            ftsStmt.executeInsert()
        }
    }

    /**
     * Searches top relevant knowledge chunks using BM25 ranking via SQLite FTS5.
     */
    fun searchKnowledge(query: String, maxResults: Int = 3): List<String> {
        val cleanQuery = query.replace("'", "").replace("\"", "").trim()
        if (cleanQuery.isBlank()) return emptyList()

        val results = mutableListOf<String>()
        val terms = cleanQuery.split("\\s+".toRegex())
            .filter { it.length > 2 }
            .joinToString(" OR ") { "$it*" }

        if (terms.isBlank()) return emptyList()

        val sql = """
            SELECT $COL_TITLE, $COL_CONTENT 
            FROM $TABLE_FTS 
            WHERE $TABLE_FTS MATCH ? 
            ORDER BY rank 
            LIMIT ?
        """.trimIndent()

        try {
            readableDatabase.rawQuery(sql, arrayOf(terms, maxResults.toString())).use { cursor ->
                val contentIdx = cursor.getColumnIndex(COL_CONTENT)
                val titleIdx = cursor.getColumnIndex(COL_TITLE)
                while (cursor.moveToNext()) {
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx) else ""
                    val content = if (contentIdx >= 0) cursor.getString(contentIdx) else ""
                    results.add("[$title]: $content")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "FTS search error: ${e.message}")
        }

        return results
    }
}
