package mx.udg.alumnos.tanken;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import mx.udg.alumnos.tanken.databinding.FragmentServicesBinding;

public final class ServicesFragment extends Fragment {
    private FragmentServicesBinding b;
    private ServicesViewModel model;
    private AlertDialog preview;
    private final ActivityResultLauncher<String[]> document =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    uri -> {
                        if (uri != null) model.preview(uri);
                    });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle saved) {
        b = FragmentServicesBinding.inflate(inflater, parent, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle saved) {
        super.onViewCreated(view, saved);
        model = new ViewModelProvider(this).get(ServicesViewModel.class);
        ServiceAdapter adapter =
                new ServiceAdapter(
                        service ->
                                startActivity(
                                        new Intent(requireContext(), ServiceDetailActivity.class)
                                                .putExtra("serviceId", service.id)));
        b.services.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.services.setAdapter(adapter);
        b.importCatalog.setOnClickListener(
                v ->
                        document.launch(
                                new String[] {
                                    "application/json", "text/plain", "application/octet-stream"
                                }));
        model.catalog()
                .observe(
                        getViewLifecycleOwner(),
                        value -> {
                            adapter.submitList(value.services);
                            b.empty.setText(R.string.services_empty);
                            b.empty.setVisibility(
                                    value.services.isEmpty() ? View.VISIBLE : View.GONE);
                            b.version.setText(
                                    getString(
                                            R.string.catalog_version,
                                            value.version,
                                            getString(
                                                    value.imported
                                                            ? R.string.catalog_user
                                                            : R.string.catalog_bundled)));
                        });
        model.error()
                .observe(
                        getViewLifecycleOwner(),
                        error -> {
                            b.error.setText(error);
                            b.error.setVisibility(error.isEmpty() ? View.GONE : View.VISIBLE);
                        });
        model.message()
                .observe(
                        getViewLifecycleOwner(),
                        message -> {
                            b.message.setText(message);
                            b.message.setVisibility(message.isEmpty() ? View.GONE : View.VISIBLE);
                        });
        model.busy().observe(getViewLifecycleOwner(), busy -> b.importCatalog.setEnabled(!busy));
        model.preview()
                .observe(
                        getViewLifecycleOwner(),
                        value -> {
                            if (value == null) return;
                            preview =
                                    new MaterialAlertDialogBuilder(requireContext())
                                            .setTitle(R.string.catalog_import_title)
                                            .setMessage(
                                                    getString(
                                                            R.string.catalog_preview,
                                                            value.version,
                                                            value.services.size()))
                                            .setNegativeButton(
                                                    R.string.schedule_cancel,
                                                    (d, w) -> model.discardPreview())
                                            .setPositiveButton(
                                                    R.string.catalog_apply,
                                                    (d, w) -> model.confirm())
                                            .create();
                            preview.setOnDismissListener(
                                    d -> {
                                        preview = null;
                                        if (!requireActivity().isChangingConfigurations())
                                            model.discardPreview();
                                    });
                            preview.show();
                        });
    }

    @Override
    public void onDestroyView() {
        if (preview != null) preview.dismiss();
        b = null;
        super.onDestroyView();
    }
}
