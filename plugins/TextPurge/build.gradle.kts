version = "1.0.0"
description = "bulk delete messages from a specific user in the current channel with /purge"
aliucord {
    changelog.set(
        """
        Changelog {added}
        ======================
        * v1.0.0 - Initial release
        """.trimIndent(),
    )

    // Excludes this plugin from publishing and global plugin repositories.
    // Set this to false if the plugin is unfinished
    deploy.set(true)

    // Builds and deploys this plugin but excludes it from global plugin repositories.
    // Set this if the plugin has reached EOL but a last update should still occur.
    // deployHidden.set(true)
}
