package com.example.smartfridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private final int bg = Color.rgb(7, 15, 35), panel = Color.rgb(16, 30, 59), blue = Color.rgb(40, 190, 255), white = Color.WHITE;
    private SharedPreferences prefs;
    private LinearLayout root, list;
    private TextView title, status;
    private String location = "Fridge";
    private JSONArray items;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("smartfridge", MODE_PRIVATE);
        try { items = new JSONArray(prefs.getString("items", "[]")); } catch (Exception e) { items = new JSONArray(); }
        showHome();
    }
    private TextView text(String s, int size) {
        TextView t = new TextView(this); t.setText(s); t.setTextColor(white); t.setTextSize(size); t.setPadding(12,10,12,10); return t;
    }
    private Button button(String s) {
        Button b = new Button(this); b.setText(s); b.setTextColor(white); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(19, 72, 112))); return b;
    }
    private void base(String heading) {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(16,18,16,12); root.setBackgroundColor(bg);
        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list, new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        list.addView(text("SMARTFRIDGE  //  INVENTORY SYSTEM", 13));
        title = text(heading, 27); title.setTextColor(blue); list.addView(title);
    }
    private void showHome() {
        base("Your food, under control.");
        TextView intro = text("OFFLINE INVENTORY  •  SYNC-READY DESIGN", 13); intro.setTextColor(blue); list.addView(intro);
        LinearLayout tabs = new LinearLayout(this);
        Button fridge = button("FRIDGE"); fridge.setOnClickListener(v -> { location="Fridge"; showInventory(); });
        Button freezer = button("FREEZER"); freezer.setOnClickListener(v -> { location="Freezer"; showInventory(); });
        Button shopping = button("SHOPPING LIST"); shopping.setOnClickListener(v -> showShopping());
        tabs.addView(fridge,new LinearLayout.LayoutParams(0,-2,1)); tabs.addView(freezer,new LinearLayout.LayoutParams(0,-2,1));
        list.addView(tabs); list.addView(shopping);
        list.addView(text("Saved items stay on this phone. Internet-powered barcode lookup and AI produce recognition are planned next steps and are not active in this starter build.", 15));
        Button vf=button("VIEW FRIDGE"); vf.setOnClickListener(w -> {location="Fridge";showInventory();}); list.addView(vf);
        Button af=button("ADD FOOD"); af.setOnClickListener(w -> addFood()); list.addView(af);
        Button sl=button("SHOPPING LIST"); sl.setOnClickListener(w -> showShopping()); list.addView(sl);
        status = text("ITEMS SAVED: " + items.length(), 14); list.addView(status);
    }
    private void showInventory() {
        base(location.toUpperCase() + " INVENTORY");
        Button add = button("+ ADD FOOD"); add.setOnClickListener(v -> addFood()); list.addView(add);
        int count=0;
        for (int i=0;i<items.length();i++) {
            JSONObject item=items.optJSONObject(i); if(item==null || !location.equals(item.optString("location"))) continue; count++;
            LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(10,8,10,8); card.setBackgroundColor(panel);
            card.addView(text(item.optString("name","Food"),18));
            card.addView(text("Quantity: "+item.optInt("qty",1)+"   •   Minimum: "+item.optInt("min",1),14));
            LinearLayout actions = new LinearLayout(this);
            Button less=button("−"); less.setOnClickListener(v->{item.put("qty",Math.max(0,item.optInt("qty",1)-1));save();showInventory();});
            Button more=button("+"); more.setOnClickListener(v->{item.put("qty",item.optInt("qty",1)+1);save();showInventory();});
            Button edit=button("EDIT"); edit.setOnClickListener(v->editItem(item));
            actions.addView(less,new LinearLayout.LayoutParams(0,-2,1)); actions.addView(more,new LinearLayout.LayoutParams(0,-2,1)); actions.addView(edit,new LinearLayout.LayoutParams(0,-2,1)); card.addView(actions);
            if(item.optInt("qty",1)<=item.optInt("min",1)) card.addView(text("LOW STOCK — consider adding to shopping list",13));
            list.addView(card);
        }
        if(count==0) list.addView(text("Nothing here yet. Tap + ADD FOOD to get started.",16));
        Button home=button("HOME"); home.setOnClickListener(w->showHome()); list.addView(home);
    }
    private void addFood() {
        EditText name = new EditText(this); name.setHint("Food name (e.g. milk)"); name.setTextColor(white); name.setHintTextColor(0xff9fb3d4);
        EditText qty = new EditText(this); qty.setHint("Quantity"); qty.setInputType(InputType.TYPE_CLASS_NUMBER); qty.setText("1"); qty.setTextColor(white);
        EditText min = new EditText(this); min.setHint("Minimum quantity"); min.setInputType(InputType.TYPE_CLASS_NUMBER); min.setText("1"); min.setTextColor(white);
        Spinner spinner = new Spinner(this); spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Fridge","Freezer"}));
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(20,0,20,0);
        form.addView(name); form.addView(qty); form.addView(min); form.addView(spinner);
        new AlertDialog.Builder(this).setTitle("Add food").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
            String n=name.getText().toString().trim(); if(n.isEmpty()){toast("Enter a food name");return;}
            JSONObject obj=new JSONObject(); try {obj.put("name",n);obj.put("qty",Math.max(0,parse(qty.getText().toString(),1)));obj.put("min",Math.max(0,parse(min.getText().toString(),1)));obj.put("location",spinner.getSelectedItem().toString());obj.put("shopping",false);items.put(obj);save();location=obj.optString("location");showInventory();}catch(Exception e){toast("Could not save item");}
        }).show();
    }
    private void editItem(JSONObject item) {
        EditText name=new EditText(this); name.setText(item.optString("name")); name.setTextColor(white);
        EditText min=new EditText(this); min.setInputType(InputType.TYPE_CLASS_NUMBER); min.setText(String.valueOf(item.optInt("min",1))); min.setTextColor(white);
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.addView(name);form.addView(min);
        new AlertDialog.Builder(this).setTitle("Edit food").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{try{item.put("name",name.getText().toString().trim());item.put("min",parse(min.getText().toString(),1));save();showInventory();}catch(Exception e){toast("Could not update");}}).setNeutralButton("Delete",(d,w)->{JSONArray next=new JSONArray();for(int i=0;i<items.length();i++)if(items.optJSONObject(i)!=item)next.put(items.opt(i));items=next;save();showInventory();}).show();
    }
    private void showShopping() {
        base("SHOPPING LIST");
        Button add=button("+ ADD TO LIST"); add.setOnClickListener(v->{
            EditText input=new EditText(this);input.setHint("Item to buy");new AlertDialog.Builder(this).setTitle("Shopping item").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Add",(d,w)->{String n=input.getText().toString().trim();if(!n.isEmpty()){JSONObject o=new JSONObject();try{o.put("name",n);o.put("shopping",true);o.put("done",false);items.put(o);save();showShopping();}catch(Exception e){}}}).show();
        });list.addView(add);
        for(int i=0;i<items.length();i++){JSONObject o=items.optJSONObject(i);if(o==null)continue;
            boolean manual=o.optBoolean("shopping",false), low=o.has("qty")&&o.optInt("qty")<=o.optInt("min",1);
            if(manual || low){CheckBox c=new CheckBox(this);c.setTextColor(white);c.setText(o.optString("name")+(low&&!manual?"  •  LOW STOCK":""));c.setChecked(o.optBoolean("done",false));c.setOnCheckedChangeListener((b,checked)->{o.put("done",checked);save();});list.addView(c);}
        }
        Button home=button("HOME"); home.setOnClickListener(w->showHome()); list.addView(home);
    }
    private void save(){prefs.edit().putString("items",items.toString()).apply();}
    private int parse(String s,int fallback){try{return Integer.parseInt(s.trim());}catch(Exception e){return fallback;}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
