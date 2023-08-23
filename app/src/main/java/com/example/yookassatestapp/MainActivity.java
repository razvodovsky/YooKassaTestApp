package com.example.yookassatestapp;

import android.content.DialogInterface;
import android.content.Intent;
import com.google.android.material.textfield.TextInputEditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import ru.yoomoney.sdk.kassa.payments.checkoutParameters.Amount;
import ru.yoomoney.sdk.kassa.payments.Checkout;
import ru.yoomoney.sdk.kassa.payments.ui.color.ColorScheme;
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.GooglePayParameters;
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.MockConfiguration;
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.PaymentMethodType;
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.PaymentParameters;
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.SavePaymentMethod;
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.TestParameters;
import ru.yoomoney.sdk.kassa.payments.TokenizationResult;
import ru.yoomoney.sdk.kassa.payments.checkoutParameters.UiParameters;

public class MainActivity extends AppCompatActivity implements TextWatcher {
    private static final int REQUEST_CODE_TOKENIZE = 33;
    private static final int REQUEST_CODE_3DS = 34;

    private TextView contractNameTextView;
    private TextInputEditText amountEditText;

    private String paymentType;
    private String amountString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Add back button to ActionBar
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        getSupportActionBar().setTitle("YooKassaTestApp");

        // Move layout up when keyboard shows
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        contractNameTextView = findViewById(R.id.contract_name);
        amountEditText = findViewById(R.id.amount);
        Button submitButton = findViewById(R.id.submit);

        amountEditText.addTextChangedListener(this);

        submitButton.setOnClickListener(submitButtonListener);

        setupContractNameTextView();
    }

    // Override back button action
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private final View.OnClickListener submitButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            amountString = Objects.requireNonNull(amountEditText.getText()).toString();

            if (validateRequiredFields()) {
                paymentType = "Пополнение счета"; //paymentTypeSpinner.getSelectedItem().toString();
                startPaymentProcess(amountString);
            }
        }
    };

    private void setupContractNameTextView() {
        contractNameTextView.setText("Contract name");
    }

    private boolean validateRequiredFields(){
        if (Objects.requireNonNull(amountEditText.getText()).toString().trim().equals("")) {
            amountEditText.setError("Should not be empty");
        }

        return !amountEditText.getText().toString().trim().equals("");
    }

    void startPaymentProcess(String amountString) {
        BigDecimal amount = new BigDecimal(amountString);

        final Set<PaymentMethodType> paymentMethodTypes = new HashSet<PaymentMethodType>();
        paymentMethodTypes.add(PaymentMethodType.BANK_CARD);

        GooglePayParameters googlePayParameters = new GooglePayParameters();

        ColorScheme gdBlueColorScheme = new ColorScheme(Color.rgb(0, 146, 218));
        UiParameters uiParameters = new UiParameters(false, gdBlueColorScheme);

        PaymentParameters paymentParameters = new PaymentParameters(
                new Amount(amount, Currency.getInstance("RUB")),
                "ShopName",
                paymentType,
                "applicationKey",
                "shopId",
                SavePaymentMethod.OFF,
                paymentMethodTypes,
                null, // gatewayId магазина для платежей Google Pay (необходим в случае, если в способах оплаты есть Google Pay)
                null, // url страницы (поддерживается только https), на которую надо вернуться после прохождения 3ds.
                null, // номер телефона пользователя. Используется для автозаполнения поля при оплате через SberPay. Поддерживаемый формат данных: "+7XXXXXXXXXX".
                googlePayParameters // настройки для токенизации через GooglePay
        );
        TestParameters testParameters = new TestParameters(true, true,
                new MockConfiguration(false, true, 1, new Amount(BigDecimal.ZERO, Currency.getInstance("RUB"))));
        Intent intent = Checkout.createTokenizeIntent(this, paymentParameters, new TestParameters(), uiParameters);
        startActivityForResult(intent, REQUEST_CODE_TOKENIZE);
    }

    void start3DS(String url) {
        Intent intent = Checkout.createConfirmationIntent(this, url, PaymentMethodType.BANK_CARD);
        startActivityForResult(intent, REQUEST_CODE_3DS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Log.e(TAG, "resultCode: " + resultCode + " data: " + data);
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_TOKENIZE) {
            switch (resultCode) {
                case RESULT_OK:
                    // Successful tokenization
                    TokenizationResult result = Checkout.createTokenizationResult(data);

                    // Send token to backend
                    showAlertDialog("Send token");
                case RESULT_CANCELED:
                    // User canceled tokenization
                    break;
            }
        }

        if (requestCode == REQUEST_CODE_3DS) {
            switch (resultCode) {
                case RESULT_OK:
                    // 3ds passed
                    /*onBackPressed();
                    Toast.makeText(getApplicationContext(),
                            getApplicationContext().getString(R.string.payment_success),
                            Toast.LENGTH_LONG)
                            .show();*/
                    showAlertDialog("Success");
                case RESULT_CANCELED:
                    // 3ds was closed
                    onBackPressed();
                    break;
                case Checkout.RESULT_ERROR:
                    // во время 3ds произошла какая-то ошибка (нет соединения или что-то еще)
                    // более подробную информацию можно посмотреть в data
                    // data.getIntExtra(Checkout.EXTRA_ERROR_CODE) - код ошибки из WebViewClient.ERROR_* или Checkout.ERROR_NOT_HTTPS_URL
                    // data.getStringExtra(Checkout.EXTRA_ERROR_DESCRIPTION) - описание ошибки (может отсутствовать)
                    // data.getStringExtra(Checkout.EXTRA_ERROR_FAILING_URL) - url по которому произошла ошибка (может отсутствовать)
                    /*onBackPressed();
                    Toast.makeText(getApplicationContext(),
                            getApplicationContext().getString(R.string.payment_error),
                            Toast.LENGTH_LONG)
                            .show();*/
                    showAlertDialog("Error");
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        // Validate fields after edit
        if (Objects.requireNonNull(amountEditText.getText()).toString().trim().equals("")) {
            amountEditText.setError("Should not be empty");
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Don't care */ }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { /* Don't care */ }

    private void showAlertDialog(@NonNull String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        onBackPressed();
                    }
                })
                // A null listener allows the button to dismiss the dialog and take no further action.
                //.setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }
}
