package app.deltacord.manager.di

import app.deltacord.manager.domain.manager.DownloadManager
import app.deltacord.manager.domain.manager.InstallManager
import app.deltacord.manager.domain.manager.PreferenceManager
import app.deltacord.manager.domain.link.DeltacordLink
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val managerModule = module {
    singleOf(::DownloadManager)
    singleOf(::PreferenceManager)
    singleOf(::InstallManager)
    singleOf(::DeltacordLink)
}