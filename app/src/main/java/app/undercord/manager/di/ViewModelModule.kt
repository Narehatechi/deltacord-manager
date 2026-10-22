package app.undercord.manager.di

import app.undercord.manager.ui.viewmodel.home.HomeViewModel
import app.undercord.manager.ui.viewmodel.installer.InstallerViewModel
import app.undercord.manager.ui.viewmodel.installer.LogViewerViewModel
import app.undercord.manager.ui.viewmodel.libraries.LibrariesViewModel
import app.undercord.manager.ui.viewmodel.settings.AdvancedSettingsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val viewModelModule = module {
    factoryOf(::InstallerViewModel)
    factoryOf(::AdvancedSettingsViewModel)
    factoryOf(::HomeViewModel)
    factoryOf(::LogViewerViewModel)
    factoryOf(::LibrariesViewModel)
}