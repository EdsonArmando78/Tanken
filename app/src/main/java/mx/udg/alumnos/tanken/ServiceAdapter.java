package mx.udg.alumnos.tanken;

import android.view.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.*;

import mx.udg.alumnos.tanken.databinding.ItemServiceBinding;

import java.util.function.Consumer;

final class ServiceAdapter extends ListAdapter<Catalog.Service, ServiceAdapter.Holder> {
    private final Consumer<Catalog.Service> open;

    ServiceAdapter(Consumer<Catalog.Service> open) {
        super(
                new DiffUtil.ItemCallback<Catalog.Service>() {
                    @Override
                    public boolean areItemsTheSame(
                            @NonNull Catalog.Service a, @NonNull Catalog.Service b) {
                        return a.id.equals(b.id);
                    }

                    @Override
                    public boolean areContentsTheSame(
                            @NonNull Catalog.Service a, @NonNull Catalog.Service b) {
                        return a.name.equals(b.name)
                                && a.location.equals(b.location)
                                && a.reviewedAt.equals(b.reviewedAt);
                    }
                });
        this.open = open;
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemServiceBinding b;

        Holder(ItemServiceBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(
                ItemServiceBinding.inflate(
                        LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Catalog.Service value = getItem(position);
        h.b.name.setText(value.name);
        h.b.location.setText(value.location);
        h.b.open.setOnClickListener(v -> open.accept(value));
    }
}
