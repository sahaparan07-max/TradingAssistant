
package com.example.tradingassistant;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.text.InputType;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.*;
import java.util.Locale;

public class MainActivity extends Activity {

    LinearLayout main, chatBox;
    EditText messageInput, entryInput, slInput, tpInput;
    TextView priceView;
    Button sendButton;

    Handler handler = new Handler(Looper.getMainLooper());

    JSONArray history = new JSONArray();

    final String AI_URL =
        "http://127.0.0.1:8080/v1/chat/completions";

    final String SYSTEM_PROMPT =
        "/no_think You are a helpful AI assistant. " +
        "Understand Assamese, Roman Assamese and English. " +
        "Reply naturally in the language used by the user. " +
        "Answer general knowledge, science, technology, education " +
        "and other everyday questions. Explain clearly. " +
        "For trading, provide educational information only. " +
        "Never guarantee profit or encourage risky real-money trading.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);

        main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(16, 16, 16, 24);
        main.setBackgroundColor(Color.rgb(16, 24, 32));

        scroll.addView(main);
        setContentView(scroll);

        title("🤖 Trading Assistant");

        priceView = new TextView(this);
        priceView.setTextColor(Color.rgb(0, 230, 118));
        priceView.setTextSize(19);
        priceView.setPadding(5, 12, 5, 12);
        main.addView(priceView);

        Button refresh = button("🔄 LIVE PRICE REFRESH");
        main.addView(refresh);
        refresh.setOnClickListener(v -> getPrices());

        title("💬 AI Chat");

        chatBox = new LinearLayout(this);
        chatBox.setOrientation(LinearLayout.VERTICAL);
        main.addView(chatBox);

        addMessage("Assistant",
            "নমস্কাৰ! মই আপোনাৰ AI Assistant। " +
            "অসমীয়া, Roman Assamese বা English-ত প্ৰশ্ন কৰক।");

        messageInput = textEdit(
            "ইয়াত লিখক / Type your message"
        );
        main.addView(messageInput);

        sendButton = button("SEND ➤");
        main.addView(sendButton);

        sendButton.setOnClickListener(v -> sendMessage());

        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        title("📊 Paper Analysis");

        entryInput = numberEdit("Entry Price");
        slInput = numberEdit("Stop Loss");
        tpInput = numberEdit("Take Profit");

        main.addView(entryInput);
        main.addView(slInput);
        main.addView(tpInput);

        Button review = button("REVIEW PAPER TRADE");
        main.addView(review);

        review.setOnClickListener(v -> reviewTrade());

        getPrices();
    }

    void title(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(23);
        t.setTypeface(null, Typeface.BOLD);
        t.setPadding(5, 20, 5, 12);
        main.addView(t);
    }

    Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        return b;
    }

    EditText textEdit(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(Color.LTGRAY);
        e.setTextSize(18);
        e.setMinLines(2);
        e.setMaxLines(5);
        e.setGravity(Gravity.TOP);
        e.setInputType(
            InputType.TYPE_CLASS_TEXT |
            InputType.TYPE_TEXT_FLAG_MULTI_LINE |
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );
        e.setImeOptions(EditorInfo.IME_ACTION_SEND);
        e.setPadding(12, 12, 12, 12);
        e.setBackgroundColor(Color.rgb(38, 50, 56));
        return e;
    }

    EditText numberEdit(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(Color.LTGRAY);
        e.setTextSize(18);
        e.setInputType(
            InputType.TYPE_CLASS_NUMBER |
            InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        e.setPadding(12, 12, 12, 12);
        e.setBackgroundColor(Color.rgb(38, 50, 56));
        return e;
    }

    void addMessage(String who, String msg) {
        TextView t = new TextView(this);
        t.setText(who + ":\n" + msg);
        t.setTextColor(Color.WHITE);
        t.setTextSize(17);
        t.setPadding(10, 12, 10, 12);
        chatBox.addView(t);
    }

    void sendMessage() {
        String msg = messageInput.getText().toString().trim();

        if (msg.isEmpty()) return;

        addMessage("You", msg);
        messageInput.setText("");
        sendButton.setEnabled(false);
        sendButton.setText("AI is thinking...");

        try {
            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content", msg);
            history.put(user);

            JSONArray messages = new JSONArray();

            JSONObject system = new JSONObject();
            system.put("role", "system");
            system.put("content", SYSTEM_PROMPT);
            messages.put(system);

            for (int i = 0; i < history.length(); i++) {
                messages.put(history.getJSONObject(i));
            }

            JSONObject request = new JSONObject();
            request.put("messages", messages);
            request.put("temperature", 0.6);
            request.put("max_tokens", 400);
            request.put("stream", false);

            String requestBody = request.toString();

            new Thread(() -> {
                String answer;

                try {
                    URL url = new URL(AI_URL);
                    HttpURLConnection con =
                        (HttpURLConnection) url.openConnection();

                    con.setRequestMethod("POST");
                    con.setConnectTimeout(15000);
                    con.setReadTimeout(180000);
                    con.setDoOutput(true);

                    con.setRequestProperty(
                        "Content-Type", "application/json"
                    );

                    OutputStream os = con.getOutputStream();
                    os.write(requestBody.getBytes("UTF-8"));
                    os.close();

                    int code = con.getResponseCode();

                    InputStream is = (code >= 200 && code < 300)
                        ? con.getInputStream()
                        : con.getErrorStream();

                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(is, "UTF-8")
                    );

                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }

                    br.close();
                    con.disconnect();

                    if (code < 200 || code >= 300) {
                        throw new Exception(response.toString());
                    }

                    JSONObject result =
                        new JSONObject(response.toString());

                    answer = result
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");

                    if (answer.trim().isEmpty()) {
                        answer = "উত্তৰ খালী আহিছে। আকৌ চেষ্টা কৰক।";
                    }

                } catch (Exception ex) {
                    answer =
                        "AI Server-ৰ সৈতে সংযোগ হোৱা নাই। " +
                        "Termux-ত llama-server চলি আছে নে পৰীক্ষা কৰক।";
                }

                final String finalAnswer = answer;

                handler.post(() -> {
                    addMessage("Assistant", finalAnswer);

                    try {
                        JSONObject assistant = new JSONObject();
                        assistant.put("role", "assistant");
                        assistant.put("content", finalAnswer);
                        history.put(assistant);
                    } catch (Exception ignored) {}

                    sendButton.setEnabled(true);
                    sendButton.setText("SEND ➤");
                });

            }).start();

        } catch (Exception ex) {
            sendButton.setEnabled(true);
            sendButton.setText("SEND ➤");
            addMessage("Assistant", "Message পঠিয়াব পৰা নগ'ল।");
        }
    }

    void getPrices() {
        priceView.setText("Loading live prices...");

        new Thread(() -> {
            StringBuilder result = new StringBuilder();

            String[] coins = {
                "BTCUSDT", "ETHUSDT", "SOLUSDT"
            };

            for (String coin : coins) {
                try {
                    URL url = new URL(
                        "https://api.binance.com/api/v3/ticker/price?symbol="
                        + coin
                    );

                    HttpURLConnection con =
                        (HttpURLConnection) url.openConnection();

                    con.setConnectTimeout(10000);
                    con.setReadTimeout(10000);

                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream())
                    );

                    JSONObject obj = new JSONObject(br.readLine());

                    result.append(coin.replace("USDT", ""))
                        .append(": $")
                        .append(obj.getString("price"))
                        .append("\n");

                    br.close();
                    con.disconnect();

                } catch (Exception ex) {
                    result.append(coin).append(": Unavailable\n");
                }
            }

            handler.post(() ->
                priceView.setText(result.toString())
            );

        }).start();
    }

    void reviewTrade() {
        try {
            double entry = Double.parseDouble(
                entryInput.getText().toString()
            );

            double sl = Double.parseDouble(
                slInput.getText().toString()
            );

            double tp = Double.parseDouble(
                tpInput.getText().toString()
            );

            double risk = Math.abs(entry - sl);
            double reward = Math.abs(tp - entry);

            if (risk == 0 || reward == 0) {
                addMessage("Assistant",
                    "Entry, Stop Loss আৰু Target সঠিককৈ দিয়ক।");
                return;
            }

            double rr = reward / risk;

            String result = String.format(
                Locale.US,
                "Paper Trade Review\nRisk: %.4f\nReward: %.4f\nRisk/Reward: 1:%.2f\n\nএইটো কেৱল শিক্ষামূলক গণনা। লাভৰ নিশ্চয়তা নহয়।",
                risk, reward, rr
            );

            addMessage("Assistant", result);

        } catch (Exception ex) {
            addMessage("Assistant",
                "অনুগ্ৰহ কৰি Entry, SL আৰু TP-ত সংখ্যা লিখক।");
        }
    }
}
