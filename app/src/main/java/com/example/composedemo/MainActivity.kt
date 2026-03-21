package com.example.composedemo

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.composedemo.ui.theme.ComposeDemoTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val dao = database.spendDao()
        
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            ComposeDemoTheme {
                val navController = rememberNavController()
                val userManager = remember { UserManager(this) }

                NavHost(
                    navController = navController,
                    startDestination = "splash"
                ) {
                    composable("splash") { SplashScreen(navController, userManager) }
                    composable("login") { LoginScreen(navController, userManager, dao) }
                    composable("signup") { SignupScreen(navController, userManager, dao) }
                    composable("main_flow") { MainFlowScreen(userManager, navController, dao) }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(navController: NavController, userManager: UserManager) {
    val scale = remember { Animatable(0f) }
    val yOffset = remember { Animatable(100f) }
    val opacity = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
        }
        launch {
            yOffset.animateTo(0f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
        }
        launch {
            opacity.animateTo(1f, animationSpec = tween(1200))
        }
        delay(2500)
        val loggedInUser = userManager.loggedInUser.first()
        if (loggedInUser == null) {
            navController.navigate("login") { popUpTo("splash") { inclusive = true } }
        } else {
            navController.navigate("main_flow") { popUpTo("splash") { inclusive = true } }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF3778E1), Color(0xFF1E4C9A)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = yOffset.value.dp).alpha(opacity.value)
        ) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Savings,
                    null,
                    modifier = Modifier.size(70.dp).scale(scale.value),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "Spend Analyzer",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                "Your Smart Financial Companion",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = 400f * scale.value,
                center = Offset(size.width, 0f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.03f),
                radius = 300f * scale.value,
                center = Offset(0f, size.height)
            )
        }
    }
}

@Composable
fun LoginScreen(navController: NavController, userManager: UserManager, dao: SpendDao) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp).border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    snackbarData = data
                )
            }
        },
        containerColor = Color(0xFFF8F9FE)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF3778E1), Color(0xFF4A8BF5))),
                    size = size
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(80.dp))
                Icon(
                    Icons.Rounded.AccountBalanceWallet,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    "Spend Analyzer",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Welcome Back",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                        Text(
                            "Please login to your account",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = { Icon(Icons.Rounded.Email, null, tint = Color(0xFF3778E1)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3778E1),
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.4f)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = Color(0xFF3778E1)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3778E1),
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.4f)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = {
                                if (email.isEmpty() || password.isEmpty()) {
                                    scope.launch { snackbarHostState.showSnackbar("Please fill all fields") }
                                    return@Button
                                }
                                isLoading = true
                                scope.launch {
                                    val user = dao.getUser(email)
                                    delay(1000)
                                    if (user != null && user.password == password) {
                                        userManager.setLoggedInUser(email)
                                        userManager.saveUser(user.name, user.email, user.password)
                                        navController.navigate("main_flow") { popUpTo("login") { inclusive = true } }
                                    } else {
                                        isLoading = false
                                        snackbarHostState.showSnackbar("Invalid email or password")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3778E1)),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Don't have an account? ", color = Color.Gray, fontSize = 14.sp)
                    Text(
                        "Sign Up",
                        color = Color(0xFF3778E1),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { navController.navigate("signup") }
                    )
                }
            }
        }
    }
}

@Composable
fun SignupScreen(navController: NavController, userManager: UserManager, dao: SpendDao) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp).border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    snackbarData = data
                )
            }
        },
        containerColor = Color(0xFFF8F9FE)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF3778E1), Color(0xFF4A8BF5))),
                    size = size
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color.White)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("Create Account", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text("Join thousands of smart spenders", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = { Icon(Icons.Rounded.Person, null, tint = Color(0xFF3778E1)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3778E1),
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.4f)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = { Icon(Icons.Rounded.Email, null, tint = Color(0xFF3778E1)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3778E1),
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.4f)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = Color(0xFF3778E1)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3778E1),
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.4f)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = {
                                if (email.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
                                    scope.launch { snackbarHostState.showSnackbar("Please fill all fields") }
                                    return@Button
                                }
                                isLoading = true
                                scope.launch {
                                    if (dao.getUser(email) != null) {
                                        isLoading = false
                                        snackbarHostState.showSnackbar("Email already registered")
                                    } else {
                                        dao.insertUser(UserEntity(email, fullName, password))
                                        userManager.saveUser(fullName, email, password)
                                        delay(800)
                                        navController.navigate("login")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3778E1)),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Already have an account? ", color = Color.Gray, fontSize = 14.sp)
                    Text(
                        "Sign In",
                        color = Color(0xFF3778E1),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { navController.navigate("login") }
                    )
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Rounded.Dashboard)
    object Export : Screen("export", "Export", Icons.Rounded.Share)
    object Cards : Screen("cards", "Cards", Icons.Rounded.CreditCard)
    object Profile : Screen("profile", "Profile", Icons.Rounded.Person)
}

@Composable
fun MainFlowScreen(userManager: UserManager, rootNavController: NavController, dao: SpendDao) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val userEmail by userManager.loggedInUser.collectAsState(initial = null)
    
    val spendRecordsEntity by if (userEmail != null) dao.getRecordsForUser(userEmail!!).collectAsState(initial = emptyList()) else remember { mutableStateOf(emptyList()) }
    val categoryLimitsEntity by if (userEmail != null) dao.getLimitsForUser(userEmail!!).collectAsState(initial = emptyList()) else remember { mutableStateOf(emptyList()) }
    val userCardsEntity by if (userEmail != null) dao.getCardsForUser(userEmail!!).collectAsState(initial = emptyList()) else remember { mutableStateOf(emptyList()) }

    val spendRecords = spendRecordsEntity.map { entity ->
        SpendRecord(
            id = entity.id.toString(),
            amount = entity.amount,
            category = SpendCategory.valueOf(entity.category),
            description = entity.description,
            date = entity.date
        )
    }

    val categoryLimits = remember(categoryLimitsEntity) {
        categoryLimitsEntity.associate { it.category to it.limitAmount }
    }
    
    var showAddBillDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val themeColor = Color(0xFF3778E1)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        actionColor = themeColor,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(16.dp)
                            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                        snackbarData = data
                    )
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(1.dp, Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    val screens = listOf(Screen.Dashboard, Screen.Export, Screen.Cards, Screen.Profile)
                    
                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = themeColor,
                                unselectedIconColor = Color.Gray,
                                indicatorColor = themeColor.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(navController, startDestination = Screen.Dashboard.route, modifier = Modifier.padding(innerPadding)) {
                composable(Screen.Dashboard.route) { 
                    DashboardScreen(userManager, spendRecords, categoryLimits, onDelete = { record ->
                        scope.launch {
                            val entity = spendRecordsEntity.find { it.id.toString() == record.id }
                            if (entity != null) dao.deleteRecord(entity)
                        }
                    }, onSetLimit = { cat, limit ->
                        scope.launch {
                            dao.setLimit(CategoryLimitEntity(userEmail = userEmail ?: "", category = cat.name, limitAmount = limit))
                        }
                    }) 
                }
                composable(Screen.Export.route) { ExportScreen(spendRecords, snackbarHostState) }
                composable(Screen.Cards.route) { 
                    CardsScreen(userCardsEntity, onAddCard = { card ->
                        scope.launch { dao.insertCard(card.copy(userEmail = userEmail ?: "")) }
                    }, onDeleteCard = { card ->
                        scope.launch { dao.deleteCard(card) }
                    }, userManager = userManager) 
                }
                composable(Screen.Profile.route) { 
                    ProfileScreen(userManager) {
                        rootNavController.navigate("login") {
                            popUpTo("main_flow") { inclusive = true }
                        }
                    } 
                }
            }
        }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        if (navBackStackEntry?.destination?.route == Screen.Dashboard.route) {
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 100.dp, end = 24.dp)
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    },
                onClick = { showAddBillDialog = true },
                containerColor = themeColor,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Bill", modifier = Modifier.size(28.dp))
            }
        }
    }

    if (showAddBillDialog) {
        AddBillDialog(
            onDismiss = { showAddBillDialog = false },
            onAdd = { record ->
                scope.launch {
                    dao.insertRecord(SpendRecordEntity(
                        userEmail = userEmail ?: "",
                        amount = record.amount,
                        category = record.category.name,
                        description = record.description,
                        date = record.date
                    ))
                }
                showAddBillDialog = false
            }
        )
    }
}

@Composable
fun DashboardScreen(
    userManager: UserManager, 
    records: List<SpendRecord>, 
    limits: Map<String, Double>,
    onDelete: (SpendRecord) -> Unit,
    onSetLimit: (SpendCategory, Double) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(SpendCategory.FOOD) }
    var showLimitDialog by remember { mutableStateOf<SpendCategory?>(null) }
    
    val userName by userManager.userName.collectAsState(initial = "User")

    LaunchedEffect(Unit) { visible = true }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 48.dp, bottom = 48.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column {
                Text("Hi, ${userName ?: "User"}!", fontSize = 14.sp, color = Color.Gray)
                Text("Spend Tracker", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            }
        }

        item { PromotionSlideshow() }

        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800)) + slideInVertically(initialOffsetY = { 40 })
            ) {
                SpendChart(records = records)
            }
        }

        val categoryLimitsTyped = SpendCategory.entries.associateWith { limits[it.name] ?: 0.0 }
        item { AIInsightCard(records, categoryLimitsTyped) }

        item {
            Text("Categories & Budgets", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 20.dp)
            ) {
                items(SpendCategory.entries) { category ->
                    val total = records.filter { it.category == category }.sumOf { it.amount }
                    val limit = limits[category.name] ?: 0.0
                    CategoryCard(
                        category = category,
                        totalSpent = total,
                        limit = limit,
                        isSelected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        onSetLimit = { showLimitDialog = category }
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Transaction Slips",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedCategory.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF3778E1),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val categoryRecords = records.filter { it.category == selectedCategory }.sortedByDescending { it.date }
        if (categoryRecords.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No records found", color = Color.LightGray, fontSize = 14.sp)
                }
            }
        } else {
            items(categoryRecords) { record ->
                SpendDetailItem(record, onDelete = { onDelete(record) })
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    if (showLimitDialog != null) {
        SetLimitDialog(
            category = showLimitDialog!!,
            currentLimit = limits[showLimitDialog!!.name] ?: 0.0,
            onDismiss = { showLimitDialog = null },
            onSave = { newLimit ->
                onSetLimit(showLimitDialog!!, newLimit)
                showLimitDialog = null
            }
        )
    }
}

@Composable
fun AIInsightCard(records: List<SpendRecord>, limits: Map<SpendCategory, Double>) {
    val insight = remember(records, limits) { generateAIInsight(records, limits) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF3778E1).copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F7FF)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF3778E1), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("AI Insight", fontWeight = FontWeight.Bold, color = Color(0xFF3778E1), fontSize = 14.sp)
                Text(insight, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
            }
        }
    }
}

fun generateAIInsight(records: List<SpendRecord>, limits: Map<SpendCategory, Double>): String {
    if (records.isEmpty()) return "Start adding transactions to get smart AI insights about your spending habits."
    
    val totalSpent = records.sumOf { it.amount }
    
    val violations = SpendCategory.entries.filter { cat ->
        val spent = records.filter { it.category == cat }.sumOf { it.amount }
        val limit = limits[cat] ?: 0.0
        limit > 0 && spent > limit
    }
    
    if (violations.isNotEmpty()) {
        return "Alert: You have exceeded your budget in ${violations.first().displayName}. Consider cutting back on non-essential expenses."
    }
    
    val highestCategory = SpendCategory.entries.maxByOrNull { cat ->
        records.filter { it.category == cat }.sumOf { it.amount }
    }
    
    if (highestCategory != null) {
        val catSpent = records.filter { it.category == highestCategory }.sumOf { it.amount }
        if (catSpent > totalSpent * 0.5) {
            return "Your spending on ${highestCategory.displayName} accounts for over 50% of your total budget. Look for ways to optimize."
        }
    }
    
    return "Great job! Your spending is well-distributed. Keep tracking to maintain your financial health."
}

@Composable
fun CategoryCard(
    category: SpendCategory,
    totalSpent: Double,
    limit: Double,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSetLimit: () -> Unit
) {
    val progress = if (limit > 0) (totalSpent / limit).coerceIn(0.0, 1.0).toFloat() else 0f
    val isOverLimit = limit > 0 && totalSpent > limit

    Card(
        modifier = Modifier
            .width(160.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) category.color else Color.LightGray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) category.color.copy(alpha = 0.05f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(category.color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(getCategoryIcon(category), null, tint = category.color, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onSetLimit, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Rounded.Settings, null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(category.displayName, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(
                text = "$${String.format(Locale.US, "%.0f", totalSpent)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = if (isOverLimit) Color.Red else Color.Black
            )
            
            if (limit > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Limit: $${limit.toInt()}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isOverLimit) Color.Red else Color.Black)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = if (isOverLimit) Color.Red else category.color,
                    trackColor = category.color.copy(alpha = 0.1f)
                )
            }
        }
    }
}

@Composable
fun SetLimitDialog(category: SpendCategory, currentLimit: Double, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var limitText by remember { mutableStateOf(if (currentLimit > 0) currentLimit.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set ${category.displayName} Limit", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = limitText,
                onValueChange = { limitText = it },
                label = { Text("Monthly Limit ($)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(onClick = { onSave(limitText.toDoubleOrNull() ?: 0.0) }, shape = RoundedCornerShape(12.dp)) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun SpendDetailItem(record: SpendRecord, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Discard Slip", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove this transaction slip?") },
            confirmButton = {
                Button(onClick = { onDelete(); showDeleteConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Keep") } },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(record.category.color.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(getCategoryIcon(record.category), null, tint = record.category.color, modifier = Modifier.size(20.dp))
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(record.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(record.date.format(DateTimeFormatter.ofPattern("dd MMM, yyyy")), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", record.amount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF3778E1)
                    )
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Rounded.Delete, null, tint = Color.Red.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Canvas(modifier = Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 16.dp)) {
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(0f, 0.5f),
                    end = Offset(size.width, 0.5f),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                val circleRadius = 5.dp.toPx()
                val diameter = circleRadius * 2
                val count = (size.width / (diameter + 4.dp.toPx())).toInt()
                for (i in 0 until count) {
                    drawCircle(
                        color = Color(0xFFF8FAFF),
                        radius = circleRadius,
                        center = Offset(i * (diameter + 4.dp.toPx()) + circleRadius, size.height)
                    )
                }
            }
        }
    }
}

fun getCategoryIcon(category: SpendCategory): ImageVector {
    return when(category) {
        SpendCategory.FOOD -> Icons.Rounded.Restaurant
        SpendCategory.TRANSPORT -> Icons.Rounded.DirectionsCar
        SpendCategory.RENT -> Icons.Rounded.Home
        SpendCategory.UTILITIES -> Icons.Rounded.Bolt
        SpendCategory.ENTERTAINMENT -> Icons.Rounded.Movie
        SpendCategory.OTHERS -> Icons.Rounded.Category
    }
}

data class PromoItem(val title: String, val subtitle: String, val icon: ImageVector, val color: Color)

@Composable
fun PromotionSlideshow() {
    val promos = listOf(
        PromoItem("Smart Saving", "Get 20% cashback on utilities this month!", Icons.Rounded.Star, Color(0xFF3778E1)),
        PromoItem("Premium Offer", "Unlock detailed analytics for 1 year at $9.99.", Icons.Rounded.WorkspacePremium, Color(0xFF9C27B0)),
        PromoItem("Budget Tips", "People who set limits save 15% more on average.", Icons.Rounded.Lightbulb, Color(0xFF4CAF50)),
        PromoItem("Invite Friends", "Earn $5 for every friend who signs up!", Icons.Rounded.GroupAdd, Color(0xFFFF9800))
    )
    
    val pagerState = rememberPagerState(pageCount = { promos.size })

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            val nextPage = (pagerState.currentPage + 1) % promos.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val promo = promos[page]
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                colors = CardDefaults.cardColors(containerColor = promo.color),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(modifier = Modifier.padding(24.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(promo.title, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        Text(promo.subtitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Icon(promo.icon, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(promos.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) Color(0xFF3778E1) else Color.LightGray.copy(alpha = 0.5f)
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(records: List<SpendRecord>, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedMonth by remember { mutableIntStateOf(LocalDate.now().monthValue) }
    var selectedCategory by remember { mutableStateOf<SpendCategory?>(null) }
    var expandedMonth by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }

    val filteredRecords = records.filter { it.date.monthValue == selectedMonth && (selectedCategory == null || it.category == selectedCategory) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 48.dp, bottom = 48.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text("Export Report", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(onClick = { expandedMonth = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Text(java.time.Month.of(selectedMonth).name, maxLines = 1, fontSize = 12.sp)
                    }
                    DropdownMenu(expanded = expandedMonth, onDismissRequest = { expandedMonth = false }) {
                        (1..12).forEach { m -> DropdownMenuItem(text = { Text(java.time.Month.of(m).name) }, onClick = { selectedMonth = m; expandedMonth = false }) }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(onClick = { expandedCategory = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Text(selectedCategory?.displayName ?: "All Categories", maxLines = 1, fontSize = 12.sp)
                    }
                    DropdownMenu(expanded = expandedCategory, onDismissRequest = { expandedCategory = false }) {
                        DropdownMenuItem(text = { Text("All Categories") }, onClick = { selectedCategory = null; expandedCategory = false })
                        SpendCategory.entries.forEach { cat -> DropdownMenuItem(text = { Text(cat.displayName) }, onClick = { selectedCategory = cat; expandedCategory = false }) }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Consolidated Summary", color = Color.Gray, fontSize = 14.sp)
                    Text("$${String.format(Locale.US, "%.2f", filteredRecords.sumOf { it.amount })}", fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val uri = generatePdf(context, filteredRecords, "Spend_Report_${System.currentTimeMillis()}.pdf")
                            uri?.let {
                                scope.launch {
                                    val res = snackbarHostState.showSnackbar("PDF saved to Downloads", actionLabel = "Open")
                                    if (res == SnackbarResult.ActionPerformed) openPdf(context, it)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3778E1))
                    ) {
                        Icon(Icons.Rounded.PictureAsPdf, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export as PDF")
                    }
                }
            }
        }

        items(filteredRecords) { SpendDetailItem(it, onDelete = {}) }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun CardsScreen(cards: List<CardEntity>, onAddCard: (CardEntity) -> Unit, onDeleteCard: (CardEntity) -> Unit, userManager: UserManager) {
    var showAddCardDialog by remember { mutableStateOf(false) }
    var cardToReveal by remember { mutableStateOf<CardEntity?>(null) }
    var passwordToVerify by remember { mutableStateOf<CardEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 48.dp, bottom = 48.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Your Cards", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                IconButton(
                    onClick = { showAddCardDialog = true },
                    modifier = Modifier.background(Color(0xFF3778E1).copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Rounded.Add, null, tint = Color(0xFF3778E1))
                }
            }
        }

        if (cards.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No cards saved yet", color = Color.Gray)
                }
            }
        } else {
            items(cards) { card ->
                val isRevealed = cardToReveal?.id == card.id
                CardItem(
                    card = card,
                    isRevealed = isRevealed,
                    onRevealClick = { 
                        if (isRevealed) cardToReveal = null
                        else passwordToVerify = card 
                    },
                    onDelete = { onDeleteCard(card) }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }

    if (showAddCardDialog) {
        AddCardDialog(onDismiss = { showAddCardDialog = false }, onAdd = { onAddCard(it); showAddCardDialog = false })
    }

    if (passwordToVerify != null) {
        PasswordVerificationDialog(
            userManager = userManager,
            onDismiss = { passwordToVerify = null },
            onVerified = {
                cardToReveal = passwordToVerify
                passwordToVerify = null
            }
        )
    }
}

@Composable
fun CardItem(card: CardEntity, isRevealed: Boolean, onRevealClick: () -> Unit, onDelete: () -> Unit) {
    val gradient = when (card.cardType) {
        "Visa" -> Brush.linearGradient(listOf(Color(0xFF1A237E), Color(0xFF3F51B5)))
        "Mastercard" -> Brush.linearGradient(listOf(Color(0xFFF44336), Color(0xFFFF9800)))
        else -> Brush.linearGradient(listOf(Color(0xFF37474F), Color(0xFF90A4AE)))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(gradient).padding(24.dp)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Icon(
                        if (card.cardType == "Visa") Icons.Rounded.CreditCard else Icons.Rounded.Payment,
                        null, tint = Color.White, modifier = Modifier.size(40.dp)
                    )
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete, null, tint = Color.White.copy(alpha = 0.5f))
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = if (isRevealed) card.cardNumber.chunked(4).joinToString(" ") else "**** **** **** " + card.cardNumber.takeLast(4),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("CARD HOLDER", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        Text(card.cardHolderName.uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("EXPIRES", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        Text(card.expiryDate, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    if (isRevealed) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("CVV", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                            Text(card.cvv, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            IconButton(
                onClick = onRevealClick,
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 40.dp)
            ) {
                Icon(
                    if (isRevealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    null, tint = Color.White
                )
            }
        }
    }
}

@Composable
fun AddCardDialog(onDismiss: () -> Unit, onAdd: (CardEntity) -> Unit) {
    var cardNumber by remember { mutableStateOf("") }
    var holderName by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var cardType by remember { mutableStateOf("Visa") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Card", fontWeight = FontWeight.Bold) },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = cardNumber, onValueChange = { if (it.length <= 16) cardNumber = it }, label = { Text("Card Number") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = holderName, onValueChange = { holderName = it }, label = { Text("Card Holder Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = expiry, onValueChange = { if (it.length <= 5) expiry = it }, label = { Text("MM/YY") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = cvv, onValueChange = { if (it.length <= 3) cvv = it }, label = { Text("CVV") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Visa", "Mastercard", "Other").forEach { type ->
                        FilterChip(
                            selected = cardType == type,
                            onClick = { cardType = type },
                            label = { Text(type) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (cardNumber.length == 16 && holderName.isNotEmpty() && expiry.isNotEmpty() && cvv.isNotEmpty()) {
                    onAdd(CardEntity(userEmail = "", cardNumber = cardNumber, cardHolderName = holderName, expiryDate = expiry, cvv = cvv, cardType = cardType))
                }
            }, shape = RoundedCornerShape(12.dp)) { Text("Save Card") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PasswordVerificationDialog(userManager: UserManager, onDismiss: () -> Unit, onVerified: () -> Unit) {
    var password by remember { mutableStateOf("") }
    val savedPassword by userManager.userPassword.collectAsState(initial = "")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Verify Identity", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Please enter your account password to reveal card details.", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (password == savedPassword) onVerified()
            }, shape = RoundedCornerShape(12.dp)) { Text("Verify") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun ProfileScreen(userManager: UserManager, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    val email by userManager.userEmail.collectAsState(initial = "")
    val name by userManager.userName.collectAsState(initial = "")

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FE))) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF3778E1), RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .padding(vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Person, null, modifier = Modifier.size(60.dp), tint = Color.White)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(name ?: "User Name", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text(email ?: "email@example.com", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }

        Column(modifier = Modifier.padding(24.dp)) {
            Text("Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    ProfileItem(Icons.Rounded.Edit, "Edit Profile")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.2f))
                    ProfileItem(Icons.Rounded.Notifications, "Notifications")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.2f))
                    ProfileItem(Icons.Rounded.Security, "Security")
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.2f))
                    ProfileItem(Icons.AutoMirrored.Rounded.Help, "Help & Support")
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = { 
                    scope.launch { 
                        userManager.setLoggedInUser(null)
                        onLogout()
                    } 
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.08f), contentColor = Color.Red),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfileItem(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF3778E1).copy(alpha = 0.05f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color(0xFF3778E1), modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBillDialog(onDismiss: () -> Unit, onAdd: (SpendRecord) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(SpendCategory.FOOD) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var expandedCat by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Transaction", fontWeight = FontWeight.Bold) },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        text = {
            Column {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount ($)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Box {
                    OutlinedButton(onClick = { expandedCat = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(selectedCategory.displayName)
                            Icon(Icons.Rounded.ArrowDropDown, null)
                        }
                    }
                    DropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                        SpendCategory.entries.forEach { cat -> DropdownMenuItem(text = { Text(cat.displayName) }, onClick = { selectedCategory = cat; expandedCat = false }) }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text(selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (amt > 0) onAdd(SpendRecord(amount = amt, category = selectedCategory, description = description, date = selectedDate))
            }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3778E1))) { Text("Save Slip") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { selectedDate = java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = dateState) }
    }
}

fun generatePdf(context: Context, records: List<SpendRecord>, fileName: String): Uri? {
    val pdfDocument = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    
    val themeColor = "#3778E1".toColorInt()
    val lightThemeColor = "#F0F7FF".toColorInt()
    val tableHeaderColor = "#F9FAFB".toColorInt()
    
    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas
    val paint = Paint()

    paint.color = themeColor
    canvas.drawRect(0f, 0f, pageWidth.toFloat(), 140f, paint)
    
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 28f
    paint.isFakeBoldText = true
    canvas.drawText("Spend Analyzer", 40f, 65f, paint)
    
    paint.textSize = 12f
    paint.isFakeBoldText = false
    canvas.drawText("Consolidated Expense Report", 40f, 90f, paint)
    
    paint.textAlign = Paint.Align.RIGHT
    paint.textSize = 10f
    canvas.drawText("Generated on: ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}", pageWidth - 40f, 65f, paint)
    paint.textAlign = Paint.Align.LEFT

    paint.color = lightThemeColor
    canvas.drawRoundRect(40f, 160f, pageWidth - 40f, 250f, 20f, 20f, paint)
    
    paint.color = themeColor
    paint.textSize = 11f
    paint.isFakeBoldText = true
    canvas.drawText("TOTAL EXPENDITURE", 65f, 195f, paint)
    
    paint.textSize = 32f
    val total = records.sumOf { it.amount }
    canvas.drawText("$${String.format(Locale.US, "%.2f", total)}", 65f, 230f, paint)
    
    paint.color = android.graphics.Color.GRAY
    paint.textSize = 12f
    paint.isFakeBoldText = false
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("${records.size} Transactions Recorded", pageWidth - 65f, 220f, paint)
    paint.textAlign = Paint.Align.LEFT

    var y = 300f
    paint.color = tableHeaderColor
    canvas.drawRect(40f, y - 20f, pageWidth - 40f, y + 10f, paint)
    
    paint.color = android.graphics.Color.DKGRAY
    paint.textSize = 10f
    paint.isFakeBoldText = true
    canvas.drawText("DATE", 50f, y, paint)
    canvas.drawText("CATEGORY", 130f, y, paint)
    canvas.drawText("DESCRIPTION", 250f, y, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("AMOUNT", pageWidth - 50f, y, paint)
    paint.textAlign = Paint.Align.LEFT
    
    y += 35f
    paint.isFakeBoldText = false
    paint.textSize = 11f

    records.forEachIndexed { index, record ->
        if (y > pageHeight - 80) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            
            paint.color = themeColor
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 40f, paint)
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 14f
            paint.isFakeBoldText = true
            canvas.drawText("Spend Analyzer Report (Cont.)", 40f, 25f, paint)
            
            y = 80f
            paint.color = tableHeaderColor
            canvas.drawRect(40f, y - 20f, pageWidth - 40f, y + 10f, paint)
            paint.color = android.graphics.Color.DKGRAY
            paint.textSize = 10f
            paint.isFakeBoldText = true
            canvas.drawText("DATE", 50f, y, paint)
            canvas.drawText("CATEGORY", 130f, y, paint)
            canvas.drawText("DESCRIPTION", 250f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("AMOUNT", pageWidth - 50f, y, paint)
            paint.textAlign = Paint.Align.LEFT
            y += 35f
            paint.isFakeBoldText = false
            paint.textSize = 11f
        }
        
        if (index % 2 != 0) {
            paint.color = "#FAFAFA".toColorInt()
            canvas.drawRect(40f, y - 22f, pageWidth - 40f, y + 8f, paint)
        }
        
        paint.color = android.graphics.Color.GRAY
        canvas.drawText(record.date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")), 50f, y, paint)
        
        paint.color = android.graphics.Color.BLACK
        canvas.drawText(record.category.displayName, 130f, y, paint)
        
        val desc = if (record.description.length > 25) record.description.take(22) + "..." else record.description
        canvas.drawText(desc, 250f, y, paint)
        
        paint.textAlign = Paint.Align.RIGHT
        paint.isFakeBoldText = true
        paint.color = themeColor
        canvas.drawText("$${String.format(Locale.US, "%.2f", record.amount)}", pageWidth - 50f, y, paint)
        paint.textAlign = Paint.Align.LEFT
        paint.isFakeBoldText = false
        
        y += 30f
    }
    
    paint.color = android.graphics.Color.LTGRAY
    canvas.drawLine(40f, pageHeight - 50f, pageWidth - 40f, pageHeight - 50f, paint)
    
    paint.textSize = 8f
    paint.color = android.graphics.Color.GRAY
    canvas.drawText("Report generated by Spend Analyzer Mobile App", 40f, pageHeight - 35f, paint)
    paint.textAlign = Paint.Align.RIGHT
    canvas.drawText("Page $pageNumber", pageWidth - 40f, pageHeight - 35f, paint)

    pdfDocument.finishPage(page)
    
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }
    val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
    try {
        uri?.let { context.contentResolver.openOutputStream(it)?.use { os -> pdfDocument.writeTo(os) } }
        return uri
    } catch (_: Exception) { return null } finally { pdfDocument.close() }
}

fun openPdf(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try { context.startActivity(Intent.createChooser(intent, "Open Report")) } catch (_: Exception) {}
}
