package mx.udg.alumnos.tanken;

import java.util.*;

/** Immutable validated snapshot; imported data is never presented as verified by the app. */
public final class Catalog {
    public final String version;
    public final List<Service> services;
    public final boolean imported;
    public final long importedAt;

    Catalog(String version, List<Service> services, boolean imported, long importedAt) {
        this.version = version;
        this.services = Collections.unmodifiableList(new ArrayList<>(services));
        this.imported = imported;
        this.importedAt = importedAt;
    }

    public static final class Service {
        public final String id, name, location, phone, extension, email, hours, source, reviewedAt;

        Service(
                String id,
                String name,
                String location,
                String phone,
                String extension,
                String email,
                String hours,
                String source,
                String reviewedAt) {
            this.id = id;
            this.name = name;
            this.location = location;
            this.phone = phone;
            this.extension = extension;
            this.email = email;
            this.hours = hours;
            this.source = source;
            this.reviewedAt = reviewedAt;
        }
    }
}
