/* 
 * Copyright (C) 2013-2014 Jorrit "Chainfire" Jongma
 * Copyright (C) 2013-2015 The OmniROM Project
 */
/* 
 * This file is part of OpenDelta.
 * 
 * OpenDelta is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * OpenDelta is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with OpenDelta. If not, see <http://www.gnu.org/licenses/>.
 */

package eu.chainfire.opendelta;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Environment;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.omnirom.omnilib.utils.OmniServiceLocator;

public class Config {
    private static Config instance = null;

    public static Config getInstance(Context context) {
        if (instance == null) {
            instance = new Config(context.getApplicationContext());
        }
        return instance;
    }

    private final static String PREF_SECURE_MODE_NAME = "secure_mode";
    private final static String PREF_SHOWN_RECOVERY_WARNING_SECURE_NAME = "shown_recovery_warning_secure";
    private final static String PREF_SHOWN_RECOVERY_WARNING_NOT_SECURE_NAME = "shown_recovery_warning_not_secure";
    private final static String PREF_AB_PERF_MODE_NAME = "ab_perf_mode";
    private final static boolean PREF_AB_PERF_MODE_DEFAULT = true;
    private static final String PROP_AB_DEVICE = "ro.build.ab_update";

    private final SharedPreferences prefs;

    private final String property_version;
    private final String property_device;
    private final String filename_base;
    private final String path_base;
    private final String path_flash_after_update;
    private String url_base_delta;
    private String url_base_update;
    private String url_base_full;
    private final boolean apply_signature;
    private final boolean inject_signature_enable;
    private final String inject_signature_keys;
    private final boolean secure_mode_enable;
    private final boolean secure_mode_default;
    private final boolean keep_screen_on;
    private final String filename_base_prefix;
    private String url_base_json;
    private final String android_version;
    private final String weekly_version_tag;
    private final String gapps_version_tag;
    private final String microg_version_tag;
    private final boolean secondaryDevice;
    private final boolean downloadOnlyDevice;

    /*
     * Using reflection voodoo instead calling the hidden class directly, to
     * dev/test outside of AOSP tree
     */
    private String getProperty(Context context, String key, String defValue) {
        try {
            Class<?> SystemProperties = context.getClassLoader().loadClass(
                    "android.os.SystemProperties");
            Method get = SystemProperties.getMethod("get", new Class[] {
                    String.class, String.class });
            return (String) get.invoke(null, new Object[] { key, defValue });
        } catch (Exception e) {
            // A lot of voodoo could go wrong here, return failure instead of
            // crash
            Logger.ex(e);
        }
        return null;
    }

    private Config(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Resources res = context.getResources();

        secondaryDevice = res.getBoolean(R.bool.secondary_device);
        downloadOnlyDevice = res.getBoolean(R.bool.download_only_device);

        property_version = getProperty(context,
                res.getString(R.string.property_version), "");
        property_device = getProperty(context,
                res.getString(R.string.property_device), "");
        filename_base = String.format(Locale.ENGLISH,
                res.getString(R.string.filename_base), property_version);

        path_base = String.format(Locale.ENGLISH, "%s%s%s%s", Environment
                .getExternalStorageDirectory().getAbsolutePath(),
                File.separator, res.getString(R.string.path_base),
                File.separator);
        path_flash_after_update = String.format(Locale.ENGLISH, "%s%s%s",
                path_base, "FlashAfterUpdate", File.separator);

        String urlBaseDelta =  OmniServiceLocator.buildBuildsDeltasRootUrl(context);
        if (TextUtils.isEmpty(urlBaseDelta)) {
            urlBaseDelta = res.getString(R.string.url_base_delta);
        }
        url_base_delta = urlBaseDelta + property_device + "/";
        url_base_update = url_base_delta;
        /*url_base_delta = String.format(Locale.ENGLISH,
                res.getString(R.string.url_base_delta), property_device);*/
        /*url_base_update = String.format(Locale.ENGLISH,
                res.getString(R.string.url_base_update), property_device);*/

        String urlBaseFull =  OmniServiceLocator.buildBuildsRootUrl(context, false);
        if (TextUtils.isEmpty(urlBaseFull)) {
            urlBaseFull = res.getString(R.string.url_base_full);
        }
        url_base_full = urlBaseFull + property_device + "/";
        /*url_base_full = String.format(Locale.ENGLISH,
                res.getString(R.string.url_base_full), property_device);*/
                
        apply_signature = res.getBoolean(R.bool.apply_signature);
        inject_signature_enable = res
                .getBoolean(R.bool.inject_signature_enable);
        inject_signature_keys = res.getString(R.string.inject_signature_keys);
        secure_mode_enable = res.getBoolean(R.bool.secure_mode_enable);
        secure_mode_default = res.getBoolean(R.bool.secure_mode_default);
        
        String urlBaseJson =  OmniServiceLocator.buildBuildsQueryUrl(context, false);
        if (TextUtils.isEmpty(urlBaseJson)) {
            urlBaseJson = res.getString(R.string.url_base_json);
        }
        url_base_json = urlBaseJson;
        /*url_base_json = res.getString(R.string.url_base_json);*/
        weekly_version_tag = res.getString(R.string.weekly_version_tag);
        gapps_version_tag = res.getString(R.string.gapps_version_tag);
        microg_version_tag = res.getString(R.string.microg_version_tag);
        android_version = getProperty(context,
                res.getString(R.string.android_version), "");
        filename_base_prefix = String.format(Locale.ENGLISH,
                res.getString(R.string.filename_base), android_version);
        boolean keep_screen_on = false;
        try {
            String[] devices = res
                    .getStringArray(R.array.keep_screen_on_devices);
            if (devices != null) {
                for (String device : devices) {
                    if (property_device.equals(device)) {
                        keep_screen_on = true;
                        break;
                    }
                }
            }
        } catch (Resources.NotFoundException e) {
        }
        this.keep_screen_on = keep_screen_on;

        if (isSecondaryDevice()) {
            urlBaseFull =  OmniServiceLocator.buildBuildsRootUrl(context, true);
            if (TextUtils.isEmpty(urlBaseFull)) {
                urlBaseFull = res.getString(R.string.url_base_full);
            }
            urlBaseJson =  OmniServiceLocator.buildBuildsQueryUrl(context, true);
            if (TextUtils.isEmpty(url_base_json)) {
                urlBaseJson = res.getString(R.string.url_base_json);
            }
            url_base_full = urlBaseFull + property_device + "/";
            url_base_json = urlBaseJson;
        }

        Logger.d("property_version: %s", property_version);
        Logger.d("property_device: %s", property_device);
        Logger.d("filename_base: %s", filename_base);
        Logger.d("filename_base_prefix: %s", filename_base_prefix);
        Logger.d("path_base: %s", path_base);
        Logger.d("path_flash_after_update: %s", path_flash_after_update);
        Logger.d("url_base_delta: %s", url_base_delta);
        Logger.d("url_base_update: %s", url_base_update);
        Logger.d("url_base_full: %s", url_base_full);
        Logger.d("url_base_json: %s", url_base_json);
        Logger.d("apply_signature: %d", apply_signature ? 1 : 0);
        Logger.d("inject_signature_enable: %d", inject_signature_enable ? 1 : 0);
        Logger.d("inject_signature_keys: %s", inject_signature_keys);
        Logger.d("secure_mode_enable: %d", secure_mode_enable ? 1 : 0);
        Logger.d("secure_mode_default: %d", secure_mode_default ? 1 : 0);
        Logger.d("keep_screen_on: %d", keep_screen_on ? 1 : 0);
    }

    public String getFilenameBase() {
        return filename_base;
    }

    public String getPathBase() {
        return path_base;
    }

    public String getPathFlashAfterUpdate() {
        return path_flash_after_update;
    }

    public String getUrlBaseDelta() {
        return url_base_delta;
    }

    public String getUrlBaseUpdate() {
        return url_base_update;
    }

    public String getUrlBaseFull() {
        return url_base_full;
    }

    public boolean getApplySignature() {
        return apply_signature;
    }

    public boolean getInjectSignatureEnable() {
        // If we have full secure mode, let signature depend on secure mode
        // setting. If not, let signature depend on config setting only

        if (getSecureModeEnable()) {
            return getSecureModeCurrent();
        } else {
            return inject_signature_enable;
        }
    }

    public String getInjectSignatureKeys() {
        return inject_signature_keys;
    }

    public boolean getSecureModeEnable() {
        return apply_signature && inject_signature_enable && secure_mode_enable;
    }

    public boolean getSecureModeDefault() {
        return secure_mode_default && getSecureModeEnable();
    }

    public boolean getSecureModeCurrent() {
        return getSecureModeEnable()
                && prefs.getBoolean(PREF_SECURE_MODE_NAME,
                        getSecureModeDefault());
    }

    public boolean setSecureModeCurrent(boolean enable) {
        prefs.edit()
                .putBoolean(PREF_SECURE_MODE_NAME,
                        getSecureModeEnable() && enable).commit();
        return getSecureModeCurrent();
    }

    public boolean getABPerfModeCurrent() {
        return prefs.getBoolean(PREF_AB_PERF_MODE_NAME, PREF_AB_PERF_MODE_DEFAULT);
    }

    public void setABPerfModeCurrent(boolean enable) {
        prefs.edit()
                .putBoolean(PREF_AB_PERF_MODE_NAME, enable).commit();
    }

    public List<String> getFlashAfterUpdateZIPs() {
        List<String> extras = new ArrayList<String>();

        File[] files = (new File(getPathFlashAfterUpdate())).listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().toLowerCase(Locale.ENGLISH).endsWith(".zip")) {
                    String filename = f.getAbsolutePath();
                    if (filename.startsWith(getPathBase())) {
                        extras.add(filename);
                    }
                }
            }
            Collections.sort(extras);
        }

        return extras;
    }

    public boolean getShownRecoveryWarningSecure() {
        return prefs.getBoolean(PREF_SHOWN_RECOVERY_WARNING_SECURE_NAME, false);
    }

    public void setShownRecoveryWarningSecure() {
        prefs.edit().putBoolean(PREF_SHOWN_RECOVERY_WARNING_SECURE_NAME, true)
                .commit();
    }

    public boolean getShownRecoveryWarningNotSecure() {
        return prefs.getBoolean(PREF_SHOWN_RECOVERY_WARNING_NOT_SECURE_NAME,
                false);
    }

    public void setShownRecoveryWarningNotSecure() {
        prefs.edit()
                .putBoolean(PREF_SHOWN_RECOVERY_WARNING_NOT_SECURE_NAME, true)
                .commit();
    }

    public boolean getKeepScreenOn() {
        return keep_screen_on;
    }

    public String getDevice() {
        return property_device;
    }

    public String getVersion() {
        return property_version;
    }

    public String getFileBaseNamePrefix() {
        return filename_base_prefix;
    }

    public String getUrlBaseJson() {
        return url_base_json;
    }

    public boolean isOfficialVersion() {
        return getVersion().indexOf(weekly_version_tag) != -1;
    }

    public String getAndroidVersion() {
        return android_version;
    }

    public static boolean isABDevice() {
        return SystemProperties.getBoolean(PROP_AB_DEVICE, false);
    }

    public boolean isGappsDevice() {
        return getVersion().indexOf(gapps_version_tag) != -1;
    }

    public boolean isMicroGDevice() {
        return getVersion().indexOf(microg_version_tag) != -1;
    }

    public String getDeviceRelativePath() {
        return "./" + getDevice();
    }

    public String getBuildTypeTag() {
        if (isOfficialVersion()) {
            return weekly_version_tag;
        }
        if (isGappsDevice()) {
            return gapps_version_tag;
        }
        if (isMicroGDevice()) {
            return microg_version_tag;
        }
        return null;
    }

    private boolean isSecondaryDevice() {
        return isGappsDevice() || secondaryDevice;
    }

    public boolean isDownloadOnlyDevice() {
        return downloadOnlyDevice;
    }
}
