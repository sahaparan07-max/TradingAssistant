package com.example.tradingassistant;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Typeface;
import android.view.View;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONObject;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    LinearLayout root;
    TextView marketText, resultText, chatText;
    EditText account, entry, prevHigh, prevLow, target, positionValue, losses;
    Spinner market, direction;
    CheckBox losingAdd;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());
    DecimalFormat df = new DecimalFormat("#,##0.########");

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28,24,28,28); root.setBackgroundColor(0xFFF5F7FA);
        scroll.addView(root); setContentView(scroll);
        TextView title = new TextView(this); title.setText("Trading Assistant"); title.setTextSize(26); title.setTypeface(null, Typeface.BOLD); add(title);
        TextView subtitle = new TextView(this); subtitle.setText("Paper trading • Risk-first • অসমীয়া"); subtitle.setTextSize(15); add(subtitle);
        marketText = new TextView(this); marketText.setText("Live price: loading…"); marketText.setTextSize(17); add(marketText);
        market = spinner(new String[]{"BTCUSDT","ETHUSDT","SOLUSDT"});
        market.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { loadPrice(); }
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        addLabel("Market"); add(market);
        addLabel("Account size (₹)"); account = edit("10000");
        addLabel("Direction"); direction = spinner(new String[]{"LONG","SHORT"}); add(direction);
        addLabel("Entry price (market quote units)"); entry = edit("");
        addLabel("Yesterday High"); prevHigh = edit("");
        addLabel("Yesterday Low"); prevLow = edit("");
        addLabel("Target price"); target = edit("");
        addLabel("Planned position value (₹)"); positionValue = edit("1000");
        addLabel("Losses in a row (0–3)"); losses = edit("0");
        losingAdd = new CheckBox(this); losingAdd.setText("Already in a losing trade and want to add to it"); add(losingAdd);
        Button analyze = new Button(this); analyze.setText("CHECK TRADE"); add(analyze);
        resultText = new TextView(this); resultText.setTextSize(16); resultText.setPadding(0,18,0,18); add(resultText);
        addLabel("AI Chat (basic offline helper)");
        EditText chatInput = edit("RSI, stop-loss, or risk সম্পর্কে সোধক");
        Button chatButton = new Button(this); chatButton.setText("Ask"); add(chatButton);
        chatText = new TextView(this); chatText.setTextSize(15); add(chatText);
        analyze.setOnClickListener(v -> analyzeTrade());
        chatButton.setOnClickListener(v -> answerChat(chatInput.getText().toString()));
        loadPrice();
    }
    void add(View v) { root.addView(v, new LinearLayout.LayoutParams(-1, -2)); }
    void addLabel(String s) { TextView t=new TextView(this); t.setText(s); t.setTypeface(null,Typeface.BOLD); t.setPadding(0,14,0,3); add(t); }
    EditText edit(String val) { EditText e=new EditText(this); e.setSingleLine(true); e.setText(val); e.setInputType(8194); add(e); return e; }
    Spinner spinner(String[] opts) { Spinner s=new Spinner(this); ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,opts); s.setAdapter(a); return s; }
    double num(EditText e) { try { return Double.parseDouble(e.getText().toString().trim()); } catch(Exception ex) { return Double.NaN; } }

    void loadPrice() {
        String symbol = market == null ? "BTCUSDT" : market.getSelectedItem().toString();
        marketText.setText("Live price: loading " + symbol + "…");
        executor.execute(() -> {
            String out = "Price unavailable. Check internet/API status.";
            try {
                URL url = new URL("https://api.binance.com/api/v3/ticker/price?symbol=" + symbol);
                HttpsURLConnection c=(HttpsURLConnection)url.openConnection(); c.setConnectTimeout(7000); c.setReadTimeout(7000);
                BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder sb=new StringBuilder(); String line; while((line=r.readLine())!=null) sb.append(line);
                JSONObject j=new JSONObject(sb.toString()); out="Live public price ("+symbol+"): "+j.getString("price")+" USDT\nSource: Binance public ticker; not an execution price.";
                c.disconnect();
            } catch(Exception ignored) {}
            final String text=out; handler.post(() -> marketText.setText(text));
        });
    }
    void analyzeTrade() {
        double a=num(account), en=num(entry), hi=num(prevHigh), lo=num(prevLow), tg=num(target), pv=num(positionValue), ls=num(losses);
        String dir=direction.getSelectedItem().toString();
        if(!finitePositive(a)||!finitePositive(en)||!finitePositive(hi)||!finitePositive(lo)||!finitePositive(tg)||!finitePositive(pv)||!Double.isFinite(ls)||ls<0||ls>3) {
            showSkip("Missing/invalid input. Enter account, entry, yesterday high/low, target, position value and losses."); return;
        }
        if(ls>=3) { showSkip("3 consecutive losses reported. Next: take 3 days off."); return; }
        if(losingAdd.isChecked()) { showSkip("Never add to a loser. Next: do not add to the losing trade."); return; }
        if(pv>a) { showSkip("Planned position exceeds account size; no leverage above the account. Next: reduce position value to at most ₹"+df.format(a)+"."); return; }
        double buffer=Math.max(en*0.001, 0.01); // small 0.1% buffer for this starter
        double stop=dir.equals("LONG") ? lo-buffer : hi+buffer;
        if((dir.equals("LONG") && !(stop<en && en>lo)) || (dir.equals("SHORT") && !(stop>en && en<hi))) {
            showSkip("Entry/previous-day levels do not create a valid stop on the required side. Next: verify candle data."); return;
        }
        double riskDistance=Math.abs(en-stop);
        double rewardDistance=dir.equals("LONG") ? tg-en : en-tg;
        if(rewardDistance<=0 || riskDistance<=0) { showSkip("No clean setup: target must be beyond entry in the trade direction. Next: review the levels."); return; }
        double rr=rewardDistance/riskDistance;
        if(rr<1.0) { showSkip("Reward is smaller than risk (R:R "+df.format(rr)+"). Next: skip this setup."); return; }
        double riskAmount=a*0.005;
        double units=riskAmount/riskDistance;
        double sizeValue=units*en;
        boolean capped=sizeValue>a;
        if(capped) { sizeValue=a; units=a/en; }
        resultText.setText("VERDICT: TAKE (paper only)\nENTRY: "+df.format(en)+"\nSTOP: "+df.format(stop)+"\nTARGET: "+df.format(tg)+
            "\nRISK AMOUNT: ₹"+df.format(riskAmount)+"\nPOSITION SIZE: "+df.format(units)+" units = ₹"+df.format(sizeValue)+
            "\nREWARD-TO-RISK: "+df.format(rr)+":1\nRULES CHECK: Rule 1 "+(capped?"FAIL (capped by account)":"OK")+
            ", Rule 2 OK, Rule 3 OK, Rule 4 OK, Rule 5 "+(capped?"OK after cap":"OK")+
            (capped?"\nPosition size was reduced to account size.":"")+
            "\n\nThis is a calculation, not a prediction. Fees, slippage, contract multipliers and gaps are not included.");
    }
    boolean finitePositive(double x) { return Double.isFinite(x) && x>0; }
    void showSkip(String why) { resultText.setText("VERDICT: SKIP\nWHY: "+why); }
    void answerChat(String q) {
        String s=q.toLowerCase();
        if(s.contains("rsi")) chatText.setText("RSI (Relative Strength Index) হৈছে price movement-ৰ গতি মাপা indicator। ই নিজে buy/sell নিশ্চিত নকৰে।");
        else if(s.contains("stop") || s.contains("loss")) chatText.setText("Stop-Loss-এ trade-ৰ পৰিকল্পিত risk সীমিত কৰাত সহায় কৰে; gap বা slippage-ৰ বাবে ক্ষতি বেছি হ'ব পাৰে।");
        else if(s.contains("risk") || s.contains("0.5")) chatText.setText("০.৫% risk = account balance × 0.005। উদাহৰণ: ₹10,000 account-ত ₹50।");
        else if(s.trim().isEmpty()) chatText.setText("প্ৰশ্ন লিখক।");
        else chatText.setText("এইটো basic offline chat। এই সংস্কৰণত সম্পূৰ্ণ AI সংযোগ নাই; AI API key APK-ৰ ভিতৰত ৰখা নিৰাপদ নহয়।");
    }
}
