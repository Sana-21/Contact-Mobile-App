package com.example.myapplication

//import androidx.datastore.core.
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

//import com.example.mycontactapplication.Manifest

class MainActivity : ComponentActivity() {

    private lateinit var contactRepository: ContactRepository
    private var contacts by mutableStateOf(emptyList<Contact>())
    private var selectedContactId by mutableStateOf(0L)
    private var isEditDialogVisible by mutableStateOf(false)


    fun makeCall(phoneNumber: String, context: Context) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        context.startActivity(intent)
    }

    fun sendSMS(phoneNumber: String, context: Context) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("sms:$phoneNumber")
        }
        context.startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        contactRepository = ContactRepository(this)

        importContacts()

        setContent {
            MyApplicationTheme(content = {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting(context = this)
                }
            }, typography = MaterialTheme.typography)
        }

        // Check and request contacts permission at runtime
        checkAndRequestContactsPermission()
    }

    private var isContactsImported by mutableStateOf(false)

    private fun importContacts() {
        if (!isContactsImported) {
            lifecycleScope.launch {
                contactRepository.importContacts()
                contacts = contactRepository.getAllContacts()
            }
            isContactsImported = true
        }
    }

    private val READ_CONTACTS_PERMISSION_REQUEST_CODE = 1001
    private val WRITE_CONTACTS_PERMISSION_REQUEST_CODE = 1002

    private fun checkAndRequestContactsPermission() {
        // Check if the READ_CONTACTS permission is granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request the READ_CONTACTS permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                READ_CONTACTS_PERMISSION_REQUEST_CODE
            )
        } else {
            // Check if the WRITE_CONTACTS permission is granted
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_CONTACTS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request the WRITE_CONTACTS permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_CONTACTS),
                    WRITE_CONTACTS_PERMISSION_REQUEST_CODE
                )
            } else {
                // Both permissions are granted, proceed with accessing the contacts provider
                importContacts()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            READ_CONTACTS_PERMISSION_REQUEST_CODE,
            WRITE_CONTACTS_PERMISSION_REQUEST_CODE -> {
                // Check if the permission is granted
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with accessing the contacts provider
                    importContacts()
                } else {
                    // Permission denied, handle accordingly (e.g., show a message to the user)
                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }


    @Composable
    private fun Greeting(context: Context) {

        Text(
            text = "Contacts",
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Column {
            LazyColumn(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f) // Use weight to allow LazyColumn to take the remaining space
            ) {
                item {
                    // This is a placeholder item that acts as a spacer
                    Spacer(modifier = Modifier.height(50.dp))
                }
                items(contacts.size) { index ->
                    val contact = contacts[index]
                    ContactItem(
                        contact = contact,
                        context = context,
                        onCallClick = { makeCall(contact.phoneNumber, context) },
                        onSendSmsClick = { sendSMS(contact.phoneNumber, context) },
                        onEditClick = {
                            selectedContactId = contact.id
                            isEditDialogVisible = true
                        },
                        onDeleteClick = {
                            lifecycleScope.launch {
                                contactRepository.deleteContact(contact)
                                contacts = contactRepository.getAllContacts()
                            }
                        }
                    )
                }
            }

            if (isEditDialogVisible) {
                EditContactDialog(
                    onDismiss = {
                        isEditDialogVisible = false
                    },
                    onSave = { newName: String, newNumber: String ->
                        lifecycleScope.launch {
                            val editedContact = Contact(
                                id = selectedContactId,
                                name = newName,
                                phoneNumber = newNumber
                            )
                            contactRepository.updateContact(editedContact)
                            contacts = contactRepository.getAllContacts()
                        }
                        isEditDialogVisible = false
                    }
                )
            }
        }
    }


    @Composable
    fun MyApp(context: Context, content: @Composable () -> Unit) {
        MyApplicationTheme(
            typography = MaterialTheme.typography,
            content = content
        )
    }

    @Composable
    private fun ContactItem(
        contact: Contact,
        context: Context,
        onCallClick: () -> Unit,
        onSendSmsClick: () -> Unit,
        onEditClick: () -> Unit,
        onDeleteClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Name: ${contact.name}\nPhone: ${contact.phoneNumber}",
                style = MaterialTheme.typography.headlineSmall,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                ContactActionButton(Icons.Default.Call) {
                    onCallClick()
                }
                ContactActionButton(Icons.Default.Email) {
                    onSendSmsClick()
                }
                ContactActionButton(Icons.Default.Edit) {
                    onEditClick()
                }
                ContactActionButton(Icons.Default.Delete) {
                    onDeleteClick()
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }


    @Composable
    private fun ContactActionButton(icon: ImageVector, onClick: () -> Unit) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun EditContactDialog(
        onDismiss: () -> Unit,
        onSave: (newName: String, newNumber: String) -> Unit
    ) {
        var newName by remember { mutableStateOf(TextFieldValue()) }
        var newNumber by remember { mutableStateOf(TextFieldValue()) }

        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text("Edit Contact") },
            text = {
                Column {
                    TextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New Name") }
                    )
                    TextField(
                        value = newNumber,
                        onValueChange = { newNumber = it },
                        label = { Text("New Number") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSave(newName.text, newNumber.text)
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Save")
                }
            },
            dismissButton = {
                Button(
                    onClick = { onDismiss() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Cancel")
                }
            }
        )
    }
}