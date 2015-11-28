CSV AuditEventHandler
=====================

The `CSVAuditEventHandler` writes audit events to CSV files.
It supports plain CSV files and tamper-evident CSV files.
Tamper-evident CSV files allow you to detect tampering afterwards.

## How CSV Files Become Tamper-Evident

To make CSV files tamper-evident, you create an initial `JCEKS` keystore,
and supply the keystore path and password
through the audit event handler configuration.

This initial keystore must contain the following entries:
* `SIGNATURE`: a pair of keys used to sign CSV files periodically
* `PASSWORD`: a SecretKey (HmacSHA256) used as the password for another keystore
    that the audit event handler creates alongside CSV files.
    These additional keystores are read-only.

The keystore password and the private key passwords must be the same.

Tamper-evident CSV files have two additional columns: `HMAC` and `SIGNATURE`.

Before writing to a CSV file, the audit event handler creates another keystore
named *csv-file-name*`.keystore`.
The keystore password is the `PASSWORD` secret key from the initial keystore.

This new `JCEKS`-type keystore is initialized with an `InitialKey` entry.
The `InitialKey` is a randomly computed secret key seed used for HMAC chaining
when signing the corresponding CSV file.
The same secret key is stored under the alias `CurrentKey`.

For each row written to the CSV file,
the audit event handler fills Base64-encoded HMAC cell for each entry,
where the value is calculated from the `InitialKey` for the first row,
and from the `CurrentKey` and the row's data for subsequent rows.
Then the audit event handler computes a SHA256 checksum of the `CurrentKey` value,
and writes it to the keystore as the new `CurrentKey` value.

In addition, the audit event handler periodically inserts special "signature rows".
Signature rows' cells are all empty, except for the signature row.
The signature cell value is a Base64-encoded signature
composed of the latest HMAC and the latest signature
(unless this is the first signature row).
The signature is then stored in the keystore under the alias `CurrentSignature`.
This makes is possible to verify that the file has not been truncated.

Furthermore, the signatures play a key role when the rotation is enabled.
On rotation, and before the file is rotated, the audit event handler checks
that the last row of the rotated file is a signature row.
To ensure that no previously rotated files have been tampered with,
the last signature row from the rotated file
is inserted as the second row of the new file.
(The first row is the header row.)

## To Create the Initial Keystore

Use the following keytool commands to create the initial keystore
used when configuring the  `CsvAuditEventHandler`:

    $ keytool \
      -genkeypair \
      -alias "Signature" \
      -dname CN=a \
      -keystore src/test/resources/keystore-signature.jks \
      -storepass password \
      -storetype JCEKS \
      -keypass password \
      -keyalg RSA \
      -sigalg SHA256withRSA
    $ keytool \
      -genseckey \
      -alias "Password" \
      -keystore src/test/resources/keystore-signature.jks \
      -storepass password \
      -storetype JCEKS \
      -keypass password \
      -keyalg HmacSHA256 \
      -keysize 256

List the keystore's content to make sure it contains the `Signature` key pair
and the `Password` secret key:

    $ keytool \
      -list \
      -keystore src/test/resources/keystore-signature.jks \
      -storepass password \
      -storetype JCEKS
