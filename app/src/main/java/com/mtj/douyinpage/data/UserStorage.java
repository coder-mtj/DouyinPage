package com.mtj.douyinpage.data; // package 关键字：声明当前类所在的包名，用于组织和区分不同模块

import android.content.Context; // 导入 Context 类，用于获取应用环境和系统服务
import android.content.SharedPreferences; // 导入 SharedPreferences，用于轻量级键值对本地存储
import android.text.TextUtils; // 导入 TextUtils，提供字符串判空等工具方法
import com.mtj.douyinpage.model.User; // 导入 User 模型类，表示关注列表中的单个用户
import java.util.ArrayList; // 导入 ArrayList，实现 List 接口的一种可变数组集合
import java.util.List; // 导入 List 接口，表示有序集合的抽象类型
import org.json.JSONArray; // 导入 JSONArray，用来表示 JSON 数组
import org.json.JSONException; // 导入 JSONException，用于捕获 JSON 解析/构建异常
import org.json.JSONObject; // 导入 JSONObject，用来表示单个 JSON 对象

/**
 * 持久化存储用户关注列表数据（SharedPreferences + JSON）
 * 使用静态方法对外提供保存、读取、清空接口
 */
public final class UserStorage { // final 修饰类：表示该类不能被继承，只作为工具类使用
    private static final String PREF_NAME = "following_pref"; // SharedPreferences 文件名常量
    private static final String KEY_USERS = "users"; // 存储用户列表 JSON 字符串的键名

    private UserStorage() { // 私有构造函数，阻止外部通过 new 创建实例（典型工具类写法）
    }

    // 获取当前应用作用域下名为 PREF_NAME 的 SharedPreferences 实例
    private static SharedPreferences getPrefs(Context context) { // static：工具方法，无需创建对象即可调用
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); // MODE_PRIVATE：文件仅限当前应用访问
    }

    /** 保存用户列表到本地 */
    public static void saveUsers(Context context, List<User> users) { // 对外公开的静态方法，接收上下文和用户列表
        JSONArray array = new JSONArray(); // 创建一个空的 JSON 数组，用于承载所有用户数据
        for (User user : users) { // 增强 for 循环，依次遍历列表中的每一个 User 对象
            JSONObject obj = new JSONObject(); // 为当前用户创建一个 JSON 对象，用于保存字段
            try { // try-catch：JSON put 可能抛出 JSONException，需要捕获
                obj.put("id", user.getId()); // 写入用户 id 字段
                obj.put("name", user.getName()); // 写入用户名
                obj.put("avatar", user.getAvatar()); // 写入头像资源 id
                obj.put("verified", user.isVerified()); // 写入是否认证
                obj.put("followed", user.isFollowed()); // 写入是否已关注
                obj.put("douyinId", user.getDouyinId()); // 写入抖音号
                obj.put("special", user.isSpecialFollow()); // 写入是否特别关注
                obj.put("remark", user.getRemark()); // 写入备注信息
            } catch (JSONException e) { // 捕获 JSON 相关异常，避免程序崩溃
                e.printStackTrace(); // 打印异常堆栈，方便调试
            }
            array.put(obj); // 将当前用户对应的 JSON 对象添加到 JSON 数组中
        }
        getPrefs(context) // 通过 getPrefs() 获取 SharedPreferences 实例
                .edit() // 进入编辑模式，准备写入数据
                .putString(KEY_USERS, array.toString()) // 把整个用户列表序列化为字符串存入 KEY_USERS
                .apply(); // 异步提交修改（不阻塞主线程）
    }

    /** 从本地读取用户列表 */
    public static List<User> loadUsers(Context context) { // 对外公开的静态方法，用于从本地还原用户列表
        String json = getPrefs(context).getString(KEY_USERS, null); // 从 SharedPreferences 取出 JSON 字符串，默认值为 null
        List<User> users = new ArrayList<>(); // 创建一个空的 ArrayList，用于存放解析结果
        if (TextUtils.isEmpty(json)) { // 如果本地还没有保存任何数据（字符串为空）
            return users; // 直接返回空列表，由调用方决定是否初始化默认数据
        }
        try { // try-catch 包裹 JSON 解析过程，防止格式错误导致崩溃
            JSONArray array = new JSONArray(json); // 使用本地 JSON 字符串构造 JSON 数组
            for (int i = 0; i < array.length(); i++) { // 使用下标 for 循环遍历 JSON 数组
                JSONObject obj = array.getJSONObject(i); // 根据下标取出当前用户对应的 JSON 对象
                users.add(new User( // 使用 JSON 中的字段构造一个新的 User 实例，并加入列表
                        obj.optInt("id"), // optInt：安全获取整型字段，缺失时返回 0
                        obj.optString("name"), // optString：安全获取字符串字段，缺失时返回空串
                        obj.optInt("avatar"), // 头像资源 id
                        obj.optBoolean("verified"), // 是否认证
                        obj.optBoolean("followed"), // 是否已关注
                        obj.optString("douyinId"), // 抖音号
                        obj.optBoolean("special"), // 是否特别关注
                        obj.optString("remark") // 备注
                ));
            }
        } catch (JSONException e) { // 捕获 JSON 解析过程中的异常
            e.printStackTrace(); // 打印异常堆栈信息，方便定位问题
        }
        return users; // 返回最终解析出的用户列表
    }

    /** 清空本地缓存（调试用） */
    public static void clear(Context context) { // 对外公开的静态方法，用于删除本地缓存数据
        getPrefs(context) // 获取 SharedPreferences 实例
                .edit() // 进入编辑模式
                .remove(KEY_USERS) // 删除 KEY_USERS 对应的键值
                .apply(); // 异步提交更改
    }
}
