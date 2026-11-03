package com.coverit.app.detect.rules

import com.coverit.app.detect.Kind
import com.coverit.app.detect.text.Around

object Contact {

    private val socialLabel = Regex("""(?i)(instagram|twitter|tiktok|telegram|threads|mastodon|bluesky|snapchat|discord|youtube|twitch|\bx\b|ig|usuario|user(?:name)?|handle|perfil|profile|redes|social)""")

    val email = Rule(Kind.EMAIL, Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}"""))

    val profile = Rule(
        Kind.PROFILE,
        Regex(
            """(?i)(?<![\w/.])(?:https?://)?(?:[a-z]{2,3}\.)?(?:linkedin\.com/(?:in|pub)|github\.com|gitlab\.com|bitbucket\.org|""" +
                """twitter\.com|x\.com|instagram\.com|facebook\.com|fb\.com|tiktok\.com|threads\.net|youtube\.com/(?:c/|user/)?|""" +
                """behance\.net|dribbble\.com|medium\.com|t\.me|wa\.me|bsky\.app/profile|snapchat\.com/add|reddit\.com/u(?:ser)?|twitch\.tv|""" +
                """pinterest\.com|vk\.com|onlyfans\.com|patreon\.com|ko-fi\.com|linktr\.ee)/@?[\w.%-]+/?""",
        ),
    )

    val handle = Rule(
        Kind.PROFILE,
        Regex("""(?<![\w@./])@[A-Za-z0-9_](?:[A-Za-z0-9_.]{1,28}[A-Za-z0-9_])(?![\w@]|\.\w)"""),
        accept = { m, _ -> m.value.any { it.isLetter() } },
        cover = { m, text -> socialLabel.containsMatchIn(Around.sentenceBefore(text, m.range.first, 30)) },
    )

    val early = listOf(email, profile, handle)
}
