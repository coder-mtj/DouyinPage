package com.mtj.douyinpage.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import com.mtj.douyinpage.model.User;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 用户关注列表本地存储工具类
 */
public final class UserStorage {
    private static final String PREF_NAME = "following_pref";
    private static final String KEY_USERS = "users";

    private UserStorage() {
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** 保存用户列表到本地 */
    public static void saveUsers(Context context, List<User> users) {
        JSONArray array = new JSONArray();
        for (User user : users) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", user.getId());
                obj.put("name", user.getName());
                obj.put("avatar", user.getAvatar());
                obj.put("verified", user.isVerified());
                obj.put("followed", user.isFollowed());
                obj.put("douyinId", user.getDouyinId());
                obj.put("special", user.isSpecialFollow());
                obj.put("remark", user.getRemark());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            array.put(obj);
        }
        getPrefs(context)
                .edit()
                .putString(KEY_USERS, array.toString())
                .apply();
    }

    /** 从本地读取用户列表 */
    public static List<User> loadUsers(Context context) {
        String json = getPrefs(context).getString(KEY_USERS, null);
        List<User> users = new ArrayList<>();
        if (TextUtils.isEmpty(json)) {
            return users;
        }
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                users.add(new User(
                        obj.optInt("id"),
                        obj.optString("name"),
                        obj.optInt("avatar"),
                        obj.optBoolean("verified"),
                        obj.optBoolean("followed"),
                        obj.optString("douyinId"),
                        obj.optBoolean("special"),
                        obj.optString("remark")
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return users;
    }

    /** 清空本地缓存（调试用） */
    public static void clear(Context context) {
        getPrefs(context)
                .edit()
                .remove(KEY_USERS)
                .apply();
    }
}
