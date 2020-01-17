# JSON Web Tokens

Provides support for signed and encrypted JSON Web Tokens (JWTs, pronounce "jot"). The following signature algorithms
are supported:

* `HS256`: HMAC-SHA-256 message authentication code using a shared symmetric key.
* `HS384`: HMAC-SHA-384
* `HS512`: HMAC-SHA-512
* `RS256`: RSA with SHA-256
* `ES256`: Elliptic Curve Digital Signature Algorithm (ECDSA) with SHA-256 and the P-256 curve.
* `ES384`: ECDSA with SHA-384 and P-384.
* `ES512`: ECDSA with SHA-512 and P-521.

The following content encryption modes are supported, although some may be unavailable on different JREs:
* `A128CBC-HS256`: AES 128-bit in CBC mode with a HMAC-SHA-256-128 hash (i.e. HS256 truncated to 128 bits).
* `A192CBC-HS384`: AES 192-bit in CBC mode with HMAC-SHA-384-192.
* `A256CBC-HS512`: AES 256-bit in CBC mode with HMAC-SHA-512-256.
* `A128GCM`: AES 128-bit in GCM mode.
* `A192GCM`: AES 192-bit in GCM mode.
* `A256GCM`: AES 256-bit in GCM mode.

The following JWE key wrapping/encryption modes are supported:
* `RSA1_5`: RSA with PKCS#1 v1.5 padding.
* `RSA-OAEP`: RSA with OAEP padding and SHA-1.
* `RSA-OAEP-256`: RSA with OAEP padding and SHA-256.
* `dir`: direct encryption with a symmetric key. 
* `A128KW`: AES KeyWrap with 128-bit key.
* `A192KW`: AES KeyWrap with 192-bit key.
* `A256KW`: AES KeyWrap with 256-bit key.

## Encryption Performance

Here are some figures on the space and time usage of the supported JWE encryption algorithms. All symmetric keys used
are sized to match the algorithm (e.g. 256-bit keys for AES-256), while RSA uses 3072-bit keys to provide roughly 
equivalent security to a 128-bit symmetric key. The benchmark involved encrypting a claims set consisting of 10 
claims with the lowercase latin alphabet as the value of the claim, for a total of 375 bytes. The encryption was 
performed 1000 times and the resulting JWT size and total time is reported. No attempt has been made to control for 
JIT warmup, garbage collection and other effects, so these figures should be taken with a large pinch of salt. 

Timings were taken using Oracle JDK 1.8.0_60 on Mac OS X 10.11.5 with a 2.6GHz Intel i7 CPU.

JWE Algorithm | Encryption Method | Encryption Time for 1000 iterations (ms) | Decryption Time (ms) | JWT Size (bytes) 
------------- | ----------------- | ---------------------------------------- | -------------------- | ---------------- 
RSA1_5        | A128CBC-HS256     | 859                                      | 21692                | 1148             
RSA1_5        | A192CBC-HS384     | 532                                      | 16435                | 1158             
RSA1_5        | A256CBC-HS512     | 638                                      | 15525                | 1169             
RSA1_5        | A128GCM           | 583                                      | 15177                | 1122             
RSA1_5        | A192GCM           | 466                                      | 15208                | 1122             
RSA1_5        | A256GCM           | 517                                      | 14825                | 1122             
RSA-OAEP      | A128CBC-HS256     | 707                                      | 14886                | 1151
RSA-OAEP      | A192CBC-HS384     | 833                                      | 15109                | 1161
RSA-OAEP      | A256CBC-HS512     | 709                                      | 14923                | 1172
RSA-OAEP      | A128GCM           | 595                                      | 14946                | 1125
RSA-OAEP      | A192GCM           | 532                                      | 15171                | 1125
RSA-OAEP      | A256GCM           | 521                                      | 15070                | 1125
RSA-OAEP-256  | A128CBC-HS256     | 533                                      | 14990                | 1156
RSA-OAEP-256  | A192CBC-HS384     | 606                                      | 15091                | 1166
RSA-OAEP-256  | A256CBC-HS512     | 668                                      | 14838                | 1177
RSA-OAEP-256  | A128GCM           | 608                                      | 15333                | 1130
RSA-OAEP-256  | A192GCM           | 532                                      | 14892                | 1130
RSA-OAEP-256  | A256GCM           | 530                                      | 15035                | 1130
dir           | A128CBC-HS256     | 73                                       | 53                   | 632
dir           | A192CBC-HS384     | 67                                       | 44                   | 642
dir           | A256CBC-HS512     | 76                                       | 44                   | 653
dir           | A128GCM           | 53                                       | 44                   | 606
dir           | A192GCM           | 58                                       | 56                   | 606
dir           | A256GCM           | 59                                       | 50                   | 606
A128KW        | A128CBC-HS256     | 137                                      | 84                   | 690
A128KW        | A192CBC-HS384     | 131                                      | 59                   | 721
A128KW        | A256CBC-HS512     | 124                                      | 66                   | 753
A128KW        | A128GCM           | 97                                       | 52                   | 642
A128KW        | A192GCM           | 86                                       | 65                   | 653
A128KW        | A256GCM           | 105                                      | 83                   | 664
A192KW        | A128CBC-HS256     | 75                                       | 53                   | 690
A192KW        | A192CBC-HS384     | 71                                       | 54                   | 721
A192KW        | A256CBC-HS512     | 63                                       | 46                   | 753
A192KW        | A128GCM           | 59                                       | 45                   | 642
A192KW        | A192GCM           | 74                                       | 48                   | 653
A192KW        | A256GCM           | 68                                       | 48                   | 664
A256KW        | A128CBC-HS256     | 62                                       | 55                   | 690
A256KW        | A192CBC-HS384     | 66                                       | 47                   | 721
A256KW        | A256CBC-HS512     | 72                                       | 47                   | 753
A256KW        | A128GCM           | 58                                       | 46                   | 642
A256KW        | A192GCM           | 68                                       | 45                   | 653
A256KW        | A256GCM           | 61                                       | 47                   | 664

From these figures we can draw some general conclusions:
* RSA encryption has a huge overhead in terms of both JWT size and speed of encryption.
* Decryption speed of RSA is absolutely abysmal! As this is the dominant operation in most use-cases for JWTs, it 
should be used only when necessary.
* There is not much to choose between the GCM and CBC/HMAC modes, although encryption is slightly faster with GCM and
 it produces slightly more compact output.
* AES KeyWrap adds only minimal overhead in terms of both speed and memory usage.

## Signature Performance

Similar performance figures have been taken for just the signing algorithms. As for the encryption benchmarks, these 
are total times for 1000 iterations. NB: these results used a 2048-bit RSA key rather than the 3072-bit keys used for
encryption.

JWS Algorithm | Signing Time (ms) | Verification Time (ms) | JWT Size (bytes) 
------------- | ----------------- | ---------------------- | ----------------
NONE          | 18                | 19                     | 544
HS256         | 21                | 24                     | 588
HS384         | 21                | 28                     | 609
HS512         | 27                | 26                     | 631
RS256         | 5443              | 179                    | 887
ES256         | 911               | 1656                   | 631
ES384         | 2272              | 3977                   | 673
ES512         | 3205              | 5225                   | 721

As for encryption, public key schemes have a considerable overhead in time and space. For RSA, signing is very slow, 
while verification is "only" around 7x slower than HMAC. For ECDSA, the situation is reversed: signing is faster than
verification, but both are poor performers. The symmetric HMAC signatures are more compact and efficient than any 
other choice, adding little overhead over no signature at all, and should be preferred where it is feasible for all 
parties to share a symmetric key (i.e., when the same system is both producer and consumer).

Note that the direct symmetric signature schemes, such as `A128GCM` or `A128CBC-HS256` already incorporate a MAC and 
so it is not necessary to add an additional HMAC if you are using these directly. However, some important points 
should be noted:

* The tag size of the AES-GCM modes is fixed at 128-bits (the maximum size supported by GCM). While this should be 
sufficient it is the absolute minimum tag size for reasonable security and provides no margin of error. For comparison, 
smallest JWS signature algorithm (HS256) uses 256-bit tags.
* The AES-CBC-HMAC modes truncate their HMAC tags to half size, so e.g., `A128CBC-HS256` has a tag size of 128-bits. 
The same comments about margin of error apply here.
* The RSA encryption modes do not provide any authentication (despite the underlying block cipher being 
authenticated) as anybody with access to the *public* key can create a valid encrypted token. If authenticity (rather
than just confidentiality) is required, then a JWS signing mode *MUST* also be used.

If in doubt, consult an expert. Even if not in doubt, consult an expert :-).
