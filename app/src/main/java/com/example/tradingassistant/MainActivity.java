
package com.example.tradingassistant;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Typeface;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    LinearLayout root;
    TextView priceText, analysisText, resultText, chatText;
    EditText entry, target, account;
    Spinner market, timeframe, direction;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());
    DecimalFormat df = new DecimalFormat("#,##0.########");

    String[] symbols = {"BTCUSDT","ETHUSDT","SOLUSDT"};
    String[] frames = {"1m","3m","5m","15m","30m","1h","4h","1d"};

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(25,20,25,30);
        root.setBackgroundColor(0xFFF5F7FA);
        scroll.addView(root);
        setContentView(scroll);

        TextView title = new TextView(this);
        title.setText("TRADING ASSISTANT PRO");
        title.setTextSize(25);
        title.setTypeface(null, Typeface.BOLD);
        add(title);

        label("অসমীয়া Trading Analysis • Paper Mode");

        label("Market");
        market = spinner(symbols);
        add(market);

        priceText = new TextView(this);
        priceText.setTextSize(18);
        add(priceText);

        market.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                loadPrice();
            }
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        label("Timeframe");
        timeframe = spinner(frames);
        add(timeframe);

        Button analyze = button("ANALYZE MARKET");
        add(analyze);

        analysisText = new TextView(this);
        analysisText.setTextSize(15);
        add(analysisText);

        analyze.setOnClickListener(v -> loadAnalysis());

        label("PAPER TRADE CALCULATOR");

        label("Direction");
        direction = spinner(new String[]{"LONG","SHORT"});
        add(direction);

        label("Account Size (₹)");
        account = edit("10000");

        label("Entry Price");
        entry = edit("");

        label("Target Price");
        target = edit("");

        Button check = button("CHECK PAPER TRADE");
        add(check);

        resultText = new TextView(this);
        resultText.setTextSize(16);
        add(resultText);

        check.setOnClickListener(v -> calculate());

        label("অসমীয়া AI Help");

        EditText question = edit("");
        question.setHint("RSI, EMA, MACD, ATR, Risk আদি সোধক");

        Button ask = button("ASK");
        add(ask);

        chatText = new TextView(this);
        chatText.setTextSize(16);
        add(chatText);

        ask.setOnClickListener(v -> answer(question.getText().toString()));

        label("সতৰ্কবাণী: এইটো শিক্ষামূলক Paper Analysis। ই real trading order নিদিয়ে।");
        loadPrice();
    }

    void add(View v) {
        root.addView(v, new LinearLayout.LayoutParams(-1,-2));
    }

    void label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(15);
        t.setTypeface(null,Typeface.BOLD);
        t.setPadding(0,15,0,5);
        add(t);
    }

    EditText edit(String s) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setText(s);
        e.setInputType(8194);
        add(e);
        return e;
    }

    Spinner spinner(String[] data) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_dropdown_item, data);
        s.setAdapter(a);
        return s;
    }

    Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        return b;
    }

    void loadPrice() {
        String symbol = market.getSelectedItem().toString();
        priceText.setText("Loading " + symbol + "...");

        executor.execute(() -> {
            String result = "Price unavailable";
            try {
                JSONObject j = getJson(
                    "https://api.binance.com/api/v3/ticker/price?symbol=" + symbol
                );
                result = symbol + "\nLive Price: " +
                    j.getString("price") + " USDT";
            } catch(Exception e) {
                result = "Price unavailable. Internet/API check কৰক.";
            }

            String out = result;
            handler.post(() -> priceText.setText(out));
        });
    }

    JSONObject getJson(String address) throws Exception {
        URL url = new URL(address);
        HttpsURLConnection c =
            (HttpsURLConnection) url.openConnection();

        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        c.setRequestMethod("GET");

        try {
            BufferedReader r = new BufferedReader(
                new InputStreamReader(c.getInputStream())
            );

            StringBuilder sb = new StringBuilder();
            String line;

            while((line = r.readLine()) != null) {
                sb.append(line);
            }

            r.close();
            return new JSONObject(sb.toString());
        } finally {
            c.disconnect();
        }
    }

    void loadAnalysis() {
        String symbol = market.getSelectedItem().toString();
        String tf = timeframe.getSelectedItem().toString();

        analysisText.setText("Market data loading...");

        executor.execute(() -> {
            String result;

            try {
                String address =
                    "https://api.binance.com/api/v3/klines?symbol=" +
                    symbol + "&interval=" + tf + "&limit=100";

                URL url = new URL(address);
                HttpsURLConnection c =
                    (HttpsURLConnection)url.openConnection();

                c.setConnectTimeout(10000);
                c.setReadTimeout(10000);

                JSONArray data;

                try {
                    BufferedReader r = new BufferedReader(
                        new InputStreamReader(c.getInputStream())
                    );

                    StringBuilder sb = new StringBuilder();
                    String line;

                    while((line = r.readLine()) != null) {
                        sb.append(line);
                    }

                    r.close();
                    data = new JSONArray(sb.toString());
                } finally {
                    c.disconnect();
                }

                double[] close = new double[data.length()];
                double[] high = new double[data.length()];
                double[] low = new double[data.length()];

                for(int i=0; i<data.length(); i++) {
                    JSONArray candle = data.getJSONArray(i);
                    close[i] = candle.getDouble(4);
                    high[i] = candle.getDouble(2);
                    low[i] = candle.getDouble(3);
                }

                double ema9 = ema(close,9);
                double ema21 = ema(close,21);
                double rsi = rsi(close,14);
                double atr = atr(high,low,close,14);

                double last = close[close.length-1];

                String trend = ema9 > ema21 ?
                    "Bullish trend" : "Bearish trend";

                String momentum = rsi > 70 ?
                    "Overbought zone" :
                    rsi < 30 ? "Oversold zone" : "Neutral zone";

                result =
                    "MARKET: " + symbol +
                    "\nTIMEFRAME: " + tf +
                    "\n\nLast Close: " + df.format(last) +
                    "\nEMA 9: " + df.format(ema9) +
                    "\nEMA 21: " + df.format(ema21) +
                    "\nRSI 14: " + df.format(rsi) +
                    "\nATR 14: " + df.format(atr) +
                    "\n\nTrend: " + trend +
                    "\nMomentum: " + momentum +
                    "\n\nEMA/RSI/ATR are indicators, not guaranteed signals.";

            } catch(Exception e) {
                result = "Analysis unavailable.\nInternet/API পুনৰ পৰীক্ষা কৰক.";
            }

            String out = result;
            handler.post(() -> analysisText.setText(out));
        });
    }

    double ema(double[] values, int period) {
        double k = 2.0/(period+1);
        double result = values[0];

        for(int i=1;i<values.length;i++) {
            result = values[i]*k + result*(1-k);
        }
        return result;
    }

    double rsi(double[] values, int period) {
        double gain=0, loss=0;

        for(int i=values.length-period;i<values.length;i++) {
            double change = values[i]-values[i-1];

            if(change>0) gain+=change;
            else loss-=change;
        }

        if(loss==0) return 100;

        double rs = (gain/period)/(loss/period);
        return 100-(100/(1+rs));
    }

    double atr(double[] h,double[] l,double[] c,int period) {
        double sum=0;

        for(int i=h.length-period;i<h.length;i++) {
            double tr=Math.max(
                h[i]-l[i],
                Math.max(
                    Math.abs(h[i]-c[i-1]),
                    Math.abs(l[i]-c[i-1])
                )
            );
            sum+=tr;
        }

        return sum/period;
    }

    void calculate() {
        try {
            double a=Double.parseDouble(account.getText().toString());
            double en=Double.parseDouble(entry.getText().toString());
            double tg=Double.parseDouble(target.getText().toString());

            if(a<=0 || en<=0 || tg<=0 || en==tg) {
                throw new Exception();
            }

            String dir=direction.getSelectedItem().toString();

            boolean valid = dir.equals("LONG") ? tg>en : tg<en;

            if(!valid) {
                resultText.setText("SKIP: Target direction ভুল হৈছে।");
                return;
            }

            double risk=a*0.005;
            double distance=Math.abs(tg-en);
            double rr=distance/(en*0.01);

            resultText.setText(
                "PAPER CALCULATION" +
                "\nEntry: "+df.format(en) +
                "\nTarget: "+df.format(tg) +
                "\nIllustrative Risk Budget: ₹"+df.format(risk) +
                "\nReward Distance: "+df.format(distance) +
                "\n\nএইটো পূৰ্ণ Stop Loss বা trade signal নহয়।"
            );

        } catch(Exception e) {
            resultText.setText("সঠিক Account, Entry আৰু Target দিয়ক।");
        }
    }

    void answer(String q) {
        String s=q.toLowerCase().trim();

        if(s.contains("rsi")) {
            chatText.setText("RSI হৈছে momentum indicator। সাধাৰণতে 70-ৰ ওপৰত overbought আৰু 30-ৰ তলত oversold বুলি চোৱা হয়।");
        } else if(s.contains("ema")) {
            chatText.setText("EMA হৈছে Exponential Moving Average। ই price trend বুজাত সহায় কৰে।");
        } else if(s.contains("macd")) {
            chatText.setText("MACD হৈছে trend আৰু momentum indicator। Signal line আৰু histogram ব্যৱহাৰ কৰা হয়।");
        } else if(s.contains("atr")) {
            chatText.setText("ATR হৈছে Average True Range। ই market volatility মাপে।");
        } else if(s.contains("risk")) {
            chatText.setText("Risk management-ত account-ৰ সৰু অংশহে risk কৰা হয়। 0.5% of ₹10000 = ₹50।");
        } else if(s.contains("hello") || s.contains("hi")) {
            chatText.setText("নমস্কাৰ! আপোনাক Trading Assistant-লৈ স্বাগতম। কি জানিব বিচাৰে?");
        } else if(s.contains("khobor")) {
            chatText.setText("মই ভাল আছোঁ। আপোনাৰ কেনে আছে? আজি Market Analysis কৰিব পাৰোঁ।");
        } else if(s.isEmpty()) {
            chatText.setText("প্ৰশ্ন লিখক।");
        } else {
            chatText.setText("এইটো Offline Help সংস্কৰণ। এই প্ৰশ্নৰ উত্তৰৰ বাবে সম্পূৰ্ণ AI Backend সংযোগ কৰিব লাগিব।");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
