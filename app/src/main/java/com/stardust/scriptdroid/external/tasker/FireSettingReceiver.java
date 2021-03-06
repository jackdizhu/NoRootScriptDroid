package com.stardust.scriptdroid.external.tasker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.stardust.scriptdroid.App;
import com.stardust.scriptdroid.external.CommonUtils;
import com.stardust.scriptdroid.external.ScriptExecutionIntentService;
import com.stardust.scriptdroid.external.open.RunIntentActivity;
import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginSettingReceiver;

/**
 * Created by Stardust on 2017/3/27.
 */

public class FireSettingReceiver extends AbstractPluginSettingReceiver {

    private static final String TAG = "FireSettingReceiver";

    @Override
    protected boolean isBundleValid(@NonNull Bundle bundle) {
        return CommonUtils.isTaskerBundleValid(bundle);
    }

    @Override
    protected boolean isAsync() {
        return true;
    }

    @Override
    protected void firePluginSetting(@NonNull Context context, @NonNull Bundle bundle) {
        context.startService(new Intent(context, ScriptExecutionIntentService.class)
                .putExtras(bundle));
    }

}
