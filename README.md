Crytographic
===========
* Public key cryptography layered on sms communication
* Messaging is intended to be as seamless as possible
* Communication currently works on GSM phone

Real Life Usage
==============
* Build and install application
* Find someone to send sms's to, i.e. another Cryptographic user
* Exchange public keys, in person, via Bluetooth
* Use application to send sms encrypted under the recipient's public key

Technical details
================
* Currently send single(not multipart) messages 122 bytes in plaintext length
* Encrypted under 1064 bit(133 bytes) RSA with PKCS#1 padding exponent 65537
* Base128 encoded ciphertext is viewable via native sms application

Current Limitations
==================
* No authentication; only confidentiality, limited integrity with PKCS#1
* No perfect forward secrecy is guaranteed
* All sent and received messages are stored unencrypted on the mobile device
* Untested on CDMA phones
* NFC development stalled

Building
=======
* Use ant(1.8.2 used for development)
* There are no external dependencies
* Requires Android 4.0+ (mainly for android beam and RSA PKCS#1 encryption)
