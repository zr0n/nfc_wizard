package com.zr0n.nfc_wizard;

import android.app.PendingIntent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private NfcAdapter nfcAdapter;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC não é suportado neste dispositivo", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC está desativado. Por favor, ative o NFC e reinicie o aplicativo.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null) {
            nfcAdapter.enableReaderMode(this, new NfcAdapter.ReaderCallback() {
                @Override
                public void onTagDiscovered(Tag tag) {
                    // Seu código para processar o tag NFC aqui
                    Log.d("Tag Discovered", tag.toString());
                    Ndef ndef = Ndef.get(tag);
                    if (ndef != null) {
                        try {
                            ndef.connect();

                            NdefMessage ndefMessage = ndef.getNdefMessage();
                            NdefRecord[] records = ndefMessage.getRecords();

                            for (NdefRecord record : records) {
                                byte[] payload = record.getPayload();
                                String text = new String(payload, StandardCharsets.UTF_8);

                                // Agora, 'text' contém os dados do tag NFC
                                Log.d("NFC", "Dados do Tag NFC: " + text);

                                // Execute qualquer lógica adicional com os dados do tag NFC, como atualizar a UI
                                runOnUiThread(() -> {
                                    TextView textView = findViewById(R.id.textView); // Substitua pelo ID real da sua TextView
                                    textView.setText(text);
                                });
                            }
                        } catch (IOException | FormatException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                ndef.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableNfcForegroundDispatch(this, nfcAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableNfcForegroundDispatch(this, nfcAdapter);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNfcIntent(intent);
    }

    private void enableNfcForegroundDispatch(Activity activity, NfcAdapter adapter) {
        Intent intent = new Intent(activity, activity.getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_MUTABLE);
        adapter.enableForegroundDispatch(activity, pendingIntent, null, null);
    }

    private void disableNfcForegroundDispatch(Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

    private void handleNfcIntent(Intent intent) {
        String action = intent.getAction();
        Log.d("NFC_Wizard", "action: " + action);
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            if (tag != null) {
                String tagInfo = readNdefData(tag);
                textView.setText(tagInfo);
            }
        }
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {

            // Extrair dados do cartão NFC
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMessages != null) {
                // Processar os dados do cartão NFC conforme necessário
                // Exemplo: exibir em uma TextView
                NdefMessage message = (NdefMessage) rawMessages[0];
                NdefRecord record = message.getRecords()[0];
                String data = new String(record.getPayload());
                // Agora, 'data' contém os dados do cartão, exiba na TextView
                TextView textView = findViewById(R.id.textView); // Substitua com o ID real da sua TextView
                textView.setText(data);
            }
        }

    }

    private String readNdefData(Tag tag) {
        Ndef ndef = Ndef.get(tag);

        if (ndef != null) {
            try {
                ndef.connect();
                NdefMessage ndefMessage = ndef.getNdefMessage();
                NdefRecord[] records = ndefMessage.getRecords();

                if (records.length > 0) {
                    return new String(records[0].getPayload());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    ndef.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return "Não foi possível ler os dados NDEF da tag.";
    }
}
