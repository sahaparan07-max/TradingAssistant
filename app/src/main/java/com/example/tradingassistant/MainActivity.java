package com.example.tradingassistant;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import android.text.InputType;
import org.json.JSONObject;
import org.json.JSONArray;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

    LinearLayout main, chatBox;
    EditText messageInput;
    TextView priceView;
    Handler handler = new Handler(Looper.getMainLooper());

    String[] coins = {"BTCUSDT", "ETHUSDT", "SOLUSDT"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(18, 18, 18, 18);
        main.setBackgroundColor(0xFF101820);
        scroll.addView(main);

        setContentView(scroll);

        title("🤖 Trading Assistant");

        priceView = new TextView(this);
        priceView.setTextColor(0xFF00E676);
        priceView.setTextSize(16);
        priceView.setPadding(8, 15, 8, 15);
        main.addView(priceView);

        Button refresh = button("🔄 Live Price Refresh");
        main.addView(refresh);
        refresh.setOnClickListener(v -> getPrices());

        title("💬 AI Chat");

        chatBox = new LinearLayout(this);
        chatBox.setOrientation(LinearLayout.VERTICAL);
        main.addView(chatBox);

        addMessage("Assistant", "নমস্কাৰ! মই আপোনাৰ Trading Assistant। অসমীয়া বা English-ত প্ৰশ্ন কৰক।");

        messageInput = new EditText(this);
        messageInput.setSingleLine(false);
        messageInput.setMinLines(2);
        messageInput.setTextColor(0xFFFFFFFF);
        messageInput.setHintTextColor(0xFFBBBBBB);
        messageInput.setHint("ইয়াত লিখক / Type your message");
        messageInput.setInputType(
            InputType.TYPE_CLASS_TEXT |
            InputType.TYPE_TEXT_FLAG_MULTI_LINE |
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );
        messageInput.setImeOptions(EditorInfo.IME_ACTION_SEND);
        messageInput.setBackgroundColor(0xFF263238);
        main.addView(messageInput);

        Button send = button("Send ➤");
        main.addView(send);
        send.setOnClickListener(v -> sendMessage());

        title("📊 Paper Analysis");

        EditText symbol = numberOrText("BTCUSDT / ETHUSDT / SOLUSDT");
        main.addView(symbol);

        EditText entry = numberOrText("Entry Price");
        main.addView(entry);

        EditText stop = numberOrText("Stop Loss");
        main.addView(stop);

        EditText target = numberOrText("Take Profit");
        main.addView(target);

        Button review = button("Review Paper Trade");
        main.addView(review);

        review.setOnClickListener(v -> {
            try {
                double e = Double.parseDouble(entry.getText().toString());
                double s = Double.parseDouble(stop.getText().toString());
                double t = Double.parseDouble(target.getText().toString());

                double risk = Math.abs(e - s);
                double reward = Math.abs(t - e);

                if (risk == 0) {
                    addMessage("Assistant", "Entry আৰু Stop Loss বেলেগ হ'ব লাগিব।");
                    return;
                }

                double rr = reward / risk;

                addMessage("Assistant",
                    "Paper Trade Review\n" +
                    "Risk: " + risk + "\n" +
                    "Potential Reward: " + reward + "\n" +
                    "Risk/Reward: 1:" + String.format("%.2f", rr) +
                    "\nএইটো কেৱল শিক্ষামূলক গণনা, লাভৰ নিশ্চয়তা নহয়।");

            } catch (Exception ex) {
                addMessage("Assistant", "অনুগ্ৰহ কৰি Entry, SL আৰু TP-ত সঠিক সংখ্যা লিখক।");
            }
        });

        getPrices();
    }

    void title(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(21);
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(0xFFFFFFFF);
        t.setPadding(5, 20, 5, 12);
        main.addView(t);
    }

    Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        return b;
    }

    EditText numberOrText(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(0xFFFFFFFF);
        e.setHintTextColor(0xFFBBBBBB);
        e.setInputType(InputType.TYPE_CLASS_TEXT |
                       InputType.TYPE_NUMBER_FLAG_DECIMAL |
                       InputType.TYPE_NUMBER_FLAG_SIGNED);
        e.setBackgroundColor(0xFF263238);
        return e;
    }

    void addMessage(String who, String msg) {
        TextView t = new TextView(this);
        t.setText(who + ":\n" + msg);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(16);
        t.setPadding(12, 12, 12, 12);
        chatBox.addView(t);
    }

    void sendMessage() {
        String msg = messageInput.getText().toString().trim();
        if (msg.isEmpty()) return;

        addMessage("You", msg);
        messageInput.setText("");

        String q = msg.toLowerCase();

        if (q.contains("hello") || q.contains("hi") ||
            q.contains("নমস্কাৰ") || q.contains("নমস্কাৰ")) {
            addMessage("Assistant", "নমস্কাৰ! আপোনাক কেনেকৈ সহায় কৰিব পাৰোঁ?");
        }
        else if (q.contains("rsi")) {
            addMessage("Assistant", "RSI হৈছে Momentum Indicator। সাধাৰণতে 70-ৰ ওপৰত Overbought আৰু 30-ৰ তলত Oversold বুলি অধ্যয়ন কৰা হয়। ই অকলে Buy/Sell সংকেত নহয়।");
        }
        else if (q.contains("macd")) {
            addMessage("Assistant", "MACD-এ দুটা EMA-ৰ পাৰ্থক্য আৰু Signal Line ব্যৱহাৰ কৰি Momentum বিশ্লেষণ কৰে।");
        }
        else if (q.contains("ema")) {
            addMessage("Assistant", "EMA-ই সাম্প্ৰতিক মূল্যক অধিক গুৰুত্ব দি গড় মূল্য দেখুৱায়।");
        }
        else if (q.contains("btc") || q.contains("bitcoin")) {
            addMessage("Assistant", "BTC-ৰ Live Price চাবলৈ ওপৰৰ Live Price Refresh বুটাম টিপক।");
        }
        else if (q.contains("eth") || q.contains("ethereum")) {
            addMessage("Assistant", "ETH-ৰ Live Price চাবলৈ ওপৰৰ Live Price Refresh বুটাম টিপক।");
        }
        else if (q.contains("sol") || q.contains("solana")) {
            addMessage("Assistant", "SOL-ৰ Live Price চাবলৈ ওপৰৰ Live Price Refresh বুটাম টিপক।");
        }
        else {
            addMessage("Assistant",
                "আপোনাৰ প্ৰশ্নটো পাইছোঁ। এই সংস্কৰণত সম্পূৰ্ণ AI Server সংযোগ হোৱা নাই। ChatGPT-ৰ দৰে স্বাভাৱিক উত্তৰৰ বাবে পিছৰ ধাপত AI Server সংযোগ কৰিম।");
        }
    }

    void getPrices() {
        priceView.setText("Loading live prices...");
        new Thread(() -> {
            StringBuilder result = new StringBuilder();

            for (String coin : coins) {
                try {
                    URL url = new URL(
                        "https://api.binance.com/api/v3/ticker/price?symbol=" + coin
                    );

                    HttpsURLConnection con =
                        (HttpsURLConnection) url.openConnection();

                    con.setConnectTimeout(10000);
                    con.setReadTimeout(10000);

                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(con.getInputStream())
                    );

                    String line = reader.readLine();
                    JSONObject obj = new JSONObject(line);

                    result.append(coin.replace("USDT", ""))
                          .append(": $")
                          .append(obj.getString("price"))
                          .append("\n");

                    reader.close();
                    con.disconnect();

                } catch (Exception ex) {
                    result.append(coin).append(": Price unavailable\n");
                }
            }

            handler.post(() -> priceView.setText(result.toString()));

        }).start();
    }
}
