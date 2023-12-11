package com.example.myapplication

sealed class ContactAction {
    object Call : ContactAction()
    object SendSMS : ContactAction()
    object Edit : ContactAction()
    object Delete : ContactAction()
}
