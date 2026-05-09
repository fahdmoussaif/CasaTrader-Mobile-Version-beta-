package com.fahd.casatrader.ui.stocks;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fahd.casatrader.databinding.FragmentStockListBinding;
import com.fahd.casatrader.ui.detail.StockDetailActivity;

public class StockListFragment extends Fragment {

    private FragmentStockListBinding binding;
    private StockListViewModel viewModel;
    private StockListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStockListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this, new StockListViewModelFactory(requireContext()))
                .get(StockListViewModel.class);

        adapter = new StockListAdapter(stock ->
                startActivity(StockDetailActivity.newIntent(requireContext(), stock.ticker)));
        binding.stocksRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.stocksRv.setAdapter(adapter);

        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.load());

        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                viewModel.setSearchQuery(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            switch (state.loadState) {
                case LOADING:
                    if (adapter.getItemCount() == 0) {
                        binding.loadingPb.setVisibility(View.VISIBLE);
                    }
                    break;
                case SUCCESS:
                    binding.loadingPb.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    adapter.submitList(state.stocks);
                    binding.emptyState.setVisibility(state.stocks.isEmpty() ? View.VISIBLE : View.GONE);
                    break;
                case ERROR:
                    binding.loadingPb.setVisibility(View.GONE);
                    binding.swipeRefresh.setRefreshing(false);
                    Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show();
                    break;
                case IDLE:
                default:
                    break;
            }
        });

        if (savedInstanceState == null) {
            viewModel.load();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}