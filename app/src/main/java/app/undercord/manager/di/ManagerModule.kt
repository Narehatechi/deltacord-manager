package app.undercord.manager.di

import app.undercord.manager.domain.manager.DownloadManager
import app.undercord.manager.domain.manager.InstallManager
import app.undercord.manager.domain.manager.PreferenceManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val managerModule = module {
    singleOf(::DownloadManager)
    singleOf(::PreferenceManager)
    singleOf(::InstallManager)
}