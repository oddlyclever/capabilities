# libwebp_jni.so registers its methods against this class by name when it loads, so R8 must keep the class and its
# native methods as they are.
-keep class com.oddlyclever.webp.NativeWebp {
    native <methods>;
}
