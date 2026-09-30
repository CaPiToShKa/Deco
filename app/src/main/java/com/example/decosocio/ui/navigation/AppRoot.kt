package com.example.decosocio.ui.navigation

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CardMembership
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.decosocio.R
import com.example.decosocio.domain.repository.AuthRepository
import com.example.decosocio.ui.demo.DemoMenuScreen
import com.example.decosocio.ui.login.LoginScreen
import com.example.decosocio.ui.membership.MembershipScreen
import com.example.decosocio.ui.news.ArticleScreen
import com.example.decosocio.ui.news.NewsScreen
import com.example.decosocio.ui.onboarding.OnboardingScreen
import com.example.decosocio.ui.privacy.ConsentsScreen
import com.example.decosocio.ui.privacy.DeleteAccountScreen
import com.example.decosocio.ui.privacy.MyDataScreen
import com.example.decosocio.ui.profile.EditProfileScreen
import com.example.decosocio.ui.profile.ProfileScreen
import com.example.decosocio.ui.rewards.CouponScreen
import com.example.decosocio.ui.rewards.RewardsScreen
import org.koin.compose.koinInject

object Routes {
    const val LOGIN = "login"
    const val ONBOARDING = "onboarding"
    const val NEWS = "news"
    const val ARTICLE = "article/{id}"
    const val MEMBERSHIP = "membership"
    const val REWARDS = "rewards"
    const val COUPON = "coupon/{id}"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "profile/edit"
    const val CONSENTS = "privacy/consents"
    const val MY_DATA = "privacy/my-data"
    const val DELETE_ACCOUNT = "privacy/delete"
    const val DEMO = "demo"

    fun article(id: String) = "article/$id"
    fun coupon(id: String) = "coupon/$id"
}

private data class TopLevel(val route: String, val label: Int, val icon: ImageVector)

private val topLevel = listOf(
    TopLevel(Routes.NEWS, R.string.tab_news, Icons.Outlined.Newspaper),
    TopLevel(Routes.MEMBERSHIP, R.string.tab_membership, Icons.Outlined.CardMembership),
    TopLevel(Routes.REWARDS, R.string.tab_rewards, Icons.Outlined.Redeem),
    TopLevel(Routes.PROFILE, R.string.tab_profile, Icons.Outlined.AccountCircle),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppRoot(auth: AuthRepository = koinInject()) {
    val navController = rememberNavController()
    val session by auth.session.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Whenever the session ends (logout, deletion, expired token) go back to the login screen.
    LaunchedEffect(session) {
        if (session == null && currentRoute != null && currentRoute != Routes.LOGIN) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (topLevel.any { it.route == currentRoute }) {
                NavigationBar {
                    topLevel.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = { navController.navigateTopLevel(item.route) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoggedIn = { needsOnboarding ->
                        navController.navigate(if (needsOnboarding) Routes.ONBOARDING else Routes.NEWS) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onDone = {
                        navController.navigate(Routes.NEWS) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.NEWS) {
                NewsScreen(onOpenArticle = { navController.navigate(Routes.article(it)) })
            }
            composable(Routes.ARTICLE, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                ArticleScreen(
                    articleId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.MEMBERSHIP) { MembershipScreen() }
            composable(Routes.REWARDS) {
                RewardsScreen(onOpenCoupon = { navController.navigate(Routes.coupon(it)) })
            }
            composable(Routes.COUPON, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                CouponScreen(
                    couponId = entry.arguments?.getString("id").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                    onConsents = { navController.navigate(Routes.CONSENTS) },
                    onMyData = { navController.navigate(Routes.MY_DATA) },
                    onDeleteAccount = { navController.navigate(Routes.DELETE_ACCOUNT) },
                    onDemoMenu = { navController.navigate(Routes.DEMO) },
                )
            }
            composable(Routes.EDIT_PROFILE) { EditProfileScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.CONSENTS) { ConsentsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.MY_DATA) { MyDataScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.DELETE_ACCOUNT) { DeleteAccountScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.DEMO) { DemoMenuScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        // NEWS is the root of the signed-in back stack (login/onboarding are popped).
        popUpTo(Routes.NEWS) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
