# 21.0.0

## Changes
* Support nested signed-then-encrypted JWTs : [COMMONS-99](https://bugster.forgerock.org/jira/browse/COMMONS-99)
* Added support for the following `EncryptionMethod`s: [CREST-286](https://bugster.forgerock.org/jira/browse/CREST-286)
    - `A192CBC-HS384`: AES 192-bit in Cipher Block Chaining (CBC) mode with HMAC-SHA-384 (requires JCE Unlimited 
    Strength).
    - `A128GCM`: AES 128-bit in Galois Counter Mode (GCM). Should be more efficient than AES-CBC with HMAC, depending
     on JRE support. OpenJDK only supports GCM from Java 8 onwards.
    - `A192GCM`: AES 192-bit GCM mode.
    - `A256GCM`: AES 256-bit GCM mode.
* Added support for the following `JweAlgorithm`s: [CREST-286](https://bugster.forgerock.org/jira/browse/CREST-286)
    - `RSA-OAEP` and `RSA-OAEP-256`: RSA encryption in Optimal Asymmetric Encryption Padding (OAEP) with either SHA-1
     or SHA-256 hashing. These modes are to be preferred over PKCS#1 v1.5 padding whenever interoperability with 
     older software is not required.
    - `dir`: Direct encryption using a shared symmetric key. Provides compact and efficient encryption where the 
    consumer of JWT tokens is also the producer (so no key sharing is required). Care should be taken to ensure keys 
    are rotated appropriately.
    - `A128KW`: AES KeyWrap ([RFC 3394](https://tools.ietf.org/html/rfc3394)) using a 128-bit key encryption key 
    (KEK). This can be useful when combined with the GCM encryption methods to reduce the risk of a key/nonce pair 
    being reused, which is very bad for the security of GCM mode.
    - `A192KW`: AES KeyWrap with 192-bit keys.
    - `A256KW`: AES KeyWrap with 256-bit keys.
* Fix decompression when decrypting compressed JWEs : [COMMONS-105](
https://bugster.forgerock.org/jira/browse/COMMONS-105)
* Support compression for JWS signed JWTs : [COMMONS-106](https://bugster.forgerock.org/jira/browse/COMMONS-106)

# 3.0.4

## Changes
* `JwtClaimsSetKey` performance improved by caching lower-case claim-names and never throwing `Exception`s for flow
    control : [COMMONS-76](https://bugster.forgerock.org/jira/browse/COMMONS-76)

# 3.0.3

## Changes
* Update to BOM version 4.2.0

# 3.0.2

## Changes
* Update to BOM version 3.0.0


# 3.0.1

## Changes
* Update to BOM version 2.1.1