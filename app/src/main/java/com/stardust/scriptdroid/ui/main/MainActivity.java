package com.stardust.scriptdroid.ui.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.stardust.app.FragmentPagerAdapterBuilder;
import com.stardust.app.NotAskAgainDialog;
import com.stardust.app.OnActivityResultDelegate;
import com.stardust.enhancedfloaty.FloatyService;
import com.stardust.pio.PFile;
import com.stardust.scriptdroid.BuildConfig;
import com.stardust.scriptdroid.Pref;
import com.stardust.scriptdroid.R;
import com.stardust.scriptdroid.autojs.AutoJs;
import com.stardust.scriptdroid.external.floatingwindow.FloatingWindowManger;
import com.stardust.scriptdroid.script.ScriptFile;
import com.stardust.scriptdroid.script.StorageScriptProvider;
import com.stardust.scriptdroid.script.sample.Sample;
import com.stardust.view.accessibility.AccessibilityService;
import com.stardust.scriptdroid.tool.AccessibilityServiceTool;
import com.stardust.scriptdroid.tool.DrawableSaver;
import com.stardust.scriptdroid.ui.BaseActivity;
import com.stardust.scriptdroid.ui.main.sample_list.SampleScriptListFragment;
import com.stardust.scriptdroid.ui.main.script_list.MyScriptListFragment;
import com.stardust.scriptdroid.ui.main.script_list.ScriptFileChooserDialogBuilder;
import com.stardust.scriptdroid.ui.main.task.TaskManagerFragment;
import com.stardust.scriptdroid.ui.settings.SettingsActivity_;
import com.stardust.scriptdroid.ui.update.VersionGuard;
import com.stardust.theme.dialog.ThemeColorMaterialDialogBuilder;
import com.stardust.util.BackPressedHandler;
import com.stardust.util.Callback;
import com.stardust.util.MessageEvent;
import com.stardust.view.DrawerAutoClose;
import com.stardust.view.accessibility.AccessibilityServiceUtils;
import com.stardust.widget.CommonMarkdownView;
import com.stardust.widget.SlidingUpPanel;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;

@EActivity(R.layout.activity_main)
public class MainActivity extends BaseActivity implements OnActivityResultDelegate.DelegateHost {

    public static final String MESSAGE_CLEAR_BACKGROUND_SETTINGS = "MESSAGE_CLEAR_BACKGROUND_SETTINGS";

    private static final String LOG_TAG = "MainActivity";
    private static final String EXTRA_ACTION = "EXTRA_ACTION";

    private static final String ACTION_ON_ACTION_RECORD_STOPPED = "ACTION_ON_ACTION_RECORD_STOPPED";
    private static final String ACTION_IMPORT_SCRIPT = "ACTION_IMPORT_SCRIPT";
    private static final String ARGUMENT_SCRIPT = "ARGUMENT_SCRIPT";
    private static final String ARGUMENT_PATH = "ARGUMENT_PATH";
    private static final String ACTION_IMPORT_SAMPLE = "I cannot find the way back to you...Eating...17.4.29";
    private static final String ARGUMENT_SAMPLE = "Take a chance on me...ok...?";


    @ViewById(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @ViewById(R.id.bottom_menu)
    SlidingUpPanel mAddBottomMenuPanel;
    @ViewById(R.id.viewpager)
    ViewPager mViewPager;
    private FragmentPagerAdapterBuilder.StoredFragmentPagerAdapter mPagerAdapter;

    private OnActivityResultDelegate.Mediator mActivityResultMediator = new OnActivityResultDelegate.Mediator();
    private DrawableSaver mDrawerHeaderBackgroundSaver, mAppbarBackgroundSaver;
    private VersionGuard mVersionGuard;
    private Intent mIntentToHandle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();
        registerBackPressHandlers();
        mIntentToHandle = getIntent();
        EventBus.getDefault().register(this);
        mVersionGuard = new VersionGuard(this);
        showAnnunciationIfNeeded();
    }

    @AfterViews
    void setUpViews() {
        setUpToolbar();
        setUpTabViewPager();
        setUpDrawerHeader();
        setUpFragment();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

    }

    private void showAnnunciationIfNeeded() {
        if (!Pref.shouldShowAnnunciation()) {
            return;
        }
        new CommonMarkdownView.DialogBuilder(this)
                .padding(36, 0, 36, 0)
                .markdown(PFile.read(getResources().openRawResource(R.raw.annunciation)))
                .title(R.string.text_annunciation)
                .positiveText(R.string.ok)
                .canceledOnTouchOutside(false)
                .show();
    }


    private void registerBackPressHandlers() {
        registerBackPressedHandler(new BackPressedHandler() {
            @Override
            public boolean onBackPressed(Activity activity) {
                if (mAddBottomMenuPanel.isShowing()) {
                    mAddBottomMenuPanel.dismiss();
                    return true;
                }
                return false;
            }
        });
        registerBackPressedHandler(new DrawerAutoClose(mDrawerLayout, Gravity.START));
        registerBackPressedHandler(new BackPressedHandler.DoublePressExit(this, R.string.text_press_again_to_exit));
    }

    private void checkPermissions() {
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        showAccessibilitySettingPromptIfDisabled();
    }

    private void showAccessibilitySettingPromptIfDisabled() {
        if (!AccessibilityServiceUtils.isAccessibilityServiceEnabled(this, AccessibilityService.class)) {
            new NotAskAgainDialog.Builder(this, "Eating...love you...miss you...17.4.12")
                    .title(R.string.text_need_to_enable_accessibility_service)
                    .content(R.string.explain_accessibility_permission)
                    .positiveText(R.string.text_go_to_setting)
                    .negativeText(R.string.text_cancel)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            AccessibilityServiceTool.enableAccessibilityService();
                        }
                    }).show();
        }
    }

    private void setUpFragment() {
        SideMenuFragment.setFragment(this, R.id.fragment_slide_menu);
    }

    @SuppressLint("SetTextI18n")
    private void setUpDrawerHeader() {
        TextView version = $(R.id.version);
        version.setText("Version " + BuildConfig.VERSION_NAME);
        mDrawerHeaderBackgroundSaver = new DrawableSaver.ImageSaver(this, "drawer_header_background", (ImageView) $(R.id.drawer_header_img));
    }

    private void setUpToolbar() {
        Toolbar toolbar = $(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string._app_name);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.text_drawer_open,
                R.string.text_drawer_close);
        drawerToggle.syncState();
        mDrawerLayout.addDrawerListener(drawerToggle);
        mAppbarBackgroundSaver = new DrawableSaver.ImageSaver(this, "appbar_background", (ImageView) $(R.id.app_bar_bg));
    }

    private void setUpTabViewPager() {
        TabLayout tabLayout = $(R.id.tab);
        mPagerAdapter = new FragmentPagerAdapterBuilder(this)
                .add(new MyScriptListFragment(), R.string.text_my_script)
                .add(new SampleScriptListFragment(), R.string.text_sample_script)
                .add(new TaskManagerFragment(), R.string.text_task_manage)
                .build();
        mViewPager.setAdapter(mPagerAdapter);
        tabLayout.setupWithViewPager(mViewPager);
        mPagerAdapter.setOnFragmentInstantiateListener(new FragmentPagerAdapterBuilder.OnFragmentInstantiateListener() {
            @Override
            public void OnInstantiate(Fragment fragment) {
                if (fragment instanceof MyScriptListFragment && mIntentToHandle != null) {
                    handleIntent(mIntentToHandle);
                    mIntentToHandle = null;
                }
            }
        });
    }

    @Click(R.id.add)
    void showAddFilePanel() {
        mAddBottomMenuPanel.show();
    }

    @Click(R.id.create_new_file)
    void createScriptFile() {
        doWithMyScriptListFragment(new Callback<MyScriptListFragment>() {
            @Override
            public void call(MyScriptListFragment myScriptListFragment) {
                myScriptListFragment.newScriptFile();
            }
        });
    }

    @Click(R.id.create_new_directory)
    void createNewDirectory() {
        doWithMyScriptListFragment(new Callback<MyScriptListFragment>() {
            @Override
            public void call(MyScriptListFragment myScriptListFragment) {
                myScriptListFragment.newDirectory();
            }
        });
    }

    @Click(R.id.import_from_file)
    void showFileChooser() {
        final StorageScriptProvider provider = StorageScriptProvider.getExternalStorageProvider();
        new ScriptFileChooserDialogBuilder(this)
                .scriptProvider(provider)
                .fileCallback(new ScriptFileChooserDialogBuilder.FileCallback() {
                    @Override
                    public void onFileSelection(MaterialDialog dialog, final ScriptFile file) {
                        dialog.dismiss();
                        provider.clearCacheExceptInitialDirectory();
                        doWithMyScriptListFragment(new Callback<MyScriptListFragment>() {
                            @Override
                            public void call(MyScriptListFragment myScriptListFragment) {
                                myScriptListFragment.importFile(file.getPath());
                            }
                        });
                    }
                })
                .title(R.string.text_please_choose_file_to_import)
                .autoDismiss(false)
                .positiveText(R.string.cancel)
                .neutralText(R.string.text_refresh)
                .onAny(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if (which == DialogAction.POSITIVE) {
                            dialog.dismiss();
                        } else {
                            provider.refreshAll();
                        }
                    }
                })
                .show();
    }

    @Click(R.id.setting)
    void startSettingActivity() {
        startActivity(new Intent(this, SettingsActivity_.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @Click(R.id.exit)
    public void exitCompletely() {
        FloatingWindowManger.hideHoverMenu();
        stopService(new Intent(this, FloatyService.class));
        AutoJs.getInstance().getScriptEngineService().stopAll();
        finish();
    }

    @Click(R.id.drawer_header_img)
    public void selectHeaderImage() {
        mDrawerHeaderBackgroundSaver.select(this, mActivityResultMediator);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVersionGuard.checkDeprecateAndUpdate();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getStringExtra(EXTRA_ACTION);
        if (action == null)
            return;
        switch (action) {
            case ACTION_ON_ACTION_RECORD_STOPPED:
                handleRecordedScript(intent.getStringExtra(ARGUMENT_SCRIPT));
                break;
            case ACTION_IMPORT_SCRIPT:
                handleImportScriptFile(intent);
                break;
            case ACTION_IMPORT_SAMPLE:
                handleImportSample(intent);
                break;
        }
    }

    private void handleImportScriptFile(Intent intent) {
        mViewPager.setCurrentItem(0, true);
        MyScriptListFragment fragment = getMyScriptListFragment();
        if (fragment == null) {
            mIntentToHandle = intent;
        } else {
            fragment.importFile(intent.getStringExtra(ARGUMENT_PATH));
        }
    }

    private void handleImportSample(Intent intent) {
        mViewPager.setCurrentItem(0, true);
        MyScriptListFragment fragment = getMyScriptListFragment();
        if (fragment == null) {
            mIntentToHandle = intent;
        } else {
            final Sample sample = (Sample) intent.getSerializableExtra(ARGUMENT_SAMPLE);
            doWithMyScriptListFragment(new Callback<MyScriptListFragment>() {
                @Override
                public void call(MyScriptListFragment myScriptListFragment) {
                    try {
                        myScriptListFragment.importFile(sample.name, getAssets().open(sample.path));
                    } catch (IOException e) {
                        e.printStackTrace();
                        Snackbar.make(mDrawerLayout, R.string.text_import_fail, Snackbar.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void handleRecordedScript(final String script) {
        new ThemeColorMaterialDialogBuilder(this)
                .title(R.string.text_recorded)
                .items(getString(R.string.text_new_file), getString(R.string.text_copy_to_clip))
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                        if (position == 0) {
                            doWithMyScriptListFragment(new Callback<MyScriptListFragment>() {
                                @Override
                                public void call(MyScriptListFragment myScriptListFragment) {
                                    myScriptListFragment.newScriptFileForScript(script);
                                }
                            });
                        } else {
                            ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
                                    .setPrimaryClip(ClipData.newPlainText("script", script));
                            Toast.makeText(MainActivity.this, R.string.text_already_copy_to_clip, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .negativeText(R.string.text_cancel)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .canceledOnTouchOutside(false)
                .show();
    }


    public static void importScriptFile(Context context, String path) {
        context.startActivity(new Intent(context, MainActivity_.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_ACTION, ACTION_IMPORT_SCRIPT)
                .putExtra(ARGUMENT_PATH, path));
    }

    public static void importSample(Context context, Sample sample) {
        context.startActivity(new Intent(context, MainActivity_.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_ACTION, ACTION_IMPORT_SAMPLE)
                .putExtra(ARGUMENT_SAMPLE, sample));
    }

    public void doWithMyScriptListFragment(final Callback<MyScriptListFragment> callback) {
        MyScriptListFragment fragment = ((MyScriptListFragment) mPagerAdapter.getStoredFragment(0));
        if (fragment != null) {
            callback.call(fragment);
            return;
        }
        mViewPager.setCurrentItem(0);
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    mViewPager.removeOnPageChangeListener(this);
                    doWithMyScriptListFragment(callback);
                }
            }
        });
    }

    @Nullable
    public MyScriptListFragment getMyScriptListFragment() {
        MyScriptListFragment fragment = ((MyScriptListFragment) mPagerAdapter.getStoredFragment(0));
        if (fragment == null) {
            mViewPager.setCurrentItem(0);
        }
        return ((MyScriptListFragment) mPagerAdapter.getStoredFragment(0));
    }

    @Click(R.id.toolbar)
    public void OnToolbarClick() {
        mAppbarBackgroundSaver.select(this, mActivityResultMediator);
    }

    @Subscribe
    public void onMessageEvent(MessageEvent event) {
        if (event.message.equals(MESSAGE_CLEAR_BACKGROUND_SETTINGS)) {
            mAppbarBackgroundSaver.reset();
            mDrawerHeaderBackgroundSaver.reset();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mActivityResultMediator.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        StorageScriptProvider.getDefault().notifyStoragePermissionGranted();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }


    public static void onRecordStop(Context context, String script) {
        Intent intent = new Intent(context, MainActivity_.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_ACTION, ACTION_ON_ACTION_RECORD_STOPPED)
                .putExtra(ARGUMENT_SCRIPT, script);
        context.startActivity(intent);
    }

    @NonNull
    @Override
    public OnActivityResultDelegate.Mediator getOnActivityResultDelegateMediator() {
        return mActivityResultMediator;
    }
}