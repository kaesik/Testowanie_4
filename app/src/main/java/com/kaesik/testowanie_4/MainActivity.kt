package com.kaesik.testowanie_4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.room.*
import kotlinx.coroutines.launch

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firstName: String,
    val lastName: String,
    val birthDate: String,
    val phone: String,
    val email: String,
    val address: String
)

@Dao
interface PersonDao {
    @Query("SELECT * FROM people ORDER BY id DESC")
    suspend fun getAll(): List<PersonEntity>

    @Insert
    suspend fun insert(person: PersonEntity): Long

    @Update
    suspend fun update(person: PersonEntity): Int

    @Query("DELETE FROM people WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}

@Database(entities = [PersonEntity::class], version = 1)
abstract class AppDb : RoomDatabase() {
    abstract fun personDao(): PersonDao
}

private fun buildDb(context: android.content.Context): AppDb {
    return Room.databaseBuilder(context, AppDb::class.java, "people.db")
        .fallbackToDestructiveMigration()
        .build()
}

private enum class Screen(val title: String) {
    HOME("Ekran główny"),
    ADD("Dodaj osobę"),
    LIST("Lista osób"),
    DELETE("Usuń osobę"),
    EDIT("Edytuj osobę")
}

private data class NavState(
    val screen: Screen,
    val selectedId: Long? = null
)

@ExperimentalMaterial3Api
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OneFilePeopleApp() }
    }
}

@ExperimentalMaterial3Api
@Composable
private fun OneFilePeopleApp() {
    val context = LocalContext.current
    val db = remember { buildDb(context.applicationContext) }
    val dao = remember { db.personDao() }

    val backStack = remember { mutableStateListOf(NavState(Screen.HOME)) }
    val current = backStack.last()

    fun navigate(screen: Screen, selectedId: Long? = null) {
        if (current.screen == screen && current.selectedId == selectedId) return
        backStack.add(NavState(screen, selectedId))
    }

    fun popBack() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    BackHandler(enabled = backStack.size > 1) { popBack() }

    val scheme = blueGrayScheme()

    MaterialTheme(colorScheme = scheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(current.screen.title) },
                    navigationIcon = {
                        if (backStack.size > 1) {
                            Text(
                                "←",
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .clickable { popBack() },
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                BottomTabs(
                    current = current.screen,
                    onSelect = { screen ->
                        when (screen) {
                            Screen.ADD, Screen.LIST, Screen.DELETE -> navigate(screen)
                            else -> navigate(Screen.HOME)
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (current.screen) {
                    Screen.HOME -> HomeScreen(
                        onAdd = { navigate(Screen.ADD) },
                        onList = { navigate(Screen.LIST) },
                        onDelete = { navigate(Screen.DELETE) }
                    )

                    Screen.ADD -> AddPersonScreen(
                        dao = dao,
                        onDone = { popBack() }
                    )

                    Screen.LIST -> ListPeopleScreen(
                        dao = dao,
                        onEdit = { id -> navigate(Screen.EDIT, id) }
                    )

                    Screen.DELETE -> DeletePeopleScreen(
                        dao = dao
                    )

                    Screen.EDIT -> EditPersonScreen(
                        dao = dao,
                        personId = current.selectedId,
                        onDone = { popBack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomTabs(current: Screen, onSelect: (Screen) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        NavigationBarItem(
            selected = current == Screen.ADD,
            onClick = { onSelect(Screen.ADD) },
            label = { Text("Dodaj") },
            icon = { Text("+") }
        )
        NavigationBarItem(
            selected = current == Screen.LIST || current == Screen.EDIT,
            onClick = { onSelect(Screen.LIST) },
            label = { Text("Lista") },
            icon = { Text("≡") }
        )
        NavigationBarItem(
            selected = current == Screen.DELETE,
            onClick = { onSelect(Screen.DELETE) },
            label = { Text("Usuń") },
            icon = { Text("🗑") }
        )
    }
}

@Composable
private fun HomeScreen(onAdd: () -> Unit, onList: () -> Unit, onDelete: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Dane osobowe", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Imię, nazwisko, data urodzenia, telefon, e-mail, adres.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text("Dodaj osobę")
        }
        Button(onClick = onList, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text("Wyświetl listę danych")
        }
        OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Text("Usuń osobę")
        }
    }
}

@Composable
private fun AddPersonScreen(dao: PersonDao, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    fun validate(): String? {
        if (firstName.isBlank()) return "Brak imienia"
        if (lastName.isBlank()) return "Brak nazwiska"
        if (birthDate.isBlank()) return "Brak daty urodzenia"
        if (phone.isBlank()) return "Brak telefonu"
        if (email.isBlank() || !email.contains("@")) return "Błędny e-mail"
        if (address.isBlank()) return "Brak adresu"
        return null
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FormField("Imię", firstName) { firstName = it }
        FormField("Nazwisko", lastName) { lastName = it }
        FormField("Data urodzenia (YYYY-MM-DD)", birthDate) { birthDate = it }
        FormField(
            label = "Numer telefonu",
            value = phone,
            keyboardType = KeyboardType.Phone
        ) { phone = it }
        FormField(
            label = "E-mail",
            value = email,
            keyboardType = KeyboardType.Email
        ) { email = it }
        FormField("Adres", address) { address = it }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                onClick = {
                    status = null
                    val err = validate()
                    if (err != null) {
                        status = err
                        return@Button
                    }
                    scope.launch {
                        dao.insert(
                            PersonEntity(
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                birthDate = birthDate.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                address = address.trim()
                            )
                        )
                        status = "Zapisano"
                        firstName = ""
                        lastName = ""
                        birthDate = ""
                        phone = ""
                        email = ""
                        address = ""
                    }
                }
            ) { Text("Zapisz") }

            OutlinedButton(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                onClick = onDone
            ) { Text("Wróć") }
        }

        if (status != null) {
            Text(status!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ListPeopleScreen(dao: PersonDao, onEdit: (Long) -> Unit) {
    val scope = rememberCoroutineScope()
    var people by remember { mutableStateOf<List<PersonEntity>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    fun reload() {
        error = null
        scope.launch {
            try {
                people = dao.getAll()
            } catch (_: Exception) {
                error = "Błąd odczytu bazy"
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(shape = RoundedCornerShape(14.dp), onClick = { reload() }) { Text("Odśwież") }
            Text(
                "Rekordy: ${people.size}",
                modifier = Modifier.align(Alignment.CenterVertically),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))

        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
            return
        }

        if (people.isEmpty()) {
            Text("Brak danych.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }

        Text(
            "Kliknij kartę, aby edytować",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(people) { p ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEdit(p.id) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${p.firstName} ${p.lastName}", style = MaterialTheme.typography.titleMedium)
                        Text("id=${p.id} • ur.: ${p.birthDate}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("tel: ${p.phone}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("email: ${p.email}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("adres: ${p.address}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditPersonScreen(dao: PersonDao, personId: Long?, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()

    var loaded by remember { mutableStateOf(false) }
    var person by remember { mutableStateOf<PersonEntity?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    fun validate(): String? {
        if (firstName.isBlank()) return "Brak imienia"
        if (lastName.isBlank()) return "Brak nazwiska"
        if (birthDate.isBlank()) return "Brak daty urodzenia"
        if (phone.isBlank()) return "Brak telefonu"
        if (email.isBlank() || !email.contains("@")) return "Błędny e-mail"
        if (address.isBlank()) return "Brak adresu"
        return null
    }

    LaunchedEffect(personId) {
        loaded = false
        error = null
        status = null
        person = null

        if (personId == null) {
            error = "Brak ID rekordu"
            loaded = true
            return@LaunchedEffect
        }

        try {
            val all = dao.getAll()
            val found = all.firstOrNull { it.id == personId }
            if (found == null) {
                error = "Nie znaleziono rekordu id=$personId"
            } else {
                person = found
                firstName = found.firstName
                lastName = found.lastName
                birthDate = found.birthDate
                phone = found.phone
                email = found.email
                address = found.address
            }
        } catch (_: Exception) {
            error = "Błąd odczytu bazy"
        } finally {
            loaded = true
        }
    }

    if (!loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (error != null) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
            OutlinedButton(shape = RoundedCornerShape(14.dp), onClick = onDone) { Text("Wróć") }
        }
        return
    }

    val currentPerson = person ?: run {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Brak danych do edycji", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(shape = RoundedCornerShape(14.dp), onClick = onDone) { Text("Wróć") }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Edycja rekordu id=${currentPerson.id}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FormField("Imię", firstName) { firstName = it }
        FormField("Nazwisko", lastName) { lastName = it }
        FormField("Data urodzenia (YYYY-MM-DD)", birthDate) { birthDate = it }
        FormField("Numer telefonu", phone, keyboardType = KeyboardType.Phone) { phone = it }
        FormField("E-mail", email, keyboardType = KeyboardType.Email) { email = it }
        FormField("Adres", address) { address = it }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                onClick = {
                    status = null
                    val err = validate()
                    if (err != null) {
                        status = err
                        return@Button
                    }

                    scope.launch {
                        val updated = currentPerson.copy(
                            firstName = firstName.trim(),
                            lastName = lastName.trim(),
                            birthDate = birthDate.trim(),
                            phone = phone.trim(),
                            email = email.trim(),
                            address = address.trim()
                        )
                        val rows = dao.update(updated)
                        status = if (rows > 0) "Zapisano zmiany" else "Nie zapisano"
                    }
                }
            ) { Text("Zapisz zmiany") }

            OutlinedButton(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                onClick = onDone
            ) { Text("Wróć") }
        }

        if (status != null) {
            Text(status!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DeletePeopleScreen(dao: PersonDao) {
    val scope = rememberCoroutineScope()
    var people by remember { mutableStateOf<List<PersonEntity>>(emptyList()) }
    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun reload() {
        error = null
        scope.launch {
            try {
                people = dao.getAll()
            } catch (_: Exception) {
                error = "Błąd odczytu bazy"
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(shape = RoundedCornerShape(14.dp), onClick = { reload() }) { Text("Odśwież") }
            if (status != null) {
                Text(status!!, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterVertically))
            }
        }

        Spacer(Modifier.height(12.dp))

        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
            return
        }

        if (people.isEmpty()) {
            Text("Brak rekordów do usunięcia.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }

        Text("Kliknij rekord, aby usunąć", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(people) { p ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            status = null
                            scope.launch {
                                val rows = dao.deleteById(p.id)
                                status = if (rows > 0) "Usunięto id=${p.id}" else "Nie usunięto id=${p.id}"
                                reload()
                            }
                        },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${p.firstName} ${p.lastName}", style = MaterialTheme.typography.titleMedium)
                        Text("id=${p.id}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("email: ${p.email}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("tel: ${p.phone}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun blueGrayScheme(): ColorScheme {
    val primary = Color(0xFF2E5B86)
    val onPrimary = Color(0xFFFFFFFF)
    val secondary = Color(0xFF5B7A99)
    val onSecondary = Color(0xFFFFFFFF)

    val background = Color(0xFFF2F4F7)
    val onBackground = Color(0xFF0E1116)

    val surface = Color(0xFFE7ECF2)
    val onSurface = Color(0xFF121821)
    val onSurfaceVariant = Color(0xFF3B4654)

    val outline = Color(0xFF98A6B5)
    val error = Color(0xFFB3261E)

    return lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        error = error
    )
}
