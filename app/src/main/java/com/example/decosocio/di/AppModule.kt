package com.example.decosocio.di

import com.example.decosocio.AppConfig
import com.example.decosocio.data.Backends
import com.example.decosocio.data.Repositories
import com.example.decosocio.data.SystemDateProvider
import com.example.decosocio.data.demo.DemoControl
import com.example.decosocio.domain.repository.AuthRepository
import com.example.decosocio.domain.repository.DateProvider
import com.example.decosocio.domain.repository.LoyaltyRepository
import com.example.decosocio.domain.repository.MemberRepository
import com.example.decosocio.domain.repository.MembershipRepository
import com.example.decosocio.domain.repository.NewsRepository
import com.example.decosocio.prefs.AppPreferences
import com.example.decosocio.push.PushManager
import com.example.decosocio.push.PushRegistrar
import com.example.decosocio.ui.demo.DemoMenuViewModel
import com.example.decosocio.ui.login.LoginViewModel
import com.example.decosocio.ui.membership.MembershipViewModel
import com.example.decosocio.ui.news.ArticleViewModel
import com.example.decosocio.ui.news.NewsViewModel
import com.example.decosocio.ui.privacy.ConsentsViewModel
import com.example.decosocio.ui.privacy.DeleteAccountViewModel
import com.example.decosocio.ui.privacy.MyDataViewModel
import com.example.decosocio.ui.profile.EditProfileViewModel
import com.example.decosocio.ui.profile.ProfileViewModel
import com.example.decosocio.ui.rewards.CouponViewModel
import com.example.decosocio.ui.rewards.RewardsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun appModule(config: AppConfig): Module = module {
    single { config }
    single<DateProvider> { SystemDateProvider() }
    single { DemoControl() }
    single<Repositories> {
        Backends.create(
            mode = config.backendMode,
            bffBaseUrl = config.bffBaseUrl,
            newsFeedUrl = config.newsFeedUrl,
            dates = get(),
            demoControl = get(),
        )
    }
    single<AuthRepository> { get<Repositories>().auth }
    single<MemberRepository> { get<Repositories>().member }
    single<MembershipRepository> { get<Repositories>().membership }
    single<LoyaltyRepository> { get<Repositories>().loyalty }
    single<NewsRepository> { get<Repositories>().news }
    single { PushManager(androidContext(), config.sfmc) }
    single<PushRegistrar> { get<PushManager>() }
    single { AppPreferences(androidContext()) }

    viewModel { LoginViewModel(get(), get(), get()) }
    viewModel { ConsentsViewModel(get(), get()) }
    viewModel { NewsViewModel(get()) }
    viewModel { params -> ArticleViewModel(params.get(), get()) }
    viewModel { MembershipViewModel(get(), get()) }
    viewModel { RewardsViewModel(get(), get()) }
    viewModel { params -> CouponViewModel(params.get(), get(), get()) }
    viewModel { ProfileViewModel(get(), get(), get(), get()) }
    viewModel { EditProfileViewModel(get()) }
    viewModel { MyDataViewModel(get()) }
    viewModel { DeleteAccountViewModel(get(), get(), get()) }
    viewModel { DemoMenuViewModel(get(), get(), get()) }
}
