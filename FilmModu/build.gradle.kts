version = 1000 + (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

cloudstream {
    authors     = listOf("keyiflerolsun")
    language    = "tr"
    description = "Kişisel uyarlama; MiBox oynatma testi bekleniyor."

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie", "TvSeries")
    iconUrl = "https://www.filmmodu.one/icon/favicon.ico"
}

dependencies { add("testImplementation", "junit:junit:4.13.2") }
