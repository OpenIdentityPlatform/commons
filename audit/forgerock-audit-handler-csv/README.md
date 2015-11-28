CSV AuditEventHandler
=====================

This is the AuditEventHandler that outputs the audit events into some CSV files. It supports plain CSV files but also some secure CSV files, that allows to check afterwards if they were tampered or not.

## How does the secure CSV files work ?

When enabling the secure CSV files, you need to provide through the configuration a file path to a an existing keystore and a password to open that keystore. 
This keystore has to contain the following entries : 
* `SIGNATURE` : it contains a pair of keys that will be used to insert periodically some signatures into the CSV files
* `PASSWORD` : it contains a SecretKey (HmacSHA256), that will be used as the password to create the keystore that will be created besides the CSV file
This keystore will be used a read-only.

Two new columns are added automatically into the CSV files; they are named : `HMAC` and `SIGNATURE`.
Prior to write the CSV file, another file named with the same name as the CSV file plus the suffix `.keystore` is created. The type of this keystore is `JCEKS` and the password to access it is the password contained in the entry named `PASSWORD` from the keystore provided from the configuration.
This keystore is initialized with an entry named `InitialKey` : this will be the seed for HMAC chaining for this file; it is computed randomly. This value is also stored under the alias `CurrentKey`

For each row written into the CSV file, then a Base64 encoded HMAC cell is filled for each entry : it is calculated from the the `InitialKey` if this is the first row, or from the `CurrentKey` if this is not the first row, and from the row's data. 
Then we compute an transformation of the key by computing a SHA256 checksum of the `CurrentKey`, and this key is written into the created keystore under the alias `CurrentKey`. 

On top of that, some special rows are inserted periodically. These rows are named "signature rows". Their content is quite simple : all the cells are empty but the signature one. This cell is filled with a Base64 encoded signature composed from the latest HMAC and the lasted signature if this is not the first one. This signature is then stored into the created keystore under the alias `CurrentSignature`. This will allow to ensure that the file was not truncated while verifying it.

Furthermore, these signatures have a key role when the rotation is enabled. When the rotation happens, we ensure that the last row of the rotated file is a signature row then the file is rotated. And to ensure that there was no tampered file in the list of previous rotated files, the last signature computed of the rotated file is inserted as the second line of the new file. (The first row is the header one.)

## Create the keystore 

Here is the keytool commands to create the keystore to provide to the CsvAuditEventHandler through the configuration.

    $ keytool -genkeypair -alias "Signature" -dname CN=a -keystore src/test/resources/keystore-signature.jks -storepass password -storetype JCEKS -keypass password -keyalg RSA -sigalg SHA256withRSA
    $ keytool -genseckey -alias "Password" -keystore src/test/resources/keystore-signature.jks -storepass password -storetype JCEKS -keypass password -keyalg HmacSHA256 -keysize 256

Then list the keystore's content to ensure what is inside.

    $ keytool -list -keystore src/test/resources/keystore-signature.jks -storepass password -storetype JCEKS

