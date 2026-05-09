package com.fahd.casatrader.ui.transactions;

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

import com.fahd.casatrader.databinding.FragmentTransactionsBinding;
import com.fahd.casatrader.ui.detail.StockDetailActivity;

public class TransactionsFragment extends Fragment {

    private FragmentTransactionsBinding binding;
    private TransactionsViewModel viewModel;
    private TransactionsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this, new TransactionsViewModelFactory(requireContext()))
                .get(TransactionsViewModel.class);

        adapter = new TransactionsAdapter(t ->
                startActivity(StockDetailActivity.newIntent(requireContext(), t.ticker)));
        binding.transactionsRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.transactionsRv.setAdapter(adapter);

        binding.swipeRefresh.setOnRefreshListener(viewModel::load);

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            switch (state.loadState) {
                case LOADING:
                    if (adapter.getItemCount() == 0) binding.loadingPb.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    binding.loadingPb.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    adapter.submitList(state.transactions);
                    binding.emptyState.setVisibility(state.transactions.isEmpty() ? View.VISIBLE : View.GONE);
                    binding.transactionsRv.setVisibility(state.transactions.isEmpty() ? View.GONE : View.VISIBLE);
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
                && viewModel.getUiState().getValue().loadState != TransactionsViewModel.LoadState.LOADING) {
            viewModel.load();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}