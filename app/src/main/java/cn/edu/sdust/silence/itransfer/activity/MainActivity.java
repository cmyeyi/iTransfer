package cn.edu.sdust.silence.itransfer.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.github.florent37.materialviewpager.MaterialViewPager;
import com.github.florent37.materialviewpager.header.HeaderDesign;

import java.io.File;

import cn.edu.sdust.silence.itransfer.R;
import cn.edu.sdust.silence.itransfer.ui.actionbutton.FloatingActionButton;
import cn.edu.sdust.silence.itransfer.ui.actionbutton.FloatingActionsMenu;
import cn.edu.sdust.silence.itransfer.ui.fragment.RecyclerViewFragment;

/**
 * 主界面
 * create by shifeiqi
 */
public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    private MaterialViewPager mViewPager;
    private DrawerLayout mDrawer;
    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar toolbar;
    private FloatingActionButton sendBtn, receiveBtn;
    private FloatingActionsMenu actionMenu;
    private RecyclerViewFragment recyclerViewHistoryFile = RecyclerViewFragment.newInstance(Environment.getExternalStorageDirectory() + "/iTransfer/files");
    private RecyclerViewFragment recyclerViewManageFile = RecyclerViewFragment.newInstance(Environment.getExternalStorageDirectory().toString());

    //管理wifi状态
    private WifiManager wifiManager;
    private boolean wifiStartState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        testWifiState();
        openWifiState();
        setTitle("");
        mkDir();
        initView();
        initListener();
        initToolbar();
        aboutDrawer();
        initViewPager();
        initLogo();
    }

    private static final int CODE_REQ_PERMISSIONS = 1;

    protected void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void aboutDrawer() {
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawer, 0, 0);
        mDrawer.setDrawerListener(mDrawerToggle);
    }

    private void testWifiState() {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled())
            wifiStartState = true;
    }

    private void openWifiState() {
        if (!wifiStartState) {
            wifiManager.setWifiEnabled(true);
        }
    }

    private void closeWifi() {
        if (!wifiStartState)
            wifiManager.setWifiEnabled(false);
    }

    private void initLogo() {
        View logo = findViewById(R.id.logo_white);
        if (logo != null)
            logo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mViewPager.notifyHeaderChanged();
                }
            });
    }

    private void initToolbar() {
        toolbar = mViewPager.getToolbar();
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            final ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowHomeEnabled(true);
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setDisplayUseLogoEnabled(true);
                actionBar.setHomeButtonEnabled(true);
            }
        }
    }

    private void initViewPager() {
        mViewPager.getViewPager().setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager()) {

            @Override
            public Fragment getItem(int position) {
                switch (position % 2) {
                    case 0:
                        return recyclerViewHistoryFile;

                    case 1:
                        return recyclerViewManageFile;

                    default:
                        return null;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position % 2) {
                    case 0:
                        return "历史记录";
                    case 1:
                        return "文件管理";
                }
                return "";
            }
        });

        mViewPager.setMaterialViewPagerListener(new MaterialViewPager.Listener() {
            @Override
            public HeaderDesign getHeaderDesign(int page) {
                switch (page) {
                    case 0:
                        return HeaderDesign.fromColorAndDrawable(
                                getResources().getColor(R.color.history_file_header),
                                getResources().getDrawable(R.drawable.history_bg));
                    case 1:
                        return HeaderDesign.fromColorAndDrawable(
                                getResources().getColor(R.color.manage_file_header),
                                getResources().getDrawable(R.drawable.file_bg));
                }
                return null;
            }
        });

        mViewPager.getViewPager().setOffscreenPageLimit(mViewPager.getViewPager().getAdapter().getCount());
        mViewPager.getPagerTitleStrip().setViewPager(mViewPager.getViewPager());
    }

    private void initListener() {
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkPermission()) {
                    Intent intent = new Intent(MainActivity.this, ChoseFileActivity.class);
                    startActivityForResult(intent, 1);
                }
            }
        });

        receiveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkPermission()) {
                    Intent intent = new Intent(MainActivity.this, ReceiveActivity.class);
                    startActivityForResult(intent, 1);
                }
            }
        });
        actionMenu.setOnTouchListener(this);
        findViewById(R.id.action_menu_wrap).setOnTouchListener(this);
    }

    private void initView() {
        sendBtn = (FloatingActionButton) findViewById(R.id.action_send);
        sendBtn.setIcon(R.drawable.send_icon);
        receiveBtn = (FloatingActionButton) findViewById(R.id.action_receive);
        receiveBtn.setIcon(R.drawable.receive_icon);
        actionMenu = (FloatingActionsMenu) findViewById(R.id.multiple_actions);
        mViewPager = (MaterialViewPager) findViewById(R.id.materialViewPager);
    }


    /**
     * 创建文件夹
     */
    private void mkDir() {
        File dir = new File(Environment.getExternalStorageDirectory() + "/iTransfer");
        if (!dir.exists())
            dir.mkdir();
        File dir2 = new File(Environment.getExternalStorageDirectory() + "/iTransfer/files");
        if (!dir2.exists())
            dir2.mkdir();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("提示");
        builder.setMessage("p2p点对点传输");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        int position = mViewPager.getViewPager().getCurrentItem();
        boolean key = recyclerViewManageFile.ismUseBackKey();
        if (position == 1) {
            boolean isHomePath = recyclerViewManageFile.isHomeDirectory();
            if (keyCode == KeyEvent.KEYCODE_BACK && key && !isHomePath) {

                recyclerViewManageFile.backPreviousDirectory();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_BACK && key && isHomePath) {
                Toast.makeText(MainActivity.this, "再按一次退出", Toast.LENGTH_SHORT).show();
                recyclerViewManageFile.setmUseBackKey(false);
                return false;
            } else if (keyCode == KeyEvent.KEYCODE_BACK && !key) {
                finish();
                return false;
            }
        } else {
            finish();
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        closeWifi();
        super.onDestroy();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        recyclerViewHistoryFile.reUpdateCurrentDir();
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() != actionMenu.getAddButtonId()) {
            if (actionMenu.isExpanded()) {
                actionMenu.collapse();
            }
        }
        return false;
    }


    public boolean checkPermission() {
        if (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) || !hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,}, CODE_REQ_PERMISSIONS);
            return false;
        }
        return true;
    }

    public boolean hasPermission(String permission) {
        return ActivityCompat.checkSelfPermission(getBaseContext(), permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODE_REQ_PERMISSIONS) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    showToast("缺少权限，请先授予权限");
                    showToast(permissions[i]);
                    return;
                }
            }
            showToast("已获得权限");
        }
    }
}
