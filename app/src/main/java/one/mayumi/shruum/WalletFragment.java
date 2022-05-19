/*
 * Copyright (c) 2017 m2049r
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package one.mayumi.shruum;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.github.brnunes.swipeablerecyclerview.SwipeableRecyclerViewTouchListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import one.mayumi.shruum.layout.TransactionInfoAdapter;
import one.mayumi.shruum.model.TransactionInfo;
import one.mayumi.shruum.model.Wallet;
import one.mayumi.shruum.util.Helper;
import one.mayumi.shruum.util.ThemeHelper;
import one.mayumi.shruum.widget.Toolbar;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class WalletFragment extends Fragment
        implements TransactionInfoAdapter.OnInteractionListener, View.OnClickListener {
    private TransactionInfoAdapter adapter;
    private final NumberFormat formatter = NumberFormat.getInstance();

    private TextView tvStreetView;
    private LinearLayout llBalance;
    private TextView tvBalance;
    private TextView tvUnconfirmedAmount;
    private TextView tvProgress;
    private ImageView ivSynced;
    private ProgressBar pbProgress;
    RecyclerView txlist;

    private boolean isFabOpen = false;
    private FloatingActionButton fab, fabReceive, fabSend, fabSign, fabVerify;
    private RelativeLayout fabScreen, fabBackground;
    private RelativeLayout fabReceiveLayout, fabSendLayout, fabSignLayout, fabVerifyLayout;
    private Animation fab_open, fab_close, rotate_forward, rotate_backward, fab_open_screen, fab_close_screen;

    private final List<String> dismissedTransactions = new ArrayList<>();

    public void resetDismissedTransactions() {
        dismissedTransactions.clear();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (activityCallback.hasWallet())
            inflater.inflate(R.menu.wallet_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        tvStreetView = view.findViewById(R.id.tvStreetView);
        llBalance = view.findViewById(R.id.llBalance);
        tvProgress = view.findViewById(R.id.tvProgress);
        pbProgress = view.findViewById(R.id.pbProgress);
        tvBalance = view.findViewById(R.id.tvBalance);
        showBalance(Helper.getDisplayAmount(0));
        tvUnconfirmedAmount = view.findViewById(R.id.tvUnconfirmedAmount);
        showUnconfirmed(0);
        ivSynced = view.findViewById(R.id.ivSynced);

        txlist = view.findViewById(R.id.list);
        adapter = new TransactionInfoAdapter(getActivity(), this);
        txlist.setAdapter(adapter);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                if ((positionStart == 0) && (txlist.computeVerticalScrollOffset() == 0))
                    txlist.scrollToPosition(positionStart);
            }
        });

        txlist.addOnItemTouchListener(
                new SwipeableRecyclerViewTouchListener(txlist,
                        new SwipeableRecyclerViewTouchListener.SwipeListener() {
                            @Override
                            public boolean canSwipeLeft(int position) {
                                return activityCallback.isStreetMode();
                            }

                            @Override
                            public boolean canSwipeRight(int position) {
                                return activityCallback.isStreetMode();
                            }

                            @Override
                            public void onDismissedBySwipeLeft(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    dismissedTransactions.add(adapter.getItem(position).hash);
                                    adapter.removeItem(position);
                                }
                            }

                            @Override
                            public void onDismissedBySwipeRight(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    dismissedTransactions.add(adapter.getItem(position).hash);
                                    adapter.removeItem(position);
                                }
                            }
                        }));

        fabScreen = view.findViewById(R.id.fabScreen);
        fabBackground = view.findViewById(R.id.fabBackground);
        fab = view.findViewById(R.id.fab);
        fabReceive = view.findViewById(R.id.fab_receive);
        fabSend = view.findViewById(R.id.fab_send);
        fabSign = view.findViewById(R.id.fab_sign);
        fabVerify = view.findViewById(R.id.fab_verify);

        fabReceiveLayout = view.findViewById(R.id.fab_receive_layout);
        fabSendLayout = view.findViewById(R.id.fab_send_layout);
        fabSignLayout = view.findViewById(R.id.fab_sign_layout);
        fabVerifyLayout = view.findViewById(R.id.fab_verify_layout);

        fab_open_screen = AnimationUtils.loadAnimation(getContext(), R.anim.fab_open_screen);
        fab_close_screen = AnimationUtils.loadAnimation(getContext(), R.anim.fab_close_screen);
        fab_open = AnimationUtils.loadAnimation(getContext(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getContext(), R.anim.fab_close);
        rotate_forward = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_forward);
        rotate_backward = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_backward);
        fab.setOnClickListener(this);
        fabReceive.setOnClickListener(this);
        fabSend.setOnClickListener(this);
        fabSign.setOnClickListener(this);
        fabVerify.setOnClickListener(this);
        fabScreen.setOnClickListener(this);
        fabBackground.setOnClickListener(this);

        if (activityCallback.isSynced()) {
            onSynced();
        }

        activityCallback.forceUpdate();

        return view;
    }

    @Override
    public void onClick(View v) {
        final int id = v.getId();
        Timber.d("onClick %d/%d", id, R.id.fabLedger);
        if (id == R.id.fab) {
            animateFAB();
        } else if (id == R.id.fab_receive) {
            animateFAB();
            activityCallback.onWalletReceive(v);
        } else if (id == R.id.fab_send) {
            animateFAB();
            activityCallback.onSendRequest(v);
        }  else if (id == R.id.fab_sign) {
            animateFAB();
            activityCallback.toSign(v);
        }  else if (id == R.id.fab_verify) {
            animateFAB();
            activityCallback.toVerify(v);
        }  else if (id == R.id.fabScreen || id == R.id.fabBackground) {
            animateFAB();
        }
    }

    public boolean isFabOpen() {
        return isFabOpen;
    }

    public void animateFAB() {
        if (isFabOpen) { // close the fab
            fabScreen.setClickable(false);
            fabScreen.startAnimation(fab_close_screen);
            fabBackground.setClickable(false);
            fabBackground.startAnimation(fab_close_screen);
            fab.startAnimation(rotate_backward);
            fabReceiveLayout.startAnimation(fab_close);
            fabReceive.setClickable(false);
            fabSendLayout.startAnimation(fab_close);
            fabSend.setClickable(false);
            fabSignLayout.startAnimation(fab_close);
            fabSign.setClickable(false);
            fabVerifyLayout.startAnimation(fab_close);
            fabVerify.setClickable(false);
            isFabOpen = false;
        } else { // open the fab
            fabScreen.setClickable(true);
            fabScreen.startAnimation(fab_open_screen);
            fabBackground.setClickable(true);
            fabBackground.startAnimation(fab_open_screen);
            fab.startAnimation(rotate_forward);
            fabReceiveLayout.setVisibility(View.VISIBLE);
            fabSendLayout.setVisibility(View.VISIBLE);
            fabSignLayout.setVisibility(View.VISIBLE);
            fabVerifyLayout.setVisibility(View.VISIBLE);

            fabReceiveLayout.startAnimation(fab_open);
            fabReceive.setClickable(true);
            fabSendLayout.startAnimation(fab_open);
            fabSend.setClickable(true);
            fabSignLayout.startAnimation(fab_open);
            fabSign.setClickable(true);
            fabVerifyLayout.startAnimation(fab_open);
            fabVerify.setClickable(true);
            isFabOpen = true;
        }
    }

    void showBalance(String balance) {
        tvBalance.setText(getResources().getString(R.string.xmr_confirmed_amount, balance));
        final boolean streetMode = activityCallback.isStreetMode();
        if (!streetMode) {
            llBalance.setVisibility(View.VISIBLE);
            tvStreetView.setVisibility(View.INVISIBLE);
        } else {
            llBalance.setVisibility(View.INVISIBLE);
            tvStreetView.setVisibility(View.VISIBLE);
        }
    }

    void showUnconfirmed(double unconfirmedAmount) {
        if (activityCallback.isStreetMode() || unconfirmedAmount == 0) {
            tvUnconfirmedAmount.setText(null);
            tvUnconfirmedAmount.setVisibility(View.GONE);
        } else {
            String unconfirmed = Helper.getFormattedAmount(unconfirmedAmount, true);
            tvUnconfirmedAmount.setText(getResources().getString(R.string.xmr_unconfirmed_amount, unconfirmed));
            tvUnconfirmedAmount.setVisibility(View.VISIBLE);
        }
    }

    void refreshBalance() {
        double unconfirmedXmr = Helper.getDecimalAmount(balance - unlockedBalance).doubleValue();
        showUnconfirmed(unconfirmedXmr);
        double amountXmr = Helper.getDecimalAmount(unlockedBalance).doubleValue();
        showBalance(Helper.getFormattedAmount(amountXmr, true));
    }

    // Callbacks from TransactionInfoAdapter
    @Override
    public void onInteraction(final View view, final TransactionInfo infoItem) {
        activityCallback.onTxDetailsRequest(view, infoItem);
    }

    // if account index has changed scroll to top?
    private int accountIndex = 0;

    public void onRefreshed(final Wallet wallet, boolean full) {
        Timber.d("onRefreshed(%b)", full);

        if (adapter.needsTransactionUpdateOnNewBlock()) {
            wallet.refreshHistory();
            full = true;
        }
        if (full) {
            List<TransactionInfo> list = new ArrayList<>();
            final long streetHeight = activityCallback.getStreetModeHeight();
            Timber.d("StreetHeight=%d", streetHeight);
            wallet.refreshHistory();
            for (TransactionInfo info : wallet.getHistory().getAll()) {
                Timber.d("TxHeight=%d, Label=%s", info.blockheight, info.subaddressLabel);
                if ((info.isPending || (info.blockheight >= streetHeight))
                        && !dismissedTransactions.contains(info.hash))
                    list.add(info);
            }
            adapter.setInfos(list);
            if (accountIndex != wallet.getAccountIndex()) {
                accountIndex = wallet.getAccountIndex();
                txlist.scrollToPosition(0);
            }
        }
        updateStatus(wallet);
    }

    public void onSynced() {
        if (!activityCallback.isWatchOnly()) {
            fabSend.setVisibility(View.VISIBLE);
            fabSend.setEnabled(true);
        }
        if (isVisible()) enableAccountsList(true); //otherwise it is enabled in onResume()
    }

    public void unsync() {
        if (!activityCallback.isWatchOnly()) {
            fabSend.setVisibility(View.INVISIBLE);
            fabSend.setEnabled(false);
        }
        if (isVisible()) enableAccountsList(false); //otherwise it is enabled in onResume()
        firstBlock = 0;
    }

    boolean walletLoaded = false;

    public void onLoaded() {
        walletLoaded = true;
        showReceive();
    }

    private void showReceive() {
        if (walletLoaded) {
            fabReceive.setVisibility(View.VISIBLE);
            fabReceive.setEnabled(true);
            fabSign.setVisibility(View.VISIBLE);
            fabSign.setEnabled(true);
            fabVerify.setVisibility(View.VISIBLE);
            fabVerify.setEnabled(true);
        }
    }

    private String syncText = null;

    public void setProgress(final String text) {
        syncText = text;
        tvProgress.setText(text);
    }

    private int syncProgress = -1;

    public void setProgress(final int n) {
        syncProgress = n;
        if (n > 100) {
            pbProgress.setIndeterminate(true);
            pbProgress.setVisibility(View.VISIBLE);
        } else if (n >= 0) {
            pbProgress.setIndeterminate(false);
            pbProgress.setProgress(n);
            pbProgress.setVisibility(View.VISIBLE);
        } else { // <0
            pbProgress.setVisibility(View.INVISIBLE);
        }
    }

    void setActivityTitle(Wallet wallet) {
        if (wallet == null) return;
        walletTitle = wallet.getName();
        walletSubtitle = wallet.getAccountLabel();
        activityCallback.setTitle(walletTitle, walletSubtitle);
        Timber.d("wallet title is %s", walletTitle);
    }

    private long firstBlock = 0;
    private String walletTitle = null;
    private String walletSubtitle = null;
    private long unlockedBalance = 0;
    private long balance = 0;

    private int accountIdx = -1;

    private void updateStatus(Wallet wallet) {
        if (!isAdded()) return;
        Timber.d("updateStatus()");
        if ((walletTitle == null) || (accountIdx != wallet.getAccountIndex())) {
            accountIdx = wallet.getAccountIndex();
            setActivityTitle(wallet);
        }
        balance = wallet.getBalance();
        unlockedBalance = wallet.getUnlockedBalance();
        refreshBalance();
        String sync;
        if (!activityCallback.hasBoundService())
            throw new IllegalStateException("WalletService not bound.");
        Wallet.ConnectionStatus daemonConnected = activityCallback.getConnectionStatus();
        if (daemonConnected == Wallet.ConnectionStatus.ConnectionStatus_Connected) {
            if (!wallet.isSynchronized()) {
                long daemonHeight = activityCallback.getDaemonHeight();
                long walletHeight = wallet.getBlockChainHeight();
                long n = daemonHeight - walletHeight;
                sync = getString(R.string.status_syncing) + " " + formatter.format(n) + " " + getString(R.string.status_remaining);
                if (firstBlock == 0) {
                    firstBlock = walletHeight;
                }
                int x = 100 - Math.round(100f * n / (1f * daemonHeight - firstBlock));
                if (x == 0) x = 101; // indeterminate
                setProgress(x);
                ivSynced.setVisibility(View.GONE);
            } else {
                sync = getString(R.string.status_synced) + " " + formatter.format(wallet.getBlockChainHeight());
                ivSynced.setVisibility(View.VISIBLE);
            }
        } else {
            sync = getString(R.string.status_wallet_connecting);
            setProgress(101);
        }
        setProgress(sync);
        // TODO show connected status somewhere
    }

    Listener activityCallback;

    // Container Activity must implement this interface
    public interface Listener {
        boolean hasBoundService();

        void forceUpdate();

        Wallet.ConnectionStatus getConnectionStatus();

        long getDaemonHeight(); //mBoundService.getDaemonHeight();

        void onSendRequest(View view);

        void onTxDetailsRequest(View view, TransactionInfo info);

        boolean isSynced();

        boolean isStreetMode();

        long getStreetModeHeight();

        boolean isWatchOnly();

        String getTxKey(String txId);

        void onWalletReceive(View view);

        void toSign(View view);

        void toVerify(View view);

        boolean hasWallet();

        Wallet getWallet();

        void setToolbarButton(int type);

        void setTitle(String title, String subtitle);

        void setSubtitle(String subtitle);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            this.activityCallback = (Listener) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement Listener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume()");
        activityCallback.setTitle(walletTitle, walletSubtitle);
        activityCallback.setToolbarButton(Toolbar.BUTTON_NONE);
        setProgress(syncProgress);
        setProgress(syncText);
        showReceive();
        if (activityCallback.isSynced()) enableAccountsList(true);
    }

    @Override
    public void onPause() {
        enableAccountsList(false);
        super.onPause();
    }

    public interface DrawerLocker {
        void setDrawerEnabled(boolean enabled);
    }

    private void enableAccountsList(boolean enable) {
        if (activityCallback instanceof DrawerLocker) {
            ((DrawerLocker) activityCallback).setDrawerEnabled(enable);
        }
    }
}
