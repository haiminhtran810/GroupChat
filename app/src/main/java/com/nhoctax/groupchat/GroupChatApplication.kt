package com.nhoctax.groupchat

import android.app.Application
import com.nhoctax.groupchat.di.AppContainer

class GroupChatApplication : Application() {
    val container: AppContainer by lazy { AppContainer() }
}
