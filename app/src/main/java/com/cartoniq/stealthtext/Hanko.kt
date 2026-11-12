package com.cartoniq.stealthtext

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec

/** Hanko = your personal signing seal. The private key lives in the Android Keystore and never leaves it. */
object Hanko {
    private const val ALIAS = "stealthtext_hanko_v1"

    private fun store(): KeyStore {
        val k = KeyStore.getInstance("AndroidKeyStore")
        k.load(null)
        return k
    }

    private fun ensureKey() {
        val k = store()
        if (k.containsAlias(ALIAS)) return
        val gen = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        gen.initialize(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
        )
        gen.generateKeyPair()
    }

    fun publicKeyB64(): String? {
        return try {
            ensureKey()
            val cert = store().getCertificate(ALIAS)
            if (cert == null) null else Base64.encodeToString(cert.publicKey.encoded, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    fun sign(data: ByteArray): ByteArray? {
        return try {
            ensureKey()
            val pk = store().getKey(ALIAS, null) as PrivateKey
            val s = Signature.getInstance("SHA256withECDSA")
            s.initSign(pk)
            s.update(data)
            s.sign()
        } catch (e: Exception) {
            null
        }
    }

    fun verify(data: ByteArray, sig: ByteArray, pubB64: String): Boolean {
        return try {
            val pub = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.decode(pubB64.trim(), Base64.DEFAULT)))
            val s = Signature.getInstance("SHA256withECDSA")
            s.initVerify(pub)
            s.update(data)
            s.verify(sig)
        } catch (e: Exception) {
            false
        }
    }

    fun isValidKey(pubB64: String): Boolean {
        return try {
            KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.decode(pubB64.trim(), Base64.DEFAULT)))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun fingerprint(pubB64: String): String {
        return try {
            val d = MessageDigest.getInstance("SHA-256").digest(Base64.decode(pubB64.trim(), Base64.DEFAULT))
            d.take(8).joinToString("") { "%02x".format(it) }.chunked(4).joinToString(" ")
        } catch (e: Exception) {
            "?"
        }
    }

    fun deleteKey() {
        try {
            store().deleteEntry(ALIAS)
        } catch (e: Exception) {
        }
    }

    /** Returns a short result line for a revealed item, or an empty string if it was not signed. */
    fun check(ctx: Context, item: Payload.Item): String {
        val sig = item.sig ?: return ""
        val data = item.signedBytes ?: return ""
        val mine = publicKeyB64()
        if (mine != null && verify(data, sig, mine)) return tr("Verified: signed by you")
        for (c in Prefs.contacts(ctx)) {
            if (verify(data, sig, c.second)) return tr("Verified: signed by") + " " + c.first
        }
        return tr("Signed, but the signer is not in your contacts, or the message was changed.")
    }
}
