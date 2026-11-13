#!/usr/bin/env python3
import re

SWAHILI_ROOTS = {
    "habari", "habar", "hujambo", "jambo", "mambo", "vipi", "salama", "shikamoo",
    "andika", "nielezee", "tengeneza", "eleza", "toa", "fanya", "jinsi",
    "mfano", "kwa", "katika", "yake", "yako", "wangu", "wako", "wao",
    "nini", "gani", "wapi", "lini", "kwanini", "sababu", "lakini",
    "kuwa", "kama", "hapa", "pale", "huyu", "hawa", "kitu", "vitu",
    "mtu", "watu", "tafadhali", "asante", "karibu", "kazi", "namba",
    "hesabu", "ongeza", "punguza", "linganisha", "tafuta", "pata",
    "ndiyo", "hapana", "si", "siyo", "wala", "zaidi", "kidogo", "sana",
    "swahili", "kiswahili", "sehemu", "sawa", "haya", "poa", "vyema",
    "mimi", "wewe", "yeye", "sisi", "nyinyi", "leo", "kesho", "jana",
    "vizuri", "bwana", "dada", "kaka", "mama", "baba", "rafiki", "shida"
}

ENGLISH_ROOTS = {
    "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
    "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
    "this", "but", "his", "by", "from", "they", "we", "say", "her",
    "she", "or", "an", "will", "my", "one", "all", "would", "there",
    "their", "what", "so", "up", "out", "if", "about", "who", "get",
    "which", "go", "me", "when", "make", "can", "like", "time", "no",
    "just", "him", "know", "take", "people", "into", "year", "your",
    "good", "some", "could", "them", "see", "other", "than", "then",
    "now", "look", "only", "come", "its", "over", "think", "also",
    "back", "after", "use", "two", "how", "our", "work", "first",
    "well", "way", "even", "new", "want", "because", "any", "these",
    "give", "day", "most", "us", "hello", "hi", "hey", "please", "help",
    "explain", "write", "create", "fix", "code", "why", "where", "how"
}

SWAHILI_PREFIX_REGEX = re.compile(
    r"^(ku|kuji|ali|ana|ata|wali|wana|wata|nili|nina|nita|uli|una|uta|tuli|tuna|tuta)[a-z]{3,}$",
    re.IGNORECASE
)

def analyze_language(text: str):
    clean = text.strip().lower()
    tokens = [t for t in re.split(r"[^a-zA-Z0-9_-]+", clean) if t]
    if not tokens:
        return "ENGLISH", False, 0, 0, "ENGLISH"

    first_token = tokens[0]
    if first_token in SWAHILI_ROOTS or SWAHILI_PREFIX_REGEX.match(first_token):
        leading_lang = "SWAHILI"
    elif first_token in ENGLISH_ROOTS:
        leading_lang = "ENGLISH"
    else:
        leading_lang = "SWAHILI"

    sw_score = 0
    en_score = 0

    for token in tokens:
        if token in SWAHILI_ROOTS:
            sw_score += 2
        elif SWAHILI_PREFIX_REGEX.match(token):
            sw_score += 1

        if token in ENGLISH_ROOTS:
            en_score += 2

    is_code_switching = (sw_score > 0 and en_score > 0)

    if sw_score > en_score:
        final_lang = "SWAHILI"
    elif en_score > sw_score:
        final_lang = "ENGLISH"
    else:
        final_lang = leading_lang

    return final_lang, is_code_switching, sw_score, en_score, leading_lang

TEST_CASES = [
    {
        "query": "Habari za asubuhi, naomba unisaidie kupika wali wa biriani.",
        "expected_lang": "SWAHILI",
        "description": "Pure Swahili"
    },
    {
        "query": "How do I implement a binary search algorithm in Python?",
        "expected_lang": "ENGLISH",
        "description": "Pure English"
    },
    {
        "query": "Niko ofisini nafanya code review lakini kuna bug kubwa sana.",
        "expected_lang": "SWAHILI",
        "description": "Swahili Dominant Code-Switching"
    },
    {
        "query": "Can you explain how this algorithm works kwa ufupi please?",
        "expected_lang": "ENGLISH",
        "description": "English Dominant Code-Switching"
    },
    {
        "query": "Mambo vipi, what is your name?",
        "expected_lang": "SWAHILI",
        "description": "Tie-Break: Leading token Swahili -> SWAHILI"
    },
    {
        "query": "Hello there, habari yako?",
        "expected_lang": "ENGLISH",
        "description": "Tie-Break: Leading token English -> ENGLISH"
    }
]

def main():
    print("=" * 60)
    print("TESTING O(1) BILINGUAL LANGUAGE ROUTING & EDGE CASES")
    print("=" * 60)

    passed = 0
    for idx, tc in enumerate(TEST_CASES, 1):
        lang, cs, sw_s, en_s, lead = analyze_language(tc["query"])
        ok = (lang == tc["expected_lang"])
        status = "PASSED" if ok else "FAILED"
        if ok:
            passed += 1
        print(f"[Test {idx}]: {tc['description']}")
        print(f"  Query: \"{tc['query']}\"")
        print(f"  Result: Lang={lang} (SW_Score={sw_s}, EN_Score={en_s}, Lead={lead}) -> [{status}]\n")

    print(f"Test Summary: {passed}/{len(TEST_CASES)} passed.")
    assert passed == len(TEST_CASES)

if __name__ == "__main__":
    main()
