package com.mtj.douyinpage;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.mtj.douyinpage.ui.FollowingActivity;

/**
 * 主 Activity - 应用启动入口
 * 
 * 职责：
 * - 作为应用的启动点
 * - 立即跳转到关注页面（FollowingActivity）
 * - 关闭自身
 */
public class MainActivity extends AppCompatActivity {
    /**
     * Activity 创建时调用
     * 初始化布局并跳转到关注页面
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置布局
        setContentView(R.layout.activity_main);
        
        // 启动关注页面 Activity
        startActivity(new Intent(this, FollowingActivity.class));
        // 关闭当前 Activity
        finish();
    }
}
