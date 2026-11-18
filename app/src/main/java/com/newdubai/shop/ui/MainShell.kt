@file:OptIn(ExperimentalMaterial3Api::class)

package com.newdubai.shop.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.newdubai.shop.util.tr
import kotlinx.coroutines.launch

private data class NavItem(val route: String, val icon: ImageVector)

private val ITEMS = listOf(
    NavItem("dash", Icons.Default.Dashboard),
    NavItem("cust", Icons.Default.People),
    NavItem("orders", Icons.Default.ContentCut),
    NavItem("meas", Icons.Default.Straighten),
    NavItem("fabric", Icons.Default.Store),
    NavItem("cash", Icons.Default.AccountBalanceWallet),
    NavItem("debt", Icons.Default.CreditCard),
    NavItem("exp", Icons.Default.Receipt),
    NavItem("rep", Icons.Default.Assessment),
    NavItem("bak", Icons.Default.CloudUpload),
    NavItem("set", Icons.Default.Settings)
)

private val PRIMARY_TABS = listOf(
    NavItem("dash", Icons.Default.Home),
    NavItem("cust", Icons.Default.People),
    NavItem("orders", Icons.Default.ContentCut),
    NavItem("fabric", Icons.Default.Store),
    NavItem("rep", Icons.Default.Assessment)
)

@Composable
fun MainShell(vm: AppVM) {
    val nav = rememberNavController()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "dash"
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    var confirmExit by remember { mutableStateOf(false) }

    BackHandler {
        when {
            drawer.isOpen -> scope.launch { drawer.close() }
            route != "dash" -> { if (!nav.popBackStack()) confirmExit = true }
            else -> confirmExit = true
        }
    }

    fun go(r: String) {
        scope.launch { drawer.close() }
        nav.navigate(r) {
            popUpTo("dash") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawer,
        scrimColor = Color.Black.copy(alpha = .58f),
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                drawerContainerColor = cs.surface,
                drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
            ) {
                Box(
                    Modifier.fillMaxWidth().background(
                        Brush.linearGradient(listOf(LapisDeep, Color(0xFF51283A), Lapis, Color(0xFF9A6545)))
                    )
                ) {
                    Column(
                        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 22.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Box(
                            Modifier.size(58.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = .12f))
                                .border(1.dp, Saffron.copy(alpha = .8f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.ContentCut, null, tint = Saffron, modifier = Modifier.size(30.dp)) }
                        Spacer(Modifier.height(14.dp))
                        Text("NEW DUBAI", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Saffron)
                        Text("خليل الرحمن ظاهري", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("TAILOR & FABRIC STUDIO", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .75f), letterSpacing = 1.4f)
                        Spacer(Modifier.height(14.dp))
                        StitchThread(Modifier.fillMaxWidth().height(16.dp), color = Saffron)
                    }
                }
                Text(
                    tr("set"),
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(start = 22.dp, top = 18.dp, bottom = 8.dp)
                )
                ITEMS.forEach { item ->
                    val selected = route == item.route || (route.startsWith("customer") && item.route == "cust")
                    NavigationDrawerItem(
                        label = { Text(tr(item.route), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        icon = { Icon(item.icon, null) },
                        selected = selected,
                        onClick = { go(item.route) },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = cs.primary.copy(alpha = .13f),
                            selectedIconColor = cs.primary,
                            selectedTextColor = cs.primary,
                            unselectedIconColor = cs.onSurfaceVariant,
                            unselectedTextColor = cs.onSurface
                        ),
                        shape = RoundedCornerShape(15.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp).height(52.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    ) {
        Scaffold(
            containerColor = cs.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                tr(if (route.startsWith("customer")) "cust" else route),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("NEW DUBAI  •  ATELIER", style = MaterialTheme.typography.labelSmall, color = cs.secondary, letterSpacing = 1.2f)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (route != "dash") nav.popBackStack()
                            else scope.launch { drawer.open() }
                        }) {
                            Icon(
                                if (route != "dash") Icons.Default.ArrowBack else Icons.Default.Menu,
                                contentDescription = if (route != "dash") "Go back" else "Open menu",
                                tint = cs.primary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { vm.locked = true }) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock app", tint = cs.primary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = cs.surface),
                    modifier = Modifier.shadow(5.dp)
                )
            },
            bottomBar = {
                Surface(
                    color = cs.surface,
                    shadowElevation = 14.dp,
                    shape = RoundedCornerShape(topStart = 23.dp, topEnd = 23.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        PRIMARY_TABS.forEach { item ->
                            val selected = route == item.route || (route.startsWith("customer") && item.route == "cust")
                            NavigationBarItem(
                                selected = selected,
                                onClick = { go(item.route) },
                                icon = {
                                    Icon(
                                        item.icon,
                                        contentDescription = tr(item.route),
                                        modifier = Modifier.size(if (selected) 25.dp else 22.dp)
                                    )
                                },
                                label = { Text(tr(item.route), maxLines = 1, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = cs.onPrimary,
                                    selectedTextColor = cs.primary,
                                    indicatorColor = cs.primary,
                                    unselectedIconColor = cs.onSurfaceVariant,
                                    unselectedTextColor = cs.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { pad ->
            NavHost(
                navController = nav,
                startDestination = "dash",
                modifier = Modifier.padding(pad),
                enterTransition = {
                    fadeIn(tween(320)) + slideInVertically(tween(360)) { it / 12 }
                },
                exitTransition = { fadeOut(tween(180)) + slideOutVertically(tween(180)) { -it / 24 } },
                popEnterTransition = { fadeIn(tween(260)) + slideInHorizontally(tween(260)) { -it / 18 } },
                popExitTransition = { fadeOut(tween(180)) }
            ) {
                composable("dash") { DashScreen(vm) }
                composable("cust") { CustomersScreen(vm) { nav.navigate("customer/$it") } }
                composable(
                    "customer/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType })
                ) {
                    CustomerProfile(vm, it.arguments?.getLong("id") ?: 0L) { nav.popBackStack() }
                }
                composable("orders") { OrdersScreen(vm) }
                composable("meas") { MeasurementsScreen(vm) }
                composable("fabric") { FabricScreen(vm) }
                composable("cash") { CashScreen(vm) }
                composable("debt") { DebtScreen(vm) { nav.navigate("customer/$it") } }
                composable("exp") { ExpensesScreen(vm) }
                composable("rep") { ReportsScreen(vm) }
                composable("bak") { BackupScreen(vm) }
                composable("set") { SettingsScreen(vm) }
            }
        }
    }
    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text(tr("exit_title")) },
            text = { Text(tr("exit_message")) },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    (context as? Activity)?.finish()
                }) { Text(tr("exit")) }
            },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text(tr("cancel")) } }
        )
    }
}