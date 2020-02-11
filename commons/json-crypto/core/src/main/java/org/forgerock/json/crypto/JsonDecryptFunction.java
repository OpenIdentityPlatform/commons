package org.forgerock.json.crypto;

import org.forgerock.json.JsonValue;
import org.forgerock.json.JsonValueException;
import org.forgerock.json.JsonValueFunctions;
import org.forgerock.json.JsonValueTraverseFunction;
import org.forgerock.util.Reject;

public class JsonDecryptFunction
  extends JsonValueTraverseFunction
{
  private final JsonDecryptor decryptor;
  
  public JsonDecryptFunction(JsonDecryptor decryptor)
  {
    super(JsonValueFunctions.identity());
    this.decryptor = ((JsonDecryptor)Reject.checkNotNull(decryptor));
  }
  
  protected Object traverseMap(JsonValue value)
  {
    if (JsonCrypto.isJsonCrypto(value))
    {
      JsonCrypto crypto = new JsonCrypto(value);
      if (crypto.getType().equals(this.decryptor.getType())) {
        try
        {
          JsonValue decrypted = this.decryptor.decrypt(crypto.getValue());
          
          return apply(decrypted);
        }
        catch (JsonCryptoException jce)
        {
          throw new JsonValueException(value, jce);
        }
      }
    }
    return super.traverseMap(value);
  }
}
