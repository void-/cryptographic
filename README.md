Crytographic
===========
Public key cryptography layered on sms communication
Messaging is intended to be as seamless as possible
Communication currently works on GSM phone

Real Life Usage
==============
* Build and install application
* Find someone to send sms's to
* Exchange public keys, in person, via NFC
* Use application to send sms encrypted under the recipient's public key

Technical details
================
* Currently send single(not multipart) messages 122 bytes in plaintext length
* Encrypted under 1064 bit(133 bytes) RSA with PKCS#1 padding exponent 65537
* Base128 encoded ciphertext is viewable via native sms application

Current Limitations
==================
* NFC completely untested
* No authentication or integrity is provided; only confidentiality
* No perfect forward secrecy is guaranteed
* All sent and received messages are stored unencrypted on the mobile device
* Untested on CDMA phones

Building
=======
* Use ant(1.8.2 used for development)
* There are no external dependencies
* Requires Android 4.0+ (mainly for android beam and RSA PKCS#1 encryption)
