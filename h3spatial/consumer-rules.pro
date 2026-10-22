# com.uber:h3-android's native library (libh3-java.so) binds to these by name, and the AAR ships no keep
# rules of its own.
# JNI_OnLoad resolves the two classes and their constructors with FindClass/GetMethodID, and every JNI
# export is named after NativeMethods. If R8 renames or strips any of them in a minified consumer build,
# the library fails to load or the first call throws — H3 stops working entirely, not on one function.
-keep class com.uber.h3core.NativeMethods { native <methods>; }
-keep class com.uber.h3core.util.LatLng { <init>(double, double); }
-keep class com.uber.h3core.exceptions.H3Exception { <init>(int); }
