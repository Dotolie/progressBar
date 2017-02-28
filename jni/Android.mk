LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := crypto
LOCAL_SRC_FILES := libcrypto.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := cryptodev
LOCAL_SRC_FILES := cryptodev.c 
LOCAL_STATIC_LIBRARIES := crypto
LOCAL_LDLIBS    := -llog
include $(BUILD_SHARED_LIBRARY)
