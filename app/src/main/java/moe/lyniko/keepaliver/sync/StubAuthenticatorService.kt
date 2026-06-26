package moe.lyniko.keepaliver.sync

import android.app.Service
import android.content.Intent
import android.os.IBinder

class StubAuthenticatorService : Service() {

    private val authenticator: StubAuthenticator by lazy {
        StubAuthenticator(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return authenticator.iBinder
    }
}
