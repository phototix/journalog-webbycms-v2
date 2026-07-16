package com.journalog.app.feature.settings

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.journalog.app.core.network.ApiClient
import com.journalog.app.data.remote.ApiService
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit = {}
) {
    val api = remember { ApiClient.create(ApiService::class.java) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var birthdate by remember { mutableStateOf("") }
    var genderPronoun by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf("") }
    var coverUrl by remember { mutableStateOf("") }
    var selectedGenderId by remember { mutableStateOf<Int?>(null) }
    var selectedCountryId by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }

    val genders = remember { mutableStateListOf<com.journalog.app.data.remote.dto.GenderOption>() }
    val countries = remember { mutableStateListOf<com.journalog.app.data.remote.dto.CountryOption>() }

    var genderExpanded by remember { mutableStateOf(false) }
    var countryExpanded by remember { mutableStateOf(false) }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) avatarUri = uri
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) coverUri = uri
    }

    // Load profile data
    LaunchedEffect(Unit) {
        try {
            val resp = api.getProfileSettings()
            if (resp.isSuccessful) {
                val u = resp.body()?.data?.get("user")
                if (u != null) {
                    name = u.name
                    bio = u.bio ?: ""
                    location = u.location ?: ""
                    website = u.website ?: ""
                    birthdate = u.birthdate ?: ""
                    genderPronoun = u.genderPronoun ?: ""
                    username = u.username
                    avatarUrl = u.avatar ?: ""
                    coverUrl = u.cover ?: ""
                    selectedGenderId = u.genderId
                    selectedCountryId = u.countryId
                }
            }
            val gendersResp = api.getGenders()
            if (gendersResp.isSuccessful) {
                gendersResp.body()?.data?.let { genders.clear(); genders.addAll(it) }
            }
            val countriesResp = api.getCountries()
            if (countriesResp.isSuccessful) {
                countriesResp.body()?.data?.let { countries.clear(); countries.addAll(it) }
            }
        } catch (_: Exception) {}
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Cover image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { coverPicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (coverUri != null) {
                        AsyncImage(model = coverUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else if (coverUrl.isNotBlank()) {
                        AsyncImage(model = coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Text("Tap to set cover", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { avatarPicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri != null) {
                        AsyncImage(model = avatarUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else if (avatarUrl.isNotBlank()) {
                        AsyncImage(model = avatarUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Text("Avatar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Username (read-only)
                OutlinedTextField(
                    value = "@$username",
                    onValueChange = {},
                    label = { Text("Username") },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                // Full Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                // Bio
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(12.dp))

                // Date of Birth
                OutlinedTextField(
                    value = birthdate,
                    onValueChange = { birthdate = it },
                    label = { Text("Date of Birth (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                // Gender dropdown
                Box {
                    OutlinedTextField(
                        value = genders.find { it.id == selectedGenderId }?.name ?: "Select",
                        onValueChange = {},
                        label = { Text("Gender") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
                        enabled = genders.isNotEmpty()
                    )
                    if (genders.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { genderExpanded = true }
                        )
                    }
                    DropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                        genders.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g.name) },
                                onClick = { selectedGenderId = g.id; genderExpanded = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Gender Pronoun
                OutlinedTextField(
                    value = genderPronoun,
                    onValueChange = { genderPronoun = it },
                    label = { Text("Gender Pronoun") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                // Country dropdown
                Box {
                    OutlinedTextField(
                        value = countries.find { it.id == selectedCountryId }?.name ?: "Select",
                        onValueChange = {},
                        label = { Text("Country") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
                        enabled = countries.isNotEmpty()
                    )
                    if (countries.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { countryExpanded = true }
                        )
                    }
                    DropdownMenu(expanded = countryExpanded, onDismissRequest = { countryExpanded = false }) {
                        countries.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = { selectedCountryId = c.id; countryExpanded = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Location
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                // Website
                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Website URL") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(24.dp))

                // Save button
                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            error = null
                            try {
                                val body = mutableMapOf<String, Any>()
                                body["name"] = name
                                body["bio"] = bio
                                body["location"] = location
                                body["website"] = website
                                body["birthdate"] = birthdate
                                body["gender_id"] = selectedGenderId ?: 0
                                body["gender_pronoun"] = genderPronoun
                                body["country_id"] = selectedCountryId ?: 0

                                // Upload avatar if changed
                                if (avatarUri != null) {
                                    val uri = avatarUri!!
                                    val inputStream = context.contentResolver.openInputStream(uri)
                                    val bytes = inputStream?.readBytes() ?: throw Exception("Cannot read file")
                                    inputStream?.close()
                                    if (bytes.size > 2 * 1024 * 1024) {
                                        throw Exception("Avatar image must be under 2MB")
                                    }
                                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                                    val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                                    val part = MultipartBody.Part.createFormData("file", "avatar.jpg", requestBody)
                                    val uploadResp = api.uploadProfileAsset("avatar", part)
                                    if (!uploadResp.isSuccessful) {
                                        val errBody = uploadResp.errorBody()?.string()
                                        val msg = try { org.json.JSONObject(errBody).optString("message", "Upload failed") } catch (_: Exception) { "Upload failed" }
                                        throw Exception(msg)
                                    }
                                    avatarUrl = uploadResp.body()?.data?.get("assetSrc") ?: avatarUrl
                                    avatarUri = null
                                }

                                // Upload cover if changed
                                if (coverUri != null) {
                                    val uri = coverUri!!
                                    val inputStream = context.contentResolver.openInputStream(uri)
                                    val bytes = inputStream?.readBytes() ?: throw Exception("Cannot read file")
                                    inputStream?.close()
                                    if (bytes.size > 2 * 1024 * 1024) {
                                        throw Exception("Cover image must be under 2MB")
                                    }
                                    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                                    val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                                    val part = MultipartBody.Part.createFormData("file", "cover.jpg", requestBody)
                                    val uploadResp = api.uploadProfileAsset("cover", part)
                                    if (!uploadResp.isSuccessful) {
                                        val errBody = uploadResp.errorBody()?.string()
                                        val msg = try { org.json.JSONObject(errBody).optString("message", "Upload failed") } catch (_: Exception) { "Upload failed" }
                                        throw Exception(msg)
                                    }
                                    coverUrl = uploadResp.body()?.data?.get("assetSrc") ?: coverUrl
                                    coverUri = null
                                }

                                val resp = api.updateProfile(body)
                                if (resp.isSuccessful) {
                                    onSaved()
                                    onBack()
                                } else {
                                    error = "Failed to save profile"
                                }
                            } catch (e: Exception) {
                                error = e.message ?: "Error"
                            }
                            isSaving = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Save Changes")
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
