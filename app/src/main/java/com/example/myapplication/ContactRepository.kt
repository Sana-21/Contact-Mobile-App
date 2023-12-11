package com.example.myapplication

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class ContactRepository(context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver
    private val contactDao: ContactDao = AppDatabase.getDatabase(context).contactDao()

    suspend fun importContacts() {
        withContext(Dispatchers.IO) {
            val contactsFromProvider = queryContactsFromProvider()
            saveContactsToDatabase(contactsFromProvider)
        }
    }

    @SuppressLint("Range")
    private fun queryContactsFromProvider(): List<Contact> {
        val contactsList = mutableListOf<Contact>()

        val uri: Uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID))
                val name =
                    it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val number =
                    it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

                val contact = Contact(id, name, number)
                contactsList.add(contact)
            }
        }

        return contactsList
    }

    private suspend fun saveContactsToDatabase(contacts: List<Contact>) {
        contactDao.insertAll(contacts)
    }

    suspend fun getAllContacts(): List<Contact> {
        return contactDao.getAllContacts()
    }

    suspend fun updateContact(contact: Contact) {
        contactDao.updateContact(contact)
    }

    suspend fun deleteContact(contact: Contact) {
        contactDao.deleteContact(contact)
    }


}
