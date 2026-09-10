package com.example;

import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import org.xmlpull.v1.XmlSerializer;
import android.util.Xml;

/** Exercises real UI actions on API25, without waiting for the clock/media timer to become idle. */
public final class LauncherReviewRunner extends Instrumentation {
    private File output;
    private String selectedTag;
    private boolean seed;
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); seed = arguments != null && "seed".equals(arguments.getString("mode")); start(); }
    @Override public void onStart() {
        output = new File(getTargetContext().getExternalFilesDir(null), "launcher-review");
        output.mkdirs();
        try {
            android.content.SharedPreferences prefs = getTargetContext().getSharedPreferences("car_launcher_preferences_2026", 0);
            if (seed) {
                require(prefs.edit().putInt("wallpaper_dim", 27).putInt("icon_size", 72).putInt("app_columns", 5)
                    .putBoolean("resume_music", true).putBoolean("auto_log_trips", false)
                    .putString("launcher_review_sentinel", "existing-install").commit(), "Could not seed old install");
                Bundle seeded = new Bundle(); seeded.putString("stream", "PASS: existing installation seeded\n"); finish(-1, seeded); return;
            }
            require("existing-install".equals(prefs.getString("launcher_review_sentinel", "")), "Upgrade erased existing data");
            require(prefs.getInt("wallpaper_dim", 0) == 27 && prefs.getInt("icon_size", 0) == 72, "Upgrade reset appearance preferences");
            android.accessibilityservice.AccessibilityServiceInfo service = getUiAutomation().getServiceInfo();
            service.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
            getUiAutomation().setServiceInfo(service);
            Intent intent = getTargetContext().getPackageManager().getLaunchIntentForPackage(getTargetContext().getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            getTargetContext().startActivity(intent);
            waitTag("nav_tab_settings");
            SystemClock.sleep(2500);
            snapshot("01-home");
            tapTag("nav_tab_settings");
            waitTag("settings_category_INTERFACE");
            snapshot("02-settings");
            tapTag("settings_category_INTERFACE");
            waitTag("settings_rail_INTERFACE");
            snapshot("03-interface-background");
            tapText("الأشرطة"); snapshot("04-interface-bars");
            tapText("التطبيقات"); snapshot("05-interface-apps");
            tapText("الهوامش"); snapshot("06-interface-margins");
            String[] sections = {"WIDGETS","SCREENSAVER","MEDIA","DRIVING","SECURITY","SYSTEM","ABOUT"};
            for (String section : sections) {
                tapTag("settings_rail_"+section);
                snapshot("07-settings-"+section.toLowerCase());
                if (section.equals("ABOUT")) {
                    require(find("about_identity", true) != null, "Missing approved identity");
                    require(find("2.2.0", false) != null, "Wrong version displayed");
                }
            }
            tapTag("nav_tab_apps");
            waitTag("apps_grid");
            snapshot("08-apps");
            require(find("app_action_hidden", true) == null, "Management visible in everyday app grid");
            AccessibilityNodeInfo first = firstApp(getUiAutomation().getRootInActiveWindow());
            require(first != null, "No installed app to exercise");
            selectedTag = first.getViewIdResourceName();
            setText("apps_search", "NoSuchDarbakApp");
            require(waitText("لا توجد نتائج") != null, "App search did not filter");
            setText("apps_search", "");
            tapTag("apps_manage"); tapTag(selectedTag);
            waitTag("app_action_favorite");
            snapshot("09-app-management");
            tapTag("app_action_favorite");
            waitText("إزالة من المفضلة");
            tapTag("app_action_hidden");
            waitText("إظهار التطبيق");
            tapText("تم");
            tapTag("apps_filter_HIDDEN");
            require(waitTag(selectedTag) != null, "Hidden app not recoverable");
            tapTag(selectedTag);
            tapTag("app_action_hidden");
            waitText("إخفاء التطبيق");
            tapTag("app_action_favorite");
            waitText("إضافة إلى المفضلة");
            tapText("تم");
            tapTag("apps_manage");
            require(waitTag(selectedTag) != null, "App not restored after unhiding");
            snapshot("10-apps-restored");
            tapTag("nav_tab_music");
            waitTag("music_queue");
            AccessibilityNodeInfo song = firstPrefix(getUiAutomation().getRootInActiveWindow(), "playlist_item_");
            require(song != null, "Seeded music not found");
            tap(song);
            SystemClock.sleep(1200);
            tapTag("btn_full_player_play");
            SystemClock.sleep(300);
            snapshot("11-music");
            require(find("<unknown>", false) == null, "Unknown metadata leaked into UI");
            AccessibilityNodeInfo seek = waitTag("music_seek");
            Rect bounds = new Rect(); seek.getBoundsInScreen(bounds);
            touch(bounds.left + bounds.width() * .55f, bounds.exactCenterY());
            snapshot("12-music-seek");
            tapDescription("مستوى الصوت");
            snapshot("13-music-volume");
            tapText("خفض"); tapText("رفع"); tapText("تم");
            tapTag("music_expand");
            require(find("music_queue", true) == null, "Expanded player still shows queue");
            snapshot("14-music-expanded");
            sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
            waitTag("music_queue");
            setText("music_search", "NoSuchDarbakSong");
            waitText("لا توجد نتائج"); snapshot("15-music-search-empty");
            setText("music_search", "");
            tapTag("nav_tab_trip");
            snapshot("16-trip");
            tapTag("nav_tab_settings");
            // This tab has its own fresh landing when returning from another page.
            waitTag("settings_category_ABOUT"); tapTag("settings_category_ABOUT");
            snapshot("17-about-return");
            tapTag("settings_back");
            waitTag("settings_category_INTERFACE");
            snapshot("18-settings-return");
            tapTag("settings_category_INTERFACE");
            sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
            waitTag("settings_category_INTERFACE");
            require(prefs.getInt("wallpaper_dim", 0) == 27 && prefs.getInt("icon_size", 0) == 72, "Navigation reset saved preferences");
            write("result.txt", "PASS: upgrade preserves data, settings categories, interface tabs, approved identity, app search, favorite/hide/restore, music playback, seek, volume, expanded player, trip, hardware Back and return navigation\n");
            Bundle result = new Bundle(); result.putString("stream", "PASS: Launcher review completed\n");
            finish(-1, result);
        } catch (Throwable failure) {
            try { snapshot("failure"); write("result.txt", "FAIL: "+failure.toString()+"\n"); } catch (Throwable ignored) {}
            Bundle result = new Bundle(); result.putString("stream", "FAIL: "+failure.toString()+"\n");
            finish(1, result);
        }
    }
    private void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private void write(String name, String text) throws Exception {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(new File(output, name)), "UTF-8")) { writer.write(text); }
    }
    private AccessibilityNodeInfo find(String value, boolean tag) { return findNode(getUiAutomation().getRootInActiveWindow(), value, tag); }
    private AccessibilityNodeInfo findNode(AccessibilityNodeInfo node, String value, boolean tag) {
        if (node == null) return null;
        String match = tag ? node.getViewIdResourceName() : node.getText() == null ? null : node.getText().toString();
        if (value.equals(match) && node.isVisibleToUser()) return node;
        for (int i=0; i<node.getChildCount(); i++) { AccessibilityNodeInfo result = findNode(node.getChild(i),value,tag); if (result != null) return result; }
        return null;
    }
    private AccessibilityNodeInfo firstApp(AccessibilityNodeInfo node) {
        if (node == null) return null;
        String id = node.getViewIdResourceName();
        if (id != null && id.startsWith("app_card_") && !id.contains("settings") && node.isVisibleToUser()) return node;
        for (int i=0;i<node.getChildCount();i++) { AccessibilityNodeInfo result = firstApp(node.getChild(i)); if(result!=null)return result; }
        return null;
    }
    private AccessibilityNodeInfo firstPrefix(AccessibilityNodeInfo node, String prefix) {
        if (node == null) return null;
        if(node.getViewIdResourceName()!=null && node.getViewIdResourceName().startsWith(prefix) && node.isVisibleToUser())return node;
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo found=firstPrefix(node.getChild(i),prefix);if(found!=null)return found;}
        return null;
    }
    private AccessibilityNodeInfo waitTag(String id) { return waitNode(id,true); }
    private AccessibilityNodeInfo waitText(String value) { return waitNode(value,false); }
    private AccessibilityNodeInfo waitNode(String value, boolean tag) {
        long until = SystemClock.uptimeMillis()+12000;
        do { AccessibilityNodeInfo node=find(value,tag); if(node!=null)return node; SystemClock.sleep(250); } while(SystemClock.uptimeMillis()<until);
        throw new AssertionError("Missing "+value);
    }
    private void tapTag(String id) {
        if (id.startsWith("settings_rail_") && find(id,true)==null) {
            AccessibilityNodeInfo rail = find("settings_rail_list",true);
            if(rail!=null) { rail.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD); SystemClock.sleep(400); }
        }
        tap(waitTag(id));
    }
    private void tapText(String value) { tap(waitText(value)); }
    private void tapDescription(String text) {
        AccessibilityNodeInfo node = findDescription(getUiAutomation().getRootInActiveWindow(),text);
        require(node!=null,"Missing action "+text); tap(node);
    }
    private AccessibilityNodeInfo findDescription(AccessibilityNodeInfo node,String text){
        if(node==null)return null;
        if(node.getContentDescription()!=null && text.equals(node.getContentDescription().toString()))return node;
        for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo n=findDescription(node.getChild(i),text);if(n!=null)return n;}return null;
    }
    private void tap(AccessibilityNodeInfo node) {
        Rect bounds = new Rect(); node.getBoundsInScreen(bounds);
        require(bounds.width()>0 && bounds.height()>0,"Empty touch target");
        touch(bounds.exactCenterX(),bounds.exactCenterY());
    }
    private void touch(float x,float y){
        long now=SystemClock.uptimeMillis();
        MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,y,0); down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        getUiAutomation().injectInputEvent(down,true); down.recycle();
        MotionEvent up=MotionEvent.obtain(now,now+80,MotionEvent.ACTION_UP,x,y,0); up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        getUiAutomation().injectInputEvent(up,true);up.recycle();SystemClock.sleep(600);
    }
    private void setText(String tag,String text){
        AccessibilityNodeInfo node=waitTag(tag); Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);
        require(node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args),"Cannot edit "+tag);SystemClock.sleep(600);
    }
    private void snapshot(String name) throws Exception {
        SystemClock.sleep(400);
        Bitmap bitmap=getUiAutomation().takeScreenshot();require(bitmap!=null,"No screenshot");
        try(FileOutputStream file=new FileOutputStream(new File(output,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,file);}bitmap.recycle();
        try(FileOutputStream file=new FileOutputStream(new File(output,name+".xml"))){
            XmlSerializer xml=Xml.newSerializer();xml.setOutput(file,"UTF-8");xml.startDocument("UTF-8",true);
            xml.startTag(null,"hierarchy");serialize(xml,getUiAutomation().getRootInActiveWindow());xml.endTag(null,"hierarchy");xml.endDocument();
        }
    }
    private void serialize(XmlSerializer xml,AccessibilityNodeInfo node)throws Exception{
        if(node==null)return;xml.startTag(null,"node");
        xml.attribute(null,"text",node.getText()==null?"":node.getText().toString());
        xml.attribute(null,"content-desc",node.getContentDescription()==null?"":node.getContentDescription().toString());
        xml.attribute(null,"resource-id",node.getViewIdResourceName()==null?"":node.getViewIdResourceName());
        Rect rect=new Rect();node.getBoundsInScreen(rect);xml.attribute(null,"bounds",rect.toShortString());
        for(int i=0;i<node.getChildCount();i++)serialize(xml,node.getChild(i));xml.endTag(null,"node");
    }
}
