package org.forgerock.json.crypto;

import org.forgerock.json.JsonValue;
import org.forgerock.util.Function;
import org.forgerock.util.Reject;

public class JsonEncryptFunction
  implements Function<JsonValue, JsonValue, JsonCryptoException>
{
  private final JsonEncryptor encryptor;
  
  public JsonEncryptFunction(JsonEncryptor encryptor)
  {
    this.encryptor = ((JsonEncryptor)Reject.checkNotNull(encryptor));
  }
  
  public JsonValue apply(JsonValue value)
    throws JsonCryptoException
  {
    return new JsonCrypto(this.encryptor.getType(), this.encryptor.encrypt(value)).toJsonValue();
  }
}
