package mx.udg.alumnos.tanken;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import mx.udg.alumnos.tanken.databinding.ActivityServiceDetailBinding;

public final class ServiceDetailActivity extends AppCompatActivity {
    private ActivityServiceDetailBinding b;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        EdgeToEdge.enable(this);
        b = ActivityServiceDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        ProductUi.insets(b.getRoot());
        b.toolbar.setNavigationOnClickListener(v -> finish());
        setActions(false);
        ServicesViewModel model = new ViewModelProvider(this).get(ServicesViewModel.class);
        model.catalog()
                .observe(
                        this,
                        catalog -> {
                            Catalog.Service match = null;
                            for (Catalog.Service s : catalog.services)
                                if (s.id.equals(getIntent().getStringExtra("serviceId"))) match = s;
                            if (match == null) {
                                b.details.setText(R.string.service_unavailable);
                                setActions(false);
                                return;
                            }
                            Catalog.Service s = match;
                            b.name.setText(s.name);
                            b.catalogVersion.setText(
                                    getString(
                                            R.string.catalog_version,
                                            catalog.version,
                                            getString(
                                                    catalog.imported
                                                            ? R.string.catalog_user
                                                            : R.string.catalog_bundled)));
                            b.details.setText(
                                    getString(
                                            R.string.service_details,
                                            s.location,
                                            s.hours.isEmpty()
                                                    ? getString(R.string.service_unspecified)
                                                    : s.hours,
                                            s.phone.isEmpty()
                                                    ? getString(R.string.service_unspecified)
                                                    : (s.extension.isEmpty()
                                                            ? s.phone
                                                            : getString(
                                                                    R.string.service_phone,
                                                                    s.phone,
                                                                    s.extension)),
                                            s.email.isEmpty()
                                                    ? getString(R.string.service_unspecified)
                                                    : s.email,
                                            s.source,
                                            s.reviewedAt));
                            b.phone.setEnabled(!s.phone.isEmpty());
                            b.email.setEnabled(!s.email.isEmpty());
                            b.source.setEnabled(true);
                            b.phone.setOnClickListener(
                                    v ->
                                            ExternalLinks.open(
                                                    this,
                                                    new Intent(
                                                            Intent.ACTION_DIAL,
                                                            Uri.parse(
                                                                    "tel:"
                                                                            + s.phone.replaceAll(
                                                                                    "[^+0-9]",
                                                                                    ""))),
                                                    false));
                            b.email.setOnClickListener(
                                    v ->
                                            ExternalLinks.open(
                                                    this,
                                                    new Intent(
                                                            Intent.ACTION_SENDTO,
                                                            Uri.fromParts("mailto", s.email, null)),
                                                    false));
                            b.source.setOnClickListener(
                                    v ->
                                            ExternalLinks.open(
                                                    this,
                                                    new Intent(
                                                            Intent.ACTION_VIEW,
                                                            Uri.parse(s.source)),
                                                    true));
                        });
        model.error()
                .observe(
                        this,
                        error -> {
                            if (!error.isEmpty() && model.catalog().getValue() == null)
                                b.details.setText(error);
                        });
    }

    private void setActions(boolean enabled) {
        b.phone.setEnabled(enabled);
        b.email.setEnabled(enabled);
        b.source.setEnabled(enabled);
    }

    @Override
    protected void onDestroy() {
        b = null;
        super.onDestroy();
    }
}
