package utilities;

import io.restassured.path.json.JsonPath;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

//Reads JWT claims and builds invalid tokens for security tests. A JWT is header.payload.signature, each Base64URL
public final class JwtUtils {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final String WRONG_SECRET = "this-is-not-the-platform-secret-0123456789";

    private JwtUtils() {
    }

    public static JsonPath claims(String token) {
        return JsonPath.from(decode(part(token, 1)));
    }

    //Same claims, signed with a secret the platform does not know
    public static String signedWithWrongSecret(String token) {
        String unsigned = part(token, 0) + "." + part(token, 1);
        return unsigned + "." + hmacSha256(unsigned, WRONG_SECRET);
    }

    //Same claims, header says "no signature needed"
    public static String withAlgNone(String token) {
        String header = encode("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        return header + "." + part(token, 1) + ".";
    }

    //Payload changed to another subject, original signature kept
    public static String withSubject(String token, String newSubject) {
        String payload = decode(part(token, 1));
        String oldSubject = claims(token).getString("sub");
        String tampered = payload.replace("\"sub\":\"" + oldSubject + "\"", "\"sub\":\"" + newSubject + "\"");
        return part(token, 0) + "." + encode(tampered) + "." + part(token, 2);
    }

    private static String part(String token, int index) {
        return token.split("\\.", -1)[index];
    }

    private static String decode(String base64Url) {
        return new String(DECODER.decode(base64Url), StandardCharsets.UTF_8);
    }

    private static String encode(String text) {
        return ENCODER.encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return ENCODER.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not sign token", e);
        }
    }
}
