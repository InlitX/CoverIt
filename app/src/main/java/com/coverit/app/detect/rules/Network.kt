package com.coverit.app.detect.rules

import com.coverit.app.detect.Kind
import com.coverit.app.detect.check.Validators

object Network {

    val all = listOf(
        Rule(Kind.IP, Regex("""(?<![\d.])\d{1,3}(?:\.\d{1,3}){3}(?![\d.])"""), accept = { m, _ -> Validators.isValidIpv4(m.value) }),
        Rule(Kind.IP, Regex("""(?<![\w:.])[0-9A-Fa-f]{0,4}(?::[0-9A-Fa-f]{0,4}){2,7}(?![\w:])"""), accept = { m, _ -> Validators.isValidIpv6(m.value) }),
        Rule(Kind.IP, Regex("""(?<![\w:-])[0-9A-Fa-f]{2}([:-])[0-9A-Fa-f]{2}(?:\1[0-9A-Fa-f]{2}){4}(?![\w:-])""")),
    )
}
