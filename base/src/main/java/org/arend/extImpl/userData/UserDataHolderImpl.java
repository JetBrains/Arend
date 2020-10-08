package org.arend.extImpl.userData;

import org.arend.ext.userData.Key;
import org.arend.ext.userData.UserDataHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UserDataHolderImpl implements UserDataHolder {
  private Map<Key<?>, Object> myUserDataMap;

  private UserDataHolderImpl(Map<Key<?>, Object> map) {
    myUserDataMap = map;
  }

  public UserDataHolderImpl() {
    myUserDataMap = null;
  }

  protected void setUserData(UserDataHolderImpl holder) {
    myUserDataMap = holder.myUserDataMap;
  }

  public UserDataHolderImpl copyUserData() {
    if (myUserDataMap == null || myUserDataMap.isEmpty()) return new UserDataHolderImpl();
    Map<Key<?>, Object> map = new HashMap<>();
    for (Map.Entry<Key<?>, Object> entry : myUserDataMap.entrySet()) {
      //noinspection unchecked
      Key<Object> key = (Key<Object>) entry.getKey();
      map.put(key, key.copy(entry.getValue()));
    }
    return new UserDataHolderImpl(map);
  }

  @Override
  public <T> @Nullable T getUserData(@NotNull Key<T> key) {
    //noinspection unchecked
    return myUserDataMap == null ? null : (T) myUserDataMap.get(key);
  }

  @Override
  public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
    if (value != null) {
      if (myUserDataMap == null) {
        myUserDataMap = new HashMap<>();
      }
      myUserDataMap.put(key, value);
    } else {
      if (myUserDataMap != null) {
        myUserDataMap.remove(key);
      }
    }
  }

  public Map<Key<?>, Object> getUserDataMap() {
    return myUserDataMap != null ? myUserDataMap : Collections.emptyMap();
  }
}
