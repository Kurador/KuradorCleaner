package com.kurador.cleaner

import android.app.Application
import com.topjohnwu.superuser.Shell

class KuradorCleanerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER or Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(120)
        )
    }
}
