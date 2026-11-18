package mx.udg.alumnos.tanken;

import org.json.*;

import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.time.LocalDate;
import java.util.*;

/** Bounded UTF-8 input, independent of file-provider size or MIME claims. */
public final class CatalogCodec {
    public static final int MAX_BYTES = 512 * 1024;

    private CatalogCodec() {}

    public static Catalog read(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] block = new byte[8192];
        int count;
        while ((count = stream.read(block)) != -1) {
            if (buffer.size() + count > MAX_BYTES) throw new IOException("Catalog too large");
            buffer.write(block, 0, count);
        }
        String text =
                StandardCharsets.UTF_8
                        .newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(buffer.toByteArray()))
                        .toString();
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        return parse(text);
    }

    public static Catalog parse(String text) {
        try {
            JSONObject root = new JSONObject(text);
            if (root.getInt("schemaVersion") != 1 || root.getDouble("schemaVersion") != 1)
                throw new IllegalArgumentException("Unsupported catalog schema");
            String version = value(root, "version", 80, true);
            JSONArray items = root.getJSONArray("services");
            if (items.length() == 0 || items.length() > 100)
                throw new IllegalArgumentException("Invalid service count");
            List<Catalog.Service> services = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                String id = value(item, "id", 64, true);
                if (!id.matches("[a-z0-9][a-z0-9_-]*") || !ids.add(id))
                    throw new IllegalArgumentException("Invalid or duplicate ID");
                String name = value(item, "name", 120, true),
                        location = value(item, "location", 300, true);
                String phone = value(item, "phone", 32, false),
                        extension = value(item, "extension", 8, false);
                String email = value(item, "email", 254, false),
                        hours = value(item, "hours", 300, false);
                String source = value(item, "source", 2048, true),
                        date = value(item, "reviewedAt", 10, true);
                if (!phone.isEmpty()
                        && (!phone.matches("[+0-9 ()-]+")
                                || phone.lastIndexOf('+') > 0
                                || !phone.replaceAll("[^0-9]", "").matches("[0-9]{7,15}")))
                    throw new IllegalArgumentException("Invalid phone");
                if (!extension.isEmpty() && (phone.isEmpty() || !extension.matches("[0-9]{1,8}")))
                    throw new IllegalArgumentException("Invalid extension");
                if (!email.isEmpty() && !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
                    throw new IllegalArgumentException("Invalid email");
                URI uri = URI.create(source);
                if (!"https".equalsIgnoreCase(uri.getScheme())
                        || uri.getHost() == null
                        || uri.getUserInfo() != null)
                    throw new IllegalArgumentException("Invalid source URL");
                LocalDate.parse(date);
                services.add(
                        new Catalog.Service(
                                id, name, location, phone, extension, email, hours, source, date));
            }
            return new Catalog(version, services, false, 0);
        } catch (JSONException invalid) {
            throw new IllegalArgumentException("Invalid catalog JSON", invalid);
        }
    }

    private static String value(JSONObject object, String key, int limit, boolean required)
            throws JSONException {
        Object raw = object.opt(key);
        if (!required && (raw == null || raw == JSONObject.NULL)) return "";
        if (!(raw instanceof String)) throw new IllegalArgumentException("Expected text: " + key);
        String text = ((String) raw).trim();
        if ((required && text.isEmpty())
                || text.length() > limit
                || text.matches("(?s).*[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F].*"))
            throw new IllegalArgumentException("Invalid field: " + key);
        return text;
    }
}
