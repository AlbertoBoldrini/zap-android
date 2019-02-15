package ln_zap.zap.fragments;

import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import ln_zap.zap.R;
import ln_zap.zap.ReceiveActivity;
import ln_zap.zap.setup.SetupActivity;
import ln_zap.zap.qrCodeScanner.QRCodeScannerActivity;
import ln_zap.zap.util.Balances;
import ln_zap.zap.util.MonetaryUtil;
import ln_zap.zap.util.Wallet;
import ln_zap.zap.util.ZapLog;


/**
 * A simple {@link Fragment} subclass.
 */
public class WalletFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener, Wallet.BalanceListener {

    private static final String LOG_TAG = "Wallet Fragment";

    private SharedPreferences mPrefs;
    private TextView mTvPrimaryBalance;
    private TextView mTvPrimaryBalanceUnit;
    private TextView mTvSecondaryBalance;
    private TextView mTvSecondaryBalanceUnit;

    private boolean mPreferenceChangeListenerRegistered = false;
    private boolean mBalanceChangeListenerRegistered = false;


    public WalletFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // Get View elements
        mTvPrimaryBalance = view.findViewById(R.id.BalancePrimary);
        mTvPrimaryBalanceUnit = view.findViewById(R.id.BalancePrimaryUnit);
        mTvSecondaryBalance = view.findViewById(R.id.BalanceSecondary);
        mTvSecondaryBalanceUnit = view.findViewById(R.id.BalanceSecondaryUnit);

        updateTotalBalanceDisplay();

        // Swap action when clicked on balance
        ConstraintLayout clBalance = view.findViewById(R.id.BalanceLayout);
        clBalance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MonetaryUtil.getInstance().switchCurrencies();
                updateTotalBalanceDisplay();
            }
        });

        // Swap action when clicked swap icon next to balance
        ImageView ivSwapImage = view.findViewById(R.id.switchButtonImage);
        ivSwapImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MonetaryUtil.getInstance().switchCurrencies();
                updateTotalBalanceDisplay();
            }
        });

        // Action when clicked on "send"
        Button btnSend = view.findViewById(R.id.sendButton);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), QRCodeScannerActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
            }
        });


        // Action when clicked on "receive"
        Button btnReceive = view.findViewById(R.id.receiveButton);
        btnReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ReceiveActivity.class);
                startActivity(intent);
            }
        });

        // Action when clicked on "setup wallet"
        Button btnSetup = view.findViewById(R.id.setupWallet);
        if (mPrefs.getBoolean("isWalletSetup", false)) {
            btnSetup.setVisibility(View.INVISIBLE);
        }
        btnSetup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SetupActivity.class);
                intent.putExtra("setupMode", SetupActivity.FULL_SETUP);
                startActivity(intent);
            }
        });

        // fetch the current balance from LND
        if (mPrefs.getBoolean("isWalletSetup", false)) {
            Wallet.getInstance().fetchBalanceFromLND();
        }

        return view;
    }

    private void updateTotalBalanceDisplay(){

        // Adapt unit text size depending on its length
        if (MonetaryUtil.getInstance().getPrimaryDisplayUnit().length() > 2){
            mTvPrimaryBalanceUnit.setTextSize(20);
        }
        else{
            mTvPrimaryBalanceUnit.setTextSize(32);
        }

        Balances balances;
        if (mPrefs.getBoolean("isWalletSetup", false)) {
            balances = Wallet.getInstance().getBalances();
        } else {
            balances = Wallet.getInstance().getDemoBalances();
        }

        mTvPrimaryBalance.setText(MonetaryUtil.getInstance().getPrimaryDisplayAmount(balances.total()));
        mTvPrimaryBalanceUnit.setText(MonetaryUtil.getInstance().getPrimaryDisplayUnit());
        mTvSecondaryBalance.setText(MonetaryUtil.getInstance().getSecondaryDisplayAmount(balances.total()));
        mTvSecondaryBalanceUnit.setText(MonetaryUtil.getInstance().getSecondaryDisplayUnit());

        ZapLog.debug(LOG_TAG,"Total balance display updated");

    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        //update if currency has been switched or new exchange data arrived
        if (key.equals("firstCurrencyIsPrimary") || key.equals("fiat_USD")){
            updateTotalBalanceDisplay();
        }
    }

    @Override
    public void onBalanceUpdated() {
        updateTotalBalanceDisplay();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register listeners
        if(!mPreferenceChangeListenerRegistered){
            mPrefs.registerOnSharedPreferenceChangeListener(this);
            mPreferenceChangeListenerRegistered = true;
        }
        if(!mBalanceChangeListenerRegistered) {
            Wallet.getInstance().registerBalanceListener(this);
            mBalanceChangeListenerRegistered = true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister listeners
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        Wallet.getInstance().unregisterBalanceListener(this);
    }

}
