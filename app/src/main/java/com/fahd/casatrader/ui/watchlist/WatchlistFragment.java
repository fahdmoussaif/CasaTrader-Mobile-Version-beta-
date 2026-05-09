package com.fahd.casatrader.ui.watchlist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fahd.casatrader.databinding.FragmentWatchlistBinding;
import com.fahd.casatrader.ui.detail.StockDetailActivity;

public class WatchlistFragment extends Fragment {

    private FragmentWatchlistBinding binding;
    private WatchlistViewModel viewModel;
    private WatchlistAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWatchlistBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this, new WatchlistViewModelFactory(requireContext()))
                .get(WatchlistViewModel.class);

        adapter = new WatchlistAdapter(new WatchlistAdapter.OnEntryActionListener() {
            @Override
            public void onEntryClick(com.fahd.casatrader.data.model.WatchlistEntry entry) {
                startActivity(StockDetailActivity.newIntent(requireContext(), entry.ticker));
            }
            @Override
            public void onRemoveClick(com.fahd.casatrader.data.model.WatchlistEntry entry) {
                viewModel.remove(entry.ticker);
            }
        });
        binding.watchlistRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.watchlistRv.setAdapter(adapter);

        binding.swipeRefresh.setOnRefreshListener(viewModel::load);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            switch (state.loadState) {
                case LOADING:
                    if (adapter.getItemCount() == 0) binding.loadingPb.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    binding.loadingPb.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    adapter.submitList(state.entries);
                    binding.emptyTv.setVisibility(state.entries.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.watchlistRv.setVisibility(state.entries.isEmpty() ? View.GONE : View.VISIBLE);
                    break;
                case ERROR:
                    binding.loadingPb.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show();
                    break;
                case IDLE:
                default: break;
            }
        });

        if (savedInstanceState == null) viewModel.load();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel.getUiState().getValue() != null
                && viewModel.getUiState().getValue().loadState != WatchlistViewModel.LoadState.LOADING) {
            viewModel.load();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}